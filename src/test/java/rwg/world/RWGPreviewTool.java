package rwg.world;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import com.google.common.base.Optional;
import rwg.biomes.base.BaseBiomes;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.biomes.realistic.ocean.RealisticBiomeOcean;
import rwg.config.ConfigRWG;
import rwg.support.Support;
import rwg.support.SupportBOP;
import rwg.support.SupportEBXL;
import rwg.support.RealisticBiomeSupport;

/** Manual visual test: {@code ./gradlew continentDebug}. */
public final class RWGPreviewTool {

    private static final int SIZE = 16000;
    private static final int BLOCKS_PER_PIXEL = 8;
    private static final int RESOLUTION = SIZE / BLOCKS_PER_PIXEL;
    private static final int PIXELS_PER_CHUNK = 16 / BLOCKS_PER_PIXEL;
    private static final int CHUNKS = SIZE / 16;
    private static final int GRID = 4;
    private static final int WORKERS = GRID * GRID;
    private static final long SEED = 927692613931855800L;
    private static final float CLIMATE_WIDTH = 2400f;
    private static final float BIOME_WIDTH = 900f;
    private static final int[] CATEGORY_COLORS = { 0x287EB3, 0x91B69C, 0x447A52, 0xD2B44B, 0x3C9166, 0x916FA3 };
    private static int[] biomeCategories;
    private static final List<BiomeGenBase> ADDON_BIOMES = new ArrayList<BiomeGenBase>();
    private static final Map<BiomeGenBase, String> ADDON_SOURCES = new IdentityHashMap<BiomeGenBase, String>();

    public static void main(String[] args) {
        BaseBiomes.load();
        Support.init(false);
        ConfigRWG.maximumContinentWidth *= 3f;
        ConfigRWG.minimumContinentWidth = ConfigRWG.maximumContinentWidth;
        ConfigRWG.minimumIslandWidth *= 2f;
        ConfigRWG.maximumIslandWidth *= 2f;
        initAddonStubs();
        SupportBOP.init();
        SupportEBXL.init();
        open();
    }

    private static void initAddonStubs() {
        try {
            for (Field field : Class.forName("biomesoplenty.api.content.BOPCBiomes").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == BiomeGenBase.class) {
                    field.set(null, debugBiome(field.getName(), "BOP"));
                }
            }
            for (Field field : Class.forName("biomesoplenty.api.content.BOPCBlocks").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Block.class) {
                    field.set(null, field.getName().contains("ash") ? Blocks.sand : Blocks.stone);
                }
            }
            for (Field field : Class.forName("extrabiomes.api.BiomeManager").getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == Optional.class) {
                    field.set(null, Optional.of(debugBiome(field.getName(), "ExtrabiomesXL")));
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not create offline BOP/EBXL biome stubs", exception);
        }
    }

    private static BiomeGenBase debugBiome(String fieldName, String source) {
        String lower = fieldName.toLowerCase();
        float temperature = lower.matches(".*(alps|arctic|boreal|frost|glacier|ice|snow|taiga|tundra).*") ? .2f
                : lower.matches(".*(bamboo|bayou|desert|jungle|lush|oasis|outback|rain|savanna|tropic|volcano).*")
                        ? 1.2f
                        : .7f;
        BiomeGenBase biome = new DebugBiome(nextBiomeId())
                .setBiomeName(prettyName(fieldName))
                .setTemperatureRainfall(temperature, .5f);
        ADDON_BIOMES.add(biome);
        ADDON_SOURCES.put(biome, source);
        return biome;
    }

    private static int nextBiomeId() {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        for (int id = biomes.length - 1; id >= 0; id--) if (biomes[id] == null) return id;
        throw new IllegalStateException("No free biome ID for offline addon stub");
    }

    private static String prettyName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).replaceAll("([A-Z])", " $1");
    }

    private static final class DebugBiome extends BiomeGenBase {

        private DebugBiome(int id) {
            super(id);
        }
    }

    public static void open() {
        ChunkManagerRealistic categoryManager =
                new ChunkManagerRealistic(SEED, true, CLIMATE_WIDTH, BIOME_WIDTH);
        List<RealisticBiomeBase> configuredBiomes = categoryManager.getConfiguredBiomes();
        biomeCategories = categoryManager.getConfiguredBiomeCategories();
        float[] height = new float[RESOLUTION * RESOLUTION];
        byte[] biomes = new byte[RESOLUTION * RESOLUTION];
        BufferedImage image = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
        CompletableFuture<?>[] jobs = new CompletableFuture[WORKERS];

        try {
            for (int tile = 0; tile < WORKERS; tile++) {
                final int tx = tile % GRID, tz = tile / GRID;
                jobs[tile] = CompletableFuture.runAsync(() -> {
                    ChunkManagerRealistic manager =
                            new ChunkManagerRealistic(SEED, true, CLIMATE_WIDTH, BIOME_WIDTH);
                    ChunkGeneratorRealistic generator = new ChunkGeneratorRealistic(manager, SEED, true);
                    RealisticBiomeBase[] chunkBiomes = new RealisticBiomeBase[256];
                    for (int cz = tz * CHUNKS / GRID; cz < (tz + 1) * CHUNKS / GRID; cz++) {
                        for (int cx = tx * CHUNKS / GRID; cx < (tx + 1) * CHUNKS / GRID; cx++) {
                            float[] chunkHeight = generator
                                    .getNewNoise(manager, cx * 16 - SIZE / 2, cz * 16 - SIZE / 2, chunkBiomes);
                            for (int z = 0; z < 16; z += BLOCKS_PER_PIXEL) {
                                int target = (cz * PIXELS_PER_CHUNK + z / BLOCKS_PER_PIXEL) * RESOLUTION
                                        + cx * PIXELS_PER_CHUNK;
                                for (int x = 0; x < 16; x += BLOCKS_PER_PIXEL) {
                                    height[target + x / BLOCKS_PER_PIXEL] = chunkHeight[x * 16 + z];
                                    biomes[target + x / BLOCKS_PER_PIXEL] = (byte) chunkBiomes[z * 16 + x].biomeID;
                                }
                            }
                        }
                    }
                }, workers);
            }
            CompletableFuture.allOf(jobs).join();
            for (int tile = 0; tile < WORKERS; tile++) {
                final int tx = tile % GRID, tz = tile / GRID;
                jobs[tile] = CompletableFuture.runAsync(
                        () -> lightTile(
                                height,
                                biomes,
                                pixels,
                                tx * RESOLUTION / GRID,
                                (tx + 1) * RESOLUTION / GRID,
                                tz * RESOLUTION / GRID,
                                (tz + 1) * RESOLUTION / GRID),
                        workers);
            }
            CompletableFuture.allOf(jobs).join();
        } finally {
            workers.shutdownNow();
        }

        JFrame frame = new JFrame("RWG_CONTINENT — seed " + SEED);
        JPanel root = new JPanel(new BorderLayout());
        HighlightState highlight = new HighlightState();
        root.add(new MapPanel(image, height, biomes, highlight));
        root.add(createSidebar(biomes, configuredBiomes, highlight), BorderLayout.EAST);
        frame.add(root);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.getRootPane()
                .registerKeyboardAction(
                        event -> frame.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                System.exit(0);
            }
        });
    }

    private static JPanel createSidebar(
            byte[] biomes, List<RealisticBiomeBase> configuredBiomes, HighlightState highlight) {
        int[] counts = new int[256];
        for (byte biome : biomes) counts[biome & 255]++;
        List<Integer> ids = new ArrayList<Integer>();
        boolean[] listed = new boolean[256];
        for (int id = 0; id < counts.length; id++) {
            if (counts[id] > 0) {
                ids.add(id);
                listed[id] = true;
            }
        }
        for (RealisticBiomeBase biome : configuredBiomes) {
            if (biome != null && !listed[biome.biomeID]) {
                ids.add(biome.biomeID);
                listed[biome.biomeID] = true;
            }
        }
        Collections.sort(ids, (a, b) -> {
            int category = displayCategory(a) - displayCategory(b);
            return category != 0 ? category : Integer.compare(counts[b], counts[a]);
        });
        Set<BiomeGenBase> supported = new HashSet<BiomeGenBase>();
        for (RealisticBiomeBase biome : configuredBiomes) if (biome != null) supported.add(biome.baseBiome);
        List<BiomeGenBase> missing = new ArrayList<BiomeGenBase>();
        for (BiomeGenBase biome : ADDON_BIOMES) if (!supported.contains(biome)) missing.add(biome);
        Collections.sort(missing, (a, b) -> a.biomeName.compareToIgnoreCase(b.biomeName));

        JPanel entries = new JPanel();
        entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
        String[] categoryNames = { "Water", "Snow", "Cold", "Hot", "Wet", "Small" };
        int[] categoryCounts = new int[categoryNames.length];
        for (int id = 0; id < counts.length; id++) {
            if (counts[id] > 0) categoryCounts[displayCategory(id)] += counts[id];
        }
        for (int displayCategory = 0; displayCategory < categoryNames.length; displayCategory++) {
            JLabel header = new JLabel(String.format(
                    "  %.2f%% - %s", categoryCounts[displayCategory] * 100d / biomes.length,
                    categoryNames[displayCategory]));
            header.setAlignmentX(0f);
            entries.add(header);
            for (int id : ids) {
                if (displayCategory(id) != displayCategory) continue;
                RealisticBiomeBase biome = RealisticBiomeBase.getBiome(id);
                double percent = counts[id] * 100d / biomes.length;
                String name = biomeName(biome);
                addSidebarEntry(
                        entries,
                        String.format("%.2f%% - %s", percent, name),
                        displayCategory == 0 ? 0x287EB3 : biomeColor(id),
                        id,
                        highlight);
            }
            for (BiomeGenBase biome : missing) {
                if (addonCategory(biome) == displayCategory) {
                    int color = CATEGORY_COLORS[displayCategory];
                    addSidebarEntry(
                            entries,
                            "N/A - " + biome.biomeName + sourceSuffix(biome),
                            shade(color, .75f),
                            -1,
                            highlight);
                }
            }
        }

        JScrollPane content = new JScrollPane(entries);
        content.setPreferredSize(new Dimension(420, 1));
        JPanel sidebar = new JPanel(new BorderLayout());
        JButton toggle = new JButton("▶");
        toggle.addActionListener(event -> {
            content.setVisible(!content.isVisible());
            toggle.setText(content.isVisible() ? "▶" : "◀");
            sidebar.revalidate();
        });
        sidebar.add(toggle, BorderLayout.WEST);
        sidebar.add(content);
        return sidebar;
    }

    private static void addSidebarEntry(
            JPanel entries, String text, int color, int biomeId, HighlightState highlight) {
        JLabel entry = new JLabel("  " + text + "  ");
        entry.setOpaque(true);
        entry.setBackground(new Color(color));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, entry.getPreferredSize().height + 6));
        entry.setAlignmentX(0f);
        if (biomeId >= 0) {
            entry.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent event) {
                    highlight.set(biomeId);
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    highlight.set(-1);
                }
            });
        }
        entries.add(entry);
    }

    private static String sourceSuffix(BiomeGenBase biome) {
        String source = ADDON_SOURCES.get(biome);
        return source == null ? "" : " (" + source + ")";
    }

    private static String biomeName(RealisticBiomeBase biome) {
        if (biome instanceof RealisticBiomeSupport) return biome.baseBiome.biomeName + sourceSuffix(biome.baseBiome);
        if (biome instanceof RealisticBiomeOcean) {
            String source = sourceSuffix(biome.baseBiome);
            return biome.baseBiome.biomeName
                    + (source.isEmpty() ? " (" + ((RealisticBiomeOcean) biome).getVariantName() + ")" : source);
        }
        return biome.baseBiome.biomeName + " (" + biome.getClass().getSimpleName() + ")";
    }

    private static int addonCategory(BiomeGenBase biome) {
        String name = biome.biomeName.toLowerCase();
        if (name.matches(".*(coral|kelp|ocean|river).*") ) return 0;
        if (name.matches(".*(alps|arctic|frost|glacier|ice|snow).*") ) return 1;
        if (name.matches(".*(bayou|bog|fen|lush|marsh|moor|rain|swamp|wetland).*") ) return 4;
        if (name.matches(".*(island|oasis|volcano).*") ) return 5;
        return biome.temperature < .5f ? 2 : 3;
    }

    private static int displayCategory(int id) {
        return biomeCategories[id];
    }

    private static void lightTile(float[] height, byte[] biomes, int[] pixels, int x0, int x1, int z0, int z1) {
        for (int z = z0; z < z1; z++) {
            for (int x = x0; x < x1; x++) {
                int i = z * RESOLUTION + x;
                float h = height[i];
                int radius = 3;
                float dx = height[z * RESOLUTION + Math.min(x + radius, RESOLUTION - 1)]
                        - height[z * RESOLUTION + Math.max(x - radius, 0)];
                float dz = height[Math.min(z + radius, RESOLUTION - 1) * RESOLUTION + x]
                        - height[Math.max(z - radius, 0) * RESOLUTION + x];
                float light = .96f + .08f * (12f + dx + dz) / (float) Math.sqrt(dx * dx + dz * dz + 432f);
                if (h >= 63f) light += clamp((h - 63f) / 120f, 0f, 1f) * .14f;
                int id = biomes[i] & 255;
                int color = h < 63f ? mix(0x071F4A, 0x4DA6D8, clamp((h - 30f) / 33f, 0f, 1f)) : biomeColor(id);
                pixels[i] = shade(color, clamp(light, .88f, 1.18f));
            }
        }
    }

    private static int biomeColor(int id) {
        return shade(CATEGORY_COLORS[displayCategory(id)], .92f + (id % 5) * .02f);
    }

    private static String categoryName(float height, int id) {
        return new String[] { "Water", "Snow", "Cold", "Hot", "Wet", "Small" }[displayCategory(id)];
    }

    private static int shade(int rgb, float amount) {
        return Math.min(255, (int) (((rgb >> 16) & 255) * amount)) << 16
                | Math.min(255, (int) (((rgb >> 8) & 255) * amount)) << 8
                | Math.min(255, (int) ((rgb & 255) * amount));
    }

    private static int mix(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = (int) (((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = (int) ((from & 255) * (1 - amount) + (to & 255) * amount);
        return r << 16 | g << 8 | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class HighlightState {

        private MapPanel map;

        private void set(int biomeId) {
            if (map != null) map.setHighlightedBiome(biomeId);
        }
    }

    private static final class MapPanel extends JPanel {

        private final BufferedImage image;
        private final float[] height;
        private final byte[] biomes;
        private final HighlightState highlight;
        private final BufferedImage hatch = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_ARGB);
        private final int[] hatchPixels = ((DataBufferInt) hatch.getRaster().getDataBuffer()).getData();
        private double zoom;
        private double x;
        private double y;
        private Point drag;
        private int hoverX = -1;
        private int hoverZ;
        private int highlightedBiome = -1;

        private MapPanel(BufferedImage image, float[] height, byte[] biomes, HighlightState highlight) {
            this.image = image;
            this.height = height;
            this.biomes = biomes;
            this.highlight = highlight;
            highlight.map = this;
            MouseAdapter mouse = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) drag = event.getPoint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    drag = null;
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (drag == null) return;
                    x += event.getX() - drag.x;
                    y += event.getY() - drag.y;
                    drag = event.getPoint();
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    hoverX = (int) ((event.getX() - x) / zoom);
                    hoverZ = (int) ((event.getY() - y) / zoom);
                    if (hoverX < 0 || hoverX >= RESOLUTION || hoverZ < 0 || hoverZ >= RESOLUTION) hoverX = -1;
                    highlight.set(hoverX < 0 ? -1 : biomes[hoverZ * RESOLUTION + hoverX] & 255);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hoverX = -1;
                    highlight.set(-1);
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    double factor = Math.pow(1.15, -event.getPreciseWheelRotation());
                    x = event.getX() - (event.getX() - x) * factor;
                    y = event.getY() - (event.getY() - y) * factor;
                    zoom *= factor;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent event) {
                    zoom = 0;
                    repaint();
                }
            });
        }

        private void setHighlightedBiome(int biomeId) {
            if (highlightedBiome == biomeId) return;
            highlightedBiome = biomeId;
            Arrays.fill(hatchPixels, 0);
            if (biomeId >= 0) {
                for (int z = 0, i = 0; z < RESOLUTION; z++) {
                    for (int x = 0; x < RESOLUTION; x++, i++) {
                        if ((biomes[i] & 255) == biomeId && (x + z) % 8 < 2) hatchPixels[i] = 0xA0FFFFFF;
                    }
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (zoom == 0) {
                zoom = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
                x = (getWidth() - image.getWidth() * zoom) / 2;
                y = (getHeight() - image.getHeight() * zoom) / 2;
            }
            Graphics2D map = (Graphics2D) graphics.create();
            map.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            map.translate(x, y);
            map.scale(zoom, zoom);
            map.drawImage(image, 0, 0, null);
            if (highlightedBiome >= 0) map.drawImage(hatch, 0, 0, null);
            map.dispose();
            paintTooltip((Graphics2D) graphics);
        }

        private void paintTooltip(Graphics2D graphics) {
            if (hoverX < 0) return;
            int i = hoverZ * RESOLUTION + hoverX;
            int id = biomes[i] & 255;
            String[] lines = {
                    "X " + (hoverX * BLOCKS_PER_PIXEL - SIZE / 2)
                            + "  Y "
                            + Math.round(height[i])
                            + "  Z "
                            + (hoverZ * BLOCKS_PER_PIXEL - SIZE / 2),
                    "Category: " + categoryName(height[i], id),
                    "Biome: " + biomeName(RealisticBiomeBase.getBiome(id)),
                    "Backing biome: " + RealisticBiomeBase.getBiome(id).baseBiome.biomeName };
            FontMetrics metrics = graphics.getFontMetrics();
            int width = 0;
            for (String line : lines) width = Math.max(width, metrics.stringWidth(line));
            int boxX = getWidth() - width - 24, boxY = getHeight() - metrics.getHeight() * lines.length - 18;
            graphics.setColor(new Color(0, 0, 0, 190));
            graphics.fillRoundRect(boxX, boxY, width + 16, metrics.getHeight() * lines.length + 10, 10, 10);
            graphics.setColor(Color.WHITE);
            for (int line = 0; line < lines.length; line++) {
                graphics.drawString(lines[line], boxX + 8, boxY + 5 + metrics.getAscent() + line * metrics.getHeight());
            }
        }
    }
}
