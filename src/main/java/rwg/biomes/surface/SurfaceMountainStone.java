package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import rwg.world.CubicWorldSettings;

/** Grass or biome material on gentle terrain, with beaches and exposed mountain rock. */
public class SurfaceMountainStone extends SurfaceBase {

    private static final SurfaceMaterial STONE = new SurfaceMaterial(Blocks.stone);
    private static final SurfaceMaterial COBBLESTONE = new SurfaceMaterial(Blocks.cobblestone);
    private static final SurfaceMaterial CLAY = new SurfaceMaterial(Blocks.stained_hardened_clay, 9);

    private final SurfaceMaterial beach;
    private final float minimumCliff;
    private final float stoneCliff;
    private final float stoneHeight;
    private final float stoneStrength;
    private final float clayCliff;
    private final SurfaceMaterial[] topMaterials = new SurfaceMaterial[16];
    public byte topByte;

    public SurfaceMountainStone(Block top, Block fill, boolean generateBeach, Block beachBlock, float minimumCliff) {
        this(top, fill, generateBeach, beachBlock, minimumCliff, 1.5f, 60f, 65f, 1.5f);
    }

    public SurfaceMountainStone(Block top, Block fill, boolean generateBeach, Block beachBlock, float minimumCliff,
            float stoneCliff, float stoneHeight, float stoneStrength, float clayCliff) {
        super(top, fill);
        beach = generateBeach && beachBlock != null ? new SurfaceMaterial(beachBlock) : null;
        this.minimumCliff = minimumCliff;
        this.stoneCliff = stoneCliff;
        this.stoneHeight = stoneHeight;
        this.stoneStrength = stoneStrength;
        this.clayCliff = clayCliff;
        for (int metadata = 0; metadata < topMaterials.length; metadata++)
            topMaterials[metadata] = new SurfaceMaterial(top, metadata);
    }

    @Override
    public int maximumDepth() {
        return 5;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > clayCliff) return CLAY;
        if (slope > minimumCliff && slope > stoneCliff - (surfaceY - stoneHeight) / stoneStrength + variation * .5f)
            return depth == 0 && ((worldX * 73428767 ^ worldZ * 912931) & 3) == 0 ? COBBLESTONE : STONE;
        if (beach != null && surfaceY < CubicWorldSettings.SEA_LEVEL) return beach;
        return depth == 0 ? topMaterials[topByte & 15] : fillerMaterial;
    }
}
