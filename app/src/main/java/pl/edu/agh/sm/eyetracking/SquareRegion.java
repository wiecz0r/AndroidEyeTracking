package pl.edu.agh.sm.eyetracking;

import org.opencv.core.Rect;

import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;

public class SquareRegion {

    private Point center;
    private int sideLength;
    private final int centerEpsilon;
    private final int sideEpsilon;

    private final Size screenSize;

    public SquareRegion(Size screenSize, Point center, int side, int centerEpsilon, int sideEpsilon) {
        this.screenSize = screenSize;
        this.center = center;
        this.sideLength = side;
        this.centerEpsilon = centerEpsilon;
        this.sideEpsilon = sideEpsilon;
    }

    public void update(Rect region) {
        Point potentialCenter = new Point(
                region.x + (region.width / 2),
                region.y + (region.height / 2)
        );

        if (Math.abs(potentialCenter.x - center.x) > centerEpsilon
                || Math.abs(potentialCenter.y - center.y) > centerEpsilon) {
            center = potentialCenter;
        }

        if (Math.abs(region.width - sideLength) > sideEpsilon) {
            sideLength = region.width;
        }
    }

    public Rect get() {
        return get(new Point(0, 0));
    }

    public Rect get(Point offset) {
        int x = getLeftCornerX() + offset.x;
        int y = getLeftCornerY() + offset.y;
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
