package com.example.android.camera2raw;

import android.content.Intent;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ArActivity extends AppCompatActivity {

    private MARFragment arFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        arFragment = (MARFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    }

    private static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US);
        return sdf.format(new Date());
    }

    public void onSwitchActivity(View view) {
        String currentDateTime = generateTimestamp();
        Vector3 trackPos = arFragment.trackPos;
        Quaternion trackRot = arFragment.trackRot;
        saveCameraPose(trackPos, trackRot, currentDateTime);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("uniquename", currentDateTime);
        startActivity(intent);
    }

    public float[] calculateWorld2CameraMatrix(float[] modelmtx, float[] viewmtx, float[] prjmtx) {

        float scaleFactor = 1.0f;
        float[] scaleMatrix = new float[16];
        float[] modelXscale = new float[16];
        float[] viewXmodelXscale = new float[16];
        float[] world2screenMatrix = new float[16];

        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;

        Matrix.multiplyMM(modelXscale, 0, modelmtx, 0, scaleMatrix, 0);
        Matrix.multiplyMM(viewXmodelXscale, 0, viewmtx, 0, modelXscale, 0);
        Matrix.multiplyMM(world2screenMatrix, 0, prjmtx, 0, viewXmodelXscale, 0);

        return world2screenMatrix;
    }

    double[] world2Screen(int screenWidth, int screenHeight, float[] inpPoint, float[] world2cameraMatrix)
    {
        float[] origin = inpPoint;
        float[] ndcCoord = new float[4];
        Matrix.multiplyMV(ndcCoord, 0,  world2cameraMatrix, 0,  origin, 0);

        ndcCoord[0] = ndcCoord[0]/ndcCoord[3];
        ndcCoord[1] = ndcCoord[1]/ndcCoord[3];

        double[] pos_2d = new double[]{0,0};
        pos_2d[0] = screenWidth  * ((ndcCoord[0] + 1.0)/2.0);
        pos_2d[1] = screenHeight * ((1.0 - ndcCoord[1])/2.0);

        return pos_2d;
    }

    private void saveCameraPose(Vector3 trackPos, Quaternion trackRot, String currentTime) {
        Camera camera = arFragment.getArSceneView().getScene().getCamera();

        float[] projmtx = new float[16];
        projmtx = camera.getProjectionMatrix().data;

        float[] viewmtx = new float[16];
        viewmtx = camera.getViewMatrix().data;

        float[] anchorMatrix = new float[16];
        anchorMatrix = arFragment.anchorBase.getWorldModelMatrix().data;

        float[] world2screenMatrix = calculateWorld2CameraMatrix(anchorMatrix, viewmtx, projmtx);
        Log.d("ScreenDisplay", Integer.toString(camera.getScene().getView().getWidth()) + ' ' + Integer.toString(camera.getScene().getView().getHeight()));

        File orientationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "POSE_" + currentTime + ".txt");
        FileWriter writer = null;
        try {
            writer = new FileWriter(orientationFile);
            writer.write(Float.toString(trackPos.x) + ' ' + Float.toString(trackPos.y) + ' ' + Float.toString(trackPos.z) + '\n');
            writer.write(Float.toString(trackRot.x) + ' ' + Float.toString(trackRot.y) + ' ' + Float.toString(trackRot.z) + ' ' + Float.toString(trackRot.w) + '\n');
            Integer[] eulerAngles = arFragment.Quat2Euler(trackRot);
            writer.write(eulerAngles[0].toString() + ' ' + eulerAngles[1].toString() + ' ' + eulerAngles[2].toString() + '\n');

            float[] inpPoint = {-0.05f, 0, -0.05f, 1};
            double[] anchor2d = world2Screen(4032,  3024, inpPoint, world2screenMatrix);
            writer.write(Double.toString(anchor2d[0]) + ' ' + Double.toString(anchor2d[1]) + '\n');

            inpPoint = new float[] {-0.05f, 0, 0.05f, 1};
            anchor2d = world2Screen(4032,  3024, inpPoint, world2screenMatrix);
            writer.write(Double.toString(anchor2d[0]) + ' ' + Double.toString(anchor2d[1]) + '\n');

            inpPoint = new float[]{0.05f, 0, 0.05f, 1};
            anchor2d = world2Screen(4032,  3024, inpPoint, world2screenMatrix);
            writer.write(Double.toString(anchor2d[0]) + ' ' + Double.toString(anchor2d[1]) + '\n');

            inpPoint = new float[]{0.05f, 0, -0.05f, 1};
            anchor2d = world2Screen(4032,  3024, inpPoint, world2screenMatrix);
            writer.write(Double.toString(anchor2d[0]) + ' ' + Double.toString(anchor2d[1]) + '\n');

            //Log.d("AnchorPos", Double.toString(anchor2d[0]) + ' ' + Double.toString(anchor2d[1]));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
