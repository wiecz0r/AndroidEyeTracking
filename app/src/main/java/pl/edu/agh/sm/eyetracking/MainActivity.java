package pl.edu.agh.sm.eyetracking;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;

import pl.edu.agh.sm.eyetracking.util.ClassifierLoader;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private FrontalCameraView cameraBridgeViewBase;
    private final ClassifierLoader loader;

    private final BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    loadClassifiers();
                    enableCameraView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public MainActivity() {
        loader = new ClassifierLoader(this);
    }

    private void loadClassifiers() {
        try {
            CascadeClassifier faceClassifier = loader.load(
                    R.raw.haarcascade_frontalface_alt2,
                    "haarcascade_frontalface_alt2.xml");

            CascadeClassifier eyeClassifier = loader.load(
                    R.raw.haarcascade_eye,
                    "haarcascade_eye.xml");

            EyeTrackingProcessor eyeTrackingProcessor = new EyeTrackingProcessor(faceClassifier, eyeClassifier);
            cameraBridgeViewBase.setCvCameraViewListener(eyeTrackingProcessor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = findViewById(R.id.frontal_camera_view);
//        cameraBridgeViewBase.setCvCameraViewListener(eyeTrackingProcessor);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
            return;
        }

        Log.d(TAG, "OpenCV library found inside package. Using it!");
        baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onPause() {
        super.onPause();
        disableCameraView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disableCameraView();
    }


    private void enableCameraView() {
        if (cameraBridgeViewBase == null) {
            return;
        }
        cameraBridgeViewBase.enableView();
    }

    private void disableCameraView() {
        if (cameraBridgeViewBase == null) {
            return;
        }
        cameraBridgeViewBase.disableView();
    }
}