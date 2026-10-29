package frc.robot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import frc.robot.Constants.AntiJamConstants;

class AntiJamTest {

  private static final double DT = 0.02;
  private static final double COMMAND = -1.0;
  private static final double STALL_AMPS = 38.0;
  private static final double RUN_AMPS = 20.0;

  private double now;
  private AntiJam antiJam;

  @BeforeEach
  void setUp() {
    now = 100.0;
    antiJam = new AntiJam(() -> now);
  }

  /** Runs for the given time and returns the last output. */
  private double run(double seconds, double command, double amps, double rpm) {
    double out = command;
    int steps = (int) Math.round(seconds / DT);
    for (int i = 0; i < steps; i++) {
      out = antiJam.calculate(command, amps, rpm);
      now += DT;
    }
    return out;
  }

  @Test
  void passesCommandThroughWhenRunningNormally() {
    assertEquals(COMMAND, run(3.0, COMMAND, RUN_AMPS, 3000), 1e-9);
    assertEquals(0, antiJam.getTotalJams());
  }

  @Test
  void heavyLoadWhileStillSpinningIsNotAJam() {
    // high current but the roller is still turning well above the jam rpm
    assertEquals(COMMAND, run(5.0, COMMAND, STALL_AMPS, 1500), 1e-9);
    assertEquals(0, antiJam.getTotalJams());
  }

  @Test
  void startupStallInsideGracePeriodIsIgnored() {
    double grace = AntiJamConstants.STARTUP_GRACE_SECONDS;
    assertEquals(COMMAND, run(grace - 0.1, COMMAND, STALL_AMPS, 0), 1e-9);
    assertEquals(0, antiJam.getTotalJams());
  }

  @Test
  void jamTriggersReversePulseThenResumes() {
    double beforeJam = AntiJamConstants.STARTUP_GRACE_SECONDS + AntiJamConstants.JAM_DETECT_SECONDS;
    // run until just before the detection completes: still forward
    assertEquals(COMMAND, run(beforeJam - 0.1, COMMAND, STALL_AMPS, 0), 1e-9);
    // push over the detection time: reverse (opposite sign of the command)
    double out = run(0.2, COMMAND, STALL_AMPS, 0);
    assertEquals(1, antiJam.getTotalJams());
    assertTrue(antiJam.isJamActive());
    assertEquals(AntiJamConstants.REVERSE_POWER, out, 1e-9);

    // after the reverse time the original command comes back, and the jam is cleared
    double afterPulse = run(AntiJamConstants.REVERSE_SECONDS + 0.05, COMMAND, RUN_AMPS, 2000);
    assertEquals(COMMAND, afterPulse, 1e-9);
    assertFalse(antiJam.isJamActive());
  }

  @Test
  void reverseDirectionFollowsCommandSign() {
    double total = AntiJamConstants.STARTUP_GRACE_SECONDS + AntiJamConstants.JAM_DETECT_SECONDS + 0.05;
    double out = run(total, 1.0, STALL_AMPS, 0);
    assertEquals(-AntiJamConstants.REVERSE_POWER, out, 1e-9);
  }

  @Test
  void repeatedJamsTriggerCooldownWithZeroOutput() {
    // continuous stall: jam #1, #2, #3 happen back to back, the 3rd one is a cooldown
    boolean sawCooldown = false;
    for (int i = 0; i < (int) (4.0 / DT); i++) {
      double out = antiJam.calculate(COMMAND, STALL_AMPS, 0);
      now += DT;
      if (antiJam.getTotalJams() >= AntiJamConstants.MAX_JAMS_IN_WINDOW && out == 0.0) {
        sawCooldown = true;
        break;
      }
    }
    assertTrue(sawCooldown, "expected a zero-output cooldown after repeated jams");
  }

  @Test
  void zeroCommandResetsEverything() {
    run(AntiJamConstants.STARTUP_GRACE_SECONDS + 0.1, COMMAND, STALL_AMPS, 0);
    assertEquals(0.0, run(0.1, 0.0, 0.0, 0), 1e-9);
    // after being stopped, a new start gets a fresh grace period again
    assertEquals(COMMAND,
        run(AntiJamConstants.STARTUP_GRACE_SECONDS - 0.1, COMMAND, STALL_AMPS, 0), 1e-9);
    assertEquals(0, antiJam.getTotalJams());
  }

  @Test
  void smallCommandBelowMinCommandNeverCountsAsJam() {
    double small = -(AntiJamConstants.MIN_COMMAND - 0.1);
    assertEquals(small, run(3.0, small, STALL_AMPS, 0), 1e-9);
    assertEquals(0, antiJam.getTotalJams());
  }

  @Test
  void releasingTheCommandForgetsEarlierJams() {
    // two jams back to back, then the trigger is released for a moment
    while (antiJam.getTotalJams() < 2) {
      antiJam.calculate(COMMAND, STALL_AMPS, 0);
      now += DT;
    }
    run(0.1, 0.0, 0.0, 0);

    // third jam inside the old 3 s window must be a normal reverse pulse, not a cooldown
    double out = COMMAND;
    while (antiJam.getTotalJams() < 3) {
      out = antiJam.calculate(COMMAND, STALL_AMPS, 0);
      now += DT;
    }
    assertEquals(AntiJamConstants.REVERSE_POWER, out, 1e-9);
  }
}
