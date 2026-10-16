package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import rwg.world.CubicWorldSettings;

/** Mountain grass, rock, and elevation-driven snow with an optional beach material. */
public final class SurfaceMountainSnow extends SurfaceBase {

    private static final SurfaceMaterial GRASS = new SurfaceMaterial(Blocks.grass);
    private static final SurfaceMaterial STONE = new SurfaceMaterial(Blocks.stone);
    private static final SurfaceMaterial COBBLESTONE = new SurfaceMaterial(Blocks.cobblestone);
    private static final SurfaceMaterial CLAY = new SurfaceMaterial(Blocks.stained_hardened_clay, 9);
    private static final SurfaceMaterial SNOW = new SurfaceMaterial(Blocks.snow);

    private final SurfaceMaterial beach;
    private final float minimumCliff;
    private final float stoneCliff;
    private final float stoneHeight;
    private final float stoneStrength;
    private final float snowCliff;
    private final float snowHeight;
    private final float snowStrength;
    private final float clayCliff;

    public SurfaceMountainSnow(Block top, Block fill, boolean generateBeach, Block beachBlock, float minimumCliff) {
        this(top, fill, generateBeach, beachBlock, minimumCliff, 1.5f, 60f, 65f, .3f, 100f, 50f, 1.5f);
    }

    public SurfaceMountainSnow(Block top, Block fill, boolean generateBeach, Block beachBlock, float minimumCliff,
            float stoneCliff, float stoneHeight, float stoneStrength, float snowCliff, float snowHeight,
            float snowStrength, float clayCliff) {
        super(top, fill);
        beach = generateBeach && beachBlock != null ? new SurfaceMaterial(beachBlock) : null;
        this.minimumCliff = minimumCliff;
        this.stoneCliff = stoneCliff;
        this.stoneHeight = stoneHeight;
        this.stoneStrength = stoneStrength;
        this.snowCliff = snowCliff;
        this.snowHeight = snowHeight;
        this.snowStrength = snowStrength;
        this.clayCliff = clayCliff;
    }

    @Override
    public int maximumDepth() {
        return 5;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (surfaceY > 110 + variation * 2f
                && slope < snowCliff + (surfaceY - snowHeight) / snowStrength + variation * .5f)
            return SNOW;
        if (slope > clayCliff) return CLAY;
        if (slope > minimumCliff && slope > stoneCliff - (surfaceY - stoneHeight) / stoneStrength + variation * .5f)
            return depth == 0 && ((worldX * 73428767 ^ worldZ * 912931) & 3) == 0 ? COBBLESTONE : STONE;
        if (beach != null && surfaceY < CubicWorldSettings.SEA_LEVEL) return beach;
        return depth == 0 ? GRASS : fillerMaterial;
    }
}
