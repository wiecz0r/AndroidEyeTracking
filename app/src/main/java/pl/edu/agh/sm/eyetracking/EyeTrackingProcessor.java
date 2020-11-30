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

    private Mat outputRGBA;
    private CascadeClassifier faceDetector;

    EyeTrackingProcessor(CascadeClassifier classifier) {
        super();
        faceDetector = classifier;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        outputRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        outputRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        process(cvCameraViewFrame);
        return outputRGBA;
    }

    private void process(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        Mat inputMat = cvCameraViewFrame.rgba();
//        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_RGB2BGRA);

        Mat progressMat = inputMat.clone();
//        Imgproc.Canny(inputMat, progressMat, 80, 90);
//        Imgproc.cvtColor(inputMat, progressMat, Imgproc.COLOR_RGBA2GRAY);

        // face detection
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(progressMat, faces);
//        faceDetector.detectMultiScale(progressMat, faces, 1.1,2,2,
//                new Size(10,10),new Size(400,400));

        // draw red rectangles for detected faces
        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(progressMat,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(255, 0, 0)
            );
        }

        progressMat.copyTo(outputRGBA);

        inputMat.release();
        progressMat.release();
    }

}
