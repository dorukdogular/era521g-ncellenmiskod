package frc.robot.commands;

import java.util.function.BooleanSupplier;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.ShootCommandConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.subsystems.FeederSubsystem;
import frc.robot.subsystems.IntakePivotSubsystem;
import frc.robot.subsystems.IntakeRollerSubsystem;
import frc.robot.subsystems.ShooterSubsystem;

/**
 * Hold-to-shoot command. Spins the flywheel at a fixed RPM. Feeding starts only after the shooter
 * has settled and {@code feedGate} is true (alignment and shot distance).
 *
 * <p>On release, the feeder, roller and pivot stop. The shooter stops too unless it was already
 * running before the command. The command runs until it is cancelled.
 */
public class ShootCommand extends Command {

  private enum State { SPINNING_UP, WAITING_AIM, SHOOTING }

  private final ShooterSubsystem shooter;
  private final FeederSubsystem feeder;
  private final IntakePivotSubsystem pivot;
  private final IntakeRollerSubsystem roller;
  private final BooleanSupplier feedGate;

  private final Timer oscTimer = new Timer();

  private State state = State.SPINNING_UP;
  private boolean pivotTargetOpen = false;
  private boolean shooterWasRunning = false;

  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller,
                      BooleanSupplier feedGate) {
    this.shooter = shooter;
    this.feeder = feeder;
    this.pivot = pivot;
    this.roller = roller;
    this.feedGate = feedGate;
    addRequirements(shooter, feeder, pivot, roller);
  }

  @Override
  public void initialize() {
    state = State.SPINNING_UP;
    pivotTargetOpen = false;
    shooterWasRunning = shooter.isRunning();

    shooter.setTargetRpm(ShooterConstants.DEFAULT_TARGET_RPM);
    shooter.start();
    feeder.stop();
    roller.stop();
    pivot.close();

    oscTimer.reset();
    oscTimer.stop();
  }

  @Override
  public void execute() {
    shooter.setTargetRpm(ShooterConstants.DEFAULT_TARGET_RPM);
    shooter.start();

    boolean ready = shooter.isReadyToFeed();
    boolean feed = feedGate.getAsBoolean();

    if (state == State.SHOOTING && !(ready && feed)) {
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

    SmartDashboard.putString("Shoot/State", state.name());
    SmartDashboard.putBoolean("Shoot/PivotTargetOpen", pivotTargetOpen);
  }

  @Override
  public void end(boolean interrupted) {
    feeder.stop();
    roller.stop();
    pivot.close();

    if (shooterWasRunning) {
      shooter.setTargetRpm(ShooterConstants.DEFAULT_TARGET_RPM);
    } else {
      shooter.stop();
    }

    oscTimer.stop();
    oscTimer.reset();

    SmartDashboard.putString("Shoot/State", "IDLE");
  }
}
