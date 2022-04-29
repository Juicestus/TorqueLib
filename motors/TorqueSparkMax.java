package org.texastorque.torquelib.motors;

import java.util.ArrayList;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxAlternateEncoder;
import com.revrobotics.SparkMaxAnalogSensor;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANSparkMaxLowLevel.PeriodicFrame;

import org.texastorque.torquelib.motors.base.TorqueEncoderMotor;
import org.texastorque.torquelib.motors.base.TorqueMotor;
import org.texastorque.torquelib.motors.base.TorquePIDMotor;
import org.texastorque.torquelib.util.KPID;

public class TorqueSparkMax extends TorqueMotor implements TorquePIDMotor, TorqueEncoderMotor {
    private CANSparkMax sparkMax;
    private RelativeEncoder sparkMaxEncoder;
    private SparkMaxAlternateEncoder alternateEncoder;
    private SparkMaxPIDController pidController;
    private SparkMaxAnalogSensor analogEncoder;
    private ArrayList<CANSparkMax> sparkMaxFollowers = new ArrayList<>();

    private final double CLICKS_PER_ROTATION = sparkMaxEncoder.getCountsPerRevolution();

    private double lastVelocity;
    private long lastVelocityTime;

    private double encoderZero = 0;

    /**
     * Construct a new TorqueSparkMax motor.
     * 
     * @param port The port (ID) of the motor.
     */
    public TorqueSparkMax(final int port) {
        super(port);

        this.lastVelocity = 0;
        this.lastVelocityTime = System.currentTimeMillis();
        sparkMax = new CANSparkMax(port, MotorType.kBrushless);
        sparkMaxEncoder = sparkMax.getEncoder();
        analogEncoder = sparkMax.getAnalog(SparkMaxAnalogSensor.Mode.kAbsolute);
        pidController = sparkMax.getPIDController();
    }

    /**
     * Add a follower SparkMax.
     * 
     * @param port The port (ID) of the follower SparkMax.
     */
    @Override
    public void addFollower(final int port) {
        sparkMaxFollowers.add(new CANSparkMax(port, MotorType.kBrushless));
    }

    /**
     * Configures the PID controller for the motor.
     * 
     * @param kPID The KPID value to configure the motor too.
     */
    @Override
    public void configurePID(final KPID kPID) {
        pidController.setP(kPID.getPGains());
        pidController.setI(kPID.getIGains());
        pidController.setD(kPID.getDGains());
        pidController.setFF(kPID.getFGains());
        double iZone;
        if ((iZone = kPID.getIZone()) > 0)
            pidController.setIZone(iZone);
        pidController.setIZone(kPID.getIGains());
        pidController.setOutputRange(kPID.getMin(), kPID.getMax());
    }

    /**
     * Sets the output of the motor to the given percent.
     * 
     * @param percent The percent the motor should output at.
     */
    @Override
    public void setPercent(final double percent) {
        sparkMax.set(percent);
        for (CANSparkMax canSparkMax : sparkMaxFollowers)
            canSparkMax.follow(sparkMax);
    }

    // Setters implemented from TorquePIDMotor

    /**
     * Set the motor's position in encoder units.
     * 
     * @param setpoint The encoder units to set the motor to.
     */
    @Override
    public void setPosition(final double setpoint) {
        setPositionRotations(setpoint / CLICKS_PER_ROTATION);
    }

    /**
     * Set the motor's position in degrees.
     * 
     * @param setpoint The degrees to set the motor to.
     */
    @Override
    public void setPositionDegrees(final double setpoint) {
        setPositionRotations(setpoint / 360);
    }

    /**
     * Set the motor's position in rotations.
     * 
     * @param setpoint The rotations to set the motor to.
     */
    @Override
    public void setPositionRotations(final double setpoint) {
        try {
            pidController.setReference(setpoint, ControlType.kPosition);
            for (CANSparkMax follower : sparkMaxFollowers)
                follower.follow(sparkMax);
        } catch (Exception e) {
            System.out.printf("TorqueSparkMax port %d: You need to configure the PID", port);
        }
    }

    /**
     * Set the motor's velocity in encoder units per second.
     * 
     * @param setpoint The encoder units per second to set the motor to.
     */
    @Override
    public void setVelocity(final double setpoint) {
        setVelocityRPS(setpoint / CLICKS_PER_ROTATION); 
    }

    /**
     * Set the motor's velocity in RPS.
     * 
     * @param setpoint The RPS to set the motor to.
     */
    @Override
    public void setVelocityRPS(final double setpoint) {
        setVelocityRPM(setpoint * 60);
    }

    /**
     * Set the motor's velocity in RPM.
     * 
     * @param setpoint The RPM to set the motor to.
     */
    @Override
    public void setVelocityRPM(final double setpoint) {
        try {
            pidController.setReference(setpoint, ControlType.kVelocity);
            for (CANSparkMax follower : sparkMaxFollowers)
                follower.follow(sparkMax);
        } catch (Exception e) {
            System.out.printf("TorqueSparkMax port %d: You need to configure the PID", port);
        } 
    }

    // Extra set methods

    /**
     * Set the motor to output a certain voltage setpoint.
     * 
     * @param setpoint The voltage to output.
     */
    public void setVoltage(final double setpoint) {
        sparkMax.setVoltage(setpoint);
        for (CANSparkMax follower : sparkMaxFollowers)
            follower.follow(sparkMax);
    }

    // Getters implemented from TorqueEncoderMotor

    /**
     * Get the position of the motor in encoder units.
     *
     * @return The position of the encoder in encoder units.
     */
    @Override
    public double getPosition() {
        return getPosition() * CLICKS_PER_ROTATION;
    }
    
    /**
     * Get the position of the motor in degrees.
     *
     * @return The position of the encoder in degrees.
     */
    @Override
    public double getPositionDegrees() {
        return getPosition() * 360;
    }

    /**
     * Get the position of the motor in rotations.
     *
     * @return The position of the encoder in rotations.
     */
    @Override
    public double getPositionRotations() {
        return sparkMaxEncoder.getPosition() - encoderZero;
    }

    /**
     * Get the velocity of the motor in encoder units per second.
     * 
     * @return acceleration in encoder units per second.
     */
    @Override
    public double getVelocity() {
        return getVelocityRPS() * CLICKS_PER_ROTATION;
    }

    /**
     * Get the velocity of the motor in RPS.
     * 
     * @return acceleration in RPS.
     */
    @Override
    public double getVelocityRPS() {
        return getVelocityRPM() / 60;
    }

    /**
     * Get the velocity of the motor in RPM.
     * 
     * @return acceleration in RPM.
     */
    @Override
    public double getVelocityRPM() {
        return sparkMaxEncoder.getVelocity();
    }

    /**
     * Get the acceleration of the motor in encoder units per second per second.
     * 
     * @return acceleration in encoder units per second per second.
     */
    @Override
    public double getAcceleration() {
        return getAccelerationRPS() * CLICKS_PER_ROTATION;
    }

      /**
     * Get the acceleration of the motor in RPS/s.
     * 
     * @return acceleration in RPM/s.
     */
    @Override
    public double getAccelerationRPS() {
        return getAccelerationRPM() / 60; 
    }

    /**
     * Get the acceleration of the motor in RPM/s.
     * 
     * @return acceleration in RPM/s.
     */
    @Override
    public double getAccelerationRPM() {
        final double currentVelocity = getVelocityRPM();
        final long currentTime = System.currentTimeMillis();

        final double acceleration = (currentVelocity - lastVelocity) / (currentTime - lastVelocityTime);

        lastVelocity = currentVelocity;
        lastVelocityTime = currentTime;

        return acceleration;
    }

    // Utility methods and SparkMax specific methods

    /**
     * Restores the lead SparkMax to factory defaults.
     */
    public void restoreFactoryDefaults() {
        sparkMax.restoreFactoryDefaults();
    }

     /**
     * @apiNote UNSAFE
     */
    public void enableVoltageCompensation() {
        sparkMax.enableVoltageCompensation(2);
        for (CANSparkMax follower : sparkMaxFollowers) {
            follower.enableVoltageCompensation(2);
        }
    }

    /**
     * @apiNote UNSAFE
     */
    public void disableVoltageCompensation() {
        sparkMax.disableVoltageCompensation();
        for (CANSparkMax follower : sparkMaxFollowers) {
            follower.disableVoltageCompensation();
        }

    }

    /** 
     * Gets voltage used by the SparkMax.
     * 
     * @return voltage used by the SparkMax.
     */
    public double getVoltage() {
        return sparkMax.getBusVoltage();
    }

    /**
     * Sets the inversion status of the lead motor.
     * 
     * @param inverted To invert or not to invert.
     */
    public void invertPolarity(final boolean invert) {
        sparkMax.setInverted(invert);
    }

    /** 
     * Configures an I-Zone on PID.
     * 
     * @param iZone The I-Zone value to set.
     * 
     * @deprecated I-Zone is now included in KPID.
     */
    @Deprecated
    public void configureIZone(final double iZone) {
        pidController.setIZone(iZone);
    }

    

    // Smart motion functions.

    /**
     * Configure needed variables for smart motion.
     * 
     * - setSmartMotionMaxVelocity() will limit the velocity in RPM of the pid
     * controller in Smart Motion mode - setSmartMotionMinOutputVelocity() will put
     * a lower bound in RPM of the pid controller in Smart Motion mode -
     * setSmartMotionMaxAccel() will limit the acceleration in RPM^2 of the pid
     * controller in Smart Motion mode - setSmartMotionAllowedClosedLoopError() will
     * set the max allowed error for the pid controller in Smart Motion mode
     * 
     * @param maxVelocity     the max velocity
     * @param minVelocity     the min velocity
     * @param maxAcceleration the maxAcceleration
     * @param allowedError    the allowed amount of error
     * @param id              the id for the pid (usually 0)
     *  
     * @author Jack Pittenger 
     */
    public void configureSmartMotion(final double maxVelocity, 
                                     final double minVelocity, 
                                     final double maxAcceleration,
                                     final double allowedError, 
                                     final int id) {
        pidController.setSmartMotionMaxVelocity(maxVelocity, id);
        pidController.setSmartMotionMinOutputVelocity(minVelocity, id);
        pidController.setSmartMotionMaxAccel(maxAcceleration, id);
        pidController.setSmartMotionAllowedClosedLoopError(allowedError, id);
    }

    // Sparkmax specific CAN utilization reduction functions.
    // Only use these methods if you know what you are doing.

    /**
     * Configures CAN frames to be quick on the lead motor.
     * 
     * @apiNote Only use these methods if you know what you are doing.
     * @author Jack Pittenger
     */
    public void configureFastLeader() {
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 2);
    }

    /**
     * Configure the CAN frames for a "dumb motor," which won't need to access CAN
     * data often or at all.
     * 
     * @apiNote Only use these methods if you know what you are doing.
     * @author Jack Pittenger
     */
    public void configureDumbCANFrame() {
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 200);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 1000);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 1000);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 2000);
    }

    /**
     * Configure the CAN frames for a "dumb motor" leader, which won't give data
     * often but will update fast for its follower.
     * 
     * @apiNote Only use these methods if you know what you are doing.
     * @author Jack Pittenger
     */
    public void configureDumbLeaderCANFrame() {
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 20);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 1000);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 1000);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 2000);
    }

    /**
     * Configures the CAN frame for a no-follower encoder-positional only sparkmax;
     * such as would be in a climber.
     * 
     * @apiNote Only use these methods if you know what you are doing.
     * @author Jack Pittenger
     */
    public void configurePositionalCANFrame() {
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 143);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 500);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 20);
        sparkMax.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 1000);
    }

    /**
     * Reduce the CAN frame interval for a follower.
     * 
     * @apiNote Only use these methods if you know what you are doing.
     * @author Jack Pittenger
     */
    public void lowerFollowerCANFrame() {
        for (CANSparkMax follower : sparkMaxFollowers) {
            follower.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 100);
            follower.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 500);
            follower.setPeriodicFramePeriod(PeriodicFrame.kStatus2, 500);
            follower.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 1000);
        }
    }

}