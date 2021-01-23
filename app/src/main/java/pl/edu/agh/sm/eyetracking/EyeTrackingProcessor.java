package pl.edu.agh.sm.eyetracking;

import androidx.core.util.Pair;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import pl.edu.agh.sm.eyetracking.detectors.EyeDetector;
import pl.edu.agh.sm.eyetracking.detectors.FaceDetector;
import pl.edu.agh.sm.eyetracking.detectors.PupilDetector;
import pl.edu.agh.sm.eyetracking.util.Circle;
import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Size;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = EyeTrackingProcessor.class.getCanonicalName();

    private final FaceDetector faceDetector;
    private final EyeDetector eyeDetector;
    private final PupilDetector pupilDetector;

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

            Rect leftEyeROI = eyeROIs.first;
            if (leftEyeROI != null) {
                drawRectangle(inputImage, leftEyeROI, new Scalar(0, 255, 0), 2);
            }

            Rect rightEyeROI = eyeROIs.second;
            if (rightEyeROI != null) {
                drawRectangle(inputImage, rightEyeROI, new Scalar(0, 255, 128), 2);
            }


            Pair<Circle, Circle> pupilROIs = pupilDetector.detect(frame, eyeROIs);

            Circle leftPupilROI = pupilROIs.first;
            if (leftPupilROI != null) {
                drawCircle(inputImage, leftPupilROI);
            }
            Circle rightPupilROI = pupilROIs.second;
            if (rightPupilROI != null) {
                drawCircle(inputImage, rightPupilROI);
            }


            if (leftEyeROI != null && leftPupilROI != null) {
                visualiseTracking(inputImage, leftEyeROI, leftPupilROI);
            }

            if (rightEyeROI != null && rightPupilROI != null) {
                visualiseTracking(inputImage, rightEyeROI, rightPupilROI);
            }
        }

        Core.flip(inputImage, inputImage, 1);

        inputImage.copyTo(outputImage);
        inputImage.release();
    }

    private void visualiseTracking(Mat inputImage, Rect eyeROI, Circle pupilROI) {
        Point pupilCenter = new Point(eyeROI.x + eyeROI.width / 2, eyeROI.y + eyeROI.height / 2);
        Imgproc.arrowedLine(inputImage,
                pupilCenter.toOpenCV(),
                pupilROI.center.toOpenCV(),
                new Scalar(255, 255, 0),
                2);

        Point arrowStart = new Point(
                pupilCenter.x,
                pupilCenter.y + eyeROI.height
        );

        int deltaX = (pupilROI.center.x - pupilCenter.x) * 3;
        int deltaY = (pupilROI.center.y - pupilCenter.y) * 3;

        Point arrowEnd = new Point(
                arrowStart.x + deltaX,
                arrowStart.y + deltaY
        );

        Imgproc.arrowedLine(inputImage,
                arrowStart.toOpenCV(),
                arrowEnd.toOpenCV(),
                new Scalar(255, 255, 0),
                4);
    }

    private void drawRectangle(Mat inputImage, Rect rect, Scalar color, int thickness) {
        Imgproc.rectangle(inputImage,
                new Point(rect.x, rect.y).toOpenCV(),
                new Point(rect.x + rect.width, rect.y + rect.height).toOpenCV(),
                color,
                thickness
        );
    }

    private void drawCircle(Mat inputImage, Circle circle) {
        Imgproc.circle(inputImage,
                circle.center.toOpenCV(),
                circle.radius,
                new Scalar(0, 255, 255),
                1);
    }

    public void setThreshold(int threshold) {
        pupilDetector.setThreshold(threshold);
    }
}
