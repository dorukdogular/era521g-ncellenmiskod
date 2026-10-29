package frc.robot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DriveCommandTest {

  @Test
  void forwardStickPushIsPositiveArcadeSpeed() {
    // Xbox left Y is negative when the stick is pushed forward.
    assertEquals(1.0, DriveCommand.forwardFromStick(-1.0), 1e-9);
    assertEquals(-1.0, DriveCommand.forwardFromStick(1.0), 1e-9);
  }

  @Test
  void rightStickPushIsClockwiseArcadeRotation() {
    // WPILib 2026 arcade rotation is counterclockwise-positive, so a right
    // push (positive X) has to come out negative.
    assertTrue(DriveCommand.turnFromStick(1.0) < 0.0);
    assertEquals(-0.4, DriveCommand.turnFromStick(0.4), 1e-9);
  }
}
