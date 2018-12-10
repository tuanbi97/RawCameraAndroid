package com.example.android.camera2raw;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;

public class MARFragment extends ArFragment {

    private class Square{
        private Renderable[] cubeRenderable = new Renderable[1];
        private Renderable[] faceRenderable = new Renderable[1];
        public Node[] Edges = new Node[4];
        public Node[] Face = new Node[1];
        public Square(Context context){
            MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.CYAN))
                    .thenAccept(
                            material -> {
                                cubeRenderable[0] = ShapeFactory.makeCube(new Vector3(.002f, .002f, .1f), Vector3.zero(), material);
                            }
                    );
            MaterialFactory.makeTransparentWithColor(context, new Color(0))
                    .thenAccept(
                            material -> {
                                faceRenderable[0] = ShapeFactory.makeCube(new Vector3(.1f, .001f, .1f), Vector3.zero(), material);
                            }
                    );
        }

        public TransformableNode DrawSquare(){
            TransformableNode parent = new TransformableNode(getTransformationSystem());
            parent.getScaleController().setMinScale(0.1f);

            Edges[0] = new Node();
            Edges[0].setRenderable(cubeRenderable[0]);
            parent.addChild(Edges[0]);
            Edges[0].setLocalPosition(new Vector3(-0.05f, 0.0f, 0.0f));

            Edges[1] = new Node();
            Edges[1].setRenderable(cubeRenderable[0]);
            parent.addChild(Edges[1]);
            Edges[1].setLocalPosition(new Vector3(0.05f, 0.0f, 0.0f));

            Edges[2] = new Node();
            Edges[2].setRenderable(cubeRenderable[0]);
            parent.addChild(Edges[2]);
            Edges[2].setLocalPosition(new Vector3(0.0f, 0.0f, 0.05f));
            Edges[2].setLocalRotation(new Quaternion(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0f), 90f)));

            Edges[3] = new Node();
            Edges[3].setRenderable(cubeRenderable[0]);
            parent.addChild(Edges[3]);
            Edges[3].setLocalPosition(new Vector3(0.0f, 0.0f, -0.05f));
            Edges[3].setLocalRotation(new Quaternion(Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0f), 90f)));

            Face[0] = new Node();
            Face[0].setRenderable(faceRenderable[0]);
            parent.addChild(Face[0]);
            Face[0].setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));

            return parent;
        }
    }

    public Vector3 trackPos;
    public Vector3 refLoc;
    public Quaternion trackRot;
    public TransformableNode anchorBase;
    public TextView rotView = null;
    public TextView distView = null;

    public MARFragment() {
        super();
        trackPos = new Vector3(0, 0, 0);
        refLoc = null;
        trackRot = new Quaternion(0, 0, 0, 0);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Square squareDrawer = new Square(getActivity());

        setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            // Create the Anchor.
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);

            refLoc = anchorNode.getWorldPosition();

            anchorNode.setParent(getArSceneView().getScene());

            //Draw a square
            TransformableNode roi = squareDrawer.DrawSquare();
            anchorBase = roi;

            roi.setParent(anchorNode);
            roi.select();
        });

        setTrackingListener();
    }

    public Integer[] Quat2Euler(Quaternion q1) {
        double test = q1.x*q1.y + q1.z*q1.w;
        double heading;
        double attitude;
        double bank;
        if (test > 0.499) { // singularity at north pole
            heading = 2 * atan2(q1.x,q1.w);
            attitude = Math.PI/2;
            bank = 0;
        }
        else
        if (test < -0.499) { // singularity at south pole
            heading = -2 * atan2(q1.x,q1.w);
            attitude = - Math.PI/2;
            bank = 0;
        }
        else {
            double sqx = q1.x * q1.x;
            double sqy = q1.y * q1.y;
            double sqz = q1.z * q1.z;
            heading = atan2(2 * q1.y * q1.w - 2 * q1.x * q1.z, 1 - 2 * sqy - 2 * sqz);
            attitude = asin(2 * test);
            bank = atan2(2 * q1.x * q1.w - 2 * q1.y * q1.z, 1 - 2 * sqx - 2 * sqz);
        }
        Integer[] ret = new Integer[3];
        ret[0] = (int) (heading*180/PI);
        ret[1] = (int) (attitude*180/PI);
        ret[2] = (int) (bank*180/PI);
        return ret;
        //Log.d("CameraRotationEuler", yaw.toString() + ' ' + pitch.toString() + ' ' + roll.toString());
    }

    private void setTrackingListener() {
        Scene.OnUpdateListener cameraPoseListener = new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                Camera camera = getArSceneView().getScene().getCamera();
//                Log.d("CameraPositionWorld", trackPos.toString());
//                Log.d("CameraRotationWorld", trackRot.toString());
//                Log.d("CameraPositionLocal", camera.getLocalPosition().toString());
                Log.d("CameraPositionWorld", camera.getWorldPosition().toString());
//                Log.d("CameraRotationLocal", camera.getLocalRotation().toString());
//                Log.d("CameraRotationWorld", camera.getWorldRotation().toString());
                Quaternion q = camera.getWorldRotation();
                Integer[] Angles = Quat2Euler(q);
//                double q0 = q.w;
//                double q1 = q.x;
//                double q2 = q.y;
//                double q3 = q.z;
//                Integer roll = (int) (atan2(2 * (q0 * q1 + q2 * q3), q0 * q0 * 2 - 1 + q3 * q3 * 2) * 180 / Math.PI);
//                Integer pitch = (int) (-asin(2 * (q1 * q3 - q0 * q2)) * 180 / Math.PI);
//                Integer yaw = (int) (atan2(2 * (q1 * q2 + q0 * q3), q0 * q0 * 2 + q1 * q1 * 2 - 1) * 180 / Math.PI);

                //Log.d("CameraRotationEuler", yaw.toString() + ' ' + pitch.toString() + ' ' + roll.toString());
                Log.d("CameraRotationEuler", Angles[0].toString() + ' ' + Angles[1].toString() + ' ' + Angles[2].toString());
                if (rotView == null) {
                    rotView = getActivity().findViewById(R.id.rotationview);
                }
                if (rotView != null) {
                    rotView.setText(Angles[0].toString() + ' ' + Angles[1].toString() + ' ' + Angles[2].toString());
                }

                trackRot = camera.getWorldRotation();
                trackPos = camera.getWorldPosition();
                if (refLoc != null){
                    trackPos = Vector3.subtract(camera.getWorldPosition(), refLoc);
                }
                if (distView == null){
                    distView = getActivity().findViewById(R.id.distanceview);
                }
                else{
                    String dist = String.format("%.4f", sqrt(trackPos.x * trackPos.x + trackPos.y * trackPos.y + trackPos.z * trackPos.z));
                    distView.setText(dist);
                }
            }
        };
        getArSceneView().getScene().addOnUpdateListener(cameraPoseListener);
    }
}
