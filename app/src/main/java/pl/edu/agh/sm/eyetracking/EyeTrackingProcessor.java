package pl.edu.agh.sm.eyetracking;

import androidx.core.util.Pair;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import pl.edu.agh.sm.eyetracking.detectors.EyeDetector;
import pl.edu.agh.sm.eyetracking.detectors.FaceDetector;
import pl.edu.agh.sm.eyetracking.detectors.PupilDetector;
import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = EyeTrackingProcessor.class.getCanonicalName();

    private final FaceDetector faceDetector;
    private final EyeDetector eyeDetector;
    private final PupilDetector pupilDetector;
//    private final PupilDetector pupilDetector;

    private Mat outputImage;

    public EyeTrackingProcessor(CascadeClassifier faceClassifier, CascadeClassifier eyeClassifier) {
        faceDetector = new FaceDetector(faceClassifier);
        eyeDetector = new EyeDetector(eyeClassifier);
        pupilDetector = new PupilDetector();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Size screenSize = new Size(width, height); 
        outputImage = new Mat(screenSize.toOpenCV(), CvType.CV_8UC4);

        faceDetector.initialize(screenSize);
        eyeDetector.initialize(screenSize);
        pupilDetector.initialize(screenSize);
    }

    @Override
    public void onCameraViewStopped() {
        pupilDetector.deinitialize();
        eyeDetector.deinitialize();
        faceDetector.deinitialize();
        
        outputImage.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        process(cvCameraViewFrame);
        return outputImage;
    }

    private void process(CameraBridgeViewBase.CvCameraViewFrame frame) {
        Mat inputImage = frame.rgba();
        
        Rect faceROI = faceDetector.detect(frame);

        if (faceROI != null) {
            drawRectangle(inputImage, faceROI, new Scalar(255, 0, 0), 3);

            Pair<Rect, Rect> eyeROIs = eyeDetector.detect(frame, faceROI);

            if (eyeROIs.first != null) {
                drawRectangle(inputImage, eyeROIs.first, new Scalar(0, 255, 0), 2);
            }
            if (eyeROIs.second != null) {
                drawRectangle(inputImage, eyeROIs.second, new Scalar(0, 255, 128), 2);
            }


            Pair<Point, Point> pupilROIs = pupilDetector.detect(frame, eyeROIs);

            if (pupilROIs.first != null) {
                Imgproc.circle(inputImage, pupilROIs.first.toOpenCV(), 4, new Scalar(0, 255, 255), 1);
            }
            if (pupilROIs.second != null) {
                Imgproc.circle(inputImage, pupilROIs.second.toOpenCV(), 4, new Scalar(0, 255, 255), 1);
            }
        }

        Core.flip(inputImage, inputImage, 1);

        inputImage.copyTo(outputImage);
        inputImage.release();
    }

    private void drawRectangle(Mat inputImage, Rect rect, Scalar color, int thickness) {
        Imgproc.rectangle(inputImage,
                new Point(rect.x, rect.y).toOpenCV(),
                new Point(rect.x + rect.width, rect.y + rect.height).toOpenCV(),
                color,
                thickness
        );
    }
}
