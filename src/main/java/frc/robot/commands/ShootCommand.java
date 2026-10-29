package frc.robot.commands;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.ShootCommandConstants;
import frc.robot.subsystems.FeederSubsystem;
import frc.robot.subsystems.HoodSubsystem;
import frc.robot.subsystems.IntakePivotSubsystem;
import frc.robot.subsystems.IntakeRollerSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.util.BallShotCounter;

/**
 * Hold-to-shoot command. While {@code armGate} is true, sets the shooter RPM and hood angle from
 * the suppliers and runs the flywheel. Feeding starts only after the shooter and hood have settled
 * and {@code feedGate} is true. Small supplier noise is ignored inside the subsystems, and a real
 * step drops Ready until the PID is back inside the band.
 *
 * <p>On release, the feeder, roller and pivot stop and the hood goes home. The shooter stops too
 * unless it was already running before the command (then the hood returns to the manual angle).
 * With a positive {@code ballGoal} the command ends once that many flywheel dips have been counted.
 */
public class ShootCommand extends Command {

  private enum State { SPINNING_UP, WAITING_AIM, SHOOTING }

  private final ShooterSubsystem shooter;
  private final FeederSubsystem feeder;
  private final HoodSubsystem hood;
  private final IntakePivotSubsystem pivot;
  private final IntakeRollerSubsystem roller;
  private final DoubleSupplier rpmSupplier;
  private final DoubleSupplier hoodSupplier;
  private final BooleanSupplier armGate;
  private final BooleanSupplier feedGate;
  private final int ballGoal;

  private final Timer oscTimer = new Timer();
  private final BallShotCounter ballCounter = new BallShotCounter(
      ShootCommandConstants.BALL_DIP_RPM,
      ShootCommandConstants.BALL_RECOVER_RPM,
      ShootCommandConstants.BALL_MIN_INTERVAL_SECONDS);

  private State state = State.SPINNING_UP;
  private boolean pivotTargetOpen = false;
  private boolean shooterWasRunning = false;

  /** Manual shot: dashboard/default RPM and hood angle, no aim gate, no ball limit. */
  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      HoodSubsystem hood,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller) {
    this(shooter, feeder, hood, pivot, roller,
        shooter::getManualRpm, hood::getManualDegrees, () -> true, () -> true, 0);
  }

  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      HoodSubsystem hood,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller,
                      DoubleSupplier rpmSupplier,
                      DoubleSupplier hoodSupplier,
                      BooleanSupplier feedGate) {
    this(shooter, feeder, hood, pivot, roller,
        rpmSupplier, hoodSupplier, () -> true, feedGate, 0);
  }

  /**
   * @param armGate when true, apply the RPM and hood suppliers and run the flywheel
   * @param feedGate extra condition (alignment, in-range distance) required before feeding
   * @param ballGoal stop after this many counted shots; 0 means run until cancelled
   */
  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      HoodSubsystem hood,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller,
                      DoubleSupplier rpmSupplier,
                      DoubleSupplier hoodSupplier,
                      BooleanSupplier armGate,
                      BooleanSupplier feedGate,
                      int ballGoal) {
    this.shooter = shooter;
    this.feeder = feeder;
    this.hood = hood;
    this.pivot = pivot;
    this.roller = roller;
    this.rpmSupplier = rpmSupplier;
    this.hoodSupplier = hoodSupplier;
    this.armGate = armGate;
    this.feedGate = feedGate;
    this.ballGoal = ballGoal;
    addRequirements(shooter, feeder, hood, pivot, roller);
  }

  @Override
  public void initialize() {
    state = State.SPINNING_UP;
    pivotTargetOpen = false;
    shooterWasRunning = shooter.isRunning();
    ballCounter.reset();

    applyShotProfile();
    feeder.stop();
    roller.stop();
    pivot.close();

    oscTimer.reset();
    oscTimer.stop();
  }

  @Override
  public void execute() {
    applyShotProfile();

    boolean ready = shooter.isReadyToFeed() && hood.isReady();
    boolean feed = feedGate.getAsBoolean();

    if (state == State.SHOOTING && !(ready && feed)) {
      // lost speed, hood angle, distance or aim: stop feeding until they are good again
      feeder.stop();
      roller.setShootAssist(false);
      oscTimer.stop();
      state = State.SPINNING_UP;
    }

    switch (state) {
      case SPINNING_UP:
      case WAITING_AIM:
        feeder.stop();
        roller.setShootAssist(false);
        if (ready && feed) {
          state = State.SHOOTING;
          pivotTargetOpen = true;
          pivot.open();
          feeder.start();
          oscTimer.reset();
          oscTimer.start();
        } else {
          state = ready ? State.WAITING_AIM : State.SPINNING_UP;
        }
        break;

      case SHOOTING:
        feeder.start();
        roller.setShootAssist(pivot.isOpen());

        if (pivot.atSetpoint()
            || oscTimer.hasElapsed(ShootCommandConstants.OSCILLATION_MAX_PERIOD)) {
          pivotTargetOpen = !pivotTargetOpen;
          if (pivotTargetOpen) {
            pivot.open();
          } else {
            pivot.close();
          }
          oscTimer.reset();
        }
        break;

      default:
        break;
    }

    ballCounter.update(
        shooter.getCurrentRPM(), shooter.getTargetRpm(), state == State.SHOOTING);

    SmartDashboard.putString("Shoot/State", state.name());
    SmartDashboard.putBoolean("Shoot/PivotTargetOpen", pivotTargetOpen);
    SmartDashboard.putNumber("Shoot/Balls", ballCounter.getCount());
    SmartDashboard.putNumber("Shoot/BallGoal", ballGoal);
  }

  /** Applies the distance (or manual) setpoints only while this shot is allowed to arm. */
  private void applyShotProfile() {
    if (!armGate.getAsBoolean()) {
      return;
    }
    shooter.setTargetRpm(rpmSupplier.getAsDouble());
    hood.setTargetDegrees(hoodSupplier.getAsDouble());
    shooter.start();
  }

  @Override
  public void end(boolean interrupted) {
    feeder.stop();
    roller.stop();
    pivot.close();

    if (shooterWasRunning) {
      // was pre-spun with the B toggle: leave it running at the manual rpm
      shooter.setTargetRpm(shooter.getManualRpm());
      hood.setTargetDegrees(hood.getManualDegrees());
    } else {
      shooter.stop();
      hood.stow();
    }

    oscTimer.stop();
    oscTimer.reset();

    SmartDashboard.putString("Shoot/State", "IDLE");
  }

  @Override
  public boolean isFinished() {
    return ballGoal > 0 && ballCounter.getCount() >= ballGoal;
  }
}
