package frc.robot;

import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.Constants.OIConstants;
import frc.robot.commands.AlignToHubCommand;
import frc.robot.commands.DriveCommand;
import frc.robot.commands.ShootCommand;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.FeederSubsystem;
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

  private final AlignToHubCommand align =
      new AlignToHubCommand(drive, vision, () -> driver.getLeftY());
  private final ShootCommand shoot = new ShootCommand(
      shooter, feeder, intakePivot, intakeRoller,
      align::isAligned);

  private void configureBindings() {
    // These toggles intentionally have NO subsystem requirements: with requirements, pressing
    // them during a shot would cancel ShootCommand.
    Trigger teleop = RobotModeTriggers.teleop();

    driver.a()
        .and(teleop)
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(intakeRoller::toggle));

    driver.b()
        .and(teleop)
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(this::toggleShooterIfIdle));

    new Trigger(() -> driver.getHID().getPOV() != -1)
        .and(teleop)
        .debounce(OIConstants.DEBOUNCE_SECONDS)
        .onTrue(Commands.runOnce(intakePivot::toggle));

    // Either trigger: turn until the hub tag is centered, shoot at 5000 rpm.
    // Forward/back stays on the left stick. Nothing is fed until the tag is centered.
    Trigger shootTrigger = driver.rightTrigger(OIConstants.TRIGGER_THRESHOLD)
        .or(driver.rightBumper())
        .or(driver.leftTrigger(OIConstants.TRIGGER_THRESHOLD))
        .or(driver.leftBumper())
        .and(teleop);
    shootTrigger.whileTrue(Commands.parallel(align, shoot));

    // Rumble only while balls are actually being fed, not merely when the wheel is up to speed.
    new Trigger(feeder::isRunning)
        .whileTrue(Commands.startEnd(
            () -> driver.setRumble(RumbleType.kBothRumble, 0.5),
            () -> driver.setRumble(RumbleType.kBothRumble, 0.0)));
  }

  /**
   * B pre-spins the wheel. A running ShootCommand (or the aim group that owns it) is the
   * requirer of the shooter; calling toggle then would zero the flywheel and drop Ready.
   */
  private void toggleShooterIfIdle() {
    if (CommandScheduler.getInstance().requiring(shooter) == null) {
      shooter.toggle();
    }
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

  /**
   * Turn onto the hub tag and shoot at 5000 rpm for the whole autonomous period.
   * Feeding starts only after the tag is centered.
   */
  public Command getAutonomousCommand() {
    AlignToHubCommand autoAlign = new AlignToHubCommand(drive, vision, () -> 0.0);
    ShootCommand autoShoot = new ShootCommand(
        shooter, feeder, intakePivot, intakeRoller,
        autoAlign::isAligned);
    return Commands.deadline(autoShoot, autoAlign).withName("AlignAndShoot");
  }
}
