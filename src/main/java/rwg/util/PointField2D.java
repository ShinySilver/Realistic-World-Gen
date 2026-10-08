package rwg.util;

/**
 * A structured point field with implementation-defined, fixed-width output records. The flat buffer contract avoids
 * per-sample allocation and permits optimized rectangular implementations later.
 */
public interface PointField2D {

    int sampleSize();

    void sample(double x, double z, double[] output);

    /** Writes row-major records of {@link #sampleSize()} doubles each. */
    default void sampleGrid(double[] output, int offset, double startX, double startZ, int width, int depth,
            double stepX, double stepZ) {
        int sampleSize = sampleSize();
        if (output == null) throw new NullPointerException("point grid output");
        if (sampleSize < 1 || width < 0
                || depth < 0
                || offset < 0
                || offset + width * depth * sampleSize > output.length)
            throw new IllegalArgumentException("invalid point grid bounds");
        double[] sample = new double[sampleSize];
        int index = offset;
        for (int z = 0; z < depth; z++) {
            double sampleZ = startZ + z * stepZ;
            for (int x = 0; x < width; x++) {
                sample(startX + x * stepX, sampleZ, sample);
                System.arraycopy(sample, 0, output, index, sampleSize);
                index += sampleSize;
            }
        }
    }
}
