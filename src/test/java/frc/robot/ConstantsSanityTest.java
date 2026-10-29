package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import frc.robot.Constants.AlignConstants;
import frc.robot.Constants.AntiJamConstants;
import frc.robot.Constants.CAN;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.FeederConstants;
import frc.robot.Constants.HoodConstants;
import frc.robot.Constants.IntakePivotConstants;
import frc.robot.Constants.IntakeRollerConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.Constants.VisionConstants;

/**
 * Guards against typos in Constants.java that could hurt a motor or the mechanism. These run with
 * `./gradlew build` / `./gradlew test`.
 */
class ConstantsSanityTest {

  @Test
  void neoCurrentLimitsAreInASafeRange() {
    // REV recommends 40-60 A for the NEO 1.1; the mechanisms here are kept at 40 A or less
    assertTrue(IntakeRollerConstants.CURRENT_LIMIT <= 40);
    assertTrue(FeederConstants.CURRENT_LIMIT <= 40);
    assertTrue(ShooterConstants.CURRENT_LIMIT <= 40);
    assertTrue(IntakePivotConstants.CURRENT_LIMIT <= 40);
    assertTrue(HoodConstants.CURRENT_LIMIT <= 40);
    assertTrue(IntakeRollerConstants.CURRENT_LIMIT >= 20);
    assertTrue(FeederConstants.CURRENT_LIMIT >= 20);
  }

  @Test
  void driveCurrentLimitsAreForCims() {
    assertTrue(DriveConstants.CURRENT_LIMIT_BRUSHED <= 40);
  }

  @Test
  void jamCurrentIsBelowTheCurrentLimit() {
    // otherwise a jammed motor could never reach the detection current
    assertTrue(AntiJamConstants.JAM_CURRENT_AMPS < IntakeRollerConstants.CURRENT_LIMIT);
    assertTrue(AntiJamConstants.JAM_CURRENT_AMPS < FeederConstants.CURRENT_LIMIT);
    assertTrue(AntiJamConstants.JAM_CURRENT_AMPS > 0.5 * FeederConstants.CURRENT_LIMIT);
    assertTrue(AntiJamConstants.REVERSE_POWER > 0 && AntiJamConstants.REVERSE_POWER <= 1.0);
    assertTrue(AntiJamConstants.REVERSE_SECONDS <= 0.5);
  }

  @Test
  void shooterVelocityGainsMatchTheNeo() {
    // kV (V/rpm) times the free speed must be the nominal voltage
    assertEquals(ShooterConstants.NOMINAL_VOLTAGE,
        ShooterConstants.KV * ShooterConstants.NEO_FREE_SPEED_RPM, 1e-9);
    assertTrue(ShooterConstants.MAX_TARGET_RPM <= 0.9 * ShooterConstants.NEO_FREE_SPEED_RPM);
    assertTrue(ShooterConstants.MIN_TARGET_RPM < ShooterConstants.MAX_TARGET_RPM);
    assertTrue(ShooterConstants.DEFAULT_TARGET_RPM >= ShooterConstants.MIN_TARGET_RPM);
    assertTrue(ShooterConstants.DEFAULT_TARGET_RPM <= ShooterConstants.MAX_TARGET_RPM);
    assertTrue(ShooterConstants.DEFAULT_KP >= 0 && ShooterConstants.DEFAULT_KP <= ShooterConstants.MAX_KP);
    assertTrue(ShooterConstants.READY_TOLERANCE_RPM < ShooterConstants.DROP_TOLERANCE_RPM);
    assertTrue(ShooterConstants.VELOCITY_MEASUREMENT_PERIOD_MS >= 8
        && ShooterConstants.VELOCITY_MEASUREMENT_PERIOD_MS <= 64);
    Set<Integer> validDepths = Set.of(1, 2, 4, 8);
    assertTrue(validDepths.contains(ShooterConstants.VELOCITY_AVERAGE_DEPTH));
  }

  @Test
  void pivotLimitsAreConsistent() {
    assertTrue(IntakePivotConstants.SOFT_LIMIT_FORWARD > IntakePivotConstants.DEFAULT_OPEN_DEGREES);
    assertTrue(IntakePivotConstants.SOFT_LIMIT_REVERSE < IntakePivotConstants.DEFAULT_CLOSED_DEGREES);
    assertTrue(IntakePivotConstants.DEFAULT_OPEN_DEGREES > IntakePivotConstants.DEFAULT_CLOSED_DEGREES);
    assertTrue(IntakePivotConstants.OUTPUT_MAX <= 1.0 && IntakePivotConstants.OUTPUT_MIN >= -1.0);
  }

  @Test
  void hubTagSetsAreCorrectAndDisjoint() {
    Set<Integer> red = new HashSet<>();
    for (int id : VisionConstants.RED_HUB_TAG_IDS) {
      red.add(id);
    }
    Set<Integer> blue = new HashSet<>();
    for (int id : VisionConstants.BLUE_HUB_TAG_IDS) {
      blue.add(id);
    }
    // the centre tags of each hub face (verified from WPILib's 2026-rebuilt-welded.json)
    assertEquals(Set.of(2, 4, 5, 10), red);
    assertEquals(Set.of(18, 20, 21, 26), blue);
    red.retainAll(blue);
    assertTrue(red.isEmpty());
    assertTrue(VisionConstants.RED_PIPELINE_INDEX != VisionConstants.BLUE_PIPELINE_INDEX);
    assertEquals(1.124, VisionConstants.TAG_HEIGHT_METERS, 1e-3);
  }

  @Test
  void rpmTableIsSafe() {
    double[][] table = VisionConstants.SHOT_BY_TY_TABLE;
    assertTrue(table.length >= 2);
    boolean rpmDecreasesAsTyIncreases = true;
    for (int i = 0; i < table.length; i++) {
      assertTrue(table[i][1] >= ShooterConstants.MIN_TARGET_RPM
          && table[i][1] <= ShooterConstants.MAX_TARGET_RPM,
          "rpm outside the safe range at row " + i);
      assertEquals(3, table[i].length, "row " + i + " must be {ty, rpm, hoodDegrees}");
      assertTrue(table[i][2] >= HoodConstants.MIN_DEGREES
          && table[i][2] <= HoodConstants.MAX_DEGREES,
          "hood angle outside the mechanical range at row " + i);
      if (i > 0) {
        assertTrue(table[i][0] != table[i - 1][0], "duplicate ty key at row " + i);
        // closer (larger ty) should never need MORE rpm than farther (smaller ty)
        boolean ok = (table[i][0] > table[i - 1][0]) == (table[i][1] <= table[i - 1][1]);
        rpmDecreasesAsTyIncreases &= ok;
      }
    }
    assertTrue(rpmDecreasesAsTyIncreases, "rpm should fall as ty rises (target closer)");
  }

  @Test
  void alignConstantsAreSane() {
    assertTrue(AlignConstants.TOLERANCE_DEGREES < AlignConstants.EXIT_TOLERANCE_DEGREES);
    assertTrue(AlignConstants.MAX_TURN_OUTPUT <= 0.6);
    assertTrue(AlignConstants.MIN_TURN_OUTPUT < AlignConstants.MAX_TURN_OUTPUT);
    assertTrue(AlignConstants.KP > 0 && AlignConstants.KP <= AlignConstants.MAX_KP);
    assertEquals(1.0, Math.abs(AlignConstants.TURN_SIGN), 0.0);
  }

  @Test
  void hoodConstantsAreConsistent() {
    assertTrue(HoodConstants.MIN_DEGREES < HoodConstants.MAX_DEGREES);
    assertTrue(HoodConstants.HOME_DEGREES >= HoodConstants.MIN_DEGREES
        && HoodConstants.HOME_DEGREES <= HoodConstants.MAX_DEGREES);
    assertTrue(HoodConstants.DEFAULT_DEGREES >= HoodConstants.MIN_DEGREES
        && HoodConstants.DEFAULT_DEGREES <= HoodConstants.MAX_DEGREES);
    assertTrue(HoodConstants.DEFAULT_KP >= 0 && HoodConstants.DEFAULT_KP <= HoodConstants.MAX_KP);
    assertTrue(HoodConstants.READY_TOLERANCE_DEGREES < HoodConstants.DROP_TOLERANCE_DEGREES);
    assertTrue(HoodConstants.RETARGET_DEADBAND_DEGREES < HoodConstants.READY_TOLERANCE_DEGREES);
    assertTrue(HoodConstants.GEAR_RATIO > 0);
    assertTrue(HoodConstants.OUTPUT_MAX <= 1.0 && HoodConstants.OUTPUT_MIN >= -1.0);
  }

  @Test
  void canIdsAreUnique() {
    Set<Integer> ids = new HashSet<>(Set.of(
        CAN.LEFT_FRONT_ID, CAN.LEFT_BACK_ID, CAN.RIGHT_FRONT_ID, CAN.RIGHT_BACK_ID,
        CAN.INTAKE_ROLLER_ID, CAN.INTAKE_PIVOT_ID, CAN.SHOOTER_ID, CAN.FEEDER_ID));
    assertTrue(ids.add(CAN.HOOD_ID), "HOOD_ID collides with another CAN id");
  }

  @Test
  void alignFadeSettingsAreSane() {
    assertTrue(AlignConstants.FADE_DEGREES > AlignConstants.TOLERANCE_DEGREES);
    assertTrue(AlignConstants.MIN_TURN_FRACTION > 0 && AlignConstants.MIN_TURN_FRACTION <= 1.0);
  }
}
