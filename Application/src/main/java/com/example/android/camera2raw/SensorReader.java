package com.example.android.camera2raw;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SensorReader{

    public float[] valuesA, valuesG;
    public int sampleIdA = 0, sampleIdG = 0, sampleId = 0;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private SensorEventListener mSensorListener;
    private Timer timer;
    private TimerTask triggerSensor;
    private MadgwickAHRS transformer;
    private TextView viewAngles;
    public int[] eulerAngles;

    private Context context;

    public SensorReader(final Context context) {

        this.context = context;

        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    valuesA = sensorEvent.values;
                    //Log.d("val Acc", String.format("%.5f %.5f %.5f %d", valuesA[0], valuesA[1], valuesA[2], sampleIdA));
                    sampleIdA++;
                }
                else
                if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                    valuesG = sensorEvent.values;
                    //Log.d("val Gyr", String.format("%.5f %.5f %.5f %d", valuesG[0], valuesG[1], valuesG[2], sampleIdG));
                    sampleIdG++;
                }
                //Log.d("check sample", Integer.toString(sampleIdA) + " " + Integer.toString(sampleIdG));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(mSensorListener,  mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mSensorListener,  mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        valuesA = new float[3];
        valuesG = new float[3];
        valuesA[0] = valuesA[1] = valuesA[2] = 0;
        valuesG[0] = valuesG[1] = valuesG[2] = 0;
        transformer = new MadgwickAHRS(context);
        viewAngles = (TextView) ((Activity) context).findViewById(R.id.viewangles);

        triggerSensor = new TimerTask() {
            @Override
            public void run() {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long nanoTime = System.nanoTime();
                        transformer.MadgwickAHRSupdateIMU(valuesG[0], valuesG[1], valuesG[2], valuesA[0], valuesA[1], valuesA[2], nanoTime);
                        eulerAngles = transformer.Quaternion2YPR();
                        viewAngles.setText("Y: " + Integer.toString((int)eulerAngles[0]) + "|P: " + Integer.toString((int)eulerAngles[1]) + "|R: " + Integer.toString((int)eulerAngles[2]));
                    }
                });
            }
        };
    }

    public void start(){
        timer = new Timer();
        timer.schedule(triggerSensor, 0, 20);
    }

    public void stop(){
        sampleIdA = 0;
        sampleIdG = 0;
        mSensorManager.unregisterListener(mSensorListener);
//        Log.d("sd", Integer.toString(sampleIdA) + ' ' + Integer.toString(sampleIdG));
//        Log.d("sd", Integer.toString(sampleId));
    }
}
