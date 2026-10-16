# Cubic worldgen contract

RWG has one production target: `CCRealisticWorldGenerator`. The fixed-height generator and its 16x16x256 arrays are
migration inputs, not APIs to preserve.

## Lifecycle

- CubicChunks calls `IPreloadFailureDelegate` on its background I/O workers when requested column/cube data is absent.
  RWG forwards generation misses into column precalculation and will forward population misses into feature planning.
  These callbacks may publish immutable plans, but must not read or mutate world blocks.
- `provideColumn` installs column-global data such as biomes. It never generates the vertical cube stack.
- `provideCube` generates exactly one requested cube without world block access.
- `populate` places a prepared feature plan into the world without causing cascading generation.

A bounded cache holds completed plans, while a separate concurrent map holds computations only until they finish. This
prevents bounded-cache eviction from duplicating simultaneous work for one X/Z column. Eviction, worker count,
traversal order, and speculative cache misses must not change output. Never return a complete vertical column as
`GenerationResult` side effects. `provideColumn` and `provideCube` remain synchronous consumers: they join the same
in-progress computation when background precalculation has not finished.

`provideCube` first chooses an empty, solid, surface, or carver-intersecting path. Empty and solid cubes avoid detailed
block work. Random streams are derived independently from the world seed, stable stage ID, and coordinates; stages do
not share mutable `Random` instances.

## Stage order

```text
terrain:    base -> geology -> caves/rivers -> surface -> bedrock
population: ores -> underground decoration -> structures -> surface features -> snow/ice
```

Structure plans may be computed early, but structure blocks are placed late so ores and stone veins cannot overwrite
them. Stable dependency-graph targets should mark completed carvers, terrain, structures, and population. Compatibility
with fixed-height third-party generators remains disabled until the native cubic pipeline is correct.

## Migration

1. Empty CubicChunks world adapter.
2. Pure `ColumnPlanner`, immutable `ColumnPlan`, bounded deduplicating cache, and parallel determinism tests.
3. Remove the fixed-height generator and array APIs.
4. Base terrain, water, bedrock, simple surfaces, and empty/solid fast paths.
5. Height-independent surface rules.
6. Geology, caves, and underground rivers.
7. Planned decorators and population stages.
8. Late structure placement.
9. Optional compatibility integrations.
