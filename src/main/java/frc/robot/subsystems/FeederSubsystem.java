package frc.robot.subsystems;

import com.revrobotics.PersistMode;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.CAN;
import frc.robot.Constants.FeederConstants;

public class FeederSubsystem extends SubsystemBase {

  private final SparkMax feeder;

  private boolean running = false;
  private double dashboardSpeed = FeederConstants.DEFAULT_SPEED;

  public FeederSubsystem() {
    feeder = new SparkMax(CAN.FEEDER_ID, MotorType.kBrushless);

    SparkMaxConfig cfg = new SparkMaxConfig();
    cfg.inverted(false)
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(FeederConstants.CURRENT_LIMIT)
        .openLoopRampRate(FeederConstants.RAMP_RATE);

    feeder.configure(cfg, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    SmartDashboard.setDefaultNumber("Feeder/Speed", dashboardSpeed);
  }

  public void start() {
    running = true;
  }

  public void stop() {
    running = false;
    feeder.set(0.0);
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void periodic() {
    if (Constants.TUNING_ENABLED) {
      dashboardSpeed = MathUtil.clamp(
          SmartDashboard.getNumber("Feeder/Speed", FeederConstants.DEFAULT_SPEED), -1.0, 1.0);
    }

    double commanded = running ? dashboardSpeed : 0.0;
    feeder.set(commanded);

    SmartDashboard.putBoolean("Feeder/Running", running);
    SmartDashboard.putNumber("Feeder/Current", feeder.getOutputCurrent());
    SmartDashboard.putNumber("Feeder/Temp", feeder.getMotorTemperature());
  }
}
