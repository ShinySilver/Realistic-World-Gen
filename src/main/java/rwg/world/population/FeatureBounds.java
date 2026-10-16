package rwg.world.population;

/** Inclusive block bounds plus the legacy X/Z population origin used by the planned feature. */
public final class FeatureBounds {

    private final int originBlockX;
    private final int originBlockZ;
    public final int minimumX;
    public final int minimumY;
    public final int minimumZ;
    public final int maximumX;
    public final int maximumY;
    public final int maximumZ;

    public FeatureBounds(int originBlockX, int originBlockZ, int minimumX, int minimumY, int minimumZ, int maximumX,
            int maximumY, int maximumZ) {
        if (minimumX > maximumX || minimumY > maximumY || minimumZ > maximumZ)
            throw new IllegalArgumentException("inverted feature bounds");
        this.originBlockX = originBlockX;
        this.originBlockZ = originBlockZ;
        this.minimumX = minimumX;
        this.minimumY = minimumY;
        this.minimumZ = minimumZ;
        this.maximumX = maximumX;
        this.maximumY = maximumY;
        this.maximumZ = maximumZ;
    }

    public int originBlockX() {
        return originBlockX;
    }

    public int originBlockZ() {
        return originBlockZ;
    }

    public boolean intersectsCube(int cubeX, int cubeY, int cubeZ) {
        int minimumCubeX = cubeX << 4;
        int minimumCubeY = cubeY << 4;
        int minimumCubeZ = cubeZ << 4;
        return minimumX <= minimumCubeX + 15 && maximumX >= minimumCubeX
                && minimumY <= minimumCubeY + 15
                && maximumY >= minimumCubeY
                && minimumZ <= minimumCubeZ + 15
                && maximumZ >= minimumCubeZ;
    }

    FeatureBounds withVerticalRange(int minimumY, int maximumY) {
        return new FeatureBounds(
                originBlockX,
                originBlockZ,
                minimumX,
                minimumY,
                minimumZ,
                maximumX,
                maximumY,
                maximumZ);
    }
}
