package rwg.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Temporary refactor guard. Remove with the other migration-only debug/verification tooling. */
public final class TemporaryNoiseVerification {

    private static final long SEED = 0x71C3E2A94D568B0FL;
    private static final int SCALAR_SAMPLES = 4096;
    private static final int POINT_SAMPLES = 512;

    private TemporaryNoiseVerification() {}

    public static void main(String[] args) throws Exception {
        verify("perlin", 0x01CD8D02CFC91947L, SCALAR_SAMPLES, new SamplerFactory() {

            @Override
            public Sampler create() {
                return scalar(new PerlinNoise(SEED));
            }
        });
        verify("open-simplex", 0xC9FAC0D00EC6F9AFL, SCALAR_SAMPLES, new SamplerFactory() {

            @Override
            public Sampler create() {
                return scalar(new OpenSimplexNoise(SEED));
            }
        });
        verify("cell", 0x8A4B1BE49DA244BCL, SCALAR_SAMPLES, new SamplerFactory() {

            @Override
            public Sampler create() {
                return cell(new CellNoise(SEED, (short) 0, true));
            }
        });
        verify("poisson-points", 0xFA78FB09B53F0E6DL, POINT_SAMPLES, new SamplerFactory() {

            @Override
            public Sampler create() {
                return points(new PoissonPointNoise(SEED, 1020D, 4));
            }
        });
        verify("continental", 0xCD8EF553D390A11EL, POINT_SAMPLES, new SamplerFactory() {

            @Override
            public Sampler create() {
                return continents(new ContinentalNoise(SEED));
            }
        });
        verifyScalarGrid(new PerlinNoise(SEED));
        verifyScalarGrid(new OpenSimplexNoise(SEED));
        CellNoise gridCell = new CellNoise(SEED, (short) 0, true);
        verifyScalarGrid(gridCell);
        verifyPointGrid(new PoissonPointNoise(SEED, 1020D, 4));
    }

    private static void verifyScalarGrid(NoiseField2D noise) {
        int width = 19;
        int depth = 13;
        float[] grid = new float[width * depth + 7];
        noise.sampleGrid(grid, 7, -193.25D, 827.75D, width, depth, .375D, -.625D);
        int index = 7;
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                float scalar = noise.sample2D(-193.25D + x * .375D, 827.75D + z * -.625D);
                if (Float.floatToRawIntBits(grid[index++]) != Float.floatToRawIntBits(scalar))
                    throw new IllegalStateException(noise.getClass().getSimpleName() + " grid differs from scalar");
            }
        }
    }

    private static void verifyPointGrid(PointField2D noise) {
        int width = 11;
        int depth = 7;
        int size = noise.sampleSize();
        double[] grid = new double[width * depth * size + 3];
        double[] scalar = new double[size];
        noise.sampleGrid(grid, 3, -4096D, 8192D, width, depth, 127D, -83D);
        int index = 3;
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                noise.sample(-4096D + x * 127D, 8192D + z * -83D, scalar);
                for (int field = 0; field < size; field++) {
                    if (Double.doubleToLongBits(grid[index++]) != Double.doubleToLongBits(scalar[field]))
                        throw new IllegalStateException("point grid differs from scalar at field " + field);
                }
            }
        }
        System.out.println("scalar/grid fallback equivalence verified");
    }

    private static void verify(String name, long golden, int count, SamplerFactory factory) throws Exception {
        long serial = fingerprint(sampleSerial(count, factory.create()));
        long reverse = fingerprint(sampleReverse(count, factory.create()));
        long parallel = fingerprint(sampleParallel(count, factory.create()));
        if (serial != reverse || serial != parallel) {
            throw new IllegalStateException(
                    name + " depends on traversal/threading: serial="
                            + hex(serial)
                            + ", reverse="
                            + hex(reverse)
                            + ", parallel="
                            + hex(parallel));
        }
        if (golden != 0L && serial != golden)
            throw new IllegalStateException(name + " changed: expected " + hex(golden) + " but got " + hex(serial));
        System.out.println(name + " noise verified: " + hex(serial));
    }

    private static long[] sampleSerial(int count, Sampler sampler) {
        long[] values = new long[count];
        for (int index = 0; index < count; index++) values[index] = sampler.sample(index);
        return values;
    }

    private static long[] sampleReverse(int count, Sampler sampler) {
        long[] values = new long[count];
        for (int index = count - 1; index >= 0; index--) values[index] = sampler.sample(index);
        return values;
    }

    private static long[] sampleParallel(final int count, final Sampler sampler) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Value>> calls = new ArrayList<Callable<Value>>(count);
            for (int index = 0; index < count; index++) {
                final int sampleIndex = index;
                calls.add(new Callable<Value>() {

                    @Override
                    public Value call() {
                        return new Value(sampleIndex, sampler.sample(sampleIndex));
                    }
                });
            }
            // A fixed shuffle also verifies that scheduling order cannot affect values.
            Collections.shuffle(calls, new java.util.Random(0x5EEDL));
            long[] values = new long[count];
            for (Future<Value> future : workers.invokeAll(calls)) {
                Value value = future.get();
                values[value.index] = value.value;
            }
            return values;
        } finally {
            workers.shutdownNow();
        }
    }

    private static Sampler scalar(final NoiseGenerator noise) {
        return new Sampler() {

            @Override
            public long sample(int index) {
                float x = x(index) / 97f;
                float z = z(index) / 113f;
                long hash = bits(noise.noise1(x));
                hash = mix(hash, bits(noise.noise2(x, z)));
                hash = mix(hash, bits(noise.noise3(x, z, (x - z) * .37f)));
                return mix(hash, Double.doubleToLongBits(noise.improvedNoise(x, z, x + z)));
            }
        };
    }

    private static Sampler cell(final CellNoise noise) {
        return new Sampler() {

            @Override
            public long sample(int index) {
                double x = x(index) / 211D;
                double z = z(index) / 197D;
                long hash = bits(noise.noise(x, z, .73D));
                hash = mix(hash, bits(noise.border(x, z, .041D, 1f)));
                hash = mix(hash, bits(noise.junction(x, z, .027D, 1f)));
                return mix(hash, Double.doubleToLongBits(noise.noise(x, x - z, z, .31D)));
            }
        };
    }

    private static Sampler points(final PoissonPointNoise noise) {
        return new Sampler() {

            @Override
            public long sample(int index) {
                double[] output = new double[10];
                noise.sampleTwo(x(index), z(index), output);
                long hash = 0xCBF29CE484222325L;
                for (double value : output) hash = mix(hash, Double.doubleToLongBits(value));
                return hash;
            }
        };
    }

    private static Sampler continents(final ContinentalNoise noise) {
        return new Sampler() {

            @Override
            public long sample(int index) {
                int x = x(index);
                int z = z(index);
                long hash = bits(noise.getValue(x, z));
                hash = mix(hash, noise.getIslandSeedCoordinates(x, z));
                return hash;
            }
        };
    }

    private static int x(int index) {
        return (index * 1103515245 + 12345) ^ index << 11;
    }

    private static int z(int index) {
        return (index * 214013 + 2531011) ^ index << 17;
    }

    private static long fingerprint(long[] values) {
        long hash = 0xCBF29CE484222325L;
        for (long value : values) hash = mix(hash, value);
        return hash;
    }

    private static long bits(float value) {
        return Float.floatToRawIntBits(value) & 0xffffffffL;
    }

    private static long mix(long hash, long value) {
        hash ^= value;
        return hash * 0x100000001B3L;
    }

    private static String hex(long value) {
        return String.format("0x%016X", value);
    }

    private interface Sampler {

        long sample(int index);
    }

    private interface SamplerFactory {

        Sampler create();
    }

    private static final class Value {

        final int index;
        final long value;

        Value(int index, long value) {
            this.index = index;
            this.value = value;
        }
    }
}
