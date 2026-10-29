package frc.robot.subsystems;

import com.revrobotics.PersistMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.FeedbackSensor;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.CAN;
import frc.robot.Constants.IntakePivotConstants;

public class IntakePivotSubsystem extends SubsystemBase {

  private final SparkMax pivot;
  private final RelativeEncoder encoder;
  private final SparkClosedLoopController pid;

  private boolean open = false;
  private double setpointDegrees;

  private double openDegrees   = IntakePivotConstants.DEFAULT_OPEN_DEGREES;
  private double closedDegrees = IntakePivotConstants.DEFAULT_CLOSED_DEGREES;

  private double kP = IntakePivotConstants.DEFAULT_KP;
  private double kI = IntakePivotConstants.DEFAULT_KI;
  private double kD = IntakePivotConstants.DEFAULT_KD;

  public IntakePivotSubsystem() {
    pivot = new SparkMax(CAN.INTAKE_PIVOT_ID, MotorType.kBrushless);

    SparkMaxConfig cfg = new SparkMaxConfig();
    cfg.inverted(false)
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(IntakePivotConstants.CURRENT_LIMIT)
        .openLoopRampRate(IntakePivotConstants.RAMP_RATE)
        .closedLoopRampRate(IntakePivotConstants.RAMP_RATE);

    cfg.encoder
        .positionConversionFactor(IntakePivotConstants.POSITION_CONVERSION_FACTOR)
        .velocityConversionFactor(IntakePivotConstants.VELOCITY_CONVERSION_FACTOR);

    cfg.closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .pid(kP, kI, kD)
        .outputRange(IntakePivotConstants.OUTPUT_MIN, IntakePivotConstants.OUTPUT_MAX);

    cfg.softLimit
        .forwardSoftLimitEnabled(true)
        .forwardSoftLimit(IntakePivotConstants.SOFT_LIMIT_FORWARD)
        .reverseSoftLimitEnabled(true)
        .reverseSoftLimit(IntakePivotConstants.SOFT_LIMIT_REVERSE);

    pivot.configure(cfg, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    encoder = pivot.getEncoder();
    encoder.setPosition(0.0);
    pid = pivot.getClosedLoopController();

    setpointDegrees = closedDegrees;
    pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);

    SmartDashboard.setDefaultNumber("Intake/OpenDegrees",   openDegrees);
    SmartDashboard.setDefaultNumber("Intake/ClosedDegrees", closedDegrees);
    SmartDashboard.setDefaultNumber("Intake/P", kP);
    SmartDashboard.setDefaultNumber("Intake/I", kI);
    SmartDashboard.setDefaultNumber("Intake/D", kD);
  }

  public void open() {
    open = true;
    setpointDegrees = openDegrees;
    pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);
  }

  public void close() {
    open = false;
    setpointDegrees = closedDegrees;
    pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);
  }

  public void toggle() {
    if (open) {
      close();
    } else {
      open();
    }
  }

  public boolean isOpen() {
    return open;
  }

  public boolean atSetpoint() {
    return Math.abs(encoder.getPosition() - setpointDegrees)
        < IntakePivotConstants.POSITION_TOLERANCE_DEGREES;
  }

  public double getPositionDegrees() {
    return encoder.getPosition();
  }

  @Override
  public void periodic() {
    if (Constants.TUNING_ENABLED) {
      // clamped so a typo on the dashboard cannot command the pivot past its soft limits
      double dashOpen = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/OpenDegrees", openDegrees),
          IntakePivotConstants.SOFT_LIMIT_REVERSE, IntakePivotConstants.SOFT_LIMIT_FORWARD);
      double dashClosed = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/ClosedDegrees", closedDegrees),
          IntakePivotConstants.SOFT_LIMIT_REVERSE, IntakePivotConstants.SOFT_LIMIT_FORWARD);
      double dashP = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/P", kP), 0.0, IntakePivotConstants.MAX_KP);
      double dashI = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/I", kI), 0.0, IntakePivotConstants.MAX_KP);
      double dashD = MathUtil.clamp(
          SmartDashboard.getNumber("Intake/D", kD), 0.0, IntakePivotConstants.MAX_KD);

      if (dashOpen != openDegrees) {
        openDegrees = dashOpen;
        if (open) {
          setpointDegrees = openDegrees;
          pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);
        }
      }
      if (dashClosed != closedDegrees) {
        closedDegrees = dashClosed;
        if (!open) {
          setpointDegrees = closedDegrees;
          pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);
        }
      }

      if (dashP != kP || dashI != kI || dashD != kD) {
        kP = dashP;
        kI = dashI;
        kD = dashD;
        SparkMaxConfig liveCfg = new SparkMaxConfig();
        liveCfg.closedLoop.pid(kP, kI, kD);
        pivot.configure(liveCfg,
            ResetMode.kNoResetSafeParameters, PersistMode.kNoPersistParameters);
      }
    }

    SmartDashboard.putNumber("Intake/Position", encoder.getPosition());
    SmartDashboard.putNumber("Intake/Setpoint", setpointDegrees);
    SmartDashboard.putBoolean("Intake/IsOpen", open);
    SmartDashboard.putBoolean("Intake/AtSetpoint", atSetpoint());
    SmartDashboard.putNumber("Intake/PivotCurrent", pivot.getOutputCurrent());
  }
}
