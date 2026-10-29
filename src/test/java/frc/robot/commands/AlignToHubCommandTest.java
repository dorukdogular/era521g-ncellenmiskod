package frc.robot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.wpi.first.math.controller.PIDController;

import frc.robot.Constants.AlignConstants;

class AlignToHubCommandTest {

  private static final double MIN = AlignConstants.MIN_TURN_OUTPUT;
  private static final double MAX = AlignConstants.MAX_TURN_OUTPUT;

  @Test
  void outputNeverExceedsMaxEvenWhenMinIsLargerThanMax() {
    // dashboard misconfiguration: Align/MinTurn > Align/MaxTurn. the cap must still win
    for (double tx = -20; tx <= 20; tx += 0.5) {
      double out = AlignToHubCommand.shapeTurn(-0.01 * tx, tx, 0.2, 0.1);
      assertTrue(Math.abs(out) <= 0.1 + 1e-9, "tx=" + tx + " out=" + out);
    }
  }

  @Test
  void signFollowsThePidOutput() {
    assertTrue(AlignToHubCommand.shapeTurn(-0.02, 4.0, MIN, MAX) < 0);
    assertTrue(AlignToHubCommand.shapeTurn(0.02, -4.0, MIN, MAX) > 0);
  }

  @Test
  void floorIsFullFarAndFadesNearTheToleranceEdge() {
    double far = AlignToHubCommand.shapeTurn(0.001, AlignConstants.FADE_DEGREES + 1, MIN, MAX);
    assertEquals(MIN, far, 1e-9);

    double edge = AlignToHubCommand.shapeTurn(0.001, AlignConstants.TOLERANCE_DEGREES, MIN, MAX);
    assertEquals(MIN * AlignConstants.MIN_TURN_FRACTION, edge, 1e-9);
  }

  @Test
  void outputGrowsMonotonicallyWithErrorWithoutJumps() {
    double previous = AlignToHubCommand.shapeTurn(
        0.01 * AlignConstants.TOLERANCE_DEGREES, AlignConstants.TOLERANCE_DEGREES, MIN, MAX);
    for (double tx = AlignConstants.TOLERANCE_DEGREES + 0.1; tx <= 15.0; tx += 0.1) {
      double out = AlignToHubCommand.shapeTurn(0.01 * tx, tx, MIN, MAX);
      assertTrue(out >= previous - 1e-9, "output dropped at tx=" + tx);
      assertTrue(out - previous < 0.02, "jump at tx=" + tx);
      previous = out;
    }
  }

  @Test
  void targetOnTheRightCommandsClockwiseRotation() {
    PIDController pid = new PIDController(0.01, 0.0, 0.0);
    assertTrue(AlignToHubCommand.aimRotation(10.0, 1.0, pid) < 0.0);
    pid.reset();
    assertTrue(AlignToHubCommand.aimRotation(-10.0, 1.0, pid) > 0.0);
  }

  @Test
  void largePidOutputPassesThroughUntilTheCap() {
    assertEquals(0.2, AlignToHubCommand.shapeTurn(0.2, 20.0, MIN, MAX), 1e-9);
    assertEquals(MAX, AlignToHubCommand.shapeTurn(0.9, 30.0, MIN, MAX), 1e-9);
  }
}
