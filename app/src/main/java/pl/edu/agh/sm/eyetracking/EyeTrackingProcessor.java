package pl.edu.agh.sm.eyetracking;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat outputImage;
    private Mat scaledGrayImage;
    private Size scaledSize;

    private int imageWidth;
    private int imageHeight;

    private final CascadeClassifier faceDetector;


    private final int SCALE = 8;


    EyeTrackingProcessor(CascadeClassifier classifier) {
        faceDetector = classifier;
    }

    private int getScaledWidth() {
        return imageWidth / SCALE;
    }

    private int getScaledHeight() {
        return imageHeight / SCALE;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;

//        outputImage = new Mat(height, width, CvType.CV_8UC4);
        scaledGrayImage = new Mat(getScaledHeight(), getScaledWidth(), CvType.CV_8UC1);
        scaledSize = new Size(getScaledWidth(), getScaledHeight());
    }

    @Override
    public void onCameraViewStopped() {
        outputImage.release();
        scaledGrayImage.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        process(cvCameraViewFrame);
        return outputImage;
    }

    private void process(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        Mat inputImage = cvCameraViewFrame.rgba();

        Imgproc.resize(cvCameraViewFrame.gray(), scaledGrayImage, scaledSize);
        // --- face detection
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(
                scaledGrayImage,
                faces,
                1.1,
                2,
                2,
                new Size(getScaledHeight() >> 2, getScaledHeight() >> 2),
                new Size(getScaledHeight(), getScaledHeight())
        );

//        Mat progressMat = new Mat((int) scaledSize.height, (int) scaledSize.width, CvType.CV_8UC4);
//        Imgproc.cvtColor(scaledGrayImage, progressMat, Imgproc.COLOR_GRAY2RGBA);
//        progressMat.copyTo(inputImage.submat(new Rect(0, 0, (int) scaledSize.width, (int) scaledSize.height)));
//        progressMat.release();

        // draw red rectangles for detected faces
        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(inputImage,
                    new Point(rect.x * SCALE, rect.y * SCALE),
                    new Point((rect.x + rect.width) * SCALE, (rect.y + rect.height) * SCALE),
                    new Scalar(255, 0, 0),
                    3
            );
        }

//        inputImage.copyTo(outputImage);
//        inputImage.release();
        outputImage = inputImage;
    }

}
