package pl.edu.agh.sm.eyetracking;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import pl.edu.agh.sm.eyetracking.util.Size;

public class Frame {

    private Mat frame;
    private final Size size;

    public Frame(Size originalSize, int scale) {
        size = new Size(
                originalSize.width / scale,
                originalSize.height / scale);
        frame = new Mat(size.toOpenCV(), CvType.CV_8UC1);
    }

    public void update(CameraBridgeViewBase.CvCameraViewFrame originalFrame) {
        Imgproc.resize(originalFrame.gray(), frame, size.toOpenCV());
    }

    public void release() {
        frame.release();
    }

    public Mat get() {
        return frame;
    }
}
