package pl.edu.agh.sm.eyetracking;

import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;

public class PointRegion {

    private static final int EPS_CENTER = 0;

    private Point center;

    private final Size screenSize;

    public PointRegion(Size screenSize) {
        this.screenSize = screenSize;

        // default face size
        center = new Point(screenSize.width / 2, screenSize.height / 2);
    }

    public void update(Point potentialCenter) {
        if (Math.abs(potentialCenter.x - center.x) > EPS_CENTER
                || Math.abs(potentialCenter.y - center.y) > EPS_CENTER) {
            center = potentialCenter;
        }
    }

    public Point get() {
        return get(new Point(0, 0));
    }

    public Point get(Point offset) {
        int x = center.x + offset.x;
        int y = center.y + offset.y;

        if (x < 0) {
            x = 0;
        }
        if (x > screenSize.width) {
            x = screenSize.width;
        }
        if (y < 0) {
            y = 0;
        }
        if (y > screenSize.height) {
            y = screenSize.height;
        }

        return new Point(x, y);
    }
}
