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

  @Override
  public void execute() {
    //drive.tankDrive(leftSupplier.getAsDouble(), rightSupplier.getAsDouble());
    // deadband first: stick drift used to creep the robot (drive.setDeadband is 0)
    drive.arcadeDrive(
        MathUtil.applyDeadband(leftSupplier.getAsDouble(), DriveConstants.DEADBAND),
        MathUtil.applyDeadband(rightSupplier.getAsDouble(), DriveConstants.DEADBAND));
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
