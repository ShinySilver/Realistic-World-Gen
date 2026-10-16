# Offline CubicChunks worldgen profiling

Run the complete profiler without opening Minecraft or preparing a save:

```text
./gradlew debug_profileWorldgen
```

The task creates and later replaces an isolated server under `build/worldgen-profiler`. It uses RWG2, the preview seed
`927692613931855800`, disables CubicChunks' automatic spawn preparation, and explicitly populates a 32x32-column spiral
around spawn from cube Y=-16 through Y=31 (block Y=-256 through Y=511). Startup, mod loading, saving, and shutdown are
outside the measured interval.

Every run writes three timestamped files under `build/profiles`:

- `.html`: a self-contained summary with throughput, cube latency percentiles, and the hottest sampled stacks;
- `.json`: stable machine-readable measurements for comparisons and regression tooling;
- `.folded`: standard semicolon-separated folded stacks suitable for FlameGraph, speedscope conversion, or direct
  inspection. Thread names are retained so CubicChunks workers and the server thread remain distinguishable.

The sampler observes every thread currently executing RWG code. It therefore includes parallel column/population
planning as well as the server's cube generation and population work without adding timers to production hot loops.

For a quick smoke profile or a focused vertical band, pass Gradle properties:

```text
./gradlew debug_profileWorldgen -PrwgProfileRadius=2 -PrwgProfileMinCubeY=-4 -PrwgProfileMaxCubeY=12
```

`rwgProfileRadius=16` means 32x32 columns. The profiler always removes its isolated world before a run, so cached disk
data cannot make later profiles look artificially faster. Do not compare the first run after dependency or JVM changes
with a warmed run without accounting for JVM compilation; use the JSON output and repeat representative runs.

For correctness checks without launching a server, use:

```text
./gradlew debug_verifyWorldgen
```
