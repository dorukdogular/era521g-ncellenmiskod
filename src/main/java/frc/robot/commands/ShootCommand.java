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

/**
 * Hold-to-shoot command. Spins the shooter up to the target RPM, moves the hood to the target
 * angle and, once the shooter and hood are ready (and the optional aim gate is true), runs the feeder, oscillates the intake pivot between its
 * OPEN and CLOSED setpoints, and uses the intake roller to assist the feed. If the shooter or the
 * gate drops out the feeder stops until both are good again. On release, the feeder, roller and
 * pivot stop/close and the hood goes home; the shooter stops too unless it was already running
 * before the command (then the hood goes back to the manual angle).
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
  private final BooleanSupplier aimGate;

  private final Timer oscTimer = new Timer();
  private State state = State.SPINNING_UP;
  private boolean pivotTargetOpen = false;
  private boolean shooterWasRunning = false;

  /** Manual shot: dashboard/default RPM and hood angle, no aim gate. */
  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      HoodSubsystem hood,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller) {
    this(shooter, feeder, hood, pivot, roller,
        shooter::getManualRpm, hood::getManualDegrees, () -> true);
  }

  public ShootCommand(ShooterSubsystem shooter,
                      FeederSubsystem feeder,
                      HoodSubsystem hood,
                      IntakePivotSubsystem pivot,
                      IntakeRollerSubsystem roller,
                      DoubleSupplier rpmSupplier,
                      DoubleSupplier hoodSupplier,
                      BooleanSupplier aimGate) {
    this.shooter = shooter;
    this.feeder = feeder;
    this.hood = hood;
    this.pivot = pivot;
    this.roller = roller;
    this.rpmSupplier = rpmSupplier;
    this.hoodSupplier = hoodSupplier;
    this.aimGate = aimGate;
    addRequirements(shooter, feeder, hood, pivot, roller);
  }

  @Override
  public void initialize() {
    state = State.SPINNING_UP;
    pivotTargetOpen = false;
    shooterWasRunning = shooter.isRunning();

    shooter.setTargetRpm(rpmSupplier.getAsDouble());
    shooter.start();
    hood.setTargetDegrees(hoodSupplier.getAsDouble());
    feeder.stop();
    roller.stop();
    pivot.close();

    oscTimer.reset();
    oscTimer.stop();
  }

  @Override
  public void execute() {
    shooter.setTargetRpm(rpmSupplier.getAsDouble());
    shooter.start();
    hood.setTargetDegrees(hoodSupplier.getAsDouble());

    boolean ready = shooter.isReadyToFeed() && hood.isReady();
    boolean aimed = aimGate.getAsBoolean();

    if (state == State.SHOOTING && !(ready && aimed)) {
      // lost speed, hood angle or aim: stop feeding until both are good again
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
        if (ready && aimed) {
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
    return false;
  }
}
