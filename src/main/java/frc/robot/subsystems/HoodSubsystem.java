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
import frc.robot.Constants.HoodConstants;
import frc.robot.util.BandLatch;

/**
 * Adjustable shooter hood on a NEO with its built-in encoder. The angle is in degrees of hood
 * travel measured from HOME, the position the hood rests in when the robot is powered on (the
 * encoder is zeroed at start-up, like the intake pivot).
 */
public class HoodSubsystem extends SubsystemBase {

  private final SparkMax hood;
  private final RelativeEncoder encoder;
  private final SparkClosedLoopController pid;

  private final BandLatch readyLatch = new BandLatch(
      HoodConstants.READY_TOLERANCE_DEGREES,
      HoodConstants.DROP_TOLERANCE_DEGREES,
      HoodConstants.READY_DEBOUNCE_SECONDS);

  private double setpointDegrees = HoodConstants.HOME_DEGREES;
  // angle used for manual shots (right trigger) and when vision has no angle to offer
  private double manualDegrees = HoodConstants.DEFAULT_DEGREES;
  private double kP = HoodConstants.DEFAULT_KP;
  private boolean ready = false;

  public HoodSubsystem() {
    hood = new SparkMax(CAN.HOOD_ID, MotorType.kBrushless);

    SparkMaxConfig cfg = new SparkMaxConfig();
    cfg.inverted(HoodConstants.INVERTED)
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(HoodConstants.CURRENT_LIMIT)
        .openLoopRampRate(HoodConstants.RAMP_RATE)
        .closedLoopRampRate(HoodConstants.RAMP_RATE);

    cfg.encoder
        .positionConversionFactor(HoodConstants.POSITION_CONVERSION_FACTOR)
        .velocityConversionFactor(HoodConstants.POSITION_CONVERSION_FACTOR / 60.0);

    cfg.closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .pid(kP, 0.0, 0.0)
        .outputRange(HoodConstants.OUTPUT_MIN, HoodConstants.OUTPUT_MAX);

    cfg.softLimit
        .forwardSoftLimitEnabled(true)
        .forwardSoftLimit(HoodConstants.MAX_DEGREES + HoodConstants.SOFT_LIMIT_MARGIN)
        .reverseSoftLimitEnabled(true)
        .reverseSoftLimit(HoodConstants.MIN_DEGREES - HoodConstants.SOFT_LIMIT_MARGIN);

    hood.configure(cfg, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    encoder = hood.getEncoder();
    encoder.setPosition(HoodConstants.HOME_DEGREES);
    pid = hood.getClosedLoopController();
    pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);

    SmartDashboard.setDefaultNumber("Hood/ManualDegrees", manualDegrees);
    SmartDashboard.setDefaultNumber("Hood/kP", kP);
  }

  /**
   * Moves the hood to {@code degrees} (clamped to the mechanical range). Targets within the
   * retarget deadband of the current one are ignored so ty noise does not make the hood hunt.
   */
  public void setTargetDegrees(double degrees) {
    double target = MathUtil.clamp(degrees, HoodConstants.MIN_DEGREES, HoodConstants.MAX_DEGREES);
    if (Math.abs(target - setpointDegrees) < HoodConstants.RETARGET_DEADBAND_DEGREES) {
      return;
    }
    applySetpoint(target);
  }

  /** Lowers the hood to its home position, even when the previous setpoint was inside the deadband. */
  public void stow() {
    applySetpoint(HoodConstants.HOME_DEGREES);
  }

  private void applySetpoint(double degrees) {
    setpointDegrees = degrees;
    pid.setSetpoint(setpointDegrees, ControlType.kPosition, ClosedLoopSlot.kSlot0);
    if (Math.abs(getPositionDegrees() - setpointDegrees) > HoodConstants.READY_TOLERANCE_DEGREES) {
      readyLatch.reset();
      ready = false;
    }
  }

  /** The angle used for manual shots (dashboard value while tuning, otherwise the constant). */
  public double getManualDegrees() {
    return manualDegrees;
  }

  public double getTargetDegrees() {
    return setpointDegrees;
  }

  public double getPositionDegrees() {
    return encoder.getPosition();
  }

  /** True once the hood has stayed at its target for the debounce time (with hysteresis). */
  public boolean isReady() {
    return ready;
  }

  @Override
  public void periodic() {
    if (Constants.TUNING_ENABLED) {
      manualDegrees = MathUtil.clamp(
          SmartDashboard.getNumber("Hood/ManualDegrees", manualDegrees),
          HoodConstants.MIN_DEGREES, HoodConstants.MAX_DEGREES);

      double newKp = MathUtil.clamp(
          SmartDashboard.getNumber("Hood/kP", kP), 0.0, HoodConstants.MAX_KP);
      if (newKp != kP) {
        kP = newKp;
        SparkMaxConfig liveCfg = new SparkMaxConfig();
        liveCfg.closedLoop.pid(kP, 0.0, 0.0);
        liveCfg.closedLoop.outputRange(HoodConstants.OUTPUT_MIN, HoodConstants.OUTPUT_MAX);
        hood.configure(liveCfg,
            ResetMode.kNoResetSafeParameters, PersistMode.kNoPersistParameters);
      }
    }

    ready = readyLatch.update(encoder.getPosition() - setpointDegrees, true);

    SmartDashboard.putNumber("Hood/Position", encoder.getPosition());
    SmartDashboard.putNumber("Hood/Setpoint", setpointDegrees);
    SmartDashboard.putBoolean("Hood/Ready", ready);
    SmartDashboard.putNumber("Hood/Current", hood.getOutputCurrent());
    SmartDashboard.putNumber("Hood/Temp", hood.getMotorTemperature());
  }
}
