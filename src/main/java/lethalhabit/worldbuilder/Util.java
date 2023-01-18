package lethalhabit.worldbuilder;

import com.google.gson.Gson;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Util {
    
    private Util() {
    }
    
    public static Map<Integer, Map<Integer, Tile>> copyWorldData(Map<Integer, Map<Integer, Tile>> data) {
        Map<Integer, Map<Integer, Tile>> copy = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Tile>> entry : data.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }
    
    public static Map<Integer, Map<Integer, Tile>> readWorldData(File worldFile) {
        try {
            return readWorldData(new FileInputStream(worldFile));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "World data could not be loaded.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return new HashMap<>();
        }
    }
    
    public static Map<Integer, Map<Integer, Tile>> readWorldData(InputStream stream) {
        Map<Integer, Map<Integer, Tile>> worldData = new HashMap<>();
        try {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Map<String, Map<String, Map<String, Double>>> strings = gson.fromJson(json, Map.class);
            for (Map.Entry<String, Map<String, Map<String, Double>>> entry : strings.entrySet()) {
                int key = Integer.parseInt(entry.getKey());
                Map<Integer, Tile> value = new HashMap<>();
                for (Map.Entry<String, Map<String, Double>> entryInner : entry.getValue().entrySet()) {
                    int keyInner = Integer.parseInt(entryInner.getKey());
                    Tile valueInner = new Tile(
                            entryInner.getValue().getOrDefault("block", -1D).intValue(),
                            entryInner.getValue().getOrDefault("liquid", -1D).intValue(),
                            entryInner.getValue().getOrDefault("interactable", -1D).intValue()
                    );
                    value.put(keyInner, valueInner);
                }
                worldData.put(key, value);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "World data could not be loaded.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return worldData;
    }
    
    public static FileFilter jsonFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".json");
            }
            
            public String getDescription() {
                return "JSON Files (.json)";
            }
        };
    }
    
    public static FileFilter pngFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".png");
            }
            
            public String getDescription() {
                return "PNG Files (.png)";
            }
        };
    }
    
    public static DocumentFilter integerInputFilter() {
        return new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                Document doc = fb.getDocument();
                StringBuilder sb = new StringBuilder();
                sb.append(doc.getText(0, doc.getLength()));
                sb.insert(offset, string);
                
                if (test(sb.toString())) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            private boolean test(String text) {
                try {
                    Integer.parseInt(text);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                Document doc = fb.getDocument();
                StringBuilder sb = new StringBuilder();
                sb.append(doc.getText(0, doc.getLength()));
                sb.replace(offset, offset + length, text);
                
                if (test(sb.toString())) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
            
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                Document doc = fb.getDocument();
                StringBuilder sb = new StringBuilder();
                sb.append(doc.getText(0, doc.getLength()));
                sb.delete(offset, offset + length);
                
                if (test(sb.toString())) {
                    super.remove(fb, offset, length);
                }
            }
        };
    }
    
    public static BufferedImage transparentImage(BufferedImage image, float transparency) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return newImage;
    }
    
    public static void createMinimapWindow(BufferedImage minimap) {
        Image scaled = Scalr.resize(minimap, WorldBuilder.WIDTH / 2, WorldBuilder.HEIGHT / 2);
        JLabel content = new JLabel(new ImageIcon(scaled));
        JFrame frame = new JFrame();
        frame.setSize(WorldBuilder.WIDTH / 2 + 50, WorldBuilder.HEIGHT / 2 + 50);
        frame.setContentPane(content);
        frame.setVisible(true);
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F) {
                    saveDialog(file -> saveMinimap(minimap, file), () -> {}, false, pngFileFilter(), "Save minimap", frame);
                }
            }
        });
    }
    
    public static void saveMinimap(BufferedImage image, File file) {
        try {
            ImageIO.write(image, "png", file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "World data could not be saved.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void saveDialog(Consumer<File> onApprove, Runnable onCancel, boolean discardable, FileFilter filter, String title, Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setFileFilter(filter);
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
        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            if (onApprove != null) {
                onApprove.accept(fileChooser.getSelectedFile());
            }
        } else if (result == JFileChooser.CANCEL_OPTION) {
            if (onCancel != null) {
                onCancel.run();
            }
        }
    }
    
}
