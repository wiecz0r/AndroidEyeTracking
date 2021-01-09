package pl.edu.agh.sm.eyetracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private FrontalCameraView cameraBridgeViewBase;
    private EyeTrackingProcessor eyeTrackingProcessor;
    private CascadeClassifier faceDetector;

    private final BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    loadClassifier();
                    enableCameraView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

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

    private void loadClassifier() {
        File cascadeFile = loadFile();
        faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        faceDetector.load(cascadeFile.getAbsolutePath());
        eyeTrackingProcessor = new EyeTrackingProcessor(faceDetector);
        cameraBridgeViewBase.setCvCameraViewListener(eyeTrackingProcessor);
    }

    private File loadFile() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream fos = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            is.close();
            fos.close();
            cascadeDir.delete();
            return cascadeFile;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
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