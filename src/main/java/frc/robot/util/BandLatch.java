package frc.robot.util;

import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Timer;

/**
 * "Settled" detector with hysteresis. The latch turns on only after the absolute error has stayed
 * inside {@code enterTolerance} for {@code debounceSeconds}, and turns off again only when the
 * error leaves the wider {@code exitTolerance} (or the latch is disabled). Used for "shooter at
 * speed" and "robot aligned", so a short dip or jitter does not make the state flicker.
 */
public class BandLatch {

  private final double enterTolerance;
  private final double exitTolerance;
  private final double debounceSeconds;
  private final DoubleSupplier clock;

  private boolean latched = false;
  private double enterSince = Double.NaN;

  public BandLatch(double enterTolerance, double exitTolerance, double debounceSeconds) {
    this(enterTolerance, exitTolerance, debounceSeconds, Timer::getFPGATimestamp);
  }

  /** Constructor with an injectable clock (seconds), used by unit tests. */
  public BandLatch(double enterTolerance,
                   double exitTolerance,
                   double debounceSeconds,
                   DoubleSupplier clock) {
    if (exitTolerance < enterTolerance) {
      throw new IllegalArgumentException("exitTolerance must be >= enterTolerance");
    }
    this.enterTolerance = enterTolerance;
    this.exitTolerance = exitTolerance;
    this.debounceSeconds = debounceSeconds;
    this.clock = clock;
  }

  /**
   * @param error signed or absolute error (the sign is ignored)
   * @param enabled false forces the latch off and restarts the debounce
   * @return the latch state after this update
   */
  public boolean update(double error, boolean enabled) {
    double absError = Math.abs(error);

    if (!enabled || Double.isNaN(absError)) {
      reset();
      return false;
    }

    if (latched) {
      latched = absError <= exitTolerance;
      if (!latched) {
        // dropped out: the debounce has to start from scratch
        enterSince = Double.NaN;
      }
      return latched;
    }

    if (absError <= enterTolerance) {
      double now = clock.getAsDouble();
      if (Double.isNaN(enterSince)) {
        enterSince = now;
      }
      if (now - enterSince >= debounceSeconds) {
        latched = true;
      }
    } else {
      enterSince = Double.NaN;
    }
    return latched;
  }

  public boolean get() {
    return latched;
  }

  public void reset() {
    latched = false;
    enterSince = Double.NaN;
  }
}
