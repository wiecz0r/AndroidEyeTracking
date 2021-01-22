package pl.edu.agh.sm.eyetracking;

import androidx.core.util.Pair;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import pl.edu.agh.sm.eyetracking.detectors.EyeDetector;
import pl.edu.agh.sm.eyetracking.detectors.FaceDetector;
import pl.edu.agh.sm.eyetracking.util.Size;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = EyeTrackingProcessor.class.getCanonicalName();

    public static final int PUPIL_SCALE = 2;

    private final FaceDetector faceDetector;
    private final EyeDetector eyeDetector;

    private Mat outputImage;

    private Mat pupilStepImage;
    private Size pupilStepImageSize;


    public EyeTrackingProcessor(CascadeClassifier faceClassifier, CascadeClassifier eyeClassifier) {
        faceDetector = new FaceDetector(faceClassifier);
        eyeDetector = new EyeDetector(eyeClassifier);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Size screenSize = new Size(width, height); 
        outputImage = new Mat(screenSize.toOpenCV(), CvType.CV_8UC4);

        faceDetector.initialize(screenSize);
        eyeDetector.initialize(screenSize);

        int scaledWidth = width / PUPIL_SCALE;
        int scaledHeight = height / PUPIL_SCALE;
        pupilStepImageSize = new Size(scaledWidth, scaledHeight);
        pupilStepImage = new Mat(pupilStepImageSize.toOpenCV(), CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        pupilStepImage.release();

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
        
        Imgproc.resize(frame.gray(), pupilStepImage, pupilStepImageSize.toOpenCV());

        Rect faceROI = faceDetector.detect(frame);

        if (faceROI != null) {
            drawRectangle(inputImage, faceROI, new Scalar(255, 0, 0), 3);

            Pair<Rect, Rect> eyeROIs = eyeDetector.detect(frame, faceROI);

            if (eyeROIs != null) {
                drawRectangle(inputImage, eyeROIs.first, new Scalar(0, 255, 0), 2);
                drawRectangle(inputImage, eyeROIs.second, new Scalar(0, 255, 128), 2);
            }
        }

        Core.flip(inputImage, inputImage, 1);

        inputImage.copyTo(outputImage);
        inputImage.release();
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
//            Imgproc.arrowedLine();
        }

        eyeROI.release();
    }

//    private void niBlackThreshold(Mat src, Mat dst, double maxValue, int blockSize, double k) {
//        Mat thresh = new Mat();
//        Mat mean = new Mat();
//        Mat sqmean = new Mat();
//        Mat variance = new Mat();
//        Mat stddev = new Mat();
//
//        Imgproc.boxFilter(src, mean, CvType.CV_32F, new Size(blockSize, blockSize),
//                new Point(-1, -1), true, Core.BORDER_REPLICATE);
//        Imgproc.sqrBoxFilter(src, sqmean, CvType.CV_32F, new Size(blockSize, blockSize),
//                new Point(-1, -1), true, Core.BORDER_REPLICATE);
//
//        Core.subtract(sqmean, mean.mul(mean), variance);
//        Core.sqrt(variance, stddev);
//
//        // BINARIZATION_NIBLACK
//        Core.multiply(stddev, new Scalar(k), thresh);
//        Core.add(mean, thresh, thresh);
//
//        thresh.convertTo(thresh, src.depth());
//
//        Mat mask = new Mat();
//        Core.compare(src, thresh, mask, Core.CMP_GT);
//        dst.setTo(new Scalar(0));
//        dst.setTo(new Scalar(maxValue), mask);
//
//        thresh.release();
//        mean.release();
//        sqmean.release();
//        variance.release();
//        stddev.release();
//        mask.release();
//    }


    private void drawRectangle(Mat inputImage, Rect rect, Scalar color, int thickness) {
        Imgproc.rectangle(inputImage,
                new Point(rect.x, rect.y),
                new Point(rect.x + rect.width, rect.y + rect.height),
                color,
                thickness
        );
    }
}
