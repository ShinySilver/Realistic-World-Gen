package rwg.world.terrain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Cached deterministic graph of gently curved links between sparse jittered-grid points. */
final class RiverNetwork {

    private static final double NODE_SPACING = 1248D;
    private static final double NODE_JITTER = .8D;
    private static final double NODE_MARGIN = (1D - NODE_JITTER) * .5D;
    private static final double NODE_MERGE_DISTANCE = NODE_SPACING * .35D;
    private static final double NODE_MERGE_DISTANCE_SQUARED = NODE_MERGE_DISTANCE * NODE_MERGE_DISTANCE;
    private static final double MAXIMUM_SAME_DIRECTION_DOT = 0.3420201433256688D; // cos(70 degrees)
    private static final int LINK_COUNT = 3;
    private static final double RIVER_WIDTH = 12D;
    private static final double VALLEY_INFLUENCE = 72D;
    private static final double EROSION_INFLUENCE = 256D;
    private static final double EROSION_MERGE_SMOOTHING = 48D;
    private static final double JUNCTION_VALLEY_RADIUS = 208D;
    private static final double JUNCTION_CENTER_OFFSET = JUNCTION_VALLEY_RADIUS * .5D;
    private static final float JUNCTION_WARP_SCALE = 180f;
    private static final float JUNCTION_WARP_STRENGTH = 58f;
    private static final double JUNCTION_DISCOVERY_RADIUS = JUNCTION_VALLEY_RADIUS + JUNCTION_CENTER_OFFSET
            + JUNCTION_WARP_STRENGTH * 2D;
    private static final float MINIMUM_JUNCTION_BASE_HEIGHT = 100f;
    private static final int BUCKET_SIZE = 128;
    private static final int REGION_SIZE = 1024;
    // Neighbor links span at most 1.8 cells per axis. The extra curve handles stay inside this conservative bound.
    private static final double BUCKET_DISCOVERY_MARGIN = NODE_SPACING * 4D;
    private static final double CURVE_TOLERANCE_SQUARED = 1D;
    private static final int MAXIMUM_SUBDIVISION_DEPTH = 10;
    private static final int EROSION_CURVE_SEGMENTS = 8;
    private static final double MEANDER_AMPLITUDE = 25.2D;
    private static final double MEANDER_WAVELENGTH = 240D;
    private static final double MEANDER_SAMPLE_SPACING = 16D;
    private static final double MOUNTAIN_SAMPLE_SPACING = 64D;
    private static final double MOUNTAIN_STRAIGHTENING_DISTANCE = 750D;
    private static final double TAU = Math.PI * 2D;

    private final long seed;
    private final MountainDistanceSampler mountains;
    private final NoiseGenerator junctionWarp;
    private final Cache<NodeKey, NodeKey> canonicalNodes = CacheBuilder.newBuilder().maximumSize(32768)
            .concurrencyLevel(4).build();
    private final Cache<NodeKey, Node> nodes = CacheBuilder.newBuilder().maximumSize(32768).concurrencyLevel(4).build();
    private final Cache<EdgeKey, Edge> edges = CacheBuilder.newBuilder().maximumSize(32768).concurrencyLevel(4).build();
    private final Cache<BucketKey, RiverSegment[]> regions = CacheBuilder.newBuilder().maximumSize(1024)
            .concurrencyLevel(4).build();
    private final Cache<BucketKey, RiverSegment[]> buckets = CacheBuilder.newBuilder().maximumSize(8192)
            .concurrencyLevel(4).build();
    private final Cache<BucketKey, Junction[]> junctionRegions = CacheBuilder.newBuilder().maximumSize(1024)
            .concurrencyLevel(4).build();
    private final Cache<BucketKey, ErosionCurve[]> erosionRegions = CacheBuilder.newBuilder().maximumSize(1024)
            .concurrencyLevel(4).build();
    private final ThreadLocal<BucketLookup> lastBucket = new ThreadLocal<BucketLookup>() {

        @Override
        protected BucketLookup initialValue() {
            return new BucketLookup();
        }
    };
    private final ThreadLocal<JunctionBucketLookup> lastJunctionBucket = new ThreadLocal<JunctionBucketLookup>() {

        @Override
        protected JunctionBucketLookup initialValue() {
            return new JunctionBucketLookup();
        }
    };
    private final ThreadLocal<ErosionRegionLookup> lastErosionRegion = new ThreadLocal<ErosionRegionLookup>() {

        @Override
        protected ErosionRegionLookup initialValue() {
            return new ErosionRegionLookup();
        }
    };

    RiverNetwork(long seed, MountainDistanceSampler mountains) {
        this.seed = seed;
        this.mountains = mountains;
        junctionWarp = NoiseSelector.createNoiseGenerator(seed ^ 0x6A09E667F3BCC909L);
    }

    float strength(int x, int z, long networkSeed, boolean suppressSurface) {
        return strength(distance(x, z, networkSeed, suppressSurface));
    }

    static float strength(float distance) {
        if (distance >= RIVER_WIDTH) return 0f;
        float normalized = 1f - distance / (float) RIVER_WIDTH;
        return normalized * normalized * (3f - 2f * normalized);
    }

    float distance(int x, int z, long networkSeed, boolean suppressSurface) {
        int bucketX = floorDiv(x, BUCKET_SIZE);
        int bucketZ = floorDiv(z, BUCKET_SIZE);
        BucketLookup lookup = lastBucket.get();
        RiverSegment[] nearby;
        if (lookup.segments != null && lookup.networkSeed == networkSeed
                && lookup.x == bucketX
                && lookup.z == bucketZ) {
            nearby = lookup.segments;
        } else {
            nearby = bucket(networkSeed, bucketX, bucketZ);
            lookup.networkSeed = networkSeed;
            lookup.x = bucketX;
            lookup.z = bucketZ;
            lookup.segments = nearby;
        }
        double nearestDistanceSquared = VALLEY_INFLUENCE * VALLEY_INFLUENCE;
        for (RiverSegment segment : nearby) {
            if (suppressSurface && !segment.carvesMountains) continue;
            nearestDistanceSquared = Math.min(nearestDistanceSquared, segment.segment.distanceSquared(x, z));
        }
        return (float) Math.sqrt(nearestDistanceSquared);
    }

    /** Packs the height-blended surface distance and strength into one allocation-free result. */
    long surfaceSample(int x, int z, long networkSeed, float mountainSuppression) {
        if (mountainSuppression <= 0f) return packSurfaceSample(distance(x, z, networkSeed, false));
        if (mountainSuppression >= 1f) return packSurfaceSample(distance(x, z, networkSeed, true));

        float allDistance = distance(x, z, networkSeed, false);
        if (allDistance >= MorphologyRules.MAXIMUM_SURFACE_RIVER_WIDTH) return packSurfaceSample(allDistance);

        int bucketX = floorDiv(x, BUCKET_SIZE);
        int bucketZ = floorDiv(z, BUCKET_SIZE);
        BucketLookup lookup = lastBucket.get();
        RiverSegment[] nearby;
        if (lookup.segments != null && lookup.networkSeed == networkSeed
                && lookup.x == bucketX
                && lookup.z == bucketZ) {
            nearby = lookup.segments;
        } else {
            nearby = bucket(networkSeed, bucketX, bucketZ);
            lookup.networkSeed = networkSeed;
            lookup.x = bucketX;
            lookup.z = bucketZ;
            lookup.segments = nearby;
        }

        double nearestMountainCarver = VALLEY_INFLUENCE * VALLEY_INFLUENCE;
        for (RiverSegment segment : nearby) {
            if (segment.carvesMountains)
                nearestMountainCarver = Math.min(nearestMountainCarver, segment.segment.distanceSquared(x, z));
        }

        float mountainCarverDistance = (float) Math.sqrt(nearestMountainCarver);
        // Fade an ordinary river in place. Interpolating its distance toward an unrelated mountain-carving edge can
        // make the channel jump, disappear, and restart as the terrain suppression changes.
        float suppressedOrdinaryDistance = lerp(
                allDistance,
                MorphologyRules.MAXIMUM_SURFACE_RIVER_WIDTH,
                mountainSuppression);
        float distance = Math.min(suppressedOrdinaryDistance, mountainCarverDistance);
        float surfaceStrength = Math.max(
                strength(allDistance) * (1f - mountainSuppression),
                strength(mountainCarverDistance));
        return packSurfaceSample(distance, surfaceStrength);
    }

    private static long packSurfaceSample(float distance) {
        return packSurfaceSample(distance, strength(distance));
    }

    private static long packSurfaceSample(float distance, float surfaceStrength) {
        return ((long) Float.floatToRawIntBits(distance) << 32)
                | (Float.floatToRawIntBits(surfaceStrength) & 0xffffffffL);
    }

    float junctionDistance(int x, int z, long networkSeed) {
        int bucketX = floorDiv(x, REGION_SIZE);
        int bucketZ = floorDiv(z, REGION_SIZE);
        JunctionBucketLookup lookup = lastJunctionBucket.get();
        Junction[] nearby;
        if (lookup.junctions != null && lookup.networkSeed == networkSeed
                && lookup.x == bucketX
                && lookup.z == bucketZ) {
            nearby = lookup.junctions;
        } else {
            nearby = junctionBucket(networkSeed, bucketX, bucketZ);
            lookup.networkSeed = networkSeed;
            lookup.x = bucketX;
            lookup.z = bucketZ;
            lookup.junctions = nearby;
        }
        double nearestDistanceSquared = JUNCTION_VALLEY_RADIUS * JUNCTION_VALLEY_RADIUS;
        if (nearby.length == 0) return (float) JUNCTION_VALLEY_RADIUS;
        double warpedX = x
                + junctionWarp.noise2(x / JUNCTION_WARP_SCALE, z / JUNCTION_WARP_SCALE) * JUNCTION_WARP_STRENGTH;
        double warpedZ = z + junctionWarp.noise2((x + 2371f) / JUNCTION_WARP_SCALE, (z - 1879f) / JUNCTION_WARP_SCALE)
                * JUNCTION_WARP_STRENGTH;
        for (Junction junction : nearby) {
            double dx = warpedX - junction.warpedCenterX;
            double dz = warpedZ - junction.warpedCenterZ;
            nearestDistanceSquared = Math.min(nearestDistanceSquared, dx * dx + dz * dz);
        }
        return (float) Math.sqrt(nearestDistanceSquared);
    }

    static float junctionStrength(float distance) {
        if (distance >= JUNCTION_VALLEY_RADIUS) return 0f;
        float normalized = 1f - distance / (float) JUNCTION_VALLEY_RADIUS;
        return normalized * normalized * (3f - 2f * normalized);
    }

    float erosionDistance(int x, int z, long networkSeed, float mountainSuppression) {
        int regionX = floorDiv(x, REGION_SIZE);
        int regionZ = floorDiv(z, REGION_SIZE);
        ErosionRegionLookup lookup = lastErosionRegion.get();
        ErosionCurve[] nearby;
        if (lookup.segments != null && lookup.networkSeed == networkSeed
                && lookup.x == regionX
                && lookup.z == regionZ) {
            nearby = lookup.segments;
        } else {
            nearby = erosionRegion(networkSeed, regionX, regionZ);
            lookup.networkSeed = networkSeed;
            lookup.x = regionX;
            lookup.z = regionZ;
            lookup.segments = nearby;
        }
        double nearest = EROSION_INFLUENCE * EROSION_INFLUENCE;
        double secondNearest = nearest;
        double nearestMountainCarver = nearest;
        double secondNearestMountainCarver = nearest;
        boolean needAllRivers = mountainSuppression < 1f;
        boolean needMountainCarvers = mountainSuppression > 0f;
        for (ErosionCurve curve : nearby) {
            double distanceSquared = EROSION_INFLUENCE * EROSION_INFLUENCE;
            for (Segment segment : curve.segments)
                distanceSquared = Math.min(distanceSquared, segment.distanceSquared(x, z));
            if (needAllRivers) {
                if (distanceSquared < nearest) {
                    secondNearest = nearest;
                    nearest = distanceSquared;
                } else if (distanceSquared < secondNearest) {
                    secondNearest = distanceSquared;
                }
            }
            if (needMountainCarvers && curve.carvesMountains) {
                if (distanceSquared < nearestMountainCarver) {
                    secondNearestMountainCarver = nearestMountainCarver;
                    nearestMountainCarver = distanceSquared;
                } else if (distanceSquared < secondNearestMountainCarver) {
                    secondNearestMountainCarver = distanceSquared;
                }
            }
        }
        float allRivers = needAllRivers
                ? (float) smoothMinimum(Math.sqrt(nearest), Math.sqrt(secondNearest), EROSION_MERGE_SMOOTHING)
                : 0f;
        float mountainCarvers = needMountainCarvers
                ? (float) smoothMinimum(
                        Math.sqrt(nearestMountainCarver),
                        Math.sqrt(secondNearestMountainCarver),
                        EROSION_MERGE_SMOOTHING)
                : 0f;
        if (!needAllRivers) return mountainCarvers;
        if (!needMountainCarvers) return allRivers;
        return lerp(allRivers, mountainCarvers, mountainSuppression);
    }

    /** Compact polynomial blend: identical to min when the distances differ by at least smoothing. */
    private static double smoothMinimum(double first, double second, double smoothing) {
        double blend = Math.max(smoothing - Math.abs(first - second), 0D) / smoothing;
        return Math.min(first, second) - blend * blend * smoothing * .25D;
    }

    private ErosionCurve[] erosionRegion(long networkSeed, int regionX, int regionZ) {
        BucketKey key = new BucketKey(networkSeed, regionX, regionZ);
        ErosionCurve[] cached = erosionRegions.getIfPresent(key);
        if (cached != null) return cached;
        try {
            return erosionRegions.get(key, new Callable<ErosionCurve[]>() {

                @Override
                public ErosionCurve[] call() {
                    double minimumX = regionX * (double) REGION_SIZE - EROSION_INFLUENCE;
                    double minimumZ = regionZ * (double) REGION_SIZE - EROSION_INFLUENCE;
                    double maximumX = (regionX + 1D) * REGION_SIZE + EROSION_INFLUENCE;
                    double maximumZ = (regionZ + 1D) * REGION_SIZE + EROSION_INFLUENCE;
                    int firstCellX = floor(minimumX / NODE_SPACING) - 2;
                    int firstCellZ = floor(minimumZ / NODE_SPACING) - 2;
                    int lastCellX = floor(maximumX / NODE_SPACING) + 2;
                    int lastCellZ = floor(maximumZ / NODE_SPACING) + 2;
                    Set<EdgeKey> represented = new HashSet<EdgeKey>();
                    ArrayList<ErosionCurve> result = new ArrayList<ErosionCurve>();
                    for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
                        for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {
                            Node node = node(new NodeKey(networkSeed, cellX, cellZ));
                            for (NodeKey neighbor : node.links) {
                                EdgeKey edgeKey = new EdgeKey(node.key, neighbor);
                                if (!represented.add(edgeKey)) continue;
                                Edge edge = edge(edgeKey);
                                ArrayList<Segment> relevantSegments = new ArrayList<Segment>();
                                for (Segment segment : edge.erosionSegments) {
                                    if (!segment.intersects(minimumX, minimumZ, maximumX, maximumZ)) continue;
                                    relevantSegments.add(segment);
                                }
                                if (!relevantSegments.isEmpty()) result.add(
                                        new ErosionCurve(
                                                relevantSegments.toArray(new Segment[relevantSegments.size()]),
                                                edge.carvesMountains));
                            }
                        }
                    }
                    return result.toArray(new ErosionCurve[result.size()]);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river-erosion region", exception.getCause());
        }
    }

    private static float lerp(float first, float second, float amount) {
        return first + (second - first) * amount;
    }

    private Junction[] junctionBucket(long networkSeed, int bucketX, int bucketZ) {
        BucketKey key = new BucketKey(networkSeed, bucketX, bucketZ);
        Junction[] cached = junctionRegions.getIfPresent(key);
        if (cached != null) return cached;
        try {
            return junctionRegions.get(key, new Callable<Junction[]>() {

                @Override
                public Junction[] call() {
                    return createJunctionBucket(networkSeed, bucketX, bucketZ);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river-junction bucket", exception.getCause());
        }
    }

    private Junction[] createJunctionBucket(long networkSeed, int bucketX, int bucketZ) {
        double minimumX = bucketX * (double) REGION_SIZE - JUNCTION_DISCOVERY_RADIUS;
        double minimumZ = bucketZ * (double) REGION_SIZE - JUNCTION_DISCOVERY_RADIUS;
        double maximumX = (bucketX + 1D) * REGION_SIZE + JUNCTION_DISCOVERY_RADIUS;
        double maximumZ = (bucketZ + 1D) * REGION_SIZE + JUNCTION_DISCOVERY_RADIUS;
        int firstCellX = floor(minimumX / NODE_SPACING) - 1;
        int firstCellZ = floor(minimumZ / NODE_SPACING) - 1;
        int lastCellX = floor(maximumX / NODE_SPACING) + 1;
        int lastCellZ = floor(maximumZ / NODE_SPACING) + 1;
        Set<NodeKey> represented = new HashSet<NodeKey>();
        ArrayList<Junction> result = new ArrayList<Junction>();
        for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {
                NodeKey canonical = canonicalNodeKey(new NodeKey(networkSeed, cellX, cellZ));
                if (!represented.add(canonical)) continue;
                Node node = node(canonical);
                if (node.links.length < 3 || node.x < minimumX
                        || node.x > maximumX
                        || node.z < minimumZ
                        || node.z > maximumZ)
                    continue;
                double[] center = valleyCenter(node);
                int nodeX = (int) Math.round(center[0]);
                int nodeZ = (int) Math.round(center[1]);
                if (mountains == null) continue;
                float mountainDistance = mountains.distanceAt(nodeX, nodeZ);
                if (mountainDistance > MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS
                        || mountains.uncarvedBaseHeightAt(nodeX, nodeZ, mountainDistance)
                                < MINIMUM_JUNCTION_BASE_HEIGHT)
                    continue;
                double warpedCenterX = center[0] + junctionWarp
                        .noise2((float) center[0] / JUNCTION_WARP_SCALE, (float) center[1] / JUNCTION_WARP_SCALE)
                        * JUNCTION_WARP_STRENGTH;
                double warpedCenterZ = center[1] + junctionWarp.noise2(
                        ((float) center[0] + 2371f) / JUNCTION_WARP_SCALE,
                        ((float) center[1] - 1879f) / JUNCTION_WARP_SCALE) * JUNCTION_WARP_STRENGTH;
                result.add(new Junction(warpedCenterX, warpedCenterZ));
            }
        }
        return result.toArray(new Junction[result.size()]);
    }

    /** Places the basin between the two incident river branches with the smallest angle. */
    private double[] valleyCenter(Node node) {
        double bestDot = -Double.MAX_VALUE;
        double centerDirectionX = 0D;
        double centerDirectionZ = 0D;
        for (int first = 0; first < node.links.length; first++) {
            double firstX = nodeCoordinate(node.key.networkSeed, node.links[first].cellX, node.links[first].cellZ, 0)
                    - node.x;
            double firstZ = nodeCoordinate(node.key.networkSeed, node.links[first].cellX, node.links[first].cellZ, 1)
                    - node.z;
            double firstLength = Math.hypot(firstX, firstZ);
            firstX /= firstLength;
            firstZ /= firstLength;
            for (int second = first + 1; second < node.links.length; second++) {
                double secondX = nodeCoordinate(
                        node.key.networkSeed,
                        node.links[second].cellX,
                        node.links[second].cellZ,
                        0) - node.x;
                double secondZ = nodeCoordinate(
                        node.key.networkSeed,
                        node.links[second].cellX,
                        node.links[second].cellZ,
                        1) - node.z;
                double secondLength = Math.hypot(secondX, secondZ);
                secondX /= secondLength;
                secondZ /= secondLength;
                double dot = firstX * secondX + firstZ * secondZ;
                if (dot <= bestDot) continue;
                bestDot = dot;
                centerDirectionX = firstX + secondX;
                centerDirectionZ = firstZ + secondZ;
            }
        }
        double directionLength = Math.hypot(centerDirectionX, centerDirectionZ);
        if (directionLength < 1.0E-8D) return new double[] { node.x, node.z };
        return new double[] { node.x + centerDirectionX / directionLength * JUNCTION_CENTER_OFFSET,
                node.z + centerDirectionZ / directionLength * JUNCTION_CENTER_OFFSET };
    }

    private RiverSegment[] bucket(long networkSeed, int bucketX, int bucketZ) {
        BucketKey key = new BucketKey(networkSeed, bucketX, bucketZ);
        RiverSegment[] cached = buckets.getIfPresent(key);
        if (cached != null) return cached;
        try {
            return buckets.get(key, new Callable<RiverSegment[]>() {

                @Override
                public RiverSegment[] call() {
                    return createBucket(networkSeed, bucketX, bucketZ);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river bucket", exception.getCause());
        }
    }

    private RiverSegment[] createBucket(long networkSeed, int bucketX, int bucketZ) {
        double minimumX = bucketX * (double) BUCKET_SIZE;
        double minimumZ = bucketZ * (double) BUCKET_SIZE;
        double maximumX = minimumX + BUCKET_SIZE;
        double maximumZ = minimumZ + BUCKET_SIZE;
        RiverSegment[] candidates = region(
                networkSeed,
                floorDiv(bucketX * BUCKET_SIZE, REGION_SIZE),
                floorDiv(bucketZ * BUCKET_SIZE, REGION_SIZE));
        ArrayList<RiverSegment> result = new ArrayList<RiverSegment>();
        for (RiverSegment segment : candidates) {
            if (segment.segment.intersects(
                    minimumX - VALLEY_INFLUENCE,
                    minimumZ - VALLEY_INFLUENCE,
                    maximumX + VALLEY_INFLUENCE,
                    maximumZ + VALLEY_INFLUENCE))
                result.add(segment);
        }
        return result.toArray(new RiverSegment[result.size()]);
    }

    private RiverSegment[] region(long networkSeed, int regionX, int regionZ) {
        BucketKey key = new BucketKey(networkSeed, regionX, regionZ);
        RiverSegment[] cached = regions.getIfPresent(key);
        if (cached != null) return cached;
        try {
            return regions.get(key, new Callable<RiverSegment[]>() {

                @Override
                public RiverSegment[] call() {
                    return createRegion(networkSeed, regionX, regionZ);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river region", exception.getCause());
        }
    }

    private RiverSegment[] createRegion(long networkSeed, int regionX, int regionZ) {
        double minimumX = regionX * (double) REGION_SIZE;
        double minimumZ = regionZ * (double) REGION_SIZE;
        double maximumX = minimumX + REGION_SIZE;
        double maximumZ = minimumZ + REGION_SIZE;
        Set<EdgeKey> represented = new HashSet<EdgeKey>();
        ArrayList<RiverSegment> result = new ArrayList<RiverSegment>();
        int firstCellX = floor((minimumX - BUCKET_DISCOVERY_MARGIN) / NODE_SPACING);
        int firstCellZ = floor((minimumZ - BUCKET_DISCOVERY_MARGIN) / NODE_SPACING);
        int lastCellX = floor((maximumX + BUCKET_DISCOVERY_MARGIN) / NODE_SPACING);
        int lastCellZ = floor((maximumZ + BUCKET_DISCOVERY_MARGIN) / NODE_SPACING);
        for (int cellZ = firstCellZ; cellZ <= lastCellZ; cellZ++) {
            for (int cellX = firstCellX; cellX <= lastCellX; cellX++) {
                Node node = node(new NodeKey(networkSeed, cellX, cellZ));
                for (NodeKey neighbor : node.links) {
                    EdgeKey edgeKey = new EdgeKey(node.key, neighbor);
                    if (!represented.add(edgeKey)) continue;
                    Edge edge = edge(edgeKey);
                    for (Segment segment : edge.segments) {
                        if (segment.intersects(
                                minimumX - VALLEY_INFLUENCE,
                                minimumZ - VALLEY_INFLUENCE,
                                maximumX + VALLEY_INFLUENCE,
                                maximumZ + VALLEY_INFLUENCE))
                            result.add(new RiverSegment(segment, edge.carvesMountains));
                    }
                }
            }
        }
        return result.toArray(new RiverSegment[result.size()]);
    }

    private Node node(NodeKey key) {
        final NodeKey canonicalKey = canonicalNodeKey(key);
        Node cached = nodes.getIfPresent(canonicalKey);
        if (cached != null) return cached;
        try {
            return nodes.get(canonicalKey, new Callable<Node>() {

                @Override
                public Node call() {
                    return createNode(canonicalKey);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river node", exception.getCause());
        }
    }

    private NodeKey canonicalNodeKey(NodeKey key) {
        NodeKey cached = canonicalNodes.getIfPresent(key);
        if (cached != null) return cached;
        NodeKey resolved = resolveCanonicalNodeKey(key);
        NodeKey raced = canonicalNodes.asMap().putIfAbsent(key, resolved);
        return raced == null ? resolved : raced;
    }

    private NodeKey resolveCanonicalNodeKey(NodeKey key) {
        NodeKey current = key;
        for (int iteration = 0; iteration < 64; iteration++) {
            NodeKey representative = current;
            long representativePriority = hash(current.networkSeed, current.cellX, current.cellZ, 23);
            double x = nodeCoordinate(current.networkSeed, current.cellX, current.cellZ, 0);
            double z = nodeCoordinate(current.networkSeed, current.cellX, current.cellZ, 1);
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    if (offsetX == 0 && offsetZ == 0) continue;
                    int cellX = current.cellX + offsetX;
                    int cellZ = current.cellZ + offsetZ;
                    double dx = nodeCoordinate(current.networkSeed, cellX, cellZ, 0) - x;
                    double dz = nodeCoordinate(current.networkSeed, cellX, cellZ, 1) - z;
                    if (dx * dx + dz * dz >= NODE_MERGE_DISTANCE_SQUARED) continue;
                    long priority = hash(current.networkSeed, cellX, cellZ, 23);
                    if (Long.compareUnsigned(priority, representativePriority) < 0) {
                        representative = new NodeKey(current.networkSeed, cellX, cellZ);
                        representativePriority = priority;
                    }
                }
            }
            if (representative.equals(current)) return current;
            current = representative;
        }
        return current;
    }

    private Node createNode(NodeKey key) {
        double x = nodeCoordinate(key.networkSeed, key.cellX, key.cellZ, 0);
        double z = nodeCoordinate(key.networkSeed, key.cellX, key.cellZ, 1);
        NodeKey[] candidates = new NodeKey[8];
        double[] candidateDistances = new double[8];
        Set<NodeKey> represented = new HashSet<NodeKey>();
        int candidateCount = 0;
        for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetZ == 0) continue;
                NodeKey candidate = canonicalNodeKey(
                        new NodeKey(key.networkSeed, key.cellX + offsetX, key.cellZ + offsetZ));
                if (candidate.equals(key) || !represented.add(candidate)) continue;
                double dx = nodeCoordinate(key.networkSeed, candidate.cellX, candidate.cellZ, 0) - x;
                double dz = nodeCoordinate(key.networkSeed, candidate.cellX, candidate.cellZ, 1) - z;
                double distance = dx * dx + dz * dz;
                int insertion = candidateCount;
                for (int index = 0; index < candidateCount; index++) {
                    if (distance > candidateDistances[index]
                            || distance == candidateDistances[index] && candidate.compareTo(candidates[index]) >= 0)
                        continue;
                    insertion = index;
                    break;
                }
                for (int shift = candidateCount; shift > insertion; shift--) {
                    candidates[shift] = candidates[shift - 1];
                    candidateDistances[shift] = candidateDistances[shift - 1];
                }
                candidates[insertion] = candidate;
                candidateDistances[insertion] = distance;
                candidateCount++;
            }
        }
        double[] candidateDirectionX = new double[candidateCount];
        double[] candidateDirectionZ = new double[candidateCount];
        for (int index = 0; index < candidateCount; index++) {
            double length = Math.sqrt(candidateDistances[index]);
            candidateDirectionX[index] = (nodeCoordinate(
                    key.networkSeed,
                    candidates[index].cellX,
                    candidates[index].cellZ,
                    0) - x) / length;
            candidateDirectionZ[index] = (nodeCoordinate(
                    key.networkSeed,
                    candidates[index].cellX,
                    candidates[index].cellZ,
                    1) - z) / length;
        }
        int selectedFirst = 0;
        int selectedSecond = 1;
        int selectedThird = 2;
        boolean foundSeparatedTriple = false;
        double selectedTotalDistance = Double.POSITIVE_INFINITY;
        double selectedWorstDot = Double.POSITIVE_INFINITY;
        for (int first = 0; first < candidateCount; first++) {
            for (int second = first + 1; second < candidateCount; second++) {
                for (int third = second + 1; third < candidateCount; third++) {
                    double firstSecond = candidateDirectionX[first] * candidateDirectionX[second]
                            + candidateDirectionZ[first] * candidateDirectionZ[second];
                    double firstThird = candidateDirectionX[first] * candidateDirectionX[third]
                            + candidateDirectionZ[first] * candidateDirectionZ[third];
                    double secondThird = candidateDirectionX[second] * candidateDirectionX[third]
                            + candidateDirectionZ[second] * candidateDirectionZ[third];
                    double worstDot = Math.max(firstSecond, Math.max(firstThird, secondThird));
                    double totalDistance = candidateDistances[first] + candidateDistances[second]
                            + candidateDistances[third];
                    boolean separated = worstDot <= MAXIMUM_SAME_DIRECTION_DOT;
                    if (separated ? !foundSeparatedTriple || totalDistance < selectedTotalDistance
                            : !foundSeparatedTriple && (worstDot < selectedWorstDot
                                    || worstDot == selectedWorstDot && totalDistance < selectedTotalDistance)) {
                        foundSeparatedTriple |= separated;
                        selectedFirst = first;
                        selectedSecond = second;
                        selectedThird = third;
                        selectedWorstDot = worstDot;
                        selectedTotalDistance = totalDistance;
                    }
                }
            }
        }
        NodeKey[] links = new NodeKey[LINK_COUNT];
        double[] linkDirectionX = new double[LINK_COUNT];
        double[] linkDirectionZ = new double[LINK_COUNT];
        int[] selected = { selectedFirst, selectedSecond, selectedThird };
        for (int index = 0; index < LINK_COUNT; index++) {
            links[index] = candidates[selected[index]];
            linkDirectionX[index] = candidateDirectionX[selected[index]];
            linkDirectionZ[index] = candidateDirectionZ[selected[index]];
        }
        int firstPrimary = 0;
        int secondPrimary = 1;
        double primaryDot = Double.POSITIVE_INFINITY;
        for (int first = 0; first < links.length; first++) {
            for (int second = first + 1; second < links.length; second++) {
                double dot = linkDirectionX[first] * linkDirectionX[second]
                        + linkDirectionZ[first] * linkDirectionZ[second];
                if (dot < primaryDot) {
                    primaryDot = dot;
                    firstPrimary = first;
                    secondPrimary = second;
                }
            }
        }
        double firstDirectionX = linkDirectionX[firstPrimary];
        double firstDirectionZ = linkDirectionZ[firstPrimary];
        double secondDirectionX = linkDirectionX[secondPrimary];
        double secondDirectionZ = linkDirectionZ[secondPrimary];
        // A node direction is an unoriented tangent axis. Align the two primary link directions to the same
        // hemisphere before averaging; otherwise opposite links cancel and incorrectly produce a perpendicular
        // tangent.
        if (firstDirectionX * secondDirectionX + firstDirectionZ * secondDirectionZ < 0D) {
            secondDirectionX = -secondDirectionX;
            secondDirectionZ = -secondDirectionZ;
        }
        double directionX = firstDirectionX + secondDirectionX;
        double directionZ = firstDirectionZ + secondDirectionZ;
        double directionLength = Math.hypot(directionX, directionZ);
        if (directionLength < 1.0E-8D) {
            directionX = -firstDirectionZ;
            directionZ = firstDirectionX;
            if ((key.hashCode() & 1) != 0) {
                directionX = -directionX;
                directionZ = -directionZ;
            }
        } else {
            directionX /= directionLength;
            directionZ /= directionLength;
        }
        return new Node(key, x, z, directionX, directionZ, links);
    }

    private double nodeCoordinate(long networkSeed, int cellX, int cellZ, int axis) {
        int cell = axis == 0 ? cellX : cellZ;
        return (cell + NODE_MARGIN + unit(hash(networkSeed, cellX, cellZ, axis)) * NODE_JITTER) * NODE_SPACING;
    }

    private long hash(long networkSeed, int cellX, int cellZ, int axis) {
        long value = seed ^ mix(networkSeed);
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellZ * 132897987541L;
        value ^= (long) axis * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private Edge edge(EdgeKey key) {
        Edge cached = edges.getIfPresent(key);
        if (cached != null) return cached;
        try {
            return edges.get(key, new Callable<Edge>() {

                @Override
                public Edge call() {
                    return createEdge(key);
                }
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create river edge", exception.getCause());
        }
    }

    private Edge createEdge(EdgeKey key) {
        Node start = node(key.first);
        Node end = node(key.second);
        double chordX = end.x - start.x;
        double chordZ = end.z - start.z;
        double length = Math.hypot(chordX, chordZ);
        double startDirectionX = start.directionX;
        double startDirectionZ = start.directionZ;
        if (startDirectionX * chordX + startDirectionZ * chordZ < 0D) {
            startDirectionX = -startDirectionX;
            startDirectionZ = -startDirectionZ;
        }
        double endDirectionX = end.directionX;
        double endDirectionZ = end.directionZ;
        if (endDirectionX * chordX + endDirectionZ * chordZ < 0D) {
            endDirectionX = -endDirectionX;
            endDirectionZ = -endDirectionZ;
        }
        double handle = length * .35D;
        ArrayList<Segment> guide = new ArrayList<Segment>();
        flatten(
                start.x,
                start.z,
                start.x + startDirectionX * handle,
                start.z + startDirectionZ * handle,
                end.x - endDirectionX * handle,
                end.z - endDirectionZ * handle,
                end.x,
                end.z,
                0,
                guide);
        Segment[] erosionSegments = coarseCubic(
                start.x,
                start.z,
                start.x + startDirectionX * handle,
                start.z + startDirectionZ * handle,
                end.x - endDirectionX * handle,
                end.z - endDirectionZ * handle,
                end.x,
                end.z);
        boolean carvesMountains = (mix(key.first.hashCode() ^ Long.rotateLeft(key.second.hashCode(), 17)) & 1L) == 0L;
        return new Edge(meander(key, guide), erosionSegments, carvesMountains);
    }

    /** Fixed-cost approximation of the pre-meander river guide used by broad erosion and canyons. */
    private static Segment[] coarseCubic(double x0, double z0, double x1, double z1, double x2, double z2, double x3,
            double z3) {
        Segment[] result = new Segment[EROSION_CURVE_SEGMENTS];
        double previousX = x0;
        double previousZ = z0;
        for (int index = 1; index <= EROSION_CURVE_SEGMENTS; index++) {
            double t = index / (double) EROSION_CURVE_SEGMENTS;
            double inverse = 1D - t;
            double x = inverse * inverse * inverse * x0 + 3D * inverse * inverse * t * x1
                    + 3D * inverse * t * t * x2
                    + t * t * t * x3;
            double z = inverse * inverse * inverse * z0 + 3D * inverse * inverse * t * z1
                    + 3D * inverse * t * t * z2
                    + t * t * t * z3;
            result[index - 1] = new Segment(previousX, previousZ, x, z);
            previousX = x;
            previousZ = z;
        }
        return result;
    }

    private Segment[] meander(EdgeKey key, List<Segment> guide) {
        double totalLength = 0D;
        for (Segment segment : guide) totalLength += Math.sqrt(segment.lengthSquared);
        if (totalLength == 0D) return guide.toArray(new Segment[guide.size()]);

        long phaseSeed = hash(key.first.networkSeed, key.first.cellX, key.first.cellZ, 11)
                ^ Long.rotateLeft(hash(key.second.networkSeed, key.second.cellX, key.second.cellZ, 17), 29);
        double initialPhase = unit(mix(phaseSeed)) * TAU;
        double[] mountainEnvelopes = mountainEnvelopes(guide, totalLength);
        double travelled = 0D;
        double previousX = guide.get(0).x;
        double previousZ = guide.get(0).z;
        ArrayList<Segment> result = new ArrayList<Segment>();

        for (Segment segment : guide) {
            double segmentLength = Math.sqrt(segment.lengthSquared);
            if (segmentLength == 0D) continue;
            int steps = Math.max(1, (int) Math.ceil(segmentLength / MEANDER_SAMPLE_SPACING));
            double normalX = -segment.dz / segmentLength;
            double normalZ = segment.dx / segmentLength;
            for (int step = 1; step <= steps; step++) {
                double along = step / (double) steps;
                double distance = travelled + segmentLength * along;
                double u = Math.min(1D, distance / totalLength);
                double envelope = Math.sin(Math.PI * u);
                envelope *= envelope;
                envelope *= interpolateMountainEnvelope(mountainEnvelopes, distance, totalLength);
                double phase = initialPhase + TAU * distance / MEANDER_WAVELENGTH;
                double lateralOffset = Math.sin(phase) * MEANDER_AMPLITUDE * envelope;
                double x = segment.x + segment.dx * along + normalX * lateralOffset;
                double z = segment.z + segment.dz * along + normalZ * lateralOffset;
                result.add(new Segment(previousX, previousZ, x, z));
                previousX = x;
                previousZ = z;
            }
            travelled += segmentLength;
        }
        return result.toArray(new Segment[result.size()]);
    }

    private double[] mountainEnvelopes(List<Segment> guide, double totalLength) {
        if (mountains == null) return new double[] { 1D };
        int intervals = Math.max(1, (int) Math.ceil(totalLength / MOUNTAIN_SAMPLE_SPACING));
        double[] result = new double[intervals + 1];
        int segmentIndex = 0;
        double segmentStart = 0D;
        for (int index = 0; index <= intervals; index++) {
            double distance = totalLength * index / intervals;
            Segment segment = guide.get(segmentIndex);
            double segmentLength = Math.sqrt(segment.lengthSquared);
            while (segmentIndex + 1 < guide.size() && distance > segmentStart + segmentLength) {
                segmentStart += segmentLength;
                segment = guide.get(++segmentIndex);
                segmentLength = Math.sqrt(segment.lengthSquared);
            }
            double along = segmentLength == 0D ? 0D : (distance - segmentStart) / segmentLength;
            int x = (int) Math.round(segment.x + segment.dx * along);
            int z = (int) Math.round(segment.z + segment.dz * along);
            double strength = mountains.distanceAt(x, z) / MOUNTAIN_STRAIGHTENING_DISTANCE;
            strength = Math.max(0D, Math.min(1D, strength));
            result[index] = strength * strength * (3D - 2D * strength);
        }
        return result;
    }

    private static double interpolateMountainEnvelope(double[] samples, double distance, double totalLength) {
        if (samples.length == 1) return samples[0];
        double position = distance / totalLength * (samples.length - 1);
        int first = Math.min(samples.length - 2, (int) position);
        double fraction = position - first;
        return samples[first] + (samples[first + 1] - samples[first]) * fraction;
    }

    private static void flatten(double x0, double z0, double x1, double z1, double x2, double z2, double x3, double z3,
            int depth, List<Segment> output) {
        if (depth >= MAXIMUM_SUBDIVISION_DEPTH
                || Math.max(lineDistanceSquared(x1, z1, x0, z0, x3, z3), lineDistanceSquared(x2, z2, x0, z0, x3, z3))
                        <= CURVE_TOLERANCE_SQUARED) {
            output.add(new Segment(x0, z0, x3, z3));
            return;
        }
        double x01 = (x0 + x1) * .5D;
        double z01 = (z0 + z1) * .5D;
        double x12 = (x1 + x2) * .5D;
        double z12 = (z1 + z2) * .5D;
        double x23 = (x2 + x3) * .5D;
        double z23 = (z2 + z3) * .5D;
        double x012 = (x01 + x12) * .5D;
        double z012 = (z01 + z12) * .5D;
        double x123 = (x12 + x23) * .5D;
        double z123 = (z12 + z23) * .5D;
        double centerX = (x012 + x123) * .5D;
        double centerZ = (z012 + z123) * .5D;
        flatten(x0, z0, x01, z01, x012, z012, centerX, centerZ, depth + 1, output);
        flatten(centerX, centerZ, x123, z123, x23, z23, x3, z3, depth + 1, output);
    }

    private static double lineDistanceSquared(double x, double z, double x0, double z0, double x1, double z1) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0D) return (x - x0) * (x - x0) + (z - z0) * (z - z0);
        double cross = (x - x0) * dz - (z - z0) * dx;
        return cross * cross / lengthSquared;
    }

    private static int floorDiv(int value, int divisor) {
        int quotient = value / divisor;
        return value < 0 && value % divisor != 0 ? quotient - 1 : quotient;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static final class Node {

        final NodeKey key;
        final double x;
        final double z;
        final double directionX;
        final double directionZ;
        final NodeKey[] links;

        Node(NodeKey key, double x, double z, double directionX, double directionZ, NodeKey[] links) {
            this.key = key;
            this.x = x;
            this.z = z;
            this.directionX = directionX;
            this.directionZ = directionZ;
            this.links = links;
        }
    }

    private static final class NodeKey implements Comparable<NodeKey> {

        final int cellX;
        final int cellZ;
        final long networkSeed;

        NodeKey(long networkSeed, int cellX, int cellZ) {
            this.networkSeed = networkSeed;
            this.cellX = cellX;
            this.cellZ = cellZ;
        }

        @Override
        public int compareTo(NodeKey other) {
            int comparison = Long.compare(networkSeed, other.networkSeed);
            if (comparison != 0) return comparison;
            comparison = Integer.compare(cellX, other.cellX);
            if (comparison != 0) return comparison;
            return Integer.compare(cellZ, other.cellZ);
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof NodeKey)) return false;
            NodeKey other = (NodeKey) value;
            return networkSeed == other.networkSeed && cellX == other.cellX && cellZ == other.cellZ;
        }

        @Override
        public int hashCode() {
            int hash = (int) (networkSeed ^ networkSeed >>> 32);
            hash = 31 * hash + cellX;
            return 31 * hash + cellZ;
        }
    }

    private static final class BucketKey {

        final long networkSeed;
        final int x;
        final int z;

        BucketKey(long networkSeed, int x, int z) {
            this.networkSeed = networkSeed;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof BucketKey)) return false;
            BucketKey other = (BucketKey) value;
            return networkSeed == other.networkSeed && x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            int hash = (int) (networkSeed ^ networkSeed >>> 32);
            hash = 31 * hash + x;
            return 31 * hash + z;
        }
    }

    private static final class BucketLookup {

        long networkSeed;
        int x;
        int z;
        RiverSegment[] segments;
    }

    private static final class JunctionBucketLookup {

        long networkSeed;
        int x;
        int z;
        Junction[] junctions;
    }

    private static final class ErosionRegionLookup {

        long networkSeed;
        int x;
        int z;
        ErosionCurve[] segments;
    }

    private static final class Junction {

        final double warpedCenterX;
        final double warpedCenterZ;

        Junction(double warpedCenterX, double warpedCenterZ) {
            this.warpedCenterX = warpedCenterX;
            this.warpedCenterZ = warpedCenterZ;
        }
    }

    private static final class EdgeKey {

        final NodeKey first;
        final NodeKey second;

        EdgeKey(NodeKey left, NodeKey right) {
            if (left.compareTo(right) <= 0) {
                first = left;
                second = right;
            } else {
                first = right;
                second = left;
            }
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof EdgeKey)) return false;
            EdgeKey other = (EdgeKey) value;
            return first.equals(other.first) && second.equals(other.second);
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }

    private static final class Edge {

        final Segment[] segments;
        final Segment[] erosionSegments;
        final boolean carvesMountains;

        Edge(Segment[] segments, Segment[] erosionSegments, boolean carvesMountains) {
            this.segments = segments;
            this.erosionSegments = erosionSegments;
            this.carvesMountains = carvesMountains;
        }
    }

    private static final class ErosionCurve {

        final Segment[] segments;
        final boolean carvesMountains;

        ErosionCurve(Segment[] segments, boolean carvesMountains) {
            this.segments = segments;
            this.carvesMountains = carvesMountains;
        }
    }

    private static final class RiverSegment {

        final Segment segment;
        final boolean carvesMountains;

        RiverSegment(Segment segment, boolean carvesMountains) {
            this.segment = segment;
            this.carvesMountains = carvesMountains;
        }
    }

    private static final class Segment {

        final double x;
        final double z;
        final double dx;
        final double dz;
        final double lengthSquared;
        final double minimumX;
        final double minimumZ;
        final double maximumX;
        final double maximumZ;

        Segment(double x0, double z0, double x1, double z1) {
            x = x0;
            z = z0;
            dx = x1 - x0;
            dz = z1 - z0;
            lengthSquared = dx * dx + dz * dz;
            minimumX = Math.min(x0, x1);
            minimumZ = Math.min(z0, z1);
            maximumX = Math.max(x0, x1);
            maximumZ = Math.max(z0, z1);
        }

        boolean intersects(double queryMinimumX, double queryMinimumZ, double queryMaximumX, double queryMaximumZ) {
            return maximumX >= queryMinimumX && minimumX <= queryMaximumX
                    && maximumZ >= queryMinimumZ
                    && minimumZ <= queryMaximumZ;
        }

        double distanceSquared(double pointX, double pointZ) {
            if (lengthSquared == 0D) return (pointX - x) * (pointX - x) + (pointZ - z) * (pointZ - z);
            double t = ((pointX - x) * dx + (pointZ - z) * dz) / lengthSquared;
            t = Math.max(0D, Math.min(1D, t));
            double offsetX = pointX - (x + t * dx);
            double offsetZ = pointZ - (z + t * dz);
            return offsetX * offsetX + offsetZ * offsetZ;
        }
    }
}
