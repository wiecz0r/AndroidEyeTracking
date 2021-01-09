package pl.edu.agh.sm.eyetracking;

import android.util.Log;

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

    private static final String TAG = EyeTrackingProcessor.class.getCanonicalName();
    private static final int EPS_CENTER = 2;
    private static final int EPS_SIZE = 5;
    private final int SCALE = 4;

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier eyeDetector;

    private Mat outputImage;
    private Mat scaledGrayImage;
    private Size scaledSize;
    private Face face;
    private Rect eye1 = new Rect(0,0,0,0);
    private Rect eye2 = new Rect(0,0,0,0);

    private int imageWidth;
    private int imageHeight;

    EyeTrackingProcessor(CascadeClassifier faceClassifier, CascadeClassifier eyeClassifier) {
        faceDetector = faceClassifier;
        eyeDetector = eyeClassifier;
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
        this.face = new Face(width, height);

        outputImage = new Mat(height, width, CvType.CV_8UC4);
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

        if (faces.toArray().length > 0) {
            Rect biggestFace = getBiggestFace(faces);
            faceStabilization(biggestFace);
            Rect rect = face.getRect();

            // draw red rectangle for biggest stabilized detected face
            Log.d(TAG, rect.toString());
            drawRectangle(inputImage, rect, new Scalar(255, 0, 0), 3);

            detectEyes(inputImage, biggestFace);
        }

//        DEBUG - draw green rectangles for all detected faces
//        for (Rect rect : faces.toArray()) {
//            drawRectangle(inputImage, rect, new Scalar(0, 255, 0), 2);
//        }

        inputImage.copyTo(outputImage);
        inputImage.release();
    }

    private void detectEyes(Mat inputImage, Rect biggestFace) {
        Mat faceROI = scaledGrayImage.submat(biggestFace);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(
                faceROI,
                eyes,
                1.1
        );

        Rect[] eyesArray = eyes.toArray();
        Log.d(TAG, String.valueOf(eyesArray.length));
        if(eyesArray.length == 2){
            eye1 = getEyeRect(faceROI, eyesArray[0]);
            eye2 = getEyeRect(faceROI, eyesArray[1]);
            drawRectangle(inputImage, eye1, new Scalar(0, 255, 0), 2);
            drawRectangle(inputImage, eye2, new Scalar(0, 255, 0), 2);
            Log.d(TAG, "Eye1: " + eye1.toString() + "\tEye2: " + eye2.toString());
        }
    }

    private Rect getEyeRect(Mat faceROI, Rect eye) {
        Point p1 = new Point(0, 0);
        faceROI.locateROI(null, p1);
        eye.x += p1.x;
        eye.y += p1.y;
        return eye;
    }

    private Rect getBiggestFace(MatOfRect faces) {
        Rect biggestFace = faces.toArray()[0];
        for (Rect rect : faces.toArray()) {
            if (rect.area() > biggestFace.area()) {
                biggestFace = rect;
            }
        }
        return biggestFace;
    }

    private void faceStabilization(Rect biggestFace) {
        Point center = new Point(
                biggestFace.x + (biggestFace.width >> 1),
                biggestFace.y + (biggestFace.height >> 1)
        );

        if (Math.abs(center.x - face.getCenter().x) > EPS_CENTER
                || Math.abs(center.y - face.getCenter().y) > EPS_CENTER) {
            face.setCenter(center);
        }
        if (Math.abs(biggestFace.width - face.getSize()) > EPS_SIZE) {
            face.setSize(biggestFace.width);
        }
    }

    private void drawRectangle(Mat inputImage, Rect rect, Scalar color, int thickness) {
        scale(rect);
        Imgproc.rectangle(inputImage,
                new Point(rect.x, rect.y),
                new Point(rect.x + rect.width, rect.y + rect.height),
                color,
                thickness
        );
    }

    private void scale(Rect rect) {
        rect.x *= SCALE;
        rect.y *= SCALE;
        rect.width *= SCALE;
        rect.height *= SCALE;
    }
}
