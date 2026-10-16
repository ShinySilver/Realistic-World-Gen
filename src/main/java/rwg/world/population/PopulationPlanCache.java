package rwg.world.population;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import rwg.world.ColumnPlan;
import rwg.world.ColumnPlanCache;

/** Bounded completed plans with in-flight deduplication, separate from terrain-plan lifetime. */
public final class PopulationPlanCache {

    private static final int MAXIMUM_SIZE = 1024;

    private final PopulationPlanner planner;
    private final ColumnPlanCache columns;
    private final Cache<Long, PopulationPlan> completed = CacheBuilder.newBuilder().maximumSize(MAXIMUM_SIZE)
            .concurrencyLevel(4).build();
    private final ConcurrentHashMap<Long, FutureTask<PopulationPlan>> inProgress = new ConcurrentHashMap<Long, FutureTask<PopulationPlan>>();

    public PopulationPlanCache(PopulationPlanner planner, ColumnPlanCache columns) {
        if (planner == null || columns == null) throw new NullPointerException("population planner inputs");
        this.planner = planner;
        this.columns = columns;
    }

    public PopulationPlan get(int columnX, int columnZ) {
        long key = key(columnX, columnZ);
        PopulationPlan cached = completed.getIfPresent(key);
        if (cached != null) return cached;
        FutureTask<PopulationPlan> created = new FutureTask<PopulationPlan>(() -> {
            ColumnPlan column = columns.get(columnX, columnZ);
            return planner.plan(columnX, columnZ, column).resolveSurfaceHeights(columns);
        });
        FutureTask<PopulationPlan> task = inProgress.putIfAbsent(key, created);
        if (task == null) {
            task = created;
            task.run();
        }
        try {
            PopulationPlan result = task.get();
            completed.put(key, result);
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while planning population " + columnX + "," + columnZ,
                    exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("Could not plan population " + columnX + "," + columnZ, cause);
        } finally {
            if (task.isDone()) inProgress.remove(key, task);
        }
    }

    public void precalculate(int columnX, int columnZ) {
        get(columnX, columnZ);
    }

    private static long key(int columnX, int columnZ) {
        return (long) columnX & 0xffffffffL | (long) columnZ << 32;
    }
}
