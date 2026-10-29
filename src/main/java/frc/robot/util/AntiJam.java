package frc.robot.util;

import java.util.ArrayDeque;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Timer;
import frc.robot.Constants.AntiJamConstants;

/**
 * Jam detector + reverse-pulse state machine for roller/feeder style motors.
 *
 * <p>Call {@link #calculate} once per loop with the commanded duty cycle and the motor's measured
 * current and velocity. It returns the duty cycle that should actually be sent to the motor:
 * the commanded value normally, a short reverse pulse after a detected jam, or 0 during the
 * cooldown that follows repeated jams.
 */
public class AntiJam {

  private enum State { NORMAL, REVERSING, COOLDOWN }

  private State state = State.NORMAL;
  private double stateEnd = 0.0;
  private double reverseSign = 0.0;

  private double activeSince = Double.NaN;
  private double jamSince = Double.NaN;

  private final ArrayDeque<Double> jamTimes = new ArrayDeque<>();
  private int totalJams = 0;
  private final DoubleSupplier clock;

  public AntiJam() {
    this(Timer::getFPGATimestamp);
  }

  /** Constructor with an injectable clock (seconds), used by unit tests. */
  public AntiJam(DoubleSupplier clock) {
    this.clock = clock;
  }

  public double calculate(double commanded, double currentAmps, double velocityRpm) {
    double now = clock.getAsDouble();

    if (Math.abs(commanded) < 0.05) {
      reset();
      return commanded;
    }

    if (Double.isNaN(activeSince)) {
      activeSince = now;
    }

    switch (state) {
      case REVERSING:
        if (now < stateEnd) {
          return reverseSign * AntiJamConstants.REVERSE_POWER;
        }
        resumeNormal(now);
        return commanded;

      case COOLDOWN:
        if (now < stateEnd) {
          return 0.0;
        }
        resumeNormal(now);
        return commanded;

      case NORMAL:
      default:
        if (now - activeSince < AntiJamConstants.STARTUP_GRACE_SECONDS) {
          jamSince = Double.NaN;
          return commanded;
        }

        boolean jamming = Math.abs(commanded) >= AntiJamConstants.MIN_COMMAND
            && currentAmps > AntiJamConstants.JAM_CURRENT_AMPS
            && Math.abs(velocityRpm) < AntiJamConstants.JAM_MAX_RPM;

        if (!jamming) {
          jamSince = Double.NaN;
          return commanded;
        }

        if (Double.isNaN(jamSince)) {
          jamSince = now;
        }
        if (now - jamSince < AntiJamConstants.JAM_DETECT_SECONDS) {
          return commanded;
        }

        // jam confirmed
        totalJams++;
        jamSince = Double.NaN;
        while (!jamTimes.isEmpty()
            && now - jamTimes.peekFirst() > AntiJamConstants.JAM_WINDOW_SECONDS) {
          jamTimes.pollFirst();
        }
        jamTimes.addLast(now);

        if (jamTimes.size() >= AntiJamConstants.MAX_JAMS_IN_WINDOW) {
          jamTimes.clear();
          state = State.COOLDOWN;
          stateEnd = now + AntiJamConstants.COOLDOWN_SECONDS;
          return 0.0;
        }

        reverseSign = -Math.signum(commanded);
        state = State.REVERSING;
        stateEnd = now + AntiJamConstants.REVERSE_SECONDS;
        return reverseSign * AntiJamConstants.REVERSE_POWER;
    }
  }

  /** True while a reverse pulse or cooldown is in progress. */
  public boolean isJamActive() {
    return state != State.NORMAL;
  }

  public int getTotalJams() {
    return totalJams;
  }

  private void resumeNormal(double now) {
    state = State.NORMAL;
    activeSince = now; // grace period again after the pulse
    jamSince = Double.NaN;
  }

  private void reset() {
    state = State.NORMAL;
    activeSince = Double.NaN;
    jamSince = Double.NaN;
    // a released command starts a fresh jam window, old jams must not count towards the cooldown
    jamTimes.clear();
  }
}
