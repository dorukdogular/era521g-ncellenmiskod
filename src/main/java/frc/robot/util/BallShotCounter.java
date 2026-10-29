package frc.robot.util;

import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.Timer;

/**
 * Counts balls from the flywheel RPM dip they cause. A ball is one dip below
 * {@code dipRpm} under the target followed by a recovery to within {@code recoverRpm}.
 * Samples taken while not feeding clear a half-finished dip and do not count it.
 */
public class BallShotCounter {

  private final double dipRpm;
  private final double recoverRpm;
  private final double minIntervalSeconds;
  private final DoubleSupplier clock;

  private int count;
  private boolean dipping;
  private double lastCountTime = Double.NEGATIVE_INFINITY;

  public BallShotCounter(double dipRpm, double recoverRpm, double minIntervalSeconds) {
    this(dipRpm, recoverRpm, minIntervalSeconds, Timer::getFPGATimestamp);
  }

  /** Constructor with an injectable clock (seconds), used by unit tests. */
  public BallShotCounter(double dipRpm,
                         double recoverRpm,
                         double minIntervalSeconds,
                         DoubleSupplier clock) {
    if (recoverRpm >= dipRpm) {
      throw new IllegalArgumentException("recoverRpm must be < dipRpm");
    }
    this.dipRpm = dipRpm;
    this.recoverRpm = recoverRpm;
    this.minIntervalSeconds = minIntervalSeconds;
    this.clock = clock;
  }

  /**
   * @param rpm measured flywheel speed
   * @param targetRpm active setpoint
   * @param feeding false while the feeder is stopped
   * @return the count after this sample
   */
  public int update(double rpm, double targetRpm, boolean feeding) {
    if (!feeding || Double.isNaN(rpm) || Double.isNaN(targetRpm)) {
      dipping = false;
      return count;
    }

    double deficit = targetRpm - rpm;
    double now = clock.getAsDouble();
    if (!dipping) {
      if (deficit >= dipRpm && now - lastCountTime >= minIntervalSeconds) {
        dipping = true;
      }
    } else if (deficit <= recoverRpm) {
      dipping = false;
      count++;
      lastCountTime = now;
    }
    return count;
  }

  public int getCount() {
    return count;
  }

  public void reset() {
    count = 0;
    dipping = false;
    lastCountTime = Double.NEGATIVE_INFINITY;
  }
}
