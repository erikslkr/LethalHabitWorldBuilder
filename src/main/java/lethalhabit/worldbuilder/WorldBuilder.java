package lethalhabit.worldbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static lethalhabit.worldbuilder.Util.*;

public final class WorldBuilder {
    
    public static final int TILE_SIZE = 100;
    
    public static final int WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    public static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    
    public static final WorldBuilder INSTANCE = new WorldBuilder();
    
    private Map<Integer, Map<Integer, Tile>> lastSavedWorldData = new HashMap<>();
    private Map<Integer, Map<Integer, Tile>> worldData = new HashMap<>();
    
    public static void main(String[] args) {
        INSTANCE.start();
    }
    
    public void start() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open world file");
        fileChooser.setFileFilter(jsonFileFilter());
        JButton createNewButton = new JButton("Create New");
        createNewButton.addActionListener(e -> {
            fileChooser.approveSelection();
        });
        ((JPanel) ((JPanel) fileChooser.getComponent(3)).getComponent(3)).add(createNewButton);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.getSelectedFile() != null) {
                worldData = readWorldData(fileChooser.getSelectedFile());
                lastSavedWorldData = readWorldData(fileChooser.getSelectedFile());
            }
            new Editor();
        } else if (result == JFileChooser.CANCEL_OPTION) {
            System.exit(0);
        }
    }
    
    public int saveWorldData(File worldFile) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String output = gson.toJson(worldData);
            Files.writeString(Path.of(worldFile.getPath()), output);
            lastSavedWorldData = copyWorldData(worldData);
            return 0;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "World data could not be saved.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
    }
    
    public Map<Integer, Map<Integer, Tile>> getWorldData() {
        return worldData;
    }
    
    public void setWorldData(Map<Integer, Map<Integer, Tile>> worldData) {
        this.worldData = worldData;
    }
    
    public Map<Integer, Map<Integer, Tile>> getLastSavedWorldData() {
        return lastSavedWorldData;
    }
    
    private WorldBuilder() { }
    
}
