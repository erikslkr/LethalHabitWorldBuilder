package lethalhabit.worldbuilder;

public record Point(int x, int y) {
    
    public Point(Point point) {
        this(point.x, point.y);
    }
    
    public Point(java.awt.Point point) {
        this(point.x, point.y);
    }
    
    public Point plus(int x, int y) {
        return new Point(this.x + x, this.y + y);
    }
    
}
