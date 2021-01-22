package pl.edu.agh.sm.eyetracking.util;

import org.opencv.core.Rect;

public class Scale {

    public static void scaleUp(Rect rect, int scale) {
        rect.x *= scale;
        rect.y *= scale;
        rect.width *= scale;
        rect.height *= scale;
    }

    public static void scaleDown(Rect rect, int scale) {
        rect.x /= scale;
        rect.y /= scale;
        rect.width /= scale;
        rect.height /= scale;
    }
}
