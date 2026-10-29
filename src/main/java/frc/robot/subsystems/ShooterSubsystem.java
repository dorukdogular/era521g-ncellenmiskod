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
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.CAN;
import frc.robot.Constants.ShooterConstants;
import frc.robot.util.BandLatch;

public class ShooterSubsystem extends SubsystemBase {

  private final SparkMax shooter;
  private final RelativeEncoder encoder;
  private final SparkClosedLoopController pid;

  private boolean running = false;
  private boolean ready = false;
  private boolean faulted = false;
  private double wrongDirectionSince = Double.NaN;

  // rpm used when nobody (e.g. vision) sets a target: B toggle, manual shot
  private double manualRpm = ShooterConstants.DEFAULT_TARGET_RPM;
  private double targetRpm = ShooterConstants.DEFAULT_TARGET_RPM;
  private double kP        = ShooterConstants.DEFAULT_KP;
  private double kV        = ShooterConstants.KV;

  private final BandLatch readyLatch = new BandLatch(
      ShooterConstants.READY_TOLERANCE_RPM,
      ShooterConstants.DROP_TOLERANCE_RPM,
      ShooterConstants.READY_DEBOUNCE_SECONDS);

  public ShooterSubsystem() {
    shooter = new SparkMax(CAN.SHOOTER_ID, MotorType.kBrushless);

    SparkMaxConfig cfg = new SparkMaxConfig();
    // positive rpm = shooting direction (was -1.0 duty before; inverted so rpm reads positive)
    cfg.inverted(true)
        .idleMode(IdleMode.kCoast)
        .smartCurrentLimit(ShooterConstants.CURRENT_LIMIT)
        .voltageCompensation(ShooterConstants.NOMINAL_VOLTAGE)
        .openLoopRampRate(ShooterConstants.OPEN_LOOP_RAMP)
        .closedLoopRampRate(ShooterConstants.CLOSED_LOOP_RAMP);

    cfg.closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .pid(kP, 0.0, 0.0)
        // never drive the flywheel backwards to slow down, let it coast
        .outputRange(0.0, 1.0);
    cfg.closedLoop.feedForward.kV(kV);

    // lighter velocity filtering than the default (32 ms / depth 8) so the reading lags less
    cfg.encoder
        .uvwMeasurementPeriod(ShooterConstants.VELOCITY_MEASUREMENT_PERIOD_MS)
        .uvwAverageDepth(ShooterConstants.VELOCITY_AVERAGE_DEPTH);

    shooter.configure(cfg, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    encoder = shooter.getEncoder();
    pid = shooter.getClosedLoopController();

    SmartDashboard.setDefaultNumber("Shooter/TargetRPM", manualRpm);
    SmartDashboard.setDefaultNumber("Shooter/kP", kP);
    SmartDashboard.setDefaultNumber("Shooter/kV", kV);
  }

  public void start() {
    if (!faulted) {
      running = true;
    }
  }

  public void stop() {
    running = false;
    ready = false;
    readyLatch.reset();
    targetRpm = manualRpm;
    shooter.set(0.0);
  }

  /** Clears a latched fault. Called when the robot is disabled. */
  public void clearFault() {
    faulted = false;
    wrongDirectionSince = Double.NaN;
  }

  public boolean isFaulted() {
    return faulted;
  }

  public void toggle() {
    if (running) {
      stop();
    } else {
      targetRpm = manualRpm;
      start();
    }
  }

  public boolean isRunning() {
    return running;
  }

  /** Sets the flywheel target, clamped to the safe range. Does not start the shooter. */
  public void setTargetRpm(double rpm) {
    targetRpm = MathUtil.clamp(
        rpm, ShooterConstants.MIN_TARGET_RPM, ShooterConstants.MAX_TARGET_RPM);
  }

  /** The RPM used for manual shots (dashboard value while tuning, otherwise the constant). */
  public double getManualRpm() {
    return manualRpm;
  }

  public double getTargetRpm() {
    return targetRpm;
  }

  public double getCurrentRPM() {
    return encoder.getVelocity();
  }

  /**
   * True once the flywheel has stayed inside the ready band for the debounce time. Stays true
   * through the small dip when a ball leaves and only drops when RPM falls well below target.
   */
  public boolean isReadyToFeed() {
    return ready;
  }

  @Override
  public void periodic() {
    if (Constants.TUNING_ENABLED) {
      double newManual = MathUtil.clamp(
          SmartDashboard.getNumber("Shooter/TargetRPM", manualRpm),
          ShooterConstants.MIN_TARGET_RPM, ShooterConstants.MAX_TARGET_RPM);
      if (!running && newManual != manualRpm) {
        targetRpm = newManual;
      }
      manualRpm = newManual;

      double newKp = MathUtil.clamp(
          SmartDashboard.getNumber("Shooter/kP", kP), 0.0, ShooterConstants.MAX_KP);
      double newKv = MathUtil.clamp(
          SmartDashboard.getNumber("Shooter/kV", kV),
          ShooterConstants.KV * ShooterConstants.MIN_KV_FACTOR,
          ShooterConstants.KV * ShooterConstants.MAX_KV_FACTOR);
      if (newKp != kP || newKv != kV) {
        kP = newKp;
        kV = newKv;
        SparkMaxConfig liveCfg = new SparkMaxConfig();
        liveCfg.closedLoop.pid(kP, 0.0, 0.0);
        liveCfg.closedLoop.feedForward.kV(kV);
        shooter.configure(liveCfg,
            ResetMode.kNoResetSafeParameters, PersistMode.kNoPersistParameters);
      }
    }

    double rpm = getCurrentRPM();

    // Safety: if the encoder reads the wrong way round (e.g. inversion not applied to the
    // encoder) the velocity PID would run away to full output. Cut the shooter and latch a fault.
    double now = Timer.getFPGATimestamp();
    if (running && rpm < -ShooterConstants.WRONG_DIRECTION_RPM) {
      if (Double.isNaN(wrongDirectionSince)) {
        wrongDirectionSince = now;
      } else if (now - wrongDirectionSince >= ShooterConstants.WRONG_DIRECTION_SECONDS) {
        faulted = true;
        stop();
      }
    } else {
      wrongDirectionSince = Double.NaN;
    }

    ready = readyLatch.update(rpm - targetRpm, running);

    if (running) {
      pid.setSetpoint(targetRpm, ControlType.kVelocity, ClosedLoopSlot.kSlot0);
    } else {
      shooter.set(0.0);
    }

    SmartDashboard.putBoolean("Shooter/Running", running);
    SmartDashboard.putNumber("Shooter/CurrentRPM", rpm);
    SmartDashboard.putNumber("Shooter/ActiveTargetRPM", targetRpm);
    SmartDashboard.putBoolean("Shooter/Ready", ready);
    SmartDashboard.putBoolean("Shooter/Fault", faulted);
    SmartDashboard.putNumber("Shooter/Current", shooter.getOutputCurrent());
    // volts actually applied per rpm. at a steady rpm this is the kV the flywheel really needs:
    // copy it into ShooterConstants.KV (or the Shooter/kV dashboard value) when it differs
    double appliedVolts = shooter.getAppliedOutput() * shooter.getBusVoltage();
    SmartDashboard.putNumber("Shooter/AppliedVolts", appliedVolts);
    SmartDashboard.putNumber("Shooter/ImpliedKV", rpm > 500.0 ? appliedVolts / rpm : 0.0);
    SmartDashboard.putNumber("Shooter/Temp", shooter.getMotorTemperature());
  }
}
