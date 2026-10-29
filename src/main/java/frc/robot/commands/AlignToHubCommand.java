package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.Constants.AlignConstants;
import frc.robot.Constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import frc.robot.util.BandLatch;

/**
 * Hold-to-align command. The driver keeps control of forward/back, only the rotation is taken
 * over and driven to zero tx on our own alliance's HUB tag. With no tag in view the robot does
 * not rotate and {@link #isAligned()} is false.
 */
public class AlignToHubCommand extends Command {

  private final DriveSubsystem drive;
  private final VisionSubsystem vision;
  private final DoubleSupplier throttleSupplier;

  private final PIDController pid =
      new PIDController(AlignConstants.KP, AlignConstants.KI, AlignConstants.KD);
  private final BandLatch alignedLatch = new BandLatch(
      AlignConstants.TOLERANCE_DEGREES,
      AlignConstants.EXIT_TOLERANCE_DEGREES,
      AlignConstants.ALIGNED_DEBOUNCE_SECONDS);

  private double maxTurn = AlignConstants.MAX_TURN_OUTPUT;
  private double minTurn = AlignConstants.MIN_TURN_OUTPUT;
  private boolean aligned = false;

  public AlignToHubCommand(DriveSubsystem drive,
                           VisionSubsystem vision,
                           DoubleSupplier throttleSupplier) {
    this.drive = drive;
    this.vision = vision;
    this.throttleSupplier = throttleSupplier;
    addRequirements(drive);

    SmartDashboard.setDefaultNumber("Align/kP", AlignConstants.KP);
    SmartDashboard.setDefaultNumber("Align/kD", AlignConstants.KD);
    SmartDashboard.setDefaultNumber("Align/MaxTurn", AlignConstants.MAX_TURN_OUTPUT);
    SmartDashboard.setDefaultNumber("Align/MinTurn", AlignConstants.MIN_TURN_OUTPUT);
  }

  public boolean isAligned() {
    return aligned;
  }

  /**
   * Applies the minimum-turn floor and the maximum-turn cap to the raw PID output. The floor fades
   * from {@code minTurn} (far from the target) to {@code MIN_TURN_FRACTION * minTurn} at the edge
   * of the tolerance band, so the output does not jump from a hard floor to 0. The cap is applied
   * last, so it always wins even if the dashboard sets maxTurn below minTurn.
   */
  static double shapeTurn(double pidOut, double tx, double minTurn, double maxTurn) {
    double fade = MathUtil.clamp(
        (Math.abs(tx) - AlignConstants.TOLERANCE_DEGREES)
            / (AlignConstants.FADE_DEGREES - AlignConstants.TOLERANCE_DEGREES),
        0.0, 1.0);
    double floor = minTurn
        * (AlignConstants.MIN_TURN_FRACTION + (1.0 - AlignConstants.MIN_TURN_FRACTION) * fade);
    double out = Math.copySign(Math.max(Math.abs(pidOut), floor), pidOut);
    return MathUtil.clamp(out, -maxTurn, maxTurn);
  }

  @Override
  public void initialize() {
    pid.reset();
    alignedLatch.reset();
    aligned = false;
  }

  @Override
  public void execute() {
    if (Constants.TUNING_ENABLED) {
      pid.setPID(
          MathUtil.clamp(SmartDashboard.getNumber("Align/kP", AlignConstants.KP),
              0.0, AlignConstants.MAX_KP),
          0.0,
          MathUtil.clamp(SmartDashboard.getNumber("Align/kD", AlignConstants.KD),
              0.0, AlignConstants.MAX_KP));
      maxTurn = MathUtil.clamp(
          SmartDashboard.getNumber("Align/MaxTurn", AlignConstants.MAX_TURN_OUTPUT), 0.0, 0.6);
      minTurn = MathUtil.clamp(
          SmartDashboard.getNumber("Align/MinTurn", AlignConstants.MIN_TURN_OUTPUT), 0.0, 0.2);
    }

    // same feel as the normal drive command: deadband + squared input
    double throttle = MathUtil.applyDeadband(throttleSupplier.getAsDouble(), DriveConstants.DEADBAND);
    throttle = Math.copySign(throttle * throttle, throttle);

    double turn = 0.0;

    if (vision.hasTarget()) {
      double tx = vision.getTxDegrees();
      aligned = alignedLatch.update(tx, true);

      if (!aligned && Math.abs(tx) > AlignConstants.TOLERANCE_DEGREES) {
        // tx > 0 means the aim point is on the right, so we rotate clockwise (positive rotation)
        double pidOut = -pid.calculate(tx, 0.0) * AlignConstants.TURN_SIGN;
        turn = shapeTurn(pidOut, tx, minTurn, maxTurn);
      } else {
        pid.reset();
      }
    } else {
      alignedLatch.reset();
      aligned = false;
      pid.reset();
    }

    // squareInputs=false: squaring would kill the small corrections near the target
    drive.arcadeDrive(throttle, turn, false);

    SmartDashboard.putBoolean("Align/Aligned", aligned);
    SmartDashboard.putNumber("Align/Turn", turn);
  }

  @Override
  public void end(boolean interrupted) {
    drive.stop();
    aligned = false;
    SmartDashboard.putBoolean("Align/Aligned", false);
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
