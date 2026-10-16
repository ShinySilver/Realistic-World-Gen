# Offline CubicChunks worldgen profiling

Run the complete profiler without opening Minecraft or preparing a save:

```text
./gradlew debug_profileWorldgen
```

The task creates and later replaces an isolated server under `build/worldgen-profiler`. It uses RWG2, the preview seed
`927692613931855800`, disables CubicChunks' automatic spawn preparation, and queues the same 33x33x33 cube volume as a
client with horizontal and vertical view distances of 16 through CubicChunks' eager `LIGHT` path. Cubes are completed
across real server ticks, including the scheduler's pacing. The volume is centered on the spawn cube. Startup, mod
loading, saving, and shutdown are outside the measured interval.

Every run writes three timestamped files under `build/profiles`:

- `.html`: a self-contained summary with throughput, completion-time percentiles, and the hottest sampled stacks;
- `.json`: stable machine-readable measurements for comparisons and regression tooling;
- `.folded`: standard semicolon-separated folded stacks suitable for FlameGraph, speedscope conversion, or direct
  inspection. Thread names are retained so CubicChunks workers and the server thread remain distinguishable.

The sampler observes every thread currently executing RWG code. It therefore includes parallel column/population
planning as well as the server's cube generation and population work without adding timers to production hot loops.

For a quick smoke profile or a focused vertical band, pass Gradle properties:

```text
./gradlew debug_profileWorldgen -PrwgProfileRadius=2 -PrwgProfileVerticalRadius=4
```

`rwgProfileRadius=16` means 33x33 columns and `rwgProfileVerticalRadius=16` means 33 cubes vertically. Explicit
`rwgProfileMinCubeY` and `rwgProfileMaxCubeY` values override the centered vertical extent for focused runs. The
profiler always removes its isolated world before a run, so cached disk data cannot make later profiles look
artificially faster. Do not compare the first run after dependency or JVM changes with a warmed run without accounting
for JVM compilation; use the JSON output and repeat representative runs.

For correctness checks without launching a server, use:

```text
./gradlew debug_verifyWorldgen
```
