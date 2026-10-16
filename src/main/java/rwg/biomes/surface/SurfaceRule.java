package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Immutable, height-independent material rule for one terrain column. */
public interface SurfaceRule {

    /** Deepest block below the surface that this rule may replace. */
    int maximumDepth();

    /** Selects the material at an absolute surface-relative depth without reading the world. */
    Block blockAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength, float mountainWeight);
}
