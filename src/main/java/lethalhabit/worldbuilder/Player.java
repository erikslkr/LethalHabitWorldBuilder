package lethalhabit.worldbuilder;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Player {
    
    private int posX;
    private int posY;
    
    private PlayerStats stats;
    
    public static Player loadPlayerData() throws IOException {
        String json = Files.readString(Path.of("resources/playerdata.json"));
        Gson gson = new Gson();
        return gson.fromJson(json, Player.class);
    }
    
}
