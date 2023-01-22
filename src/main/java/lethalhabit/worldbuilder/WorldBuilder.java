package lethalhabit.worldbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static lethalhabit.worldbuilder.Util.*;

public final class WorldBuilder {
    
    public static int TILE_SIZE = 100;
    
    public static final int WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    public static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    
    public static final int TILE_GROUP_SIZE = 16;
    public static final int LIQUID_GROUP_SIZE = 2;
    
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
    
    private static int getBlockOrientationOffset(int chunkX, int chunkY) {
        boolean above = WorldBuilder.INSTANCE.getWorldData().get(chunkX).getOrDefault(chunkY - 1, Tile.EMPTY).block < 0;
        boolean below = WorldBuilder.INSTANCE.getWorldData().get(chunkX).getOrDefault(chunkY + 1, Tile.EMPTY).block < 0;
        boolean left = WorldBuilder.INSTANCE.getWorldData().getOrDefault(chunkX - 1, new HashMap<>()).getOrDefault(chunkY, Tile.EMPTY).block < 0;
        boolean right = WorldBuilder.INSTANCE.getWorldData().getOrDefault(chunkX + 1, new HashMap<>()).getOrDefault(chunkY, Tile.EMPTY).block < 0;
        if (above) {
            if (below) {
                if (left) {
                    return right ? 15 : 12;
                } else {
                    return right ? 14 : 13;
                }
            } else {
                if (left) {
                    return right ? 11 : 8;
                } else {
                    return right ? 10 : 9;
                }
            }
        } else {
            if (below) {
                if (left) {
                    return right ? 3 : 4;
                } else {
                    return right ? 2 : 5;
                }
            } else {
                if (left) {
                    return right ? 7 : 6;
                } else {
                    return right ? 1 : 0;
                }
            }
        }
    }
    
    private static int getLiquidOrientationOffset(int chunkX, int chunkY) {
        int above = WorldBuilder.INSTANCE.getWorldData().get(chunkX).getOrDefault(chunkY - 1, Tile.EMPTY).liquid;
        if (above >= 0) {
            return 0;
        } else {
            return 1;
        }
    }
    
    public void autoShapeChunk(int chunkX, int chunkY, boolean suppressAdjacentUpdates) {
        Map<Integer, Tile> column = getWorldData().getOrDefault(chunkX, new HashMap<>());
        Tile currentTile = new Tile(column.getOrDefault(chunkY, Tile.EMPTY));
        if (!currentTile.equals(Tile.EMPTY)) {
            int liquidIndex = -1;
            int blockIndex = -1;
            if (currentTile.liquid >= 0) {
                liquidIndex = (currentTile.liquid / LIQUID_GROUP_SIZE) * LIQUID_GROUP_SIZE + getLiquidOrientationOffset(chunkX, chunkY);
            }
            if (currentTile.block >= 0) {
                blockIndex = (currentTile.block / TILE_GROUP_SIZE) * TILE_GROUP_SIZE + getBlockOrientationOffset(chunkX, chunkY);
            }
            if (liquidIndex != currentTile.liquid || blockIndex != currentTile.block) {
                column.put(chunkY, new Tile(blockIndex, liquidIndex, currentTile.interactable));
                getWorldData().put(chunkX, column);
            }
        }
        if (!suppressAdjacentUpdates) {
            autoShapeChunk(chunkX + 1, chunkY, true);
            autoShapeChunk(chunkX - 1, chunkY, true);
            autoShapeChunk(chunkX, chunkY + 1, true);
            autoShapeChunk(chunkX, chunkY - 1, true);
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
