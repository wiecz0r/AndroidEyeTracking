package pl.edu.agh.sm.eyetracking;

import org.opencv.core.Rect;

import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;

public class SquareRegion {

    private static final int EPS_CENTER = 2 * 8;
    private static final int EPS_SIZE = 5 * 8;

    private Point center;
    private int sideLength;

    private final Size screenSize;

    public SquareRegion(Size screenSize) {
        this.screenSize = screenSize;

        // default face size
        center = new Point(screenSize.width / 2, screenSize.height / 2);
        sideLength = screenSize.width / 4;
    }

    public void update(Rect region) {
        Point potentialCenter = new Point(
                region.x + (region.width / 2),
                region.y + (region.height / 2)
        );

        if (Math.abs(potentialCenter.x - center.x) > EPS_CENTER
                || Math.abs(potentialCenter.y - center.y) > EPS_CENTER) {
            center = potentialCenter;
        }

        if (Math.abs(region.width - sideLength) > EPS_SIZE) {
            sideLength = region.width;
        }
    }

    public Rect get() {
        int x = getLeftCornerX();
        int y = getLeftCornerY();
        int width = sideLength;
        int height = sideLength;

        if (x < 0) {
            width += x;
            x = 0;
        }
        if (x + width > screenSize.width) {
            width = screenSize.width - x;
        }
        if (y < 0) {
            height += y;
            y = 0;
        }
        if (y + height > screenSize.height) {
            height = screenSize.height - y;
        }

        return new Rect(x, y, width, height);
    }

    private int getLeftCornerX() {
        return center.x - sideLength / 2;
    }

    private int getLeftCornerY() {
        return center.y - sideLength / 2;
    }
}
