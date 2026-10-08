package rwg.util;

/**
 * A seed-bound scalar field. Coordinates are already in the field's noise space; callers own world-to-noise scaling.
 * Implementations may override the grid fallback with vectorized or native batch generation.
 */
public interface NoiseField2D {

    float sample2D(double x, double z);

    /** Writes row-major values, advancing X within a row and Z between rows. */
    default void sampleGrid(float[] output, int offset, double startX, double startZ, int width, int depth,
            double stepX, double stepZ) {
        if (output == null) throw new NullPointerException("noise grid output");
        if (width < 0 || depth < 0 || offset < 0 || offset + width * depth > output.length)
            throw new IllegalArgumentException("invalid noise grid bounds");
        int index = offset;
        for (int z = 0; z < depth; z++) {
            double sampleZ = startZ + z * stepZ;
            for (int x = 0; x < width; x++) output[index++] = sample2D(startX + x * stepX, sampleZ);
        }
    }
}
