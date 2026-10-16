package rwg.world;

import java.io.File;
import java.util.Arrays;

import net.minecraft.init.Blocks;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.BiomeRegistryBootstrap;
import rwg.world.sample.BiomeBlendSample;

/** Focused checks for cubic classification, vertical limits, and the simple milestone-four material rules. */
public final class CubicTerrainPipelineVerification {

    private CubicTerrainPipelineVerification() {}

    public static void main(String[] arguments) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = BiomeRegistryBootstrap.create(false);
        BiomeRegistration registration = registry.registrations().get(0);
        // Runtime construction is covered by the in-game smoke test because the pipeline now owns world-bound carvers.

        ColumnPlan ordinary = constantPlan(100f, registration);
        require(kind(ordinary, 7) == CubeTerrainPipeline.GenerationKind.EMPTY, "high cube must remain empty");
        require(
                kind(ordinary, 0) == CubeTerrainPipeline.GenerationKind.DETAILED,
                "potential carver cube bypassed detailed generation");
        require(kind(ordinary, -20) == CubeTerrainPipeline.GenerationKind.SOLID, "deep safe cube lost solid path");
        require(kind(ordinary, 6) == CubeTerrainPipeline.GenerationKind.DETAILED, "surface cube must be detailed");

        ColumnPlan ocean = constantPlan(20f, registration);
        require(kind(ocean, 2) == CubeTerrainPipeline.GenerationKind.WATER, "ocean cube must use water path");
        require(CubeTerrainPipeline.baseBlock(20, 21) == Blocks.water, "sea-level gap must contain water");
        require(
                CubeTerrainPipeline.baseBlock(20, CubicWorldSettings.SEA_LEVEL) == Blocks.air,
                "water exceeded sea level");
        require(
                registration.surfaceRules[0].materialAt(0, 0, 100, 0, 0f, 0f, 0f, 0f, 0f).block
                        == registration.biome.topBlock,
                "resolved surface rule did not provide the biome top block");
        require(
                ordinary.maximumSurfaceDepth() == registration.surfaceRules[0].maximumDepth(),
                "column plan did not retain the declared surface depth");

        ColumnPlan negative = constantPlan(-10f, registration);
        require(kind(negative, -1) == CubeTerrainPipeline.GenerationKind.DETAILED, "negative surface was clipped");
        require(kind(ordinary, -64) == CubeTerrainPipeline.GenerationKind.DETAILED, "bedrock cube bypassed stages");
        require(
                kind(ordinary, -65) == CubeTerrainPipeline.GenerationKind.EMPTY,
                "cube below generation range survived");
        require(
                CubeTerrainPipeline.isBedrockY(CubicWorldSettings.MIN_GENERATION_Y),
                "bedrock is not anchored to minimum generation Y");
        require(!CubeTerrainPipeline.isBedrockY(0), "bedrock remained anchored to vanilla Y=0");

        ColumnPlan tall = constantPlan(300f, registration);
        require(kind(tall, 18) == CubeTerrainPipeline.GenerationKind.DETAILED, "surface above Y=255 was clipped");
        require(kind(tall, 20) == CubeTerrainPipeline.GenerationKind.EMPTY, "air above tall mountain was not empty");
        System.out.println("RWG cubic terrain pipeline verification passed");
    }

    private static CubeTerrainPipeline.GenerationKind kind(ColumnPlan plan, int cubeY) {
        return CubeTerrainPipeline.classify(plan, cubeY);
    }

    private static ColumnPlan constantPlan(float height, BiomeRegistration registration) {
        float[] heights = new float[ColumnPlan.COLUMN_COUNT];
        float[] baseHeights = new float[ColumnPlan.COLUMN_COUNT];
        float[] rivers = new float[ColumnPlan.COLUMN_COUNT];
        float[] mountains = new float[ColumnPlan.COLUMN_COUNT];
        float[] slopes = new float[ColumnPlan.COLUMN_COUNT];
        float[] undergroundRivers = new float[ColumnPlan.COLUMN_COUNT];
        float[] riverJunctions = new float[ColumnPlan.COLUMN_COUNT];
        float[] variations = new float[ColumnPlan.COLUMN_COUNT];
        float[] broadVariations = new float[ColumnPlan.COLUMN_COUNT];
        BiomeRegistration[] registrations = new BiomeRegistration[ColumnPlan.COLUMN_COUNT];
        Arrays.fill(heights, height);
        Arrays.fill(baseHeights, height);
        Arrays.fill(registrations, registration);
        return new ColumnPlan(
                heights,
                baseHeights,
                registrations,
                rivers,
                mountains,
                undergroundRivers,
                riverJunctions,
                slopes,
                variations,
                broadVariations,
                new BiomeBlendSample(new BiomeRegistration[] { registration }, new float[] { 1f }));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
