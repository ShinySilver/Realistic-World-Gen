package rwg.world.profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.cardinalstar.cubicchunks.server.CubeProviderServer;
import com.cardinalstar.cubicchunks.server.CubeProviderServer.EagerCubeLoadRequest;
import com.cardinalstar.cubicchunks.world.api.ICubeProviderServer.Requirement;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Real-world CubicChunks benchmark with allocation-light wall-clock sampling. */
public final class WorldgenProfiler {

    private static final long SAMPLE_INTERVAL_MILLIS = 2L;

    private WorldgenProfiler() {}

    public static void profilePacedAndStop(MinecraftServer server) {
        WorldServer world = server.worldServerForDimension(0);
        if (world == null) throw new IllegalStateException("Profiler overworld is unavailable");
        long expectedSeed = Long.getLong("rwg.profileSeed", 927692613931855800L);
        if (world.getSeed() != expectedSeed)
            throw new IllegalStateException("Profiler world seed is " + world.getSeed() + ", expected " + expectedSeed);

        int radius = Integer.getInteger("rwg.profileRadius", 16);
        int verticalRadius = Integer.getInteger("rwg.profileVerticalRadius", 16);
        int centerCubeY = Math.floorDiv(world.getSpawnPoint().posY, 16);
        int minimumCubeY = Integer.getInteger("rwg.profileMinCubeY", centerCubeY - verticalRadius);
        int maximumCubeY = Integer.getInteger("rwg.profileMaxCubeY", centerCubeY + verticalRadius);
        PacedProfile profile = new PacedProfile(server, world, radius, minimumCubeY, maximumCubeY);
        profile.start();
    }

    public static final class PacedProfile {

        private final MinecraftServer server;
        private final WorldServer world;
        private final int radius;
        private final int minimumCubeY;
        private final int maximumCubeY;
        private final List<ColumnPosition> columns;
        private final List<EagerCubeLoadRequest> requests = new ArrayList<EagerCubeLoadRequest>();
        private final List<Long> completionNanos = new ArrayList<Long>();
        private final StackSampler sampler = new StackSampler();
        private long started;

        PacedProfile(MinecraftServer server, WorldServer world, int radius, int minimumCubeY, int maximumCubeY) {
            if (radius < 1 || radius > 128) throw new IllegalArgumentException("profile radius must be 1..128");
            if (minimumCubeY > maximumCubeY)
                throw new IllegalArgumentException("minimum cube Y exceeds maximum cube Y");
            this.server = server;
            this.world = world;
            this.radius = radius;
            this.minimumCubeY = minimumCubeY;
            this.maximumCubeY = maximumCubeY;
            ChunkCoordinates spawn = world.getSpawnPoint();
            this.columns = spiral(Math.floorDiv(spawn.posX, 16), Math.floorDiv(spawn.posZ, 16), radius);
        }

        void start() {
            CubeProviderServer provider = (CubeProviderServer) world.getChunkProvider();
            started = System.nanoTime();
            sampler.start();
            for (ColumnPosition column : columns) {
                for (int cubeY = minimumCubeY; cubeY <= maximumCubeY; cubeY++) {
                    requests.add(provider.loadCubeEagerly(column.x, cubeY, column.z, Requirement.LIGHT));
                }
            }
            FMLCommonHandler.instance().bus().register(this);
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            long now = System.nanoTime();
            for (int index = requests.size() - 1; index >= 0; index--) {
                if (requests.get(index).isCompleted()) {
                    completionNanos.add(now - started);
                    requests.remove(index);
                }
            }
            if (!requests.isEmpty()) return;

            FMLCommonHandler.instance().bus().unregister(this);
            sampler.stop();
            long elapsed = System.nanoTime() - started;
            Collections.sort(completionNanos);
            Result result = write(
                    world,
                    radius,
                    minimumCubeY,
                    maximumCubeY,
                    columns.size(),
                    completionNanos,
                    elapsed,
                    sampler);
            System.out.println("RWG paced worldgen profile written to " + result.html.getAbsolutePath());
            server.initiateShutdown();
        }
    }

    private static Result write(WorldServer world, int radius, int minimumCubeY, int maximumCubeY, int columns,
            List<Long> cubeNanos, long elapsed, StackSampler sampler) {
        File directory = new File(System.getProperty("rwg.profileDir", "build/profiles"));
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IllegalStateException("Could not create profile directory " + directory.getAbsolutePath());
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        String stem = "rwg-worldgen-" + timestamp;
        File folded = new File(directory, stem + ".folded");
        File json = new File(directory, stem + ".json");
        File html = new File(directory, stem + ".html");
        Collections.sort(cubeNanos);
        double seconds = elapsed / 1.0E9D;
        try {
            writeFolded(folded, sampler.samples);
            writeJson(json, world, radius, minimumCubeY, maximumCubeY, columns, cubeNanos, seconds, sampler.count);
            writeHtml(
                    html,
                    json,
                    folded,
                    world,
                    radius,
                    minimumCubeY,
                    maximumCubeY,
                    columns,
                    cubeNanos,
                    seconds,
                    sampler);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write RWG worldgen profile", exception);
        }
        return new Result(html, json, folded);
    }

    private static void writeFolded(File output, Map<String, Integer> samples) throws IOException {
        List<String> stacks = new ArrayList<String>(samples.keySet());
        Collections.sort(stacks);
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        try {
            for (String stack : stacks) {
                writer.write(stack);
                writer.write(' ');
                writer.write(Integer.toString(samples.get(stack)));
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    private static void writeJson(File output, WorldServer world, int radius, int minimumCubeY, int maximumCubeY,
            int columns, List<Long> timings, double seconds, int samples) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        try {
            writer.write(
                    String.format(
                            Locale.ROOT,
                            "{\n  \"seed\": %d,\n  \"radius\": %d,\n  \"columns\": %d,\n  \"minCubeY\": %d,\n  \"maxCubeY\": %d,\n  \"cubes\": %d,\n  \"seconds\": %.6f,\n  \"cubesPerSecond\": %.3f,\n  \"completionMs\": {\"p50\": %.3f, \"p95\": %.3f, \"p99\": %.3f, \"max\": %.3f},\n  \"stackSamples\": %d\n}\n",
                            world.getSeed(),
                            radius,
                            columns,
                            minimumCubeY,
                            maximumCubeY,
                            timings.size(),
                            seconds,
                            timings.size() / seconds,
                            percentile(timings, .50),
                            percentile(timings, .95),
                            percentile(timings, .99),
                            nanosToMillis(timings.get(timings.size() - 1)),
                            samples));
        } finally {
            writer.close();
        }
    }

    private static void writeHtml(File output, File json, File folded, WorldServer world, int radius, int minimumCubeY,
            int maximumCubeY, int columns, List<Long> timings, double seconds, StackSampler sampler)
            throws IOException {
        List<Map.Entry<String, Integer>> top = new ArrayList<Map.Entry<String, Integer>>(sampler.samples.entrySet());
        Collections.sort(top, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        try {
            writer.write("<!doctype html><meta charset=\"utf-8\"><title>RWG worldgen profile</title>");
            writer.write(
                    "<style>body{font:14px system-ui;margin:2rem;max-width:1200px}table{border-collapse:collapse;width:100%}td,th{padding:.35rem;border-bottom:1px solid #ddd;text-align:left}code{font-size:12px} .n{font-variant-numeric:tabular-nums}</style>");
            writer.write("<h1>RWG CubicChunks worldgen profile</h1><table>");
            row(writer, "Seed", Long.toString(world.getSeed()));
            row(writer, "Area", (radius * 2 + 1) + "×" + (radius * 2 + 1) + " columns (" + columns + ")");
            row(
                    writer,
                    "Vertical range",
                    "cube " + minimumCubeY
                            + "…"
                            + maximumCubeY
                            + " / block "
                            + (minimumCubeY * 16)
                            + "…"
                            + (maximumCubeY * 16 + 15));
            row(writer, "Generated cubes", Integer.toString(timings.size()));
            row(writer, "Wall time", format(seconds) + " s");
            row(writer, "Throughput", format(timings.size() / seconds) + " cubes/s");
            row(
                    writer,
                    "Completion time p50 / p95 / p99 / max",
                    format(percentile(timings, .50)) + " / "
                            + format(percentile(timings, .95))
                            + " / "
                            + format(percentile(timings, .99))
                            + " / "
                            + format(nanosToMillis(timings.get(timings.size() - 1)))
                            + " ms");
            row(writer, "Samples", Integer.toString(sampler.count));
            row(writer, "Raw data", escape(json.getName()) + " · " + escape(folded.getName()));
            writer.write("</table><h2>Top sampled stacks</h2><table><tr><th>Samples</th><th>Stack</th></tr>");
            for (int index = 0; index < Math.min(100, top.size()); index++) {
                Map.Entry<String, Integer> entry = top.get(index);
                writer.write(
                        "<tr><td class=\"n\">" + entry.getValue()
                                + "</td><td><code>"
                                + escape(entry.getKey())
                                + "</code></td></tr>");
            }
            writer.write("</table>");
        } finally {
            writer.close();
        }
    }

    private static void row(BufferedWriter writer, String name, String value) throws IOException {
        writer.write("<tr><th>" + escape(name) + "</th><td class=\"n\">" + escape(value) + "</td></tr>");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static double percentile(List<Long> sorted, double percentile) {
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * percentile) - 1);
        return nanosToMillis(sorted.get(Math.max(0, index)));
    }

    private static double nanosToMillis(long nanoseconds) {
        return nanoseconds / 1.0E6D;
    }

    private static List<ColumnPosition> spiral(int centerX, int centerZ, int radius) {
        int diameter = radius * 2 + 1;
        List<ColumnPosition> result = new ArrayList<ColumnPosition>(diameter * diameter);
        for (int ring = 0; ring <= radius; ring++) {
            for (int x = -ring; x <= ring; x++) {
                addIfInside(result, centerX, centerZ, x, -ring, radius);
                if (ring != 0) addIfInside(result, centerX, centerZ, x, ring, radius);
            }
            for (int z = -ring + 1; z < ring; z++) {
                addIfInside(result, centerX, centerZ, -ring, z, radius);
                if (ring != 0) addIfInside(result, centerX, centerZ, ring, z, radius);
            }
        }
        return result;
    }

    private static void addIfInside(List<ColumnPosition> result, int centerX, int centerZ, int dx, int dz, int radius) {
        if (dx >= -radius && dx <= radius && dz >= -radius && dz <= radius)
            result.add(new ColumnPosition(centerX + dx, centerZ + dz));
    }

    static final class Result {

        final File html;
        final File json;
        final File folded;

        Result(File html, File json, File folded) {
            this.html = html;
            this.json = json;
            this.folded = folded;
        }
    }

    private static final class ColumnPosition {

        final int x;
        final int z;

        ColumnPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class StackSampler implements Runnable {

        final Map<String, Integer> samples = new HashMap<String, Integer>();
        final AtomicBoolean running = new AtomicBoolean();
        Thread sampler;
        int count;

        void start() {
            running.set(true);
            sampler = new Thread(this, "RWG worldgen stack sampler");
            sampler.setDaemon(true);
            sampler.start();
        }

        void stop() {
            running.set(false);
            try {
                sampler.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run() {
            while (running.get()) {
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey() != sampler && isWorldgen(entry.getValue()))
                        record(entry.getKey(), entry.getValue());
                }
                try {
                    Thread.sleep(SAMPLE_INTERVAL_MILLIS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private static boolean isWorldgen(StackTraceElement[] trace) {
            for (StackTraceElement frame : trace) if (frame.getClassName().startsWith("rwg.")) return true;
            return false;
        }

        private void record(Thread thread, StackTraceElement[] trace) {
            if (trace.length == 0) return;
            StringBuilder folded = new StringBuilder(trace.length * 48);
            folded.append("thread-").append(thread.getName().replace(';', '_').replace(' ', '_'));
            for (int index = trace.length - 1; index >= 0; index--) {
                folded.append(';');
                StackTraceElement frame = trace[index];
                folded.append(frame.getClassName()).append('.').append(frame.getMethodName());
            }
            String stack = folded.toString();
            Integer previous = samples.get(stack);
            samples.put(stack, previous == null ? 1 : previous + 1);
            count++;
        }
    }
}
