package lethalhabit.worldbuilder;

public final class Camera {
    
    public static final int MAX_SPEED = 20;
    public static final int MIN_SPEED = 1;
    
    private int speed;
    
    /**
     * The center of the camera
     */
    private Point position;
    
    public Camera(Point position, int speed) {
        this.position = position;
        this.speed = speed;
    }
    
    public Camera(int xPosition, int yPosition, int speed) {
        this(new Point(xPosition, yPosition), speed);
    }
    
    public void setPosition(Point position) {
        this.position = position;
    }
    
    public void setPosition(int x, int y) {
        this.position = new Point(x, y);
    }
    
    public Point getPosition() {
        return position;
    }
    
    public void setSpeed(int speed) {
        this.speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }
    
    public int getSpeed() {
        return speed;
    }
    
    public void move(int x, int y) {
        position = position.plus(x, y);
    }
    
    public void moveX(int amount) {
        move(amount, 0);
    }
    
    public void moveY(int amount) {
        move(0, amount);
    }
    
    public void moveLeft() { moveX(-speed); }
    public void sneakLeft() { moveX(-MIN_SPEED); }
    public void sprintLeft() { moveX(-MAX_SPEED); }
    
    public void moveRight() { moveX(speed); }
    public void sneakRight() { moveX(MIN_SPEED); }
    public void sprintRight() { moveX(MAX_SPEED); }
    
    public void moveDown() { moveY(speed); }
    public void sneakDown() { moveY(MIN_SPEED); }
    public void sprintDown() { moveY(-MAX_SPEED); }
    
    public void moveUp() { moveY(-speed); }
    public void sneakUp() { moveY(-MIN_SPEED); }
    public void sprintUp() { moveY(-MAX_SPEED); }
    
}
