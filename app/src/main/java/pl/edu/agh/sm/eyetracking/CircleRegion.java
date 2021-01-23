package pl.edu.agh.sm.eyetracking;

import org.opencv.core.KeyPoint;

import pl.edu.agh.sm.eyetracking.util.Circle;
import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;

public class CircleRegion {

    private Point center;
    private int radius;
    private final int centerEpsilon;
    private final int radiusEpsilon;

    private final Size screenSize;

    public CircleRegion(Size screenSize, Point center, int radius, int centerEpsilon, int radiusEpsilon) {
        this.screenSize = screenSize;
        this.center = center;
        this.radius = radius;
        this.centerEpsilon = centerEpsilon;
        this.radiusEpsilon = radiusEpsilon;
    }

    public void update(KeyPoint blob) {
        Point potentialCenter = new Point(
                (int) blob.pt.x,
                (int) blob.pt.y
        );

        if (Math.abs(potentialCenter.x - center.x) > centerEpsilon
                || Math.abs(potentialCenter.y - center.y) > centerEpsilon) {
            center = potentialCenter;
        }

        if (Math.abs(blob.size - radius) > radiusEpsilon) {
            radius = (int) blob.size;
        }
    }

    public Circle get() {
        return get(new Point(0, 0));
    }

    public Circle get(Point offset) {
        int x = center.x + offset.x;
        int y = center.y + offset.y;
        int radius = this.radius;

        if (x < 0) {
            x = 0;
        }
        if (x - radius < 0) {
            radius = x;
        }
        if (x + radius > screenSize.width) {
            radius = screenSize.width - x;
        }
        if (y < 0) {
            y = 0;
        }
        if (y - radius < 0) {
            radius = y;
        }
        if (y + radius > screenSize.height) {
            radius = screenSize.height;
        }

        return new Circle(
                new Point(x, y),
                radius
        );
    }
}
