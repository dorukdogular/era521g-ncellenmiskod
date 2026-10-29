package frc.robot.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BandLatchTest {

  private double now;
  // shooter-like numbers: enter +-150, exit +-350, 0.12 s debounce
  private BandLatch latch;

  @BeforeEach
  void setUp() {
    now = 0.0;
    latch = new BandLatch(150.0, 350.0, 0.12, () -> now);
  }

  private boolean step(double error, boolean enabled, double dt) {
    now += dt;
    return latch.update(error, enabled);
  }

  @Test
  void notReadyUntilDebounceElapsed() {
    assertFalse(step(10, true, 0.0));   // first sample starts the timer
    assertFalse(step(10, true, 0.06));
    assertFalse(step(10, true, 0.05));  // 0.11 s
    assertTrue(step(10, true, 0.02));   // 0.13 s
  }

  @Test
  void leavingTheBandBeforeDebounceRestartsTheTimer() {
    step(10, true, 0.0);
    step(10, true, 0.10);
    assertFalse(step(200, true, 0.01)); // outside the enter band: restart
    assertFalse(step(10, true, 0.10));  // only 0.0 s inside again
    assertFalse(step(10, true, 0.10));
    assertTrue(step(10, true, 0.05));
  }

  @Test
  void staysLatchedThroughSmallDipAndDropsOnlyOutsideExitBand() {
    step(0, true, 0.0);
    assertTrue(step(0, true, 0.2));
    assertTrue(step(-300, true, 0.02));  // ball-exit dip: still ready
    assertTrue(step(340, true, 0.02));   // overshoot inside exit band: still ready
    assertFalse(step(-400, true, 0.02)); // real drop
    assertFalse(step(0, true, 0.02));    // needs the debounce again
    assertTrue(step(0, true, 0.2));
  }

  @Test
  void signOfErrorIsIgnored() {
    step(-100, true, 0.0);
    assertTrue(step(-100, true, 0.2));
  }

  @Test
  void disablingResetsTheLatch() {
    step(0, true, 0.0);
    assertTrue(step(0, true, 0.2));
    assertFalse(step(0, false, 0.02));
    assertFalse(step(0, true, 0.02)); // debounce restarts
    assertTrue(step(0, true, 0.2));
  }

  @Test
  void nanErrorNeverLatches() {
    step(Double.NaN, true, 0.0);
    assertFalse(step(Double.NaN, true, 1.0));
  }

  @Test
  void exitToleranceMustNotBeSmallerThanEnter() {
    assertThrows(IllegalArgumentException.class, () -> new BandLatch(5.0, 1.0, 0.1, () -> 0.0));
  }
}
