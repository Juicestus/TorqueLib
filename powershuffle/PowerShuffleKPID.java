package org.texastorque.torquelib.powershuffle;

import org.texastorque.util.KPID;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.shuffleboard.ComplexWidget;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;

public class PowerShuffleKPID extends KPID implements Sendable {
    private String name;
    private ComplexWidget widget;

    public PowerShuffleKPID(String name) {
        super();
        this.name = name;
        widget = Shuffleboard.getTab("KPID").add(name, this).withWidget(PowerShuffleWidgets.PIDManager.getIdentifier());
    }

    public PowerShuffleKPID(String name, double pGains, double iGains, double dGains, double fGains, double minOutput,
            double maxOutput) {
        super(pGains, iGains, dGains, fGains, minOutput, maxOutput);
        this.name = name;
        widget = Shuffleboard.getTab("KPID").add(name, this).withWidget(PowerShuffleWidgets.PIDManager.getIdentifier());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.addStringProperty("name", this::getName, this::setName);
        builder.addDoubleProperty("p", this::p, this::setP);
        builder.addDoubleProperty("i", this::i, this::setI);
        builder.addDoubleProperty("d", this::d, this::setD);
        builder.addDoubleProperty("f", this::f, this::setF);
        builder.addDoubleProperty("setpoint", () -> 0, x -> {
        });
    }
}