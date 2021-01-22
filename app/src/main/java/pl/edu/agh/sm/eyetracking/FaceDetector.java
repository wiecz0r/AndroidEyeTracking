package pl.edu.agh.sm.eyetracking;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import pl.edu.agh.sm.eyetracking.util.Size;

public class FaceDetector {

    private static final String TAG = FaceDetector.class.getCanonicalName();
    private static final int SCALE = 8;
    private static final int MAX_SKIPPED_FRAMES = 10;

    private final CascadeClassifier classifier;
    private Frame frame;
    private SquareRegion region;

    private int skippedFrames = MAX_SKIPPED_FRAMES;

    private boolean initialized;

    public FaceDetector(CascadeClassifier classifier) {
        this.classifier = classifier;
    }

    public void initialize(Size screenSize) {
        frame = new Frame(screenSize, SCALE);
        region = new SquareRegion(screenSize);
    }

    public Rect detect(CameraBridgeViewBase.CvCameraViewFrame originalFrame) {
        frame.update(originalFrame);

        Mat image = frame.get();
        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(
                image,
                faces,
                1.1 //,
//                2,
//                2,
//                new Size((int) faceStepImageSize.height >> 2, (int) faceStepImageSize.height >> 2),
//                new Size(faceStepImageSize.height, faceStepImageSize.height)
        );

        if (faces.toArray().length == 0) {
            if (skippedFrames == 0) {
                Log.d(TAG, "No face detected for " + MAX_SKIPPED_FRAMES + "frames");
                return null;
                // or: throw exception
            }
            skippedFrames--;

            Log.d(TAG, "Face region reused: " + region.get().toString());
            return region.get();
        }

        skippedFrames = MAX_SKIPPED_FRAMES;

        Rect biggestFace = getBiggestFace(faces);
        Utils.scale(biggestFace, SCALE);

        region.update(biggestFace);

        Log.d(TAG, "Face region updated: " + region.get().toString());
        return region.get();
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

    public void deinitialize() {
        if (!initialized) {
            return;
        }
        frame.release();

        frame = null;
        region = null;
        initialized = false;
    }
}
