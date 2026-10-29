package frc.robot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BallShotCounterTest {

  private double now;
  private BallShotCounter counter;

  @BeforeEach
  void setUp() {
    now = 10.0;
    counter = new BallShotCounter(180.0, 80.0, 0.15, () -> now);
  }

  @Test
  void steadyRpmDoesNotCount() {
    assertEquals(0, counter.update(4000, 4000, true));
    now += 0.5;
    assertEquals(0, counter.update(3900, 4000, true));
  }

  @Test
  void dipThenRecoveryCountsOneBall() {
    counter.update(3700, 4000, true); // 300 under: dip starts
    now += 0.05;
    counter.update(3600, 4000, true);
    now += 0.05;
    assertEquals(1, counter.update(3950, 4000, true)); // recovered
    now += 0.05;
    assertEquals(1, counter.update(4000, 4000, true));
  }

  @Test
  void secondBallMustWaitForTheInterval() {
    counter.update(3700, 4000, true);
    now += 0.05;
    assertEquals(1, counter.update(4000, 4000, true));

    now += 0.05; // only 0.05 s since the count, interval is 0.15
    counter.update(3600, 4000, true);
    now += 0.05;
    assertEquals(1, counter.update(4000, 4000, true));

    now += 0.10;
    counter.update(3600, 4000, true);
    now += 0.05;
    assertEquals(2, counter.update(4000, 4000, true));
  }

  @Test
  void stoppingTheFeederCancelsAHalfFinishedDip() {
    counter.update(3600, 4000, true);
    now += 0.05;
    assertEquals(0, counter.update(4000, 4000, false));
    now += 0.20;
    // recovery while not feeding must not count, and the next real dip can
    assertEquals(0, counter.update(4000, 4000, true));
    counter.update(3600, 4000, true);
    now += 0.05;
    assertEquals(1, counter.update(4000, 4000, true));
  }

  @Test
  void eightBalls() {
    for (int i = 0; i < 8; i++) {
      counter.update(3600, 4000, true);
      now += 0.05;
      counter.update(4000, 4000, true);
      now += 0.2;
    }
    assertEquals(8, counter.getCount());
  }

  @Test
  void recoverMustBeBelowTheDip() {
    assertThrows(IllegalArgumentException.class,
        () -> new BallShotCounter(100, 100, 0.1, () -> 0.0));
  }
}
