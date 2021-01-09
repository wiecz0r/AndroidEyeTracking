package pl.edu.agh.sm.eyetracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.face.Facemark;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private FrontalCameraView cameraBridgeViewBase;
    private EyeTrackingProcessor eyeTrackingProcessor;
    private CascadeClassifier faceDetector;
    private Facemark facemark;

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
        File cascadeFile = loadFile(R.raw.haarcascade_frontalface_alt2, "cascade", "haarcascade_frontalface_alt2.xml");
        File facemarkFile = loadFile(R.raw.face_landmark_model, "landmark", "face_landmark_model.dat");

        faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        faceDetector.load(cascadeFile.getAbsolutePath());

        facemark = org.opencv.face.Face.createFacemarkKazemi();
        facemark.loadModel(facemarkFile.getAbsolutePath());

        eyeTrackingProcessor = new EyeTrackingProcessor(faceDetector, facemark);
        cameraBridgeViewBase.setCvCameraViewListener(eyeTrackingProcessor);
    }

    private File loadFile(int resource, String dir, String filename) {
        try {
            InputStream is = getResources().openRawResource(resource);
            File directory = getDir(dir, Context.MODE_PRIVATE);
            File file = new File(directory, filename);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            is.close();
            fos.close();
            directory.delete();
            return file;
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