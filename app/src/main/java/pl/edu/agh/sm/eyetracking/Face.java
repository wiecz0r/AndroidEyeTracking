package pl.edu.agh.sm.eyetracking;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class Face {

    public static final int SCALE = 8;


    private Point center;
    private Integer size;

    // default face
    public Face(int width, int height) {
        this.center = new Point(width >> 1, height >> 1);
        this.size = width / 4;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public double getX() {
        return this.center.x - (double) this.size / 2;
    }

    public double getY() {
        return this.center.y - (double) this.size / 2;
    }

    public Rect getRect() {
        Rect roi = new Rect((int) getX(), (int) getY(), getSize(), getSize());
        scale(roi);
        return roi;
    }

    private static void scale(Rect rect) {
        rect.x *= SCALE;
        rect.y *= SCALE;
        rect.width *= SCALE;
        rect.height *= SCALE;
    }
}
