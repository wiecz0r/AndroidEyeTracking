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

    public static final int EYE_SCALE = 4;
    public static final int PUPIL_SCALE = 2;

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier eyeDetector;

    private Mat outputImage;

    private Mat faceStepImage;
    private Size faceStepImageSize;

    private Mat eyeStepImage;
    private Size eyeStepImageSize;

    private Mat pupilStepImage;
    private Size pupilStepImageSize;

    private Face face;

    EyeTrackingProcessor(CascadeClassifier faceClassifier, CascadeClassifier eyeClassifier) {
        faceDetector = faceClassifier;
        eyeDetector = eyeClassifier;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        this.face = new Face(width, height);

        outputImage = new Mat(height, width, CvType.CV_8UC4);

        faceStepImageSize = new Size(width / Face.SCALE,height / Face.SCALE);
        faceStepImage = new Mat(faceStepImageSize, CvType.CV_8UC1);

        eyeStepImageSize = new Size(width / EYE_SCALE, height / EYE_SCALE);
        eyeStepImage = new Mat(eyeStepImageSize, CvType.CV_8UC1);

        int scaledWidth = width / PUPIL_SCALE;
        int scaledHeight = height / PUPIL_SCALE;
        pupilStepImageSize = new Size(scaledWidth, scaledHeight);
        pupilStepImage = new Mat(pupilStepImageSize, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        pupilStepImage.release();
        eyeStepImage.release();
        faceStepImage.release();
        outputImage.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        process(cvCameraViewFrame);
        return outputImage;
    }

    private void process(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        Mat inputImage = cvCameraViewFrame.rgba();
        Imgproc.resize(cvCameraViewFrame.gray(), faceStepImage, faceStepImageSize);
        Imgproc.resize(cvCameraViewFrame.gray(), eyeStepImage, eyeStepImageSize);
        Imgproc.resize(cvCameraViewFrame.gray(), pupilStepImage, pupilStepImageSize);

        detectFace(inputImage);

        inputImage.copyTo(outputImage);
        inputImage.release();
    }

    private void detectFace(Mat inputImage) {
        // --- face detection
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(
                faceStepImage,
                faces,
                1.1 //,
//                2,
//                2,
//                new Size((int) faceStepImageSize.height >> 2, (int) faceStepImageSize.height >> 2),
//                new Size(faceStepImageSize.height, faceStepImageSize.height)
        );

        if (faces.toArray().length > 0) {
            Rect biggestFace = getBiggestFace(faces);
            Rect stabilisedFace = stabiliseFace(biggestFace);

            // draw red rectangle for biggest stabilized detected face
            Log.d(TAG, stabilisedFace.toString());
            drawRectangle(inputImage, stabilisedFace, new Scalar(255, 0, 0), 3);

            detectEyes(inputImage, stabilisedFace);
        }
    }

    private void detectEyes(Mat inputImage, Rect face) {
        face.x /= EYE_SCALE;
        face.y /= EYE_SCALE;
        face.width /= EYE_SCALE;
        face.height /= EYE_SCALE;

        Mat faceROI = eyeStepImage.submat(face);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(
                faceROI,
                eyes,
                1.1
        );

        Rect[] eyesArray = eyes.toArray();
        //Log.d(TAG, String.valueOf(eyesArray.length));

        // iterate detected eyes
        for (Rect rect : eyesArray) {
            Rect eye = getEyeRect(faceROI, rect);

            eye.x *= EYE_SCALE;
            eye.y *= EYE_SCALE;
            eye.width *= EYE_SCALE;
            eye.height *= EYE_SCALE;

            // draw green rectangle for detected eye
            drawRectangle(inputImage, eye, new Scalar(0, 255, 0), 2);
            Log.d(TAG, "Eye: " + eye.toString());

            detectPupil(inputImage, eye);

            // debug only
            break;
        }
    }

    private void detectPupil(Mat inputImage, Rect eye) {
        eye.x /= PUPIL_SCALE;
        eye.y /= PUPIL_SCALE;
        eye.width /= PUPIL_SCALE;
        eye.height /= PUPIL_SCALE;

        Mat eyeROI = pupilStepImage.submat(eye);
        //TODO

//        Imgproc.GaussianBlur(eyeROI, eyeROI, new Size(7 ,7), 0, 0);
        Imgproc.threshold(eyeROI, eyeROI, 64, 255, Imgproc.THRESH_BINARY);

//        Mat progressMat = new Mat(eye.height, eye.width, CvType.CV_8UC4);
//        Imgproc.cvtColor(eyeROI, progressMat, Imgproc.COLOR_GRAY2RGBA);
//        progressMat.copyTo(inputImage.submat(new Rect(0, 0, eye.width, eye.height)));
//        progressMat.release();

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

    private Rect stabiliseFace(Rect biggestFace) {
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

        return face.getRect();
    }

    private void drawRectangle(Mat inputImage, Rect rect, Scalar color, int thickness) {
        Imgproc.rectangle(inputImage,
                new Point(rect.x, rect.y),
                new Point(rect.x + rect.width, rect.y + rect.height),
                color,
                thickness
        );
    }
}
