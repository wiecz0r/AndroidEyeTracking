package pl.edu.agh.sm.eyetracking.detectors;

import android.util.Log;

import androidx.core.util.Pair;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.sm.eyetracking.Frame;
import pl.edu.agh.sm.eyetracking.PointRegion;
import pl.edu.agh.sm.eyetracking.util.Point;
import pl.edu.agh.sm.eyetracking.util.Scale;
import pl.edu.agh.sm.eyetracking.util.Size;

public class PupilDetector {

    private static final String TAG = PupilDetector.class.getCanonicalName();
    private static final int SCALE = 2;

    private Frame frame;
    private PointRegion leftPupilRegion;
    private PointRegion rightPupilRegion;

    private boolean initialized;

    public void initialize(Size screenSize) {
        frame = new Frame(screenSize, SCALE);
        leftPupilRegion = new PointRegion(screenSize);
        rightPupilRegion = new PointRegion(screenSize);

        initialized = true;
    }

    public Pair<Point, Point> detect(CameraBridgeViewBase.CvCameraViewFrame originalFrame, Pair<Rect, Rect> eyeROIs) {
        if (!initialized) {
            return new Pair<>(null, null);
            // or: throw exception
        }
        frame.update(originalFrame);

        Point leftPupil = detect(leftPupilRegion, eyeROIs.first);
        Point rightPupil = detect(rightPupilRegion, eyeROIs.second);

        return new Pair<>(leftPupil, rightPupil);
    }

    private Point detect(PointRegion region, Rect eyeROI) {
        if (eyeROI == null) {
            return null;
        }

        Scale.scaleDown(eyeROI, SCALE);
        eyeROI.y = eyeROI.y + eyeROI.height / 4;
        eyeROI.height = 3 * eyeROI.height / 4;

        Mat eyeSubMat = frame.get().submat(eyeROI);

        Imgproc.threshold(eyeSubMat, eyeSubMat, 69, 255, Imgproc.THRESH_BINARY);

        Imgproc.erode(eyeSubMat, eyeSubMat, new Mat(), new Point(-1, -1).toOpenCV(), 2);
        Imgproc.dilate(eyeSubMat, eyeSubMat, new Mat(), new Point(-1, -1).toOpenCV(), 4);
        Imgproc.medianBlur(eyeSubMat, eyeSubMat, 5);

        MatOfKeyPoint blob = new MatOfKeyPoint();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        detector.detect(eyeSubMat, blob);

        eyeSubMat.release();

        List<KeyPoint> potentialPupils = new ArrayList<>();
        for (KeyPoint point : blob.toArray()) {
            if (point.size * 2 > eyeROI.width) {
                continue;
            }

            potentialPupils.add(point);
        }

        Scale.scaleUp(eyeROI, SCALE);
        Point eyeOffset = new Point(eyeROI.x, eyeROI.y);

        if (potentialPupils.isEmpty()) {
            Log.d(TAG, "No pupil detected");
            return region.get(eyeOffset);
        }

        updateRegion(region, potentialPupils);
        Log.d(TAG, "Pupil region updated: " + region.get(eyeOffset).toString());
        return region.get(eyeOffset);
    }

    private void updateRegion(PointRegion region, List<KeyPoint> potentialPupils) {
        KeyPoint biggestPupil = potentialPupils.get(0);
        for (KeyPoint keyPoint : potentialPupils) {
            if (keyPoint.size > biggestPupil.size) {
                biggestPupil = keyPoint;
            }
        }

        Point center = new Point(
                (int) biggestPupil.pt.x * SCALE,
                (int) biggestPupil.pt.y * SCALE
        );

        region.update(center);
    }

    public void deinitialize() {
        if (!initialized) {
            return;
        }
        frame.release();

        rightPupilRegion = null;
        leftPupilRegion = null;
        frame = null;
        initialized = false;
    }
}
