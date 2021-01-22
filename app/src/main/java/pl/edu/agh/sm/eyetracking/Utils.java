package pl.edu.agh.sm.eyetracking;

import org.opencv.core.Rect;

public class Utils {

    public static void scale(Rect rect, int scale) {
        rect.x *= scale;
        rect.y *= scale;
        rect.width *= scale;
        rect.height *= scale;
    }
}
