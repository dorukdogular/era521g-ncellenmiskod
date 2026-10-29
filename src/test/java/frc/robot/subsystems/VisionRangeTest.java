package frc.robot.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisionRangeTest {

  @Test
  void inRangeTargetIsAShot() {
    assertTrue(VisionSubsystem.inRangeShot(true, true, true, false));
  }

  @Test
  void visibleTargetOutsideTheTableIsNotAShot() {
    assertFalse(VisionSubsystem.inRangeShot(true, true, false, true));
    assertTrue(VisionSubsystem.outOfRange(true, true, false));
  }

  @Test
  void lostTargetHoldsTheLastGoodShotOnlyWhileFresh() {
    assertTrue(VisionSubsystem.inRangeShot(true, false, false, true));
    assertFalse(VisionSubsystem.inRangeShot(true, false, false, false));
    assertFalse(VisionSubsystem.outOfRange(true, false, false));
  }

  @Test
  void distanceModeOffNeverReportsATableShot() {
    assertFalse(VisionSubsystem.inRangeShot(false, true, true, true));
    assertFalse(VisionSubsystem.outOfRange(false, true, false));
  }

  @Test
  void slewSnapsFromNaNAndLimitsASpike() {
    assertEquals(4.0, VisionSubsystem.slewToward(Double.NaN, 4.0, 0.5), 1e-9);
    assertEquals(1.5, VisionSubsystem.slewToward(1.0, 8.0, 0.5), 1e-9);
    assertEquals(0.5, VisionSubsystem.slewToward(1.0, -4.0, 0.5), 1e-9);
    assertEquals(1.2, VisionSubsystem.slewToward(1.0, 1.2, 0.5), 1e-9);
  }
}
