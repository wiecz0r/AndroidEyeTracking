package pl.edu.agh.sm.eyetracking.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassifierLoader {

    private static final String DIRECTORY = "classifier";

    private final AppCompatActivity activity;

    public ClassifierLoader(AppCompatActivity activity) {
        this.activity = activity;
    }

    public CascadeClassifier load(int resource, String filename) throws IOException {
        File classifierFile = copyFile(resource, filename);
        CascadeClassifier classifier = new CascadeClassifier(classifierFile.getAbsolutePath());
        classifier.load(classifierFile.getAbsolutePath());
        return classifier;
    }

    private File copyFile(int resource, String filename) throws IOException {
        InputStream is = activity.getResources().openRawResource(resource);
        File directory = activity.getDir(DIRECTORY, Context.MODE_PRIVATE);
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
    }
}
