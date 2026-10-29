package frc.robot;

import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.Constants.OIConstants;
import frc.robot.Constants.ShootCommandConstants;
import frc.robot.Constants.VisionConstants;
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
    // false iken sag tetik dashboard rpm/hood kullanir (tablo olcmek icin). mac'ta tuning kapali, bu anahtar okunmaz.
    SmartDashboard.setDefaultBoolean("Shoot/UseDistance", true);
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
  private final AlignToHubCommand alignForManual =
      new AlignToHubCommand(drive, vision, () -> driver.getLeftY(), this::distanceEnabled);
  private final ShootCommand distanceShoot = new ShootCommand(
      shooter, feeder, hood, intakePivot, intakeRoller,
      this::shotRpm, this::shotHood,
      this::armManualShot, this::feedManualShot, 0);
  private final ShootCommand aimedShoot = new ShootCommand(
      shooter, feeder, hood, intakePivot, intakeRoller,
      this::shotRpm, this::shotHood,
      this::armAimedShot, this::feedAimedShot, 0);

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

    // Right trigger: distance hood/RPM, turn onto the tag when one is in range, feed after the PID
    // settles. With no tag, falls back to the manual RPM and hood and does not wait for alignment.
    Trigger shootTrigger = driver.rightTrigger(OIConstants.TRIGGER_THRESHOLD)
        .or(driver.rightBumper())
        .and(teleop);
    shootTrigger.whileTrue(Commands.parallel(alignForManual, distanceShoot));

    // Left trigger: same distance shot, plus turn onto our hub tag. Feed waits for alignment.
    Trigger autoAimTrigger = driver.leftTrigger(OIConstants.TRIGGER_THRESHOLD)
        .or(driver.leftBumper())
        .and(teleop);
    autoAimTrigger.whileTrue(Commands.parallel(align, aimedShoot));

    // Rumble only while balls are actually being fed, not merely when the wheel is up to speed.
    new Trigger(feeder::isRunning)
        .whileTrue(Commands.startEnd(
            () -> driver.setRumble(RumbleType.kBothRumble, 0.5),
            () -> driver.setRumble(RumbleType.kBothRumble, 0.0)));
  }

  /**
   * Match always uses the distance table. With tuning on, {@code Shoot/UseDistance = false}
   * forces the manual RPM and hood so a test shot can fill the table.
   */
  private boolean distanceEnabled() {
    if (!VisionConstants.USE_DISTANCE_SHOT) {
      return false;
    }
    if (Constants.TUNING_ENABLED) {
      return SmartDashboard.getBoolean("Shoot/UseDistance", true);
    }
    return true;
  }

  /** Teleop shot. The tuning dashboard can force manual RPM while the table is being measured. */
  private double shotRpm() {
    return rpmForShot(false);
  }

  /** Autonomous ignores the tuning override and always uses the distance table. */
  private double autoRpm() {
    return rpmForShot(true);
  }

  private double rpmForShot(boolean forceTable) {
    if (!(forceTable ? VisionConstants.USE_DISTANCE_SHOT : distanceEnabled())) {
      return shooter.getManualRpm();
    }
    if (vision.hasInRangeShot()) {
      return vision.getTargetRpm(shooter.getManualRpm());
    }
    if (vision.isOutOfRange()) {
      return shooter.getTargetRpm();
    }
    return shooter.getManualRpm();
  }

  /** Teleop hood. Same override rules as {@link #shotRpm}. */
  private double shotHood() {
    return hoodForShot(false);
  }

  private double autoHood() {
    return hoodForShot(true);
  }

  private double hoodForShot(boolean forceTable) {
    if (!(forceTable ? VisionConstants.USE_DISTANCE_SHOT : distanceEnabled())) {
      return hood.getManualDegrees();
    }
    if (vision.hasInRangeShot()) {
      return vision.getTargetHoodDegrees(hood.getManualDegrees());
    }
    if (vision.isOutOfRange()) {
      return hood.getTargetDegrees();
    }
    return hood.getManualDegrees();
  }

  /** Right trigger: arm on a valid distance, or on manual values when the camera sees nothing. */
  private boolean armManualShot() {
    if (!distanceEnabled()) {
      return true;
    }
    return vision.hasInRangeShot() || !vision.hasTarget();
  }

  /**
   * Right trigger feeds once settled. An in-range tag also has to be aligned. No tag keeps the
   * manual pit shot. A tag outside the table never feeds.
   */
  private boolean feedManualShot() {
    if (!distanceEnabled()) {
      return true;
    }
    if (vision.isOutOfRange()) {
      return false;
    }
    if (vision.hasInRangeShot()) {
      return alignForManual.isAligned();
    }
    return true;
  }

  /** Left trigger does not spin up to a guessed RPM before a real distance exists. */
  private boolean armAimedShot() {
    if (!distanceEnabled()) {
      return true;
    }
    return vision.hasInRangeShot();
  }

  private boolean feedAimedShot() {
    return align.isAligned() && armAimedShot();
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
   * Aim at our hub, set hood and RPM from the distance table, and feed until 8 balls are counted.
   * Ends earlier on the timeout if the tag never appears or the dips are not detected.
   */
  public Command getAutonomousCommand() {
    AlignToHubCommand autoAlign = new AlignToHubCommand(drive, vision, () -> 0.0);
    ShootCommand shoot = new ShootCommand(
        shooter, feeder, hood, intakePivot, intakeRoller,
        this::autoRpm, this::autoHood,
        () -> !VisionConstants.USE_DISTANCE_SHOT || vision.hasInRangeShot(),
        () -> autoAlign.isAligned()
            && (!VisionConstants.USE_DISTANCE_SHOT || vision.hasInRangeShot()),
        ShootCommandConstants.AUTO_BALL_COUNT);
    return Commands.deadline(shoot, autoAlign)
        .withTimeout(ShootCommandConstants.AUTO_TIMEOUT_SECONDS)
        .withName("Shoot8");
  }
}
