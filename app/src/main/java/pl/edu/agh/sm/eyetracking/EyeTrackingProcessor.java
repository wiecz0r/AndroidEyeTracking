package pl.edu.agh.sm.eyetracking;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
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

            if (stabilisedFace.x + stabilisedFace.width > 1280) return;
            if (stabilisedFace.y + stabilisedFace.height > 720) return;
            if (stabilisedFace.x < 0 || stabilisedFace.y < 0) return;

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
        }

        faceROI.release();
    }

    private void detectPupil(Mat inputImage, Rect eye) {
        eye.x /= PUPIL_SCALE;
        eye.y = (eye.y + eye.height / 4) / PUPIL_SCALE;
        eye.width /= PUPIL_SCALE;
        eye.height = 3 * (eye.height / 4) / PUPIL_SCALE;

        Mat eyeROI = pupilStepImage.submat(eye);

//        niBlackThreshold(eyeROI, eyeROI, 255, 31, -0.2);
        Imgproc.threshold(eyeROI, eyeROI, 69, 255, Imgproc.THRESH_BINARY);

        Imgproc.erode(eyeROI, eyeROI, new Mat(), new Point(-1, -1), 2);
        Imgproc.dilate(eyeROI, eyeROI, new Mat(), new Point(-1, -1), 4);
        Imgproc.medianBlur(eyeROI, eyeROI, 5);

        MatOfKeyPoint blob = new MatOfKeyPoint();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        detector.detect(eyeROI, blob);

        KeyPoint[] pupilsArray = blob.toArray();

        for(KeyPoint point : pupilsArray) {
            int size = (int) (point.size * PUPIL_SCALE);
            if (point.size * 2 > eye.width) {
                continue;
            }

            Point center = point.pt.clone();
            center.x *= PUPIL_SCALE;
            center.y *= PUPIL_SCALE;

            center.x += eye.x * PUPIL_SCALE;
            center.y += eye.y * PUPIL_SCALE;
            //Log.d(TAG, "+b: " + b);
            Imgproc.circle(inputImage, center, size, new Scalar(0, 255, 255), 1);
        }

        eyeROI.release();
    }

    private void niBlackThreshold(Mat src, Mat dst, double maxValue, int blockSize, double k) {
        Mat thresh = new Mat();
        Mat mean = new Mat();
        Mat sqmean = new Mat();
        Mat variance = new Mat();
        Mat stddev = new Mat();

        Imgproc.boxFilter(src, mean, CvType.CV_32F, new Size(blockSize, blockSize),
                new Point(-1, -1), true, Core.BORDER_REPLICATE);
        Imgproc.sqrBoxFilter(src, sqmean, CvType.CV_32F, new Size(blockSize, blockSize),
                new Point(-1, -1), true, Core.BORDER_REPLICATE);

        Core.subtract(sqmean, mean.mul(mean), variance);
        Core.sqrt(variance, stddev);

        // BINARIZATION_NIBLACK
        Core.multiply(stddev, new Scalar(k), thresh);
        Core.add(mean, thresh, thresh);

        thresh.convertTo(thresh, src.depth());

        Mat mask = new Mat();
        Core.compare(src, thresh, mask, Core.CMP_GT);
        dst.setTo(new Scalar(0));
        dst.setTo(new Scalar(maxValue), mask);

        thresh.release();
        mean.release();
        sqmean.release();
        variance.release();
        stddev.release();
        mask.release();
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
