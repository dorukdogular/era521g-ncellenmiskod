package frc.robot.subsystems;

import java.util.Optional;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.VisionConstants;
import frc.robot.LimelightHelpers;

/**
 * Limelight wrapper. The camera has one AprilTag pipeline per alliance; the pipeline's "ID Filters"
 * (set in the Limelight web UI) limit tracking to the centre tags of that alliance's HUB. This
 * class selects the pipeline from the alliance and additionally checks the tag id in software.
 * Data from a stale or wrong pipeline is never used. tx/ty are only for aiming and range.
 */
public class VisionSubsystem extends SubsystemBase {

  private enum AllianceChoice { AUTO, RED, BLUE }

  // minimum time between pipeline switch requests
  private static final double PIPELINE_RETRY_SECONDS = 0.25;

  private static final String NAME = VisionConstants.LIMELIGHT_NAME;

  private final SendableChooser<AllianceChoice> allianceChooser = new SendableChooser<>();

  private Optional<Alliance> lastAlliance = Optional.empty();
  private double lastPipelineRequest = Double.NEGATIVE_INFINITY;

  private double lastHeartbeat = Double.NaN;
  private double lastHeartbeatChange = Double.NEGATIVE_INFINITY;
  private boolean heartbeatEverChanged = false;

  private boolean hasTarget = false;
  private int tagId = -1;
  private double txDegrees = 0.0;
  private double tyDegrees = 0.0;
  private double distanceMeters = Double.NaN;
  private double aimOffsetDegrees = VisionConstants.AIM_OFFSET_DEGREES;

  public VisionSubsystem() {
    allianceChooser.setDefaultOption("AUTO (driver station)", AllianceChoice.AUTO);
    allianceChooser.addOption("RED", AllianceChoice.RED);
    allianceChooser.addOption("BLUE", AllianceChoice.BLUE);
    SmartDashboard.putData("Vision/AllianceOverride", allianceChooser);
    SmartDashboard.setDefaultNumber("Vision/AimOffsetDeg", aimOffsetDegrees);

    // tags are matte prints, the leds only add glare; the pipeline's leds are off anyway
    LimelightHelpers.setLEDMode_ForceOff(NAME);
  }

  /** True when a fresh frame shows one of our own HUB centre tags. */
  public boolean hasTarget() {
    return hasTarget;
  }

  /**
   * Horizontal error to the aim point in degrees (tx minus the aim offset). Positive = the aim
   * point is to the right of where the shooter points, so the robot must rotate clockwise.
   */
  public double getTxDegrees() {
    return txDegrees;
  }

  /** Vertical angle to the tag in degrees. Larger (more positive) means closer. */
  public double getTyDegrees() {
    return tyDegrees;
  }

  /** Informational camera-to-tag floor distance in meters (NaN if invalid). Not used for driving. */
  public double getDistanceMeters() {
    return distanceMeters;
  }

  private Optional<Alliance> resolveAlliance() {
    switch (allianceChooser.getSelected()) {
      case RED:
        return Optional.of(Alliance.Red);
      case BLUE:
        return Optional.of(Alliance.Blue);
      case AUTO:
      default:
        Optional<Alliance> fromDs = DriverStation.getAlliance();
        if (fromDs.isPresent()) {
          lastAlliance = fromDs;
        }
        return lastAlliance;
    }
  }

  private static boolean contains(int[] ids, int id) {
    for (int candidate : ids) {
      if (candidate == id) {
        return true;
      }
    }
    return false;
  }

  private double computeDistance(double tyDegrees) {
    double angleRad = Math.toRadians(VisionConstants.CAMERA_PITCH_DEGREES + tyDegrees);
    double heightDiff = VisionConstants.TAG_HEIGHT_METERS - VisionConstants.CAMERA_HEIGHT_METERS;
    if (angleRad <= 0.05 || heightDiff <= 0.0) {
      return Double.NaN;
    }
    double d = heightDiff / Math.tan(angleRad);
    if (d < VisionConstants.MIN_VALID_DISTANCE_METERS
        || d > VisionConstants.MAX_VALID_DISTANCE_METERS) {
      return Double.NaN;
    }
    return d;
  }

  @Override
  public void periodic() {
    double now = Timer.getFPGATimestamp();
    Optional<Alliance> alliance = resolveAlliance();

    int desiredPipeline = -1;
    int[] hubIds = new int[0];
    if (alliance.isPresent()) {
      boolean red = alliance.get() == Alliance.Red;
      desiredPipeline = red
          ? VisionConstants.RED_PIPELINE_INDEX : VisionConstants.BLUE_PIPELINE_INDEX;
      hubIds = red ? VisionConstants.RED_HUB_TAG_IDS : VisionConstants.BLUE_HUB_TAG_IDS;
    }

    int activePipeline = (int) Math.round(LimelightHelpers.getCurrentPipelineIndex(NAME));
    boolean pipelineOk = desiredPipeline >= 0 && activePipeline == desiredPipeline;
    if (desiredPipeline >= 0 && !pipelineOk
        && now - lastPipelineRequest >= PIPELINE_RETRY_SECONDS) {
      LimelightHelpers.setPipelineIndex(NAME, desiredPipeline);
      lastPipelineRequest = now;
    }

    // heartbeat: increments every frame. if it was ticking and then stops, the data is stale.
    // if the key never changes (older firmware) the staleness check is skipped.
    double heartbeat = LimelightHelpers.getHeartbeat(NAME);
    if (Double.isNaN(lastHeartbeat)) {
      lastHeartbeat = heartbeat;
      lastHeartbeatChange = now;
    } else if (heartbeat != lastHeartbeat) {
      lastHeartbeat = heartbeat;
      lastHeartbeatChange = now;
      heartbeatEverChanged = true;
    }
    boolean fresh = !heartbeatEverChanged
        || now - lastHeartbeatChange <= VisionConstants.STALE_DATA_SECONDS;

    if (Constants.TUNING_ENABLED) {
      aimOffsetDegrees = MathUtil.clamp(
          SmartDashboard.getNumber("Vision/AimOffsetDeg", aimOffsetDegrees), -15.0, 15.0);
    }

    boolean tv = LimelightHelpers.getTV(NAME);
    int rawTagId = tv ? (int) LimelightHelpers.getFiducialID(NAME) : -1;
    hasTarget = pipelineOk && fresh && tv && contains(hubIds, rawTagId);
    tagId = rawTagId;

    if (hasTarget) {
      txDegrees = LimelightHelpers.getTX(NAME) - aimOffsetDegrees;
      tyDegrees = LimelightHelpers.getTY(NAME);
      distanceMeters = computeDistance(tyDegrees);
    } else {
      txDegrees = 0.0;
      tyDegrees = 0.0;
      distanceMeters = Double.NaN;
    }

    SmartDashboard.putString("Vision/Alliance", alliance.map(Enum::name).orElse("UNKNOWN"));
    SmartDashboard.putBoolean("Vision/HasTarget", hasTarget);
    SmartDashboard.putNumber("Vision/RawTagId", tagId);
    SmartDashboard.putBoolean("Vision/PipelineOk", pipelineOk);
    SmartDashboard.putNumber("Vision/ActivePipeline", activePipeline);
    SmartDashboard.putBoolean("Vision/DataFresh", fresh);
    SmartDashboard.putNumber("Vision/Tx", txDegrees);
    SmartDashboard.putNumber("Vision/Ty", tyDegrees);
    SmartDashboard.putNumber("Vision/Distance", Double.isNaN(distanceMeters) ? -1.0 : distanceMeters);
    SmartDashboard.putNumber("Vision/LatencyMs",
        LimelightHelpers.getLatency_Pipeline(NAME) + LimelightHelpers.getLatency_Capture(NAME));
  }
}
