package rwg.world;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Bounded completed-plan cache plus an unbounded-only-while-running map that deduplicates concurrent computation. */
public final class ColumnPlanCache {

    // A default 32-chunk client view covers 33x33 = 1089 columns. Keep one complete view resident so speculative
    // classification cannot evict a column before its vertically adjacent cube requests consume the same plan.
    private static final int DEFAULT_MAXIMUM_SIZE = 2048;

    private final ColumnPlanner planner;
    private final Cache<Long, ColumnPlan> completed;
    private final ConcurrentHashMap<Long, FutureTask<ColumnPlan>> inProgress = new ConcurrentHashMap<Long, FutureTask<ColumnPlan>>();
    private final AtomicLong computationCount = new AtomicLong();

    public ColumnPlanCache(ColumnPlanner planner) {
        this(planner, DEFAULT_MAXIMUM_SIZE);
    }

    ColumnPlanCache(ColumnPlanner planner, int maximumSize) {
        if (planner == null) throw new NullPointerException("column planner");
        if (maximumSize < 1) throw new IllegalArgumentException("maximum size must be positive");
        this.planner = planner;
        completed = CacheBuilder.newBuilder().maximumSize(maximumSize).concurrencyLevel(4).build();
    }

    public ColumnPlan get(int chunkX, int chunkZ) {
        long key = key(chunkX, chunkZ);
        ColumnPlan cached = completed.getIfPresent(key);
        if (cached != null) return cached;

        FutureTask<ColumnPlan> created = new FutureTask<ColumnPlan>(() -> {
            computationCount.incrementAndGet();
            return planner.plan(chunkX, chunkZ);
        });
        FutureTask<ColumnPlan> task = inProgress.putIfAbsent(key, created);
        if (task == null) {
            task = created;
            task.run();
        }
        try {
            ColumnPlan result = task.get();
            completed.put(key, result);
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while planning column " + chunkX + "," + chunkZ, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("Could not plan column " + chunkX + "," + chunkZ, cause);
        } finally {
            if (task.isDone()) inProgress.remove(key, task);
        }
    }

    /**
     * Runs on the caller; CubicChunks invokes this through its background preload-failure callback. Do not add a second
     * executor here: that would oversubscribe CC's workers and make provideCube wait through two queues.
     */
    public void precalculate(int chunkX, int chunkZ) {
        get(chunkX, chunkZ);
    }

    long computationCount() {
        return computationCount.get();
    }

    private static long key(int chunkX, int chunkZ) {
        return (long) chunkX & 0xffffffffL | (long) chunkZ << 32;
    }
}
