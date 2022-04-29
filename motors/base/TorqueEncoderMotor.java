package org.texastorque.torquelib.motors.base;

/**
 * Interface to include encoder feedback methods for motors.
 * 
 * @author Justus Languell
 */
public interface TorqueEncoderMotor {

    public double getPosition();
    public double getPositionDegrees();
    public double getPositionRotations();

    public double getVelocity();
    public double getVelocityRPS();
    public double getVelocityRPM();

    public double getAcceleration();
    public double getAccelerationRPS();
    public double getAccelerationRPM();
}