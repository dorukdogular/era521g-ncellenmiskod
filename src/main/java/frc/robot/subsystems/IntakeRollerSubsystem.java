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
import frc.robot.Constants.IntakeRollerConstants;
import frc.robot.Constants.ShootCommandConstants;

public class IntakeRollerSubsystem extends SubsystemBase {

  private final SparkMax roller;
  private final IntakePivotSubsystem pivot;

  private boolean toggleRunning = false;
  private boolean shootAssist = false;
  private double dashboardSpeed = IntakeRollerConstants.DEFAULT_SPEED;

  public IntakeRollerSubsystem(IntakePivotSubsystem pivot) {
    this.pivot = pivot;

    roller = new SparkMax(CAN.INTAKE_ROLLER_ID, MotorType.kBrushless);

    SparkMaxConfig cfg = new SparkMaxConfig();
    cfg.inverted(false)
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(IntakeRollerConstants.CURRENT_LIMIT)
        .openLoopRampRate(IntakeRollerConstants.RAMP_RATE);

    roller.configure(cfg, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    SmartDashboard.setDefaultNumber("Intake/RollerSpeed", IntakeRollerConstants.DEFAULT_SPEED);
  }

  public void start() {
    if (pivot.isOpen()) {
      toggleRunning = true;
    }
  }

  public void stop() {
    toggleRunning = false;
    shootAssist = false;
    roller.set(0.0);
  }

  public void toggle() {
    if (toggleRunning) {
      stop();
    } else {
      start();
    }
  }

  public boolean isRunning() {
    return toggleRunning;
  }

  /**
   * Used by ShootCommand: runs the roller at the assist speed (to help the feed) regardless of
   * the toggle state. The motor is only written in periodic().
   */
  public void setShootAssist(boolean enabled) {
    shootAssist = enabled;
  }

  @Override
  public void periodic() {
    if (Constants.TUNING_ENABLED) {
      dashboardSpeed = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/RollerSpeed", IntakeRollerConstants.DEFAULT_SPEED),
          -1.0, 1.0);
    }

    if (toggleRunning && !pivot.isOpen()) {
      toggleRunning = false;
    }

    double commanded = 0.0;
    if (shootAssist) {
      commanded = ShootCommandConstants.ROLLER_ASSIST_SPEED;
    } else if (toggleRunning) {
      commanded = dashboardSpeed;
    }

    roller.set(commanded);

    SmartDashboard.putBoolean("Intake/RollerRunning", toggleRunning);
    SmartDashboard.putNumber("Intake/RollerCurrent", roller.getOutputCurrent());
    SmartDashboard.putNumber("Intake/RollerTemp", roller.getMotorTemperature());
  }
}
