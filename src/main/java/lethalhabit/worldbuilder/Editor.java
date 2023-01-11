package lethalhabit.worldbuilder;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static lethalhabit.worldbuilder.Util.*;

public class Editor extends JFrame {
    
    private final List<BufferedImage> TILEMAP = new ArrayList<>();
    private final List<BufferedImage> LIQUID_TILEMAP = new ArrayList<>();
    private final List<BufferedImage> INTERACTABLE_TILEMAP = new ArrayList<>();
    private final List<Integer> activeKeys = new ArrayList<>();
    
    private final EditorPane editorPane;
    private final TileToolbar toolbar;
    private final LiquidToolbar sidebarR;
    private final InteractableToolbar sidebarL;
    
    private Map<Integer, Map<Integer, Tile>> importedWorldData = null;
    private int importedWorldOffsetX = 0;
    private int importedWorldOffsetY = 0;
    
    private boolean inferOrientation = true;
    
    public Editor() {
        loadTilemap();
        loadLiquidTilemap();
        loadInteractableTilemap();
        setTitle("Lethal Habit - World Builder");
        setSize(1300, 800);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        setResizable(true);
        setLayout(new BorderLayout());
        editorPane = new EditorPane();
        toolbar = new TileToolbar();
        sidebarR = new LiquidToolbar();
        sidebarL = new InteractableToolbar();
        add(editorPane, BorderLayout.CENTER);
        add(toolbar, BorderLayout.PAGE_START);
        add(sidebarR, BorderLayout.LINE_END);
        add(sidebarL, BorderLayout.LINE_START);
        setVisible(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_K -> {
                        // toggle toolbar
                        toolbar.setVisible(!toolbar.isVisible());
                        if (toolbar.isVisible()) {
                            add(toolbar, BorderLayout.PAGE_START);
                        } else {
                            remove(toolbar);
                        }
                        revalidate();
                    }
                    case KeyEvent.VK_L -> {
                        // toggle sidebar
                        sidebarL.setVisible(!sidebarL.isVisible());
                        if (sidebarL.isVisible()) {
                            add(sidebarL, BorderLayout.LINE_END);
                        } else {
                            remove(sidebarL);
                        }
                        revalidate();
                    }
                    case KeyEvent.VK_O -> {
                        sidebarR.setVisible(!sidebarR.isVisible());
                        if (sidebarR.isVisible()) {
                            add(sidebarR, BorderLayout.LINE_END);
                        } else {
                            remove(sidebarR);
                        }
                        revalidate();
                    }
                    case KeyEvent.VK_P -> {
                        // toggle orientation inferring mode
                        inferOrientation = !inferOrientation;
                    }
                    case KeyEvent.VK_RIGHT -> {
                        // move imported world right
                        if (importedWorldData != null) {
                            importedWorldOffsetX += 1;
                        }
                    }
                    case KeyEvent.VK_LEFT -> {
                        // move imported world left
                        if (importedWorldData != null) {
                            importedWorldOffsetX -= 1;
                        }
                    }
                    case KeyEvent.VK_UP -> {
                        // move imported world up
                        if (importedWorldData != null) {
                            importedWorldOffsetY -= 1;
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        // move imported world down
                        if (importedWorldData != null) {
                            importedWorldOffsetY += 1;
                        }
                    }
                    case KeyEvent.VK_Q -> {
                        // move toolbar selection left
                        toolbar.prepareSelection();
                        toolbar.select((Math.max(-1, (toolbar.selection - 1)) % TILEMAP.size() + TILEMAP.size()) % TILEMAP.size());
                    }
                    case KeyEvent.VK_E -> {
                        // move toolbar selection right
                        toolbar.prepareSelection();
                        toolbar.select((toolbar.selection + 1) % TILEMAP.size());
                    }
                    case KeyEvent.VK_C -> {
                        // toggle toolbar selection
                        toolbar.toggleSelection();
                    }
                    case KeyEvent.VK_V -> {
                        // toggle sidebar selection
                        sidebarR.toggleSelection();
                    }
                    case KeyEvent.VK_B -> {
                        sidebarL.toggleSelection();
                    }
                    case KeyEvent.VK_SPACE -> {
                        // move sidebar selection down
                        sidebarR.prepareSelection();
                        sidebarR.select((sidebarR.selection + 1) % LIQUID_TILEMAP.size());
                    }
                    case KeyEvent.VK_G -> {
                        // toggle grid drawing
                        editorPane.drawGrid = !editorPane.drawGrid;
                    }
                    case KeyEvent.VK_T -> {
                        // teleport
                        JTextField xField = new JTextField(5);
                        JTextField yField = new JTextField(5);
                        ((PlainDocument) xField.getDocument()).setDocumentFilter(integerInputFilter());
                        ((PlainDocument) yField.getDocument()).setDocumentFilter(integerInputFilter());
                        JPanel inputPanel = new JPanel();
                        inputPanel.add(new JLabel("X "));
                        inputPanel.add(xField);
                        inputPanel.add(Box.createHorizontalStrut(15));
                        inputPanel.add(new JLabel("Y "));
                        inputPanel.add(yField);
                        int result = JOptionPane.showConfirmDialog(Editor.this, inputPanel, "Teleport to", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            int newX = Math.max(editorPane.minimumX(), Integer.parseInt(xField.getText()));
                            int newY = Math.max(editorPane.minimumY(), Integer.parseInt(yField.getText()));
                            editorPane.camera.setPosition(newX, newY);
                        }
                    }
                    case KeyEvent.VK_M -> {
                        // show map
                        BufferedImage minimap = generateMinimap();
                        tempCreateMinimapWindow(minimap);
                    }
                    case KeyEvent.VK_X -> {
                        // undo
                        if (!editorPane.recentWorldStates.isEmpty()) {
                            editorPane.recentlyUndoneWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                            WorldBuilder.INSTANCE.setWorldData(copyWorldData(editorPane.recentWorldStates.pop()));
                        }
                    }
                    case KeyEvent.VK_Y -> {
                        // redo
                        if (!editorPane.recentlyUndoneWorldStates.isEmpty()) {
                            editorPane.recentWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                            WorldBuilder.INSTANCE.setWorldData(editorPane.recentlyUndoneWorldStates.pop());
                        }
                    }
                    case KeyEvent.VK_F3 -> {
                        // toggle show position in corner
                        editorPane.showPosition = !editorPane.showPosition;
                    }
                    case KeyEvent.VK_F4 -> {
                        // show tile indices
                        editorPane.showTileIndices = !editorPane.showTileIndices;
                    }
                    case KeyEvent.VK_I -> {
                        // open import world dialog
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Import world file");
                        fileChooser.setFileFilter(jsonFileFilter());
                        int result = fileChooser.showOpenDialog(Editor.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            importedWorldData = readWorldData(fileChooser.getSelectedFile());
                        }
                    }
                    case KeyEvent.VK_ENTER -> {
                        // merge imported world data into world data
                        editorPane.recentWorldStates.push(copyWorldData(WorldBuilder.INSTANCE.getWorldData()));
                        Map<Integer, Map<Integer, Tile>> newWorldData = WorldBuilder.INSTANCE.getWorldData();
                        for (Map.Entry<Integer, Map<Integer, Tile>> entry : importedWorldData.entrySet()) {
                            int columnIndex = entry.getKey() + importedWorldOffsetX;
                            Map<Integer, Tile> currentColumn = WorldBuilder.INSTANCE.getWorldData().getOrDefault(columnIndex, new HashMap<>());
                            for (Map.Entry<Integer, Tile> entry1 : entry.getValue().entrySet()) {
                                int rowIndex = entry1.getKey() + importedWorldOffsetY;
                                Tile tile = entry1.getValue();
                                if (columnIndex >= 0 && rowIndex >= 0) {
                                    currentColumn.put(rowIndex, tile);
                                }
                            }
                            newWorldData.put(columnIndex, currentColumn);
                        }
                        WorldBuilder.INSTANCE.setWorldData(newWorldData);
                        importedWorldData = null;
                        importedWorldOffsetX = 0;
                        importedWorldOffsetY = 0;
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        // remove imported world
                        importedWorldData = null;
                        importedWorldOffsetX = 0;
                        importedWorldOffsetY = 0;
                    }
                    case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 -> {
                        // select n-th tile
                        toolbar.prepareSelection();
                        toolbar.select(e.getKeyCode() - 0x31);
                    }
                    case KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD7, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD9 -> {
                        // select n-th tile
                        sidebarR.prepareSelection();
                        sidebarR.select(e.getKeyCode() - 0x61);
                    }
                    case KeyEvent.VK_J -> {
                        // save
                        save(null, null, false);
                    }
                    default -> {
                        // add keys that respond to holding to the list
                        activeKeys.add(e.getKeyCode());
                    }
                }
            }
            
            public void keyReleased(KeyEvent e) {
                activeKeys.removeIf(key -> key == e.getKeyCode());
            }
        });
    }
    
    private static void tempCreateMinimapWindow(BufferedImage minimap) {
        Image scaled = Scalr.resize(minimap, WorldBuilder.WIDTH / 2, WorldBuilder.HEIGHT / 2);
        JLabel content = new JLabel(new ImageIcon(scaled));
        JFrame frame = new JFrame();
        frame.setSize(WorldBuilder.WIDTH / 2 + 50, WorldBuilder.HEIGHT / 2 + 50);
        frame.setContentPane(content);
        frame.setVisible(true);
    }
    
    private BufferedImage generateMinimap() {
        Map<Integer, Map<Integer, Tile>> worldData = copyWorldData(WorldBuilder.INSTANCE.getWorldData());
        int width = (worldData.keySet().stream().max(Integer::compareTo).orElse(1) + 1) * WorldBuilder.TILE_SIZE;
        int height = (worldData.values().stream().map(map -> map.keySet().stream().max(Integer::compareTo).orElse(1)).max(Integer::compareTo).orElse(1) + 2) * WorldBuilder.TILE_SIZE;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        for (Map.Entry<Integer, Map<Integer, Tile>> entry : worldData.entrySet()) {
            int x = entry.getKey() * WorldBuilder.TILE_SIZE;
            Map<Integer, Tile> column = entry.getValue();
            for (Map.Entry<Integer, Tile> entryInner : column.entrySet()) {
                int y = entryInner.getKey() * WorldBuilder.TILE_SIZE;
                Tile tile = entryInner.getValue();
                if (tile.liquid >= 0) {
                    graphics.drawImage(LIQUID_TILEMAP.get(tile.liquid), x, y, null);
                }
                if (tile.block >= 0) {
                    graphics.drawImage(TILEMAP.get(tile.block), x, y, null);
                }
            }
        }
        return image;
    }
    
    private void loadTilemap() {
        for (int i = 0; ; i++) {
            try {
                InputStream stream = getClass().getResourceAsStream("/tiles/tile" + i + ".png");
                TILEMAP.add(ImageIO.read(stream));
            } catch (Exception ex) {
                break;
            }
        }
    }
    
    private void loadLiquidTilemap() {
        for (int i = 0; ; i++) {
            try {
                InputStream stream = getClass().getResourceAsStream("/liquids/liquid" + i + ".png");
                LIQUID_TILEMAP.add(ImageIO.read(stream));
            } catch (Exception ex) {
                break;
            }
        }
    }
    
    private void loadInteractableTilemap() {
        for (int i = 0; ; i++) {
            try {
                InputStream stream = getClass().getResourceAsStream("/interactables/interactable" + i + ".png");
                INTERACTABLE_TILEMAP.add(ImageIO.read(stream));
            } catch (Exception ex) {
                break;
            }
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (WorldBuilder.INSTANCE.getWorldData().equals(WorldBuilder.INSTANCE.getLastSavedWorldData())) {
            System.exit(0);
        } else {
            save(System::exit, Editor::new, true);
        }
    }
    
    public void save(Consumer<Integer> onApprove, Runnable onCancel, boolean discardable) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save world file");
        fileChooser.setFileFilter(jsonFileFilter());
        if (discardable) {
            JButton discardButton = new JButton("Exit");
            discardButton.addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(fileChooser, "Do you really want to discard all your changes?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    System.exit(0);
                }
            });
            ((JPanel) ((JPanel) fileChooser.getComponent(3)).getComponent(3)).add(discardButton);
        }
        int result = fileChooser.showSaveDialog(Editor.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            int success = WorldBuilder.INSTANCE.saveWorldData(fileChooser.getSelectedFile());
            if (onApprove != null) {
                onApprove.accept(success);
            }
        } else if (result == JFileChooser.CANCEL_OPTION) {
            if (onCancel != null) {
                onCancel.run();
            }
        }
    }
    
    public class EditorPane extends JPanel {
        
        private final Camera camera = new Camera(minimumX(), minimumY(), 2);
        
        private final Stack<Map<Integer, Map<Integer, Tile>>> recentWorldStates = new Stack<>();
        private final Stack<Map<Integer, Map<Integer, Tile>>> recentlyUndoneWorldStates = new Stack<>();
        
        private Point mousePosition;
        private boolean mouseInPane = true;
        private int activeMouseButton = -1;
        private int chunkX = -1;
        private int chunkY = -1;
        
        private boolean drawGrid = true;
        private boolean showPosition = false;
        private boolean showTileIndices = false;
        
        private long ticks = 0;
        
        public EditorPane() {
            addMouseWheelListener(new MouseInputAdapter() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    camera.setSpeed(camera.getSpeed() - e.getWheelRotation());
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    mousePosition = new Point(e.getPoint()).plus(0, WorldBuilder.TILE_SIZE);
                }
                
                public void mouseDragged(MouseEvent e) {
                    mouseMoved(e);
                }
            });
            addMouseListener(new MouseInputAdapter() {
                public void mousePressed(MouseEvent e) {
                    addUndoCheckpoint();
                    activeMouseButton = e.getButton();
                }
                
                public void mouseReleased(MouseEvent e) {
                    activeMouseButton = -1;
                }
                
                public void mouseEntered(MouseEvent e) {
                    mouseInPane = true;
                }
                
                public void mouseExited(MouseEvent e) {
                    mouseInPane = false;
                }
            });
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setFont(g.getFont().deriveFont(16f).deriveFont(Font.BOLD));
            g.setColor(Color.BLACK);
            if (drawGrid) {
                // DRAW VERTICAL GRID LINES
                for (int i = 0; i <= WorldBuilder.WIDTH / WorldBuilder.TILE_SIZE; i++) {
                    int offset = camera.getPosition().x() % WorldBuilder.TILE_SIZE;
                    int x = i * WorldBuilder.TILE_SIZE - offset;
                    g.drawLine(x, 0, x, WorldBuilder.HEIGHT);
                }
                // DRAW HORIZONTAL GRID LINES
                for (int i = 0; i <= WorldBuilder.HEIGHT / WorldBuilder.TILE_SIZE; i++) {
                    int offset = camera.getPosition().y() % WorldBuilder.TILE_SIZE;
                    int y = i * WorldBuilder.TILE_SIZE - offset;
                    g.drawLine(0, y, WorldBuilder.WIDTH, y);
                }
            }
            // DRAW TILES
            int offsetX = camera.getPosition().x() / WorldBuilder.TILE_SIZE - 10;
            int offsetY = camera.getPosition().y() / WorldBuilder.TILE_SIZE - 5;
            for (int i = offsetX; i <= WorldBuilder.WIDTH / WorldBuilder.TILE_SIZE + offsetX; i++) {
                for (int j = offsetY; j <= WorldBuilder.HEIGHT / WorldBuilder.TILE_SIZE + offsetY; j++) {
                    int x = (i + (WorldBuilder.WIDTH / 2) / WorldBuilder.TILE_SIZE + 1) * WorldBuilder.TILE_SIZE - camera.getPosition().x();
                    int y = (j + (WorldBuilder.HEIGHT / 2) / WorldBuilder.TILE_SIZE) * WorldBuilder.TILE_SIZE - camera.getPosition().y();
                    boolean hovered = mouseInPane && mousePosition != null && mousePosition.x() >= x && mousePosition.x() <= x + WorldBuilder.TILE_SIZE && mousePosition.y() >= y + WorldBuilder.TILE_SIZE && mousePosition.y() <= y + 2 * WorldBuilder.TILE_SIZE;
                    BufferedImage blockImage = null, liquidImage = null, interactableImage = null;
                    Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(i);
                    if (column != null) {
                        Tile tile = column.get(j);
                        if (tile != null) {
                            if (tile.block >= 0) {
                                blockImage = TILEMAP.get(tile.block);
                            }
                            if (tile.liquid >= 0) {
                                liquidImage = LIQUID_TILEMAP.get(tile.liquid);
                            }
                            if (tile.interactable >= 0) {
                                interactableImage = INTERACTABLE_TILEMAP.get(tile.interactable);
                            }
                        }
                    }
                    if (liquidImage != null) {
                        g.drawImage(liquidImage, x, y, null);
                    }
                    if (blockImage != null) {
                        g.drawImage(blockImage, x, y, null);
                    }
                    if (interactableImage != null) {
                        g.drawImage(interactableImage, x, y, null);
                    }
                    if (hovered) {
                        if (toolbar.selection >= 0) {
                            g.drawImage(transparentImage(TILEMAP.get(toolbar.selection), 0.35f), x, y, null);
                        }
                        if (sidebarR.selection >= 0) {
                            g.drawImage(transparentImage(LIQUID_TILEMAP.get(sidebarR.selection), 0.35f), x, y, null);
                        }
                        if (sidebarL.selection >= 0) {
                            g.drawImage(transparentImage(INTERACTABLE_TILEMAP.get(sidebarL.selection), 0.35f), x, y, null);
                        }
                        chunkX = i;
                        chunkY = j;
                    }
                    if (importedWorldData != null) {
                        // draw imported map
                        BufferedImage importedTileImage = null, importedLiquidImage = null, importedInteractableImage = null;
                        Map<Integer, Tile> importedColumn = importedWorldData.get(i - importedWorldOffsetX);
                        if (importedColumn != null) {
                            Tile importedTile = importedColumn.get(j - importedWorldOffsetY);
                            if (importedTile != null) {
                                if (importedTile.block >= 0) {
                                    importedTileImage = TILEMAP.get(importedTile.block);
                                }
                                if (importedTile.liquid >= 0) {
                                    importedLiquidImage = LIQUID_TILEMAP.get(importedTile.liquid);
                                }
                                if (importedTile.interactable >= 0) {
                                    importedInteractableImage = INTERACTABLE_TILEMAP.get(importedTile.interactable);
                                }
                            }
                        }
                        if (importedLiquidImage != null) {
                            g.drawImage(transparentImage(importedLiquidImage, 0.5f), x, y, null);
                        }
                        if (importedTileImage != null) {
                            g.drawImage(transparentImage(importedTileImage, 0.5f), x, y, null);
                        }
                        if (importedInteractableImage != null) {
                            g.drawImage(transparentImage(importedInteractableImage, 0.5f), x, y, null);
                        }
                    }
                    if (showTileIndices) {
                        String string = i + " | " + j;
                        int stringX = x + (WorldBuilder.TILE_SIZE - g.getFontMetrics().stringWidth(string)) / 2;
                        int stringY = y + WorldBuilder.TILE_SIZE - (WorldBuilder.TILE_SIZE - g.getFontMetrics().getHeight()) / 2;
                        g.setColor(Color.BLUE);
                        g.drawString(i + " | " + j, stringX, stringY);
                    }
                }
            }
            if (showPosition) {
                g.setColor(Color.RED);
                g.setFont(g.getFont().deriveFont(Font.BOLD));
                g.drawString("X: " + camera.getPosition().x(), 5, 20);
                g.drawString("Y: " + camera.getPosition().y(), 5, 40);
            }
            repaint();
        }
        
        @Override
        public void repaint() {
            super.repaint();
            updateCamera();
            checkClicks();
            ticks++;
        }
        
        private int minimumX() {
            return (WorldBuilder.WIDTH + WorldBuilder.TILE_SIZE) / 2 - 8;
        }
        
        private int minimumY() {
            return (WorldBuilder.HEIGHT + WorldBuilder.TILE_SIZE) / 2 - 87;
        }
        
        private boolean canMoveLeft(int speed) {
            return camera.getPosition().x() - speed >= minimumX() - 2;
        }
        
        private boolean canMoveUp(int speed) {
            return camera.getPosition().y() - speed >= minimumY() - 2;
        }
        
        private void checkClicks() {
            if (chunkX >= 0 && chunkY >= 0) {
                switch (activeMouseButton) {
                    case 1 -> { // left click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().getOrDefault(chunkX, new HashMap<>());
                        Tile current = column.get(chunkY);
                        column.put(chunkY, new Tile(
                                toolbar.selection >= 0 ? toolbar.selection : (current == null ? -1 : current.block),
                                sidebarR.selection >= 0 ? sidebarR.selection : (current == null ? -1 : current.liquid),
                                sidebarL.selection >= 0 ? sidebarL.selection : (current == null ? -1 : current.interactable)
                        ));
                        WorldBuilder.INSTANCE.getWorldData().put(chunkX, column);
                        if (inferOrientation) {
                            WorldBuilder.INSTANCE.updateChunk(chunkX, chunkY, false);
                        }
                    }
                    case 2 -> { // middle click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(chunkX);
                        if (column != null) {
                            Tile tile = column.get(chunkY);
                            if (tile != null) {
                                if (tile.block >= 0) {
                                    toolbar.prepareSelection();
                                    toolbar.select(tile.block);
                                }
                                if (tile.liquid >= 0) {
                                    sidebarR.prepareSelection();
                                    sidebarR.select(tile.liquid);
                                }
                            }
                        }
                    }
                    case 3 -> { // right click
                        Map<Integer, Tile> column = WorldBuilder.INSTANCE.getWorldData().get(chunkX);
                        if (column != null) {
                            column.remove(chunkY);
                            if (column.isEmpty()) {
                                WorldBuilder.INSTANCE.getWorldData().remove(chunkX);
                            } else {
                                WorldBuilder.INSTANCE.getWorldData().put(chunkX, column);
                            }
                        }
                        if (inferOrientation) {
                            WorldBuilder.INSTANCE.updateChunk(chunkX, chunkY, false);
                        }
                    }
                }
            }
        }
        
        private void updateCamera() {
            if (activeKeys != null && camera != null) {
                int speed = camera.getSpeed();
                if (activeKeys.contains(KeyEvent.VK_SHIFT)) {
                    speed = Camera.MIN_SPEED;
                } else if (activeKeys.contains(KeyEvent.VK_ALT)) {
                    speed = Camera.MAX_SPEED;
                }
                if (activeKeys.contains(KeyEvent.VK_A) && canMoveLeft(speed)) {
                    camera.moveX(-speed);
                } else if (activeKeys.contains(KeyEvent.VK_D)) {
                    camera.moveX(speed);
                }
                if (activeKeys.contains(KeyEvent.VK_W) && canMoveUp(speed)) {
                    camera.moveY(-speed);
                } else if (activeKeys.contains(KeyEvent.VK_S)) {
                    camera.moveY(speed);
                }
            }
        }
    
        private void addUndoCheckpoint() {
            Map<Integer, Map<Integer, Tile>> currentWorld = copyWorldData(WorldBuilder.INSTANCE.getWorldData());
            if (recentWorldStates.isEmpty() || !currentWorld.equals(recentWorldStates.peek())) {
                recentWorldStates.push(currentWorld);
            }
        }
        
    }
    
    public abstract class Toolbar extends JScrollPane {
        
        public static final int MARGIN = 15;
        
        protected int recentSelection = -1;
        protected int selection = -1;
        
        protected abstract void select(int selection);
        
        public final int getSelection() {
            return selection;
        }
        
        public final void toggleSelection() {
            if (recentSelection == -1) {
                recentSelection = selection;
                select(-1);
            } else {
                select(recentSelection);
                recentSelection = -1;
            }
        }
        
        public final void prepareSelection() {
            if (recentSelection >= 0) {
                toggleSelection();
            }
        }
        
        public class ToolbarElement extends JLabel {
            
            private final Toolbar parent;
            private final BufferedImage image;
            private final int index;
            
            private boolean selected = true;
            private boolean hovered = false;
            
            public ToolbarElement(Toolbar parent, BufferedImage image, int index) {
                this.parent = parent;
                this.image = image;
                this.index = index;
                setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
                setIcon(new ImageIcon(this.image));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                    }
                    
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                    }
                });
            }
            
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if (selected) {
                    setIcon(new ImageIcon(this.image));
                    setBorder(new MatteBorder(MARGIN, MARGIN, MARGIN, MARGIN, new Color(!inferOrientation || parent instanceof InteractableToolbar ? 0x4287f5 : 0xdb2137)));
                } else {
                    setIcon(new ImageIcon(hovered ? this.image : transparentImage(this.image, 0.35f)));
                    setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
                }
                repaint();
            }
            
            @Override
            public void repaint() {
                super.repaint();
                selected = (parent != null && parent.getSelection() == index);
            }
            
        }
    }
    
    public class LiquidToolbar extends Toolbar {
        
        public LiquidToolbar() {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            for (int i = 0; i < LIQUID_TILEMAP.size(); i++) {
                int index = i;
                BufferedImage liquidTile = LIQUID_TILEMAP.get(i);
                ToolbarElement image = new ToolbarElement(this, liquidTile, index);
                image.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        prepareSelection();
                        select(index);
                    }
                });
                image.setVisible(true);
                content.add(image);
            }
            setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
            setViewportView(content);
            setVisible(true);
        }
        
        @Override
        public void select(int selection) {
            this.selection = selection;
            if (selection >= 0) {
                JScrollBar scrollBar = getVerticalScrollBar();
                int min = scrollBar.getMinimum();
                int max = scrollBar.getMaximum();
                double ratio = Math.max(0, Math.min(1, (double) selection / ((double) LIQUID_TILEMAP.size() - 1) - 0.2));
                scrollBar.setValue(min + (int) (ratio * (max - min)));
            }
        }
        
    }
    
    public class InteractableToolbar extends Toolbar {
        
        public InteractableToolbar() {
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            for (int i = 0; i < INTERACTABLE_TILEMAP.size(); i++) {
                int index = i;
                BufferedImage liquidTile = INTERACTABLE_TILEMAP.get(i);
                ToolbarElement image = new ToolbarElement(this, liquidTile, index);
                image.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        prepareSelection();
                        select(index);
                    }
                });
                image.setVisible(true);
                content.add(image);
            }
            setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
            setViewportView(content);
            setVisible(true);
        }
    
        @Override
        public void select(int selection) {
            this.selection = selection;
            if (selection >= 0) {
                JScrollBar scrollBar = getVerticalScrollBar();
                int min = scrollBar.getMinimum();
                int max = scrollBar.getMaximum();
                double ratio = Math.max(0, Math.min(1, (double) selection / ((double) LIQUID_TILEMAP.size() - 1) - 0.2));
                scrollBar.setValue(min + (int) (ratio * (max - min)));
            }
        }
        
    }
    
    public class TileToolbar extends Toolbar {
        
        public TileToolbar() {
            JPanel content = new JPanel();
            content.setLayout(new FlowLayout());
            for (int i = 0; i < TILEMAP.size(); i++) {
                int index = i;
                BufferedImage tile = TILEMAP.get(i);
                ToolbarElement image = new ToolbarElement(this, tile, index);
                image.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        prepareSelection();
                        select(index);
                    }
                });
                image.setVisible(true);
                content.add(image);
            }
            setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
            setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
            setViewportView(content);
            setVisible(true);
        }
        
        @Override
        public void select(int selection) {
            this.selection = selection;
            if (selection >= 0) {
                JScrollBar scrollBar = getHorizontalScrollBar();
                int min = scrollBar.getMinimum();
                int max = scrollBar.getMaximum();
                double ratio = Math.max(0, Math.min(1, (double) selection / ((double) TILEMAP.size() - 1) - 0.2));
                scrollBar.setValue(min + (int) (ratio * (max - min)));
            }
        }
        
    }
    
}
