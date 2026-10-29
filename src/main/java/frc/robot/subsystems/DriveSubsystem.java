package frc.robot.subsystems;

import com.revrobotics.PersistMode;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.CAN;
import frc.robot.Constants.DriveConstants;

public class DriveSubsystem extends SubsystemBase {

  private final SparkMax leftLeader;
  private final SparkMax leftFollower;
  private final SparkMax rightLeader;
  private final SparkMax rightFollower;

  private final DifferentialDrive drive;

  private final SlewRateLimiter leftLimiter  = new SlewRateLimiter(DriveConstants.SLEW_RATE_LIMIT);
  private final SlewRateLimiter rightLimiter = new SlewRateLimiter(DriveConstants.SLEW_RATE_LIMIT);

  public DriveSubsystem() {
    leftLeader    = new SparkMax(CAN.LEFT_FRONT_ID,  MotorType.kBrushed);
    leftFollower  = new SparkMax(CAN.LEFT_BACK_ID,   MotorType.kBrushed);
    rightLeader   = new SparkMax(CAN.RIGHT_FRONT_ID, MotorType.kBrushed);
    rightFollower = new SparkMax(CAN.RIGHT_BACK_ID,  MotorType.kBrushed);

    SparkMaxConfig leftLeaderCfg = baseConfig();
    leftLeaderCfg.inverted(DriveConstants.LEFT_INVERTED);

    SparkMaxConfig rightLeaderCfg = baseConfig();
    rightLeaderCfg.inverted(DriveConstants.RIGHT_INVERTED);

    SparkMaxConfig leftFollowerCfg = baseConfig();
    leftFollowerCfg.follow(leftLeader);

    SparkMaxConfig rightFollowerCfg = baseConfig();
    rightFollowerCfg.follow(rightLeader);

    leftLeader.configure(leftLeaderCfg,
        ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    leftFollower.configure(leftFollowerCfg,
        ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    rightLeader.configure(rightLeaderCfg,
        ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    rightFollower.configure(rightFollowerCfg,
        ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    drive = new DifferentialDrive(leftLeader::set, rightLeader::set);
    drive.setDeadband(0.0);
    drive.setMaxOutput(DriveConstants.MAX_OUTPUT);
    drive.setSafetyEnabled(true);
    drive.setExpiration(DriveConstants.SAFETY_EXPIRATION);
  }

  private static SparkMaxConfig baseConfig() {
    SparkMaxConfig cfg = new SparkMaxConfig();
    cfg.idleMode(IdleMode.kBrake)
        .smartCurrentLimit(DriveConstants.CURRENT_LIMIT_BRUSHED)
        .openLoopRampRate(DriveConstants.OPEN_LOOP_RAMP)
        .closedLoopRampRate(DriveConstants.CLOSED_LOOP_RAMP);
    return cfg;
  }

  public void tankDrive(double leftRaw, double rightRaw) {
    double left  = MathUtil.applyDeadband(leftRaw,  DriveConstants.DEADBAND);
    double right = MathUtil.applyDeadband(rightRaw, DriveConstants.DEADBAND);

    left  = Math.copySign(left  * left,  left);
    right = Math.copySign(right * right, right);

    left  = leftLimiter.calculate(left);
    right = rightLimiter.calculate(right);

    drive.tankDrive(left, right, false);
  }

  public void arcadeDrive(double left, double right) {
    drive.arcadeDrive(left, right);
  }

  /** Arcade drive with explicit input squaring (use false for closed-loop outputs like align). */
  public void arcadeDrive(double speed, double rotation, boolean squareInputs) {
    drive.arcadeDrive(speed, rotation, squareInputs);
  }

  public void stop() {
    drive.stopMotor();
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Drive/LeftCurrent",  leftLeader.getOutputCurrent());
    SmartDashboard.putNumber("Drive/RightCurrent", rightLeader.getOutputCurrent());
    SmartDashboard.putNumber("Drive/LeftAppliedOutput",  leftLeader.getAppliedOutput());
    SmartDashboard.putNumber("Drive/RightAppliedOutput", rightLeader.getAppliedOutput());
  }
}
