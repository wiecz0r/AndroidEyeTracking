package pl.edu.agh.sm.eyetracking;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class EyeTrackingProcessor implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat outputRGBA;

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
        // TODO magic with inputMat and progressMat

        progressMat.copyTo(outputRGBA);

        inputMat.release();
        progressMat.release();
    }
}
