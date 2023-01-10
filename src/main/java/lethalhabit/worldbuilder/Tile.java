package lethalhabit.worldbuilder;

public final class Tile {
    
    public final int block;
    public final int liquid;
    
    public Tile(int block, int liquid) {
        this.block = block;
        this.liquid = liquid;
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof Tile tile && this.block == tile.block && this.liquid == tile.liquid;
    }
    
    @Override
    public String toString() {
        return "Tile[block=" + block + ";liquid=" + liquid + "]";
    }
    
}
