package rwg.world;

import java.io.File;

import rwg.config.ConfigRWG;
import rwg.util.ContinentalNoise;

/** Headless sampling utility for calibrating the approximate maximum-ocean setting. */
public final class OceanCoverageCalibration {

    private static final long SEED = 927692613931855800L;

    private OceanCoverageCalibration() {}

    public static void main(String[] args) {
        File config = new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg"));
        int size = Integer.getInteger("rwg.calibrationSize", 16384);
        int step = Integer.getInteger("rwg.calibrationStep", 16);
        String targets = System.getProperty("rwg.oceanTargets", "1.0,0.5,0.4,0.3");
        ConfigRWG.init(config);
        System.out.println("RWG ocean calibration config: " + config.getAbsolutePath());
        for (String text : targets.split(",")) {
            float target = Float.parseFloat(text.trim());
            ConfigRWG.maximumOceanFraction = target;
            ContinentalNoise noise = new ContinentalNoise(SEED);
            long ocean = 0L;
            long total = 0L;
            int half = size / 2;
            for (int z = -half; z < half; z += step) {
                for (int x = -half; x < half; x += step) {
                    if (noise.getValue(x, z) < 0f) ocean++;
                    total++;
                }
            }
            System.out.printf("target %.3f -> sampled ocean %.4f (%d points)%n", target, ocean / (double) total, total);
        }
    }
}
