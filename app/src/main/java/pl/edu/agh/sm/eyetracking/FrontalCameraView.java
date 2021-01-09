package pl.edu.agh.sm.eyetracking;

import android.content.Context;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class FrontalCameraView extends JavaCameraView {

    public FrontalCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraIndex = CAMERA_ID_BACK;
    }
}
