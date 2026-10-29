package frc.robot;

import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.Constants.OIConstants;
import frc.robot.commands.AlignToHubCommand;
import frc.robot.commands.DriveCommand;
import frc.robot.commands.ShootCommand;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.FeederSubsystem;
import frc.robot.subsystems.HoodSubsystem;
import frc.robot.subsystems.IntakePivotSubsystem;
import frc.robot.subsystems.IntakeRollerSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.VisionSubsystem;

public class RobotContainer {

  private final DriveSubsystem drive = new DriveSubsystem();
  private final IntakePivotSubsystem intakePivot = new IntakePivotSubsystem();
  private final IntakeRollerSubsystem intakeRoller = new IntakeRollerSubsystem(intakePivot);
  private final ShooterSubsystem shooter = new ShooterSubsystem();
  private final FeederSubsystem feeder = new FeederSubsystem();
  private final HoodSubsystem hood = new HoodSubsystem();
  private final VisionSubsystem vision = new VisionSubsystem();

  private final CommandXboxController driver =
      new CommandXboxController(OIConstants.DRIVER_CONTROLLER_PORT);

  public RobotContainer() {
    configureDefaultCommands();
    configureBindings();
  }

  private void configureDefaultCommands() {
    drive.setDefaultCommand(new DriveCommand(
        drive,
        () -> driver.getLeftY(),
        () -> driver.getRightX()));
  }

  private void configureBindings() {
    // These toggles intentionally have NO subsystem requirements: with requirements, pressing
    // them during a shot would cancel ShootCommand.
    driver.a()
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(intakeRoller::toggle));

    driver.b()
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(shooter::toggle));

    new Trigger(() -> driver.getHID().getPOV() != -1)
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(intakePivot::toggle));

    // manual shot: right trigger / right bumper (manual rpm, no aiming)
    Trigger shootTrigger = driver.rightTrigger(OIConstants.TRIGGER_THRESHOLD)
        .or(driver.rightBumper());
    shootTrigger.whileTrue(new ShootCommand(shooter, feeder, hood, intakePivot, intakeRoller));

    // auto aim + shoot: left trigger / left bumper. rotates to our hub tag, spins up to the
    // distance rpm and sets the hood angle meanwhile, and only feeds when the shooter and hood
    // are ready AND aligned.
    // forward/back stays on the left stick (same sign as DriveCommand).
    AlignToHubCommand align = new AlignToHubCommand(drive, vision, () -> driver.getLeftY());
    ShootCommand autoShoot = new ShootCommand(
        shooter, feeder, hood, intakePivot, intakeRoller,
        () -> vision.getTargetRpm(shooter.getManualRpm()),
        () -> vision.getTargetHoodDegrees(hood.getManualDegrees()),
        align::isAligned);

    Trigger autoAimTrigger = driver.leftTrigger(OIConstants.TRIGGER_THRESHOLD)
        .or(driver.leftBumper());
    autoAimTrigger.whileTrue(Commands.parallel(align, autoShoot));

    // rumble when aligned and the shooter is ready
    new Trigger(() -> align.isAligned() && shooter.isReadyToFeed())
        .whileTrue(Commands.startEnd(
            () -> driver.setRumble(RumbleType.kBothRumble, 0.5),
            () -> driver.setRumble(RumbleType.kBothRumble, 0.0)));

    //driver.button(8).whileTrue(new InstantCommand(() -> feeder.start()));
  }

  /**
   * Called when the robot is disabled. The shooter/roller/feeder "running" flags would otherwise
   * survive the disable and start the motors again on the next enable.
   */
  public void onDisabled() {
    shooter.stop();
    shooter.clearFault();
    intakeRoller.stop();
    feeder.stop();
    driver.setRumble(RumbleType.kBothRumble, 0.0);
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
