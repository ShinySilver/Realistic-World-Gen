# Cubic worldgen contract

RWG has one production target: `CCRealisticWorldGenerator`. The fixed-height generator and its 16x16x256 arrays have
been removed; future stages operate on immutable plans and individual cubes.

## Lifecycle

- CubicChunks calls `IPreloadFailureDelegate` on its background I/O workers when requested column/cube data is absent.
  RWG forwards generation misses into column precalculation and population misses into feature planning.
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

## Population plans

Population planning has its own bounded, in-flight-deduplicating cache; it does not extend the lifetime or scope of
`ColumnPlan`. A `CubicFeature` receives pure column/blend inputs and returns an immutable `PlannedFeature`, or `null`
when it has no work. Every plan has a stable owner cube, deterministic seed, population stage, and conservative block
bounds. The owner rule makes output independent of which intersecting cube is requested first. Bounds let population
prepare affected terrain once before any legacy Minecraft `WorldGenerator` reads or writes across a cube edge.

The built-in biome recipes now use this contract and no fixed-height `BiomeDecorator.decorate` entry point remains.
Their default plan is deliberately small: one immutable invocation per blended biome recipe. `PlannedFeature` is an
interface so expensive or large features can later publish more specific candidate/component plans without changing
the scheduler. Placement remains on the population thread; planning never reads or mutates world blocks. Vanilla's
monolithic biome decoration adapter was removed because it mixes ores and surface features and cannot obey this graph.

### Future connectivity culling

If underground population or lighting becomes traversal-bound, test a deliberately lossy cube-connectivity index before
building a precise graph. During off-thread cube planning, count solid opaque blocks on each of the six faces (or keep
a few face bitsets packed into `long`s). A completely sealed face may stop propagation; every uncertain case remains
open. The only correctness invariant is one-sided error: false positives merely do extra work, while a false negative
must never hide reachable generation. Incremental bitwise flood propagation should target the common sealed-cube case,
not reconstruct exact interior connectivity. Add this only after profiling representative underground stages.

### Future ores

The `rwg:ores` dependency slot is intentionally a no-op until ores use region-owned large-vein plans. Do not add
independent vanilla-sized deposits per cube. A vein must be derived once from the world seed and stable region
coordinates, publish immutable bounds/shape parameters off-thread, and let every intersecting cube reproduce only its
own clipped portion. This keeps seams and results independent of request order while leaving structures later in the
graph so ore replacement can never damage them.

## Cube-generation invariants

Cube generation uses one small `CubeGenerationContext` containing the cube position, absolute Y bounds, immutable
`ColumnPlan`, and target `Cube`. Every terrain stage receives that context; stages must not fetch the plan again, invent
their own coordinate convention, or query world blocks.

Build one RWG-owned CubicChunks `DependencyGraph` completely before generation starts; do not use the global
`BuiltinWorldDecorators.CUBIC_VANILLA`. RWG stages receive the explicit `CubeGenerationContext`, avoiding hidden
thread-local state or repeated plan lookups. Establish stable targets for base-complete, carvers-complete, and
terrain-complete. The graph orders cube mutation; parallel work remains in immutable planning.

Classify each requested cube before allocating storage:

- cubes wholly above both the highest surface and sea level remain storage-free air cubes;
- deep cubes may use `UniformBlockView` only when no registered later stage can intersect them;
- mixed cubes allocate once and use local 0..15 coordinates, with absolute Y for every height decision;
- bedrock is relative to the configured minimum generation Y, never hard-coded to Y=0;
- sea level is one named setting, never repeated as `62` or `63` literals.

The solid fast path must accept later stage-intersection tests, so adding caves cannot silently leave a uniform cube
uncarved. `provideCube` obtains its plan exactly once and emits no neighboring cube side effects. When its column is
null, that column is the sole allowed side effect. Stage RNGs derive from world seed, stable stage ID, and cube
coordinates, never execution order or shared mutable state.

Milestone 4 tests cover a storage-free air cube, a uniform deep cube, a mixed surface cube, sea-level water, negative
cube Y, bedrock at the configured lower bound, heights above Y=255, request-order determinism, and one plan reused by
several vertical cubes. Keep the implementation to one context, one ordered graph, and the existing cache until
profiling demonstrates that another abstraction is needed.

## Surface-rule invariants

Milestone 5 replaces the temporary simple painter without changing cube classification or adding another cache:

- A surface rule receives the existing `CubeGenerationContext`, local X/Z, absolute surface Y, and resolved column
  inputs. It never scans downward from a fixed world ceiling or reads blocks through `World`.
- Rules paint only their declared maximum depth. The pipeline derives its solid fast-path margin from the maximum depth
  of the frozen rule set; it is not left as an unrelated magic constant.
- A cube paints only the surface bands that intersect its own Y range. Filler crossing a cube boundary is reproduced
  from absolute depth, without loading or generating the neighboring cube.
- Cliff, beach, river, and other boundary signals that require neighboring heights are computed in `ColumnPlanner`
  from its existing halo and stored immutably in `ColumnPlan`. Surface generation never fetches neighboring plans.
- Rule selection is resolved once per X/Z column in the plan. No per-block registry lookup, per-column rule object, or
  shared mutable random source is introduced.
- Preserve the storage-free air and uniform deep-cube paths. Adding a rule with a deeper reach must automatically make
  affected cubes detailed.

Start with the smallest rule contract that can replace the built-in surfaces. Specialized legacy painters are ported
only when their visible behavior is still wanted; fixed-height compatibility signatures are deleted rather than
wrapped.

`ColumnPlan` resolves one composed `SurfaceRule` per X/Z and records the largest declared depth. Its one-block planning
halo supplies seam-free slope, while world-seeded variation signals are cached with the other column inputs. Rules
return shared block-and-metadata materials; cube painting performs no allocation, registry lookup, neighbor fetch, or
random mutation. The fixed-height painter API and unsupported compatibility registrations have been removed.

## Migration

1. Empty CubicChunks world adapter.
2. Pure `ColumnPlanner`, immutable `ColumnPlan`, bounded deduplicating cache, and parallel determinism tests.
3. Remove the fixed-height generator and array APIs. (complete)
4. Base terrain, water, bedrock, simple surfaces, and empty/solid fast paths. (complete)
5. Height-independent surface rules. (complete)
6. Geology, caves, and underground rivers. (native cube stages complete; gameplay tuning remains)
7. Planned biome features and population stages. (built-in surface recipes and native underground lakes complete;
   large-vein ores and final snow/ice remain)
8. Late structure placement.
9. Optional compatibility integrations.
