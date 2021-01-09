package pl.edu.agh.sm.eyetracking;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Facemark;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = EyeTrackingProcessor.class.getCanonicalName();
    private static final int EPS_CENTER = 2;
    private static final int EPS_SIZE = 5;
    private final int SCALE = 6;

    private final CascadeClassifier faceDetector;
    private final Facemark facemark;

    private Mat outputImage;
    private Mat scaledGrayImage;
    private Size scaledSize;
    private Face face;

    private int imageWidth;
    private int imageHeight;

    EyeTrackingProcessor(CascadeClassifier classifier, Facemark facemark) {
        this.faceDetector = classifier;
        this.facemark = facemark;
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

        ArrayList<MatOfPoint2f> landmarks = new ArrayList<>();

        if (faces.toArray().length > 0) {
            Rect biggestFace = getBiggestFace(faces);
            faceStabilization(biggestFace);
            Rect rect = face.getRect();
            facemark.fit(scaledGrayImage, faces, landmarks);

//            for (int i=0; i<landmarks.size(); i++) {
                MatOfPoint2f lm = landmarks.get(0);
                int a = lm.cols();
                for (int j=36; j<42; j++) {
                    double [] dp = lm.get(j,0);
                    Point p = new Point(dp[0] * SCALE, dp[1] * SCALE);
                    Imgproc.circle(inputImage,p,2,new Scalar(222),1);
//                }
            }

            // draw red rectangle for biggest stabilized detected face
            Log.d(TAG, rect.toString());
            drawRectangle(inputImage, rect, new Scalar(255, 0, 0), 3);
        }

        // DEBUG - draw green rectangles for all detected faces
//        for (Rect rect : faces.toArray()) {
//            drawRectangle(inputImage, rect, new Scalar(0, 255, 0), 2);
//        }

        inputImage.copyTo(outputImage);
        inputImage.release();
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
