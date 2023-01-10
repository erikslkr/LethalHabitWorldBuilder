package lethalhabit.worldbuilder;

public final class Tile {
    
    public static final Tile EMPTY = new Tile(-1, -1);
    
    public final int block;
    public final int liquid;
    
    public Tile(int block, int liquid) {
        this.block = block;
        this.liquid = liquid;
    }
    
    public Tile(Tile other) {
        this(other.block, other.liquid);
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
