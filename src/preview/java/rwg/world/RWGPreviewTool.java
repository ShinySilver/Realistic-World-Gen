package rwg.world;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.minecraft.world.ColorizerGrass;

import one.profiler.AsyncProfiler;
import rwg.ConfigRWG;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceCanyon;
import rwg.biomes.surface.SurfaceDesert;
import rwg.biomes.surface.SurfaceDesertMountain;
import rwg.biomes.surface.SurfaceIslandMountainStone;
import rwg.biomes.surface.SurfaceMesa;
import rwg.biomes.surface.SurfaceMountainPolar;
import rwg.biomes.surface.SurfaceMountainSnow;
import rwg.biomes.surface.SurfaceMountainStone;
import rwg.biomes.surface.SurfaceMountainStoneMix1;
import rwg.biomes.surface.SurfacePolar;
import rwg.biomes.surface.SurfaceRedDesert;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;
import rwg.world.sample.ColumnSample;

/** Interactive window-resolution preview backed by the active world-generation pipeline. */
public final class RWGPreviewTool {

    private static final int WORKERS = 16;
    private static final double OVERSCAN = .10d;
    private static final double MAXIMUM_VIEW_SPAN = 40960d;
    private static final long DEFAULT_SEED = 927692613931855800L;
    private static final int[] CATEGORY_COLORS = { 0x8DB360, 0x73A653, 0xB89D68, 0x858585, 0xA7C36A, 0xC27652, 0x557E58,
            0x66A878, 0x4E916B, 0x397A5D, 0x3979A8, 0x9A8457, 0xD6C49A, 0x3288BD, 0x185A8D };
    private static final int[] CLIMATE_COLORS = { 0xE8F4FF, 0x74B86A, 0xD6A34A, 0x286B3A };
    private static final int CLIMATE_OCEAN_COLOR = 0x287EAE;
    private static final int CLIMATE_RIVER_COLOR = 0x3C91BE;
    private static final int CLIMATE_JUNCTION_COLOR = 0xE02020;
    private static final int MOUNTAIN_RIDGE_COLOR = 0xFF2020;
    private static final int HEIGHT_LAND_COLOR = 0x68A94F;
    private static final int HEIGHT_DEEP_OCEAN_COLOR = 0x174F82;
    private static final int HEIGHT_SHALLOW_OCEAN_COLOR = 0x3C91BE;
    private static final int SAND_COLOR = 0xD8C17A;
    private static final int RED_SAND_COLOR = 0xA95832;
    private static final int SNOW_COLOR = 0xF2F8FF;
    private static final int STONE_COLOR = 0x777777;
    private static int[] registrationColors;

    private RWGPreviewTool() {}

    public static void main(String[] args) {
        ConfigRWG.initPreview(previewConfig(args));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        initializePreviewColors(registry);
        for (String argument : args) {
            if (argument.startsWith("--probe-point=")) {
                debugPoint(registry, argument.substring("--probe-point=".length()));
                return;
            }
            if (argument.startsWith("--probe-blended-chunk=")) {
                debugProbe(registry, argument.substring("--probe-blended-chunk=".length()));
                return;
            }
        }
        if (Arrays.asList(args).contains("--instrument")) profile(registry);
        else open(registry);
    }

    private static File previewConfig(String[] args) {
        for (String argument : args)
            if (argument.startsWith("--config=")) return new File(argument.substring("--config=".length()));
        return new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg"));
    }

    private static void open(BiomeRegistry registry) {
        PreviewPanel panel = new PreviewPanel(registry);
        JFrame frame = new JFrame("RWG preview");
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.getRootPane().registerKeyboardAction(
                event -> frame.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.getRootPane().registerKeyboardAction(
                event -> panel.cycleLayer(),
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.setVisible(true);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                panel.close();
                System.exit(0);
            }
        });
        panel.scheduleFullRender();
    }

    private static final class PreviewPanel extends JPanel {

        private final BiomeRegistry registry;
        private final ThreadPoolExecutor coordinator = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        private final ThreadPoolExecutor hoverCoordinator = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        private final AtomicInteger generation = new AtomicInteger();
        private final AtomicInteger hoverGeneration = new AtomicInteger();
        private final JLabel hover = overlayLabel(" ");
        private final JLabel viewport = overlayLabel(" ");
        private final JLabel progress = overlayLabel(" ");
        private final JPanel controls;
        private final JComboBox<Layer> layers;
        private final Timer panTimer;
        private final Timer zoomTimer;
        private volatile RenderedCanvas canvas;
        private volatile Layer layer = Layer.BIOME;
        private long seed = DEFAULT_SEED;
        private double centerX;
        private double centerZ;
        private double blocksPerPixel = 16d;
        private Point dragStart;
        private double dragCenterX;
        private double dragCenterZ;
        private long lastPanRenderNanos;
        private int hoverX;
        private int hoverZ;
        private RenderedCanvas pendingHoverCanvas;
        private int pendingHoverIndex = -1;

        private PreviewPanel(BiomeRegistry registry) {
            this.registry = registry;
            setLayout(null);
            JTextField seedField = new JTextField(Long.toString(seed), 17);
            seedField.setFocusTraversalKeysEnabled(false);
            JButton apply = new JButton("Apply");
            apply.setFocusTraversalKeysEnabled(false);
            apply.addActionListener(event -> {
                try {
                    seed = Long.parseLong(seedField.getText().trim());
                    scheduleFullRender();
                } catch (NumberFormatException exception) {
                    progress.setText("Invalid seed");
                }
            });
            seedField.addActionListener(event -> apply.doClick());
            JPanel seedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            seedRow.add(seedField);
            seedRow.add(apply);
            layers = new JComboBox<Layer>(Layer.values());
            layers.setFocusTraversalKeysEnabled(false);
            layers.addActionListener(event -> setLayer((Layer) layers.getSelectedItem()));
            controls = new JPanel();
            controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
            controls.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            controls.add(seedRow);
            controls.add(layers);
            hover.setBackground(new Color(245, 245, 245, 190));
            hover.setVisible(false);
            add(controls);
            add(hover);
            add(viewport);
            add(progress);

            panTimer = new Timer(100, event -> {
                lastPanRenderNanos = System.nanoTime();
                requestRender(false);
            });
            panTimer.setRepeats(false);
            zoomTimer = new Timer(350, event -> requestRender(true));
            zoomTimer.setRepeats(false);
            MouseAdapter mouse = new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent event) {
                    dragStart = event.getPoint();
                    dragCenterX = centerX;
                    dragCenterZ = centerZ;
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    centerX = dragCenterX - (event.getX() - dragStart.x) * blocksPerPixel;
                    centerZ = dragCenterZ - (event.getY() - dragStart.y) * blocksPerPixel;
                    schedulePanRender();
                    showHover(event.getX(), event.getY());
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    showHover(event.getX(), event.getY());
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hover.setVisible(false);
                    hoverGeneration.incrementAndGet();
                    hoverCoordinator.getQueue().clear();
                    pendingHoverCanvas = null;
                    pendingHoverIndex = -1;
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    double previousScale = blocksPerPixel;
                    double nextScale = clamp(
                            previousScale * Math.pow(1.18d, event.getPreciseWheelRotation()),
                            .25d,
                            maximumBlocksPerPixel());
                    if (nextScale == previousScale) return;
                    double worldX = viewOriginX() + event.getX() * blocksPerPixel;
                    double worldZ = viewOriginZ() + event.getY() * blocksPerPixel;
                    blocksPerPixel = nextScale;
                    centerX = worldX + (getWidth() * .5d - event.getX()) * blocksPerPixel;
                    centerZ = worldZ + (getHeight() * .5d - event.getY()) * blocksPerPixel;
                    updateViewportLabel();
                    zoomTimer.restart();
                    showHover(event.getX(), event.getY());
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent event) {
                    blocksPerPixel = Math.min(blocksPerPixel, maximumBlocksPerPixel());
                    updateViewportLabel();
                    scheduleFullRender();
                }
            });
        }

        private void scheduleFullRender() {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            zoomTimer.restart();
        }

        private void schedulePanRender() {
            long now = System.nanoTime();
            long elapsedMillis = (now - lastPanRenderNanos) / 1_000_000L;
            if (elapsedMillis >= 100L) {
                panTimer.stop();
                lastPanRenderNanos = now;
                requestRender(false);
            } else if (!panTimer.isRunning()) {
                panTimer.setInitialDelay((int) (100L - elapsedMillis));
                panTimer.start();
            }
        }

        private void requestRender(boolean full) {
            if (getWidth() <= 0 || getHeight() <= 0) return;
            final int request = full ? generation.incrementAndGet() : generation.get();
            final int width = (int) Math.ceil(getWidth() * (1d + OVERSCAN * 2d));
            final int height = (int) Math.ceil(getHeight() * (1d + OVERSCAN * 2d));
            final double scale = blocksPerPixel;
            final double originX = centerX - width * scale * .5d;
            final double originZ = centerZ - height * scale * .5d;
            final long requestedSeed = seed;
            progress.setText(full ? "Refining…" : "Filling edges…");
            if (full) {
                coordinator.getQueue().clear();
                RenderedCanvas target = prepareCanvas(width, height, originX, originZ, scale, requestedSeed, false);
                canvas = target;
                repaint();
                coordinator.execute(() -> generate(request, target, true));
            } else {
                coordinator.execute(() -> {
                    if (generation.get() != request) return;
                    RenderedCanvas target = prepareCanvas(width, height, originX, originZ, scale, requestedSeed, true);
                    canvas = target;
                    SwingUtilities.invokeLater(this::repaint);
                    generate(request, target, false);
                });
            }
        }

        private RenderedCanvas prepareCanvas(int width, int height, double originX, double originZ, double scale,
                long requestedSeed, boolean copySamples) {
            RenderedCanvas target = new RenderedCanvas(width, height, originX, originZ, scale, requestedSeed);
            RenderedCanvas previous = canvas;
            target.reproject(previous);
            if (copySamples) target.copyAlignedSamples(previous);
            return target;
        }

        private void generate(int request, RenderedCanvas target, boolean full) {
            ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
            CompletableFuture<?>[] jobs = new CompletableFuture[WORKERS];
            AtomicInteger rows = new AtomicInteger();
            ChunkManager manager = new ChunkManager(target.seed, true, registry);
            WorldGenerator generator = new WorldGenerator(target.seed, manager.worldgenSelector());
            try {
                for (int worker = 0; worker < WORKERS; worker++) {
                    final int firstRow = worker;
                    jobs[worker] = CompletableFuture.runAsync(() -> {
                        for (int z = firstRow; z < target.height; z += WORKERS) {
                            if (generation.get() != request) return;
                            for (int x = 0; x < target.width; x++) {
                                int index = z * target.width + x;
                                if (!full && target.valid[index]) continue;
                                WorldGenerator.PreviewColumn column = generator.samplePreviewPoint(
                                        (int) Math.floor(target.originX + (x + .5d) * target.scale),
                                        (int) Math.floor(target.originZ + (z + .5d) * target.scale));
                                target.set(index, column, layer);
                            }
                            if ((rows.incrementAndGet() & 15) == 0)
                                SwingUtilities.invokeLater(() -> { if (generation.get() == request) repaint(); });
                        }
                    }, workers);
                }
                CompletableFuture.allOf(jobs).join();
            } finally {
                workers.shutdownNow();
            }
            if (generation.get() != request) return;
            target.recolor(layer);
            SwingUtilities.invokeLater(() -> {
                if (generation.get() != request) return;
                progress.setText(coordinator.getQueue().isEmpty() ? "Complete" : "Filling edges…");
                repaint();
            });
        }

        private void setLayer(Layer newLayer) {
            layer = newLayer;
            RenderedCanvas current = canvas;
            if (current != null) current.recolor(newLayer);
            if (hover.isVisible()) showHover(hoverX, hoverZ);
            repaint();
        }

        private void cycleLayer() {
            layers.setSelectedIndex((layers.getSelectedIndex() + 1) % layers.getItemCount());
        }

        private void showHover(int screenX, int screenZ) {
            hoverX = screenX;
            hoverZ = screenZ;
            RenderedCanvas current = canvas;
            if (current == null) return;
            double worldX = viewOriginX() + screenX * blocksPerPixel;
            double worldZ = viewOriginZ() + screenZ * blocksPerPixel;
            int x = (int) Math.floor((worldX - current.originX) / current.scale);
            int z = (int) Math.floor((worldZ - current.originZ) / current.scale);
            if (x < 0 || z < 0 || x >= current.width || z >= current.height) {
                generateHoverPoint(current, -1, screenX, screenZ, worldX, worldZ);
                return;
            }
            int index = z * current.width + x;
            if (!current.valid[index]) {
                generateHoverPoint(current, index, screenX, screenZ, worldX, worldZ);
                return;
            }
            setHoverFromSample(
                    screenX,
                    screenZ,
                    worldX,
                    worldZ,
                    current.baseHeights[index],
                    current.heights[index],
                    current.registrations[index] & 65535,
                    current.categories[index],
                    current.climates[index],
                    current.climateCategories[index],
                    current.rawMountainDistances[index],
                    current.mountainDistances[index],
                    current.valleyDistances[index],
                    current.valleyStrengths[index]);
        }

        private void generateHoverPoint(RenderedCanvas target, int index, int screenX, int screenZ, double worldX,
                double worldZ) {
            if (pendingHoverCanvas == target && pendingHoverIndex == index) return;
            pendingHoverCanvas = target;
            pendingHoverIndex = index;
            hover.setVisible(false);
            int request = hoverGeneration.incrementAndGet();
            hoverCoordinator.getQueue().clear();
            hoverCoordinator.execute(() -> {
                ChunkManager manager = new ChunkManager(target.seed, true, registry);
                WorldGenerator.PreviewColumn column = new WorldGenerator(target.seed, manager.worldgenSelector())
                        .samplePreviewPoint((int) Math.floor(worldX), (int) Math.floor(worldZ));
                if (hoverGeneration.get() != request) return;
                if (index >= 0 && canvas == target) target.set(index, column, layer);
                SwingUtilities.invokeLater(() -> {
                    if (hoverGeneration.get() != request) return;
                    pendingHoverCanvas = null;
                    pendingHoverIndex = -1;
                    setHoverFromSample(
                            screenX,
                            screenZ,
                            worldX,
                            worldZ,
                            column.baseHeight,
                            column.height,
                            column.sample.biome.registration.id,
                            (byte) column.sample.morphology.category.ordinal(),
                            (byte) column.sample.climate.climate.ordinal(),
                            (byte) column.sample.climate.category.ordinal(),
                            column.sample.morphology.rawMountainDistance,
                            column.sample.morphology.mountainDistance,
                            column.sample.morphology.valleyDistance,
                            column.sample.morphology.valleyStrength);
                    repaint();
                });
            });
        }

        private void setHoverFromSample(int screenX, int screenZ, double worldX, double worldZ, float baseHeight,
                float finalHeight, int registrationId, byte category, byte climate, byte climateCategory,
                float rawMountainDistance, float effectiveMountainDistance, float valleyDistance,
                float valleyStrength) {
            BiomeRegistration registration = registry.registration(registrationId);
            String coordinates = String.format("X: %.0f&nbsp;&nbsp;Z: %.0f", worldX, worldZ);
            String placement = Climate.values()[climate & 255] + " / "
                    + TerrainCategory.values()[category & 255]
                    + (registration.subcategory == TerrainSubcategory.CORE ? "" : " / " + registration.subcategory);
            String value;
            if (layer == Layer.CATEGORY) {
                value = "Climate / terrain: " + placement
                        + "<br>Base height: "
                        + String.format("%.1f", baseHeight)
                        + "<br>Mountain distance: "
                        + String.format("%.1f → %.1f", rawMountainDistance, effectiveMountainDistance)
                        + "<br>Valley: "
                        + String.format("%.2f (distance %.1f)", valleyStrength, valleyDistance);
            } else if (layer == Layer.CLIMATE) {
                value = "Climate/Category: " + Climate.values()[climate & 255]
                        + " / "
                        + TerrainSubcategory.values()[climateCategory & 255];
            } else {
                value = "Biome: " + registration.biome.biomeName
                        + " (#"
                        + registration.biome.biomeID
                        + ")"
                        + "<br>Registry entry: #"
                        + registration.id
                        + (registration.builtin ? " (built-in)" : " (integration)")
                        + "<br>Terrain base: "
                        + registration.terrain.getClass().getSimpleName()
                        + "<br>Surface: "
                        + surfaceNames(registration.surfaces)
                        + "<br>Selection: "
                        + registration.climate
                        + " / "
                        + registration.category
                        + " / "
                        + registration.subcategory
                        + " (weight "
                        + registration.weight
                        + ")"
                        + "<br>Base height: "
                        + String.format("%.1f", baseHeight)
                        + "&nbsp;&nbsp;Final height: "
                        + String.format("%.1f", finalHeight)
                        + "<br>Valley strength: "
                        + String.format("%.2f", valleyStrength);
            }
            setHover(screenX, screenZ, coordinates, value);
        }

        private static String surfaceNames(SurfaceBase[] surfaces) {
            if (surfaces.length == 0) return "none";
            StringBuilder names = new StringBuilder();
            for (SurfaceBase surface : surfaces) {
                if (names.length() != 0) names.append(", ");
                names.append(surface.getClass().getSimpleName());
            }
            return names.toString();
        }

        private void setHover(int cursorX, int cursorZ, String coordinates, String detail) {
            hover.setText("<html>" + coordinates + "<br>" + detail + "</html>");
            java.awt.Dimension size = hover.getPreferredSize();
            int x = cursorX + 14;
            int z = cursorZ + 14;
            if (x + size.width > getWidth() - 4) x = cursorX - size.width - 14;
            if (z + size.height > getHeight() - 4) z = cursorZ - size.height - 14;
            hover.setBounds(Math.max(4, x), Math.max(4, z), size.width, size.height);
            hover.setVisible(true);
        }

        private double viewOriginX() {
            return centerX - getWidth() * blocksPerPixel * .5d;
        }

        private double viewOriginZ() {
            return centerZ - getHeight() * blocksPerPixel * .5d;
        }

        private double maximumBlocksPerPixel() {
            int longestSide = Math.max(1, Math.max(getWidth(), getHeight()));
            return Math.max(.25d, MAXIMUM_VIEW_SPAN / longestSide);
        }

        private void updateViewportLabel() {
            viewport.setText(
                    String.format(
                            "Viewport: %,.0f × %,.0f blocks  (%.2f blocks/px)",
                            getWidth() * blocksPerPixel,
                            getHeight() * blocksPerPixel,
                            blocksPerPixel));
        }

        private void close() {
            generation.incrementAndGet();
            coordinator.shutdownNow();
            hoverGeneration.incrementAndGet();
            hoverCoordinator.shutdownNow();
        }

        @Override
        public void doLayout() {
            controls.setBounds(8, 8, 285, 65);
            viewport.setBounds(8, Math.max(8, getHeight() - 60), Math.min(430, getWidth() - 16), 24);
            progress.setBounds(Math.max(8, getWidth() - 150), Math.max(8, getHeight() - 32), 142, 24);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            RenderedCanvas current = canvas;
            if (current == null) return;
            double x = (current.originX - viewOriginX()) / blocksPerPixel;
            double z = (current.originZ - viewOriginZ()) / blocksPerPixel;
            double width = current.width * current.scale / blocksPerPixel;
            double height = current.height * current.scale / blocksPerPixel;
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    current.scale < blocksPerPixel ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                            : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics2D.drawImage(
                    current.image,
                    (int) Math.floor(x),
                    (int) Math.floor(z),
                    (int) Math.ceil(width),
                    (int) Math.ceil(height),
                    null);
            graphics2D.dispose();
        }
    }

    private static final class RenderedCanvas {

        private static final int SHADING_RADIUS = 3;
        private final int width;
        private final int height;
        private final double originX;
        private final double originZ;
        private final double scale;
        private final long seed;
        private final float[] heights;
        private final float[] baseHeights;
        private final short[] registrations;
        private final byte[] categories;
        private final byte[] hostCategories;
        private final byte[] climates;
        private final byte[] climateCategories;
        private final float[] rawMountainDistances;
        private final float[] mountainDistances;
        private final float[] valleyDistances;
        private final float[] valleyStrengths;
        private final boolean[] valid;
        private final BufferedImage image;
        private final int[] pixels;

        private RenderedCanvas(int width, int height, double originX, double originZ, double scale, long seed) {
            this.width = width;
            this.height = height;
            this.originX = originX;
            this.originZ = originZ;
            this.scale = scale;
            this.seed = seed;
            heights = new float[width * height];
            baseHeights = new float[width * height];
            registrations = new short[width * height];
            categories = new byte[width * height];
            hostCategories = new byte[width * height];
            climates = new byte[width * height];
            climateCategories = new byte[width * height];
            rawMountainDistances = new float[width * height];
            mountainDistances = new float[width * height];
            valleyDistances = new float[width * height];
            valleyStrengths = new float[width * height];
            valid = new boolean[width * height];
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }

        private void reproject(RenderedCanvas source) {
            if (source == null || source.seed != seed) return;
            double x = (source.originX - originX) / scale;
            double z = (source.originZ - originZ) / scale;
            double projectedWidth = source.width * source.scale / scale;
            double projectedHeight = source.height * source.scale / scale;
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(
                    source.image,
                    (int) Math.floor(x),
                    (int) Math.floor(z),
                    (int) Math.ceil(projectedWidth),
                    (int) Math.ceil(projectedHeight),
                    null);
            graphics.dispose();
        }

        private void copyAlignedSamples(RenderedCanvas source) {
            if (source == null || source.seed != seed || source.scale != scale) return;
            int shiftX = (int) Math.round((source.originX - originX) / scale);
            int shiftZ = (int) Math.round((source.originZ - originZ) / scale);
            if (Math.abs(source.originX - originX - shiftX * scale) > .001d
                    || Math.abs(source.originZ - originZ - shiftZ * scale) > .001d)
                return;
            int targetX = Math.max(0, shiftX);
            int targetZ = Math.max(0, shiftZ);
            int sourceX = Math.max(0, -shiftX);
            int sourceZ = Math.max(0, -shiftZ);
            int copyWidth = Math.min(width - targetX, source.width - sourceX);
            int copyHeight = Math.min(height - targetZ, source.height - sourceZ);
            if (copyWidth <= 0 || copyHeight <= 0) return;
            for (int row = 0; row < copyHeight; row++) {
                int from = (sourceZ + row) * source.width + sourceX;
                int to = (targetZ + row) * width + targetX;
                System.arraycopy(source.heights, from, heights, to, copyWidth);
                System.arraycopy(source.baseHeights, from, baseHeights, to, copyWidth);
                System.arraycopy(source.registrations, from, registrations, to, copyWidth);
                System.arraycopy(source.categories, from, categories, to, copyWidth);
                System.arraycopy(source.hostCategories, from, hostCategories, to, copyWidth);
                System.arraycopy(source.climates, from, climates, to, copyWidth);
                System.arraycopy(source.climateCategories, from, climateCategories, to, copyWidth);
                System.arraycopy(source.rawMountainDistances, from, rawMountainDistances, to, copyWidth);
                System.arraycopy(source.mountainDistances, from, mountainDistances, to, copyWidth);
                System.arraycopy(source.valleyDistances, from, valleyDistances, to, copyWidth);
                System.arraycopy(source.valleyStrengths, from, valleyStrengths, to, copyWidth);
                System.arraycopy(source.valid, from, valid, to, copyWidth);
            }
        }

        private void set(int index, WorldGenerator.PreviewColumn column, Layer layer) {
            heights[index] = column.height;
            baseHeights[index] = column.baseHeight;
            registrations[index] = (short) column.sample.biome.registration.id;
            categories[index] = (byte) column.sample.morphology.category.ordinal();
            hostCategories[index] = (byte) column.sample.biome.registration.category.ordinal();
            climates[index] = (byte) column.sample.climate.climate.ordinal();
            climateCategories[index] = (byte) column.sample.climate.category.ordinal();
            rawMountainDistances[index] = column.sample.morphology.rawMountainDistance;
            mountainDistances[index] = column.sample.morphology.mountainDistance;
            valleyDistances[index] = column.sample.morphology.valleyDistance;
            valleyStrengths[index] = column.sample.morphology.valleyStrength;
            valid[index] = true;
            pixels[index] = color(index, layer);
            refreshSlopeDependents(index, layer);
        }

        /** Re-shades only pixels whose east/west/north/south slope stencil includes the new sample. */
        private void refreshSlopeDependents(int index, Layer layer) {
            int x = index % width;
            int z = index / width;
            recolorIfValid(index, layer);
            recolorIfValid(z * width + Math.max(0, x - SHADING_RADIUS), layer);
            recolorIfValid(z * width + Math.min(width - 1, x + SHADING_RADIUS), layer);
            recolorIfValid(Math.max(0, z - SHADING_RADIUS) * width + x, layer);
            recolorIfValid(Math.min(height - 1, z + SHADING_RADIUS) * width + x, layer);
            recolorIfValid(z * width + Math.max(0, x - 1), layer);
            recolorIfValid(z * width + Math.min(width - 1, x + 1), layer);
            recolorIfValid(Math.max(0, z - 1) * width + x, layer);
            recolorIfValid(Math.min(height - 1, z + 1) * width + x, layer);
        }

        private void recolorIfValid(int index, Layer layer) {
            if (valid[index]) pixels[index] = color(index, layer);
        }

        private void recolor(Layer layer) {
            for (int index = 0; index < pixels.length; index++) if (valid[index]) pixels[index] = color(index, layer);
        }

        private int color(int index, Layer layer) {
            boolean regionalLayer = layer == Layer.CATEGORY || layer == Layer.CLIMATE;
            float elevation = regionalLayer ? baseHeights[index] : heights[index];
            int color;
            if (layer == Layer.CLIMATE) {
                int terrainCategory = categories[index] & 255;
                if (isOcean(index)) color = CLIMATE_OCEAN_COLOR;
                else if (terrainCategory == TerrainCategory.RIVER.ordinal()) color = CLIMATE_RIVER_COLOR;
                else if (mountainDistances[index] <= Math.max(16d, scale)) color = CLIMATE_JUNCTION_COLOR;
                else {
                    int climate = climates[index] & 255;
                    color = CLIMATE_COLORS[climate];
                    TerrainSubcategory category = TerrainSubcategory.values()[climateCategories[index] & 255];
                    if (category == TerrainSubcategory.COLD_BORDER && climate > 0)
                        color = mix(color, CLIMATE_COLORS[climate - 1], .33f);
                    else if (category == TerrainSubcategory.HOT_BORDER && climate + 1 < CLIMATE_COLORS.length)
                        color = mix(color, CLIMATE_COLORS[climate + 1], .33f);
                }
            } else if (layer == Layer.CATEGORY && rawMountainDistances[index] <= Math.max(1d, scale * .75d)) {
                return MOUNTAIN_RIDGE_COLOR;
            } else if (layer == Layer.CATEGORY) {
                int category = categories[index] & 255;
                if (category == TerrainCategory.RIVER.ordinal() && elevation >= 63f) {
                    category = hostCategories[index] & 255;
                }
                color = CATEGORY_COLORS[category];
            } else {
                color = biomeColor(registrations[index] & 65535, elevation);
            }
            int x = index % width;
            int z = index / width;
            int westX = Math.max(0, x - SHADING_RADIUS);
            int eastX = Math.min(width - 1, x + SHADING_RADIUS);
            int northZ = Math.max(0, z - SHADING_RADIUS);
            int southZ = Math.min(height - 1, z + SHADING_RADIUS);
            int west = z * width + westX;
            int east = z * width + eastX;
            int north = northZ * width + x;
            int south = southZ * width + x;
            if (!valid[west] || !valid[east] || !valid[north] || !valid[south]) return color;
            float[] shadingHeights = regionalLayer ? baseHeights : heights;
            float referenceSpan = SHADING_RADIUS * 2f;
            float dx = (shadingHeights[east] - shadingHeights[west]) * referenceSpan
                    / (float) ((eastX - westX) * scale);
            float dz = (shadingHeights[south] - shadingHeights[north]) * referenceSpan
                    / (float) ((southZ - northZ) * scale);
            float directional = (12f + dx + dz) / (float) Math.sqrt(dx * dx + dz * dz + 432f);
            float light = .78f + .40f * directional;
            if (elevation >= 63f) light += clamp((elevation - 63f) / 120f, 0f, 1f) * .14f;
            color = shade(color, clamp(light, .55f, 1.45f));
            return contour(color, index, elevation, shadingHeights);
        }

        private int contour(int color, int index, float elevation, float[] contourHeights) {
            int x = index % width;
            int z = index / width;
            int east = z * width + Math.min(width - 1, x + 1);
            int south = Math.min(height - 1, z + 1) * width + x;
            if (!valid[east] || !valid[south]) return color;
            int contour = (int) Math.floor(elevation / 10f);
            int eastContour = (int) Math.floor(contourHeights[east] / 10f);
            int southContour = (int) Math.floor(contourHeights[south] / 10f);
            if (contour == eastContour && contour == southContour) return color;
            boolean major = contour % 5 == 0 || eastContour % 5 == 0 || southContour % 5 == 0;
            return shade(color, major ? .48f : .68f);
        }

        private boolean isOcean(int index) {
            int category = categories[index] & 255;
            return category == TerrainCategory.SHALLOW_OCEAN.ordinal()
                    || category == TerrainCategory.DEEP_OCEAN.ordinal();
        }

    }

    private enum Layer {

        BIOME("Biomes"),
        CATEGORY("Terrain categories"),
        CLIMATE("Climates");

        private final String label;

        Layer(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static JLabel overlayLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(new Color(245, 245, 245, 230));
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        return label;
    }

    private static void profile(BiomeRegistry registry) {
        profilePass(registry, DEFAULT_SEED ^ 0x5DEECE66DL, 1400, 900);
        profilePass(registry, DEFAULT_SEED ^ 0xC0FFEE1234L, 1400, 900);
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for profiling warm-up", exception);
        }
        restartProfiler();
        long started = System.nanoTime();
        profilePass(registry, DEFAULT_SEED, 1400, 900);
        System.out
                .println(String.format("Measured preview generation: %.3f s", (System.nanoTime() - started) / 1.0E9D));
        stopProfiler();
    }

    private static void profilePass(BiomeRegistry registry, long seed, int width, int height) {
        RenderedCanvas canvas = new RenderedCanvas(width, height, -width * 8d, -height * 8d, 16d, seed);
        PreviewPanel panel = new PreviewPanel(registry);
        panel.canvas = canvas;
        int request = panel.generation.incrementAndGet();
        panel.generate(request, canvas, true);
        panel.close();
    }

    private static void restartProfiler() {
        try {
            AsyncProfiler profiler = AsyncProfiler.getInstance();
            profiler.execute("stop");
            profiler.execute("start,event=cpu,interval=1ms");
        } catch (Exception exception) {
            throw new RuntimeException("Could not restart async-profiler after warm-up", exception);
        }
    }

    private static void debugProbe(BiomeRegistry registry, String coordinates) {
        String[] parts = coordinates.split(",");
        if (parts.length != 2) throw new IllegalArgumentException("Chunk coordinates must be x,z");
        int chunkX = Integer.parseInt(parts[0]);
        int chunkZ = Integer.parseInt(parts[1]);
        ChunkManager manager = new ChunkManager(DEFAULT_SEED, true, registry);
        WorldGenerator.ChunkTerrain chunk = new WorldGenerator(DEFAULT_SEED, manager.worldgenSelector())
                .sampleChunk(chunkX, chunkZ);
        float minimum = Float.POSITIVE_INFINITY;
        float maximum = Float.NEGATIVE_INFINITY;
        float baseMinimum = Float.POSITIVE_INFINITY;
        float baseMaximum = Float.NEGATIVE_INFINITY;
        long fingerprint = 0xcbf29ce484222325L;
        for (int index = 0; index < chunk.heights.length; index++) {
            minimum = Math.min(minimum, chunk.heights[index]);
            maximum = Math.max(maximum, chunk.heights[index]);
            baseMinimum = Math.min(baseMinimum, chunk.baseHeights[index]);
            baseMaximum = Math.max(baseMaximum, chunk.baseHeights[index]);
            fingerprint = (fingerprint ^ Float.floatToIntBits(chunk.heights[index])) * 0x100000001b3L;
            fingerprint = (fingerprint ^ chunk.registrations[index].id) * 0x100000001b3L;
        }
        System.out.println("RWG component registry entries=" + registry.registrations().size());
        System.out.println(
                "chunk=" + chunkX
                        + ","
                        + chunkZ
                        + " height="
                        + minimum
                        + ".."
                        + maximum
                        + " baseHeight="
                        + baseMinimum
                        + ".."
                        + baseMaximum
                        + " fingerprint=0x"
                        + Long.toHexString(fingerprint));
    }

    private static void debugPoint(BiomeRegistry registry, String coordinates) {
        String[] parts = coordinates.split(",");
        if (parts.length != 2) throw new IllegalArgumentException("Point coordinates must be x,z");
        int x = Integer.parseInt(parts[0]);
        int z = Integer.parseInt(parts[1]);
        ChunkManager manager = new ChunkManager(DEFAULT_SEED, true, registry);
        WorldGenerator.PreviewColumn column = new WorldGenerator(DEFAULT_SEED, manager.worldgenSelector())
                .samplePreviewPoint(x, z);
        ColumnSample sample = column.sample;
        System.out.println("point=" + x + "," + z);
        System.out.println(
                "continent=" + sample.continent.distance
                        + " climate="
                        + sample.climate.climate
                        + " climateSelector="
                        + sample.climate.selector);
        System.out.println(
                "category=" + sample.morphology.category
                        + " rawMountainDistance="
                        + sample.morphology.rawMountainDistance
                        + " mountainDistance="
                        + sample.morphology.mountainDistance
                        + " mountainStrength="
                        + sample.morphology.mountainStrength
                        + " plateauSide="
                        + sample.morphology.plateauSide
                        + " riverStrength="
                        + sample.morphology.riverStrength
                        + " riverDistance="
                        + sample.morphology.riverDistance
                        + " valleyStrength="
                        + sample.morphology.valleyStrength
                        + " valleyDistance="
                        + sample.morphology.valleyDistance
                        + " baseHeight="
                        + column.baseHeight
                        + " finalHeight="
                        + column.height);
        System.out.println(
                "biome=" + sample.biome.registration.biome.biomeName + " registryId=" + sample.biome.registration.id);
    }

    private static void stopProfiler() {
        try {
            String output = System.getProperty("rwg.asyncProfilerOutput");
            if (output == null) throw new IllegalStateException("Missing async-profiler output path");
            AsyncProfiler.getInstance().execute("stop,file=" + output + ",title=RWG preview world generation");
        } catch (Exception exception) {
            throw new RuntimeException("Could not stop async-profiler", exception);
        }
    }

    private static int biomeColor(int registrationId, float height) {
        if (height < 63f) return mix(0x123C69, 0x4CA3D9, clamp((height - 28f) / 35f, 0f, 1f));
        return registrationColors[registrationId];
    }

    private static void initializePreviewColors(BiomeRegistry registry) {
        initializeGrassColorizer();
        registrationColors = new int[registry.registrations().size()];
        for (BiomeRegistration registration : registry.registrations()) {
            int color = registration.biome.getBiomeGrassColor(0, 64, 0) & 0xffffff;
            if (registration.builtin) {
                for (SurfaceBase surface : registration.surfaces) {
                    if (surface instanceof SurfaceRedDesert || surface instanceof SurfaceCanyon
                            || surface instanceof SurfaceMesa) {
                        color = RED_SAND_COLOR;
                        break;
                    }
                    if (surface instanceof SurfaceDesert || surface instanceof SurfaceDesertMountain) {
                        color = SAND_COLOR;
                        break;
                    }
                    if (surface instanceof SurfacePolar || surface instanceof SurfaceMountainSnow
                            || surface instanceof SurfaceMountainPolar) {
                        color = SNOW_COLOR;
                        break;
                    }
                    if (surface instanceof SurfaceMountainStone || surface instanceof SurfaceMountainStoneMix1
                            || surface instanceof SurfaceIslandMountainStone) {
                        color = STONE_COLOR;
                        break;
                    }
                }
            }
            registrationColors[registration.id] = color;
        }
    }

    private static void initializeGrassColorizer() {
        InputStream stream = RWGPreviewTool.class.getResourceAsStream("/assets/minecraft/textures/colormap/grass.png");
        if (stream == null) throw new IllegalStateException("Minecraft grass color map is missing from the classpath");
        try {
            BufferedImage image = ImageIO.read(stream);
            int[] colors = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), colors, 0, image.getWidth());
            ColorizerGrass.setGrassBiomeColorizer(colors);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Minecraft's grass color map", exception);
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {}
        }
    }

    private static int mix(int first, int second, float amount) {
        float inverse = 1f - amount;
        return (int) ((first >> 16 & 255) * inverse + (second >> 16 & 255) * amount) << 16
                | (int) ((first >> 8 & 255) * inverse + (second >> 8 & 255) * amount) << 8
                | (int) ((first & 255) * inverse + (second & 255) * amount);
    }

    private static int shade(int color, float light) {
        int red = Math.min(255, (int) ((color >> 16 & 255) * light));
        int green = Math.min(255, (int) ((color >> 8 & 255) * light));
        int blue = Math.min(255, (int) ((color & 255) * light));
        return red << 16 | green << 8 | blue;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
