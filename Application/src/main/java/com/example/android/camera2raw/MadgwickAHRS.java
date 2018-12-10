package com.example.android.camera2raw;

import android.content.Context;

public class MadgwickAHRS {

    private double q0, q1, q2, q3;
    private double beta;
    private double lastUpdate = -1;

    private Context context;
    public MadgwickAHRS(Context context){
        this.q0 = 1.0;
        this.q1 = this.q2 = this.q3 = 0.0;
        this.beta = 0.1;
    }

    public int[] Quaternion2YPR() {

        double roll = Math.atan2(2 * (q0 * q1 + q2 * q3), q0 * q0 * 2 - 1 + q3 * q3 * 2);
        double pitch = -Math.asin(2 * (q1 * q3 - q0 * q2));
        double yaw = Math.atan2(2 * (q1 * q2 + q0 * q3), q0 * q0 * 2 + q1 * q1 * 2 - 1);

        int[] ret = new int[3];
        ret[0] = (int)(yaw * 180 / Math.PI);
        ret[1] = (int)(pitch * 180 / Math.PI);
        ret[2] = (int)(roll * 180 / Math.PI);
        return ret;
    }

    public void MadgwickAHRSupdateIMU(double gx, double gy, double gz, double ax, double ay, double az, long nanoTime) {

        double sec = (1.0 * nanoTime) / 1000000000;
        if (lastUpdate == -1){
            lastUpdate = sec;
            return;
        }
        double deltaT = sec - lastUpdate;
        lastUpdate = sec;

        //Rate of change of quaternion from gyroscope
        double qDot1 = 0.5 * (-q1 * gx - q2 * gy - q3 * gz);
        double qDot2 = 0.5 * (q0 * gx + q2 * gz - q3 * gy);
        double qDot3 = 0.5 * (q0 * gy - q1 * gz + q3 * gx);
        double qDot4 = 0.5 * (q0 * gz + q1 * gy - q2 * gx);


        //Compute feedback only if accelerometer measurement valid(avoids NaN in accelerometer normalisation)
        if (!((ax == 0.0) && (ay == 0.0) && (az == 0.0))) {

            //Normalise accelerometer measurement

            double recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            double ax1 = ax * recipNorm;
            double ay1 = ay * recipNorm;
            double az1 = az * recipNorm;

            //Auxiliary variables to avoid repeated arithmetic

            double _2q0 = 2.0 * q0;
            double _2q1 = 2.0 * q1;
            double _2q2 = 2.0 * q2;
            double _2q3 = 2.0 * q3;
            double _4q0 = 4.0 * q0;
            double _4q1 = 4.0 * q1;
            double _4q2 = 4.0 * q2;
            double _8q1 = 8.0 * q1;
            double _8q2 = 8.0 * q2;
            double q0q0 = q0 * q0;
            double q1q1 = q1 * q1;
            double q2q2 = q2 * q2;
            double q3q3 = q3 * q3;

            //Gradient decent algorithm corrective step

            double s0 = (_4q0 * q2q2) + (_2q2 * ax1) + (_4q0 * q1q1) - (_2q1 * ay1);
            double s1 = _4q1 * q3q3 - _2q3 * ax1 + 4.0 * q0q0 * q1 - (_2q0 * ay1) - _4q1 + (_8q1 * q1q1) + (_8q1 * q2q2) + (_4q1 * az1);
            double s2 = (4.0 * q0q0 * q2) + _2q0 * ax1 + _4q2 * q3q3 - (_2q3 * ay1) - _4q2 + (_8q2 * q1q1) + (_8q2 * q2q2) + (_4q2 * az1);
            double s3 = (4.0 * q1q1 * q3) - _2q1 * ax1 + 4.0 * q2q2 * q3 - _2q2 * ay1;
            recipNorm = invSqrt(s0 *s0 + s1 * s1 + s2 * s2 + s3 * s3); //normalise step magnitude
            double s0a = s0 * recipNorm;
            double s1a = s1 * recipNorm;
            double s2a = s2 * recipNorm;
            double s3a = s3 * recipNorm;

            //Apply feedback step
            qDot1 -= beta * s0a;
            qDot2 -= beta * s1a;
            qDot3 -= beta * s2a;
            qDot4 -= beta * s3a;
        }

        //Integrate rate of change of quaternion to yield quaternion
        q0 += qDot1 * deltaT;
        q1 += qDot2 * deltaT;
        q2 += qDot3 * deltaT;
        q3 += qDot4 * deltaT;

        //Normalise quaternion
        double recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);

        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
    }

    public static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= (1.5d - xhalf * x * x);
        return x;
    }
}
