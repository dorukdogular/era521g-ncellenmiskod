package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.DriveConstants;
import frc.robot.subsystems.DriveSubsystem;

public class DriveCommand extends Command {

  private final DriveSubsystem drive;
  private final DoubleSupplier leftSupplier;
  private final DoubleSupplier rightSupplier;

  public DriveCommand(DriveSubsystem drive,
                      DoubleSupplier leftSupplier,
                      DoubleSupplier rightSupplier) {
    this.drive = drive;
    this.leftSupplier = leftSupplier;
    this.rightSupplier = rightSupplier;
    addRequirements(drive);
  }

  /**
   * Xbox left Y is negative when the stick is pushed forward. Arcade forward is positive.
   */
  public static double forwardFromStick(double leftY) {
    return -leftY;
  }

  /**
   * Xbox right X is positive to the right. WPILib 2026 arcade rotation is
   * counterclockwise-positive, so a right push has to be negated to turn clockwise.
   */
  public static double turnFromStick(double rightX) {
    return -rightX;
  }

  @Override
  public void execute() {
    // deadband after the sign fix. DifferentialDrive squares these and its own deadband is 0.
    drive.arcadeDrive(
        MathUtil.applyDeadband(forwardFromStick(leftSupplier.getAsDouble()), DriveConstants.DEADBAND),
        MathUtil.applyDeadband(turnFromStick(rightSupplier.getAsDouble()), DriveConstants.DEADBAND));
  }

  @Override
  public void end(boolean interrupted) {
    drive.stop();
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
