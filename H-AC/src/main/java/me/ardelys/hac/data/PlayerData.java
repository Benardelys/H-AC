package me.ardelys.hac.data;

import me.ardelys.hac.checks.CheckType;
import me.ardelys.hac.checks.DetectionConfidence;
import me.ardelys.hac.client.injector.InjectorDetectionResult;
import me.ardelys.hac.utils.MathUtil;
import me.ardelys.hac.utils.PlayerUtil;
import me.ardelys.hac.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final UUID uuid;
    private final Map<String, Double> violations = new ConcurrentHashMap<>();

    private Location lastLocation;
    private Location currentLocation;
    private double deltaX;
    private double deltaY;
    private double deltaZ;
    private double deltaXZ;
    private double lastDeltaX;
    private double lastDeltaY;
    private double lastDeltaZ;
    private double lastDeltaXZ;
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float deltaYaw;
    private float deltaPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;

    private boolean onGround;
    private boolean lastOnGround;
    private boolean mathematicallyOnGround;
    private int groundTicks;
    private int airTicks;
    private int serverAirTicks;

    private Vector expectedVelocity = new Vector(0, 0, 0);
    private int velocityTicks = 100;
    private boolean velocityHandled = true;

    private int teleportGraceTicks = 100;
    private int respawnGraceTicks = 100;
    private int joinGraceTicks = 100;
    private int vehicleGraceTicks = 100;
    private int riptideGraceTicks = 100;
    private int elytraBoostGraceTicks = 100;
    private int pistonTicks = 0;
    private int inventoryOpenTicks = 0;
    private long lastInventoryOpenTime = 0;

    private boolean onIce = false;
    private boolean onSlime = false;
    private boolean onHoney = false;
    private boolean onClimbable = false;
    private boolean inWeb = false;
    private boolean inLiquid = false;
    private boolean onLiquid = false;
    private boolean onSoul = false;
    private boolean onPiston = false;

    private int iceTicks = 0;
    private int slimeTicks = 0;
    private int honeyTicks = 0;
    private int climbableTicks = 0;
    private int webTicks = 0;
    private int liquidTicks = 0;

    private UUID lastTargetUuid;
    private int lastTargetEntityId;
    private long lastAttackTime = 0;
    private long lastSwingTime = 0;
    private double lastReach = 0.0;
    private float preAttackYaw = 0.0f;
    private float preAttackPitch = 0.0f;
    private long preAttackTime = 0;

    private long lastTargetCrosshairTime = 0;
    private int triggerBotBuffer = 0;

    private final Set<String> registeredChannels = ConcurrentHashMap.newKeySet();
    private String detectedClientName;
    private DetectionConfidence clientDetectionConfidence;
    private double clientConfidenceScore = 0.0;
    private final List<String> clientSignals = new java.util.concurrent.CopyOnWriteArrayList<>();
    private InjectorDetectionResult injectorResult;

    private final Object clickLock = new Object();
    private long lastClickTimestamp = 0;
    private final long[] recentClicks = new long[64];
    private int clickHead = 0;
    private int clickTotal = 0;
    private final long[] clickDelays = new long[32];
    private int clickDelayHead = 0;
    private int clickDelayCount = 0;

    private long lastMovePacketTime = TimeUtil.now();
    private long lastMovePacketDelta = 50;
    private long lastBlockPlaceTime = 0;
    private int fastPlaceBuffer = 0;
    private long lastBlockBreakTime = 0;
    private long lastBowShootTime = 0;
    private long bowDrawStartTime = 0;
    private long lastItemConsumeTime = 0;
    private long consumeStartTime = 0;
    private long lastProjectileTime = 0;
    private double timerBalance = 0.0;
    private long lastTimerCheckTime = TimeUtil.now();

    private double speedBuffer = 0.0;
    private double airDistance = 0.0;
    private int pitchLockTicks = 0;
    private int linearAimTicks = 0;
    private int consecutiveSprintSneakTicks = 0;
    private int consecutiveLowHungerSprintTicks = 0;

    private String clientBrand = "vanilla";
    private boolean suspiciousBrand = false;
    private final Map<CheckType, Long> recentCategoryViolations = new ConcurrentHashMap<>();

    private boolean debugWatched = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getPing() {
        Player player = getPlayer();
        return PlayerUtil.getPing(player);
    }

    public void processMove(Location from, Location to) {
        if (from == null || to == null) return;
        boolean posChanged = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        boolean rotChanged = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
        processMove(from, to, posChanged, rotChanged);
    }

    @SuppressWarnings("deprecation")
    public void processMove(Location from, Location to, boolean positionChanged, boolean rotationChanged) {
        if (from == null || to == null) {
            return;
        }

        long now = TimeUtil.now();
        if (this.lastMovePacketTime > 0) {
            this.lastMovePacketDelta = Math.max(1, now - this.lastMovePacketTime);
        } else {
            this.lastMovePacketDelta = 50;
        }
        this.lastMovePacketTime = now;

        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            this.lastLocation = null;
            this.currentLocation = to.clone();
            this.deltaX = 0.0;
            this.deltaY = 0.0;
            this.deltaZ = 0.0;
            this.deltaXZ = 0.0;
            this.lastDeltaX = 0.0;
            this.lastDeltaY = 0.0;
            this.lastDeltaZ = 0.0;
            this.lastDeltaXZ = 0.0;
            this.serverAirTicks = 0;
            markTeleport();
            return;
        }

        this.lastLocation = from;
        this.currentLocation = to;

        if (positionChanged) {
            this.lastDeltaX = this.deltaX;
            this.lastDeltaY = this.deltaY;
            this.lastDeltaZ = this.deltaZ;
            this.lastDeltaXZ = this.deltaXZ;

            this.deltaX = to.getX() - from.getX();
            this.deltaY = to.getY() - from.getY();
            this.deltaZ = to.getZ() - from.getZ();
            this.deltaXZ = MathUtil.hypot(deltaX, deltaZ);
        } else {
            this.lastDeltaX = this.deltaX;
            this.lastDeltaY = this.deltaY;
            this.lastDeltaZ = this.deltaZ;
            this.lastDeltaXZ = this.deltaXZ;
            this.deltaX = 0.0;
            this.deltaY = 0.0;
            this.deltaZ = 0.0;
            this.deltaXZ = 0.0;
        }

        if (rotationChanged) {
            this.lastYaw = this.yaw;
            this.lastPitch = this.pitch;
            this.yaw = to.getYaw();
            this.pitch = to.getPitch();

            this.lastDeltaYaw = this.deltaYaw;
            this.lastDeltaPitch = this.deltaPitch;
            this.deltaYaw = MathUtil.getAngleDistance(yaw, lastYaw);
            this.deltaPitch = Math.abs(pitch - lastPitch);
        } else {
            this.lastDeltaYaw = this.deltaYaw;
            this.lastDeltaPitch = this.deltaPitch;
            this.deltaYaw = 0.0f;
            this.deltaPitch = 0.0f;
        }

        Player player = getPlayer();
        this.lastOnGround = this.onGround;
        this.onGround = player != null && player.isOnGround();
        this.mathematicallyOnGround = (Math.abs(to.getY() % 0.015625) < 0.0001);

        if (this.onGround) {
            this.groundTicks++;
            this.airTicks = 0;
        } else {
            this.airTicks++;
            this.groundTicks = 0;
        }

        this.teleportGraceTicks = Math.min(200, this.teleportGraceTicks + 1);
        this.respawnGraceTicks = Math.min(200, this.respawnGraceTicks + 1);
        this.joinGraceTicks = Math.min(200, this.joinGraceTicks + 1);
        this.vehicleGraceTicks = Math.min(200, this.vehicleGraceTicks + 1);
        this.velocityTicks = Math.min(200, this.velocityTicks + 1);
        this.riptideGraceTicks = Math.min(200, this.riptideGraceTicks + 1);
        this.elytraBoostGraceTicks = Math.min(200, this.elytraBoostGraceTicks + 1);
        if (this.pistonTicks > 0) this.pistonTicks--;
        if (this.inventoryOpenTicks > 0) this.inventoryOpenTicks++;

        if (positionChanged) {
            updateBlockCollisions(to);
        } else {
            if (this.iceTicks > 0) this.iceTicks--;
            if (this.slimeTicks > 0) this.slimeTicks--;
            if (this.honeyTicks > 0) this.honeyTicks--;
            if (this.climbableTicks > 0) this.climbableTicks--;
            if (this.webTicks > 0) this.webTicks--;
            if (this.liquidTicks > 0) this.liquidTicks--;
        }
    }

    private void updateBlockCollisions(Location loc) {
        PlayerUtil.EnvironmentScan env = PlayerUtil.scanEnvironment(loc);

        this.onIce = env.onIce();
        this.onSlime = env.onSlime();
        this.onHoney = env.onHoney();
        this.onClimbable = env.onClimbable();
        this.inWeb = env.inWeb();
        this.inLiquid = env.inLiquid();
        this.onLiquid = env.onLiquid();
        this.onSoul = env.onSoul();
        this.onPiston = env.onPiston();

        if (env.onPiston()) {
            this.pistonTicks = 15;
        }

        if (env.onIce()) {
            this.iceTicks = 20;
        } else if (this.iceTicks > 0) {
            this.iceTicks--;
        }

        if (env.onSlime()) {
            this.slimeTicks = 30;
        } else if (this.slimeTicks > 0) {
            this.slimeTicks--;
        }

        if (env.onHoney()) {
            this.honeyTicks = 25;
        } else if (this.honeyTicks > 0) {
            this.honeyTicks--;
        }

        if (env.onClimbable()) {
            this.climbableTicks = 15;
        } else if (this.climbableTicks > 0) {
            this.climbableTicks--;
        }

        if (env.inWeb()) {
            this.webTicks = 20;
        } else if (this.webTicks > 0) {
            this.webTicks--;
        }

        if (env.inLiquid() || env.onLiquid()) {
            this.liquidTicks = 20;
        } else if (this.liquidTicks > 0) {
            this.liquidTicks--;
        }

        if (env.hasSolidGround() || this.mathematicallyOnGround) {
            this.serverAirTicks = 0;
        } else {
            this.serverAirTicks++;
        }
    }

    public void addClickSample() {
        long now = TimeUtil.now();
        synchronized (clickLock) {
            if (lastClickTimestamp > 0) {
                long delay = now - lastClickTimestamp;
                if (delay > 0 && delay < 1000) {
                    clickDelays[clickDelayHead] = delay;
                    clickDelayHead = (clickDelayHead + 1) & 31;
                    if (clickDelayCount < 32) {
                        clickDelayCount++;
                    }
                }
            }
            this.lastClickTimestamp = now;

            recentClicks[clickHead] = now;
            clickHead = (clickHead + 1) & 63;
            if (clickTotal < 64) {
                clickTotal++;
            }
        }
        this.lastSwingTime = now;
    }

    public int getCpsLastSecond() {
        long threshold = TimeUtil.now() - 1000L;
        int count = 0;
        synchronized (clickLock) {
            for (int i = 0; i < clickTotal; i++) {
                if (recentClicks[i] >= threshold) {
                    count++;
                }
            }
        }
        return count;
    }

    public double getClickStdDev() {
        synchronized (clickLock) {
            if (clickDelayCount < 20) return 10.0;
            return MathUtil.standardDeviation(clickDelays, clickDelayCount);
        }
    }

    public double getClickKurtosis() {
        synchronized (clickLock) {
            if (clickDelayCount < 12) return 0.0;
            return MathUtil.kurtosis(clickDelays, clickDelayCount);
        }
    }

    public List<Long> getClickDelaysSnapshot() {
        synchronized (clickLock) {
            List<Long> list = new ArrayList<>(clickDelayCount);
            for (int i = 0; i < clickDelayCount; i++) {
                list.add(clickDelays[i]);
            }
            return list;
        }
    }

    public void clearClickSamples() {
        synchronized (clickLock) {
            clickDelayCount = 0;
            clickDelayHead = 0;
            clickTotal = 0;
            clickHead = 0;
            lastClickTimestamp = 0;
        }
    }

    public void setExpectedVelocity(Vector velocity) {
        if (velocity == null) return;
        this.expectedVelocity = velocity.clone();
        this.velocityTicks = 0;
        this.velocityHandled = false;
    }

    public void markJoin() {
        this.joinGraceTicks = 0;
    }

    public void markTeleport() {
        this.teleportGraceTicks = 0;
    }

    public void markRespawn() {
        this.respawnGraceTicks = 0;
    }

    public void markVehicle() {
        this.vehicleGraceTicks = 0;
    }

    public void markRiptide() {
        this.riptideGraceTicks = 0;
    }

    public void markElytraBoost() {
        this.elytraBoostGraceTicks = 0;
    }

    public void markPiston() {
        this.pistonTicks = 15;
    }

    public void markInventoryOpen() {
        this.inventoryOpenTicks = 1;
        this.lastInventoryOpenTime = TimeUtil.now();
    }

    public void markInventoryClose() {
        this.inventoryOpenTicks = 0;
    }

    public int getInventoryOpenTicks() {
        return inventoryOpenTicks;
    }

    public long getLastInventoryOpenTime() {
        return lastInventoryOpenTime;
    }

    public double getViolationLevel(String checkName) {
        if (checkName == null) return 0.0;
        return violations.getOrDefault(checkName.toLowerCase(), 0.0);
    }

    public void setViolationLevel(String checkName, double vl) {
        if (checkName == null) return;
        if (Double.isNaN(vl) || Double.isInfinite(vl) || vl <= 0.0) {
            violations.remove(checkName.toLowerCase());
        } else {
            violations.put(checkName.toLowerCase(), Math.min(1000.0, vl));
        }
    }

    public void setAllViolations(Map<String, Double> newViolations) {
        if (newViolations == null) return;
        for (Map.Entry<String, Double> entry : newViolations.entrySet()) {
            setViolationLevel(entry.getKey(), entry.getValue());
        }
    }

    public void decayViolations(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            return;
        }
        for (Map.Entry<String, Double> entry : violations.entrySet()) {
            double current = entry.getValue();
            if (Double.isNaN(current) || Double.isInfinite(current) || current <= amount) {
                violations.remove(entry.getKey());
            } else {
                violations.put(entry.getKey(), Math.min(1000.0, Math.max(0.0, current - amount)));
            }
        }
    }

    public double getTotalViolationLevel() {
        double total = 0.0;
        for (double vl : violations.values()) {
            if (!Double.isNaN(vl) && !Double.isInfinite(vl) && vl > 0.0) {
                total += vl;
            }
        }
        return total;
    }

    public void clearViolations() {
        violations.clear();
    }

    public boolean isExemptMovement() {
        Player p = getPlayer();
        if (p == null || !p.isOnline() || p.isDead()) return true;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return true;
        if (p.isFlying() || p.getAllowFlight()) return true;
        if (p.isGliding() || p.isRiptiding()) return true;
        if (p.isInsideVehicle()) return true;
        return teleportGraceTicks < 20
                || respawnGraceTicks < 35
                || joinGraceTicks < 40
                || vehicleGraceTicks < 25
                || riptideGraceTicks < 35
                || elytraBoostGraceTicks < 40
                || pistonTicks > 0;
    }

    public void cleanReferences() {
        this.lastLocation = null;
        this.currentLocation = null;
        this.lastTargetUuid = null;
        this.lastTargetEntityId = 0;
        this.speedBuffer = 0.0;
        this.airDistance = 0.0;
        this.pitchLockTicks = 0;
        this.linearAimTicks = 0;
        this.consecutiveSprintSneakTicks = 0;
        this.consecutiveLowHungerSprintTicks = 0;
        this.fastPlaceBuffer = 0;
        this.bowDrawStartTime = 0;
        this.consumeStartTime = 0;
        this.preAttackYaw = 0.0f;
        this.preAttackPitch = 0.0f;
        this.preAttackTime = 0;
        this.lastTargetCrosshairTime = 0;
        this.triggerBotBuffer = 0;
        this.registeredChannels.clear();
        this.detectedClientName = null;
        this.clientDetectionConfidence = null;
        this.clientConfidenceScore = 0.0;
        this.clientSignals.clear();
        this.recentCategoryViolations.clear();
        clearClickSamples();
    }

    public void markDeath() {
        this.expectedVelocity = new Vector(0, 0, 0);
        this.velocityTicks = 100;
        this.velocityHandled = true;
        this.lastTargetUuid = null;
        this.lastTargetEntityId = 0;
        this.lastAttackTime = 0;
        this.lastSwingTime = 0;
        this.speedBuffer = 0.0;
        this.airDistance = 0.0;
        this.pitchLockTicks = 0;
        this.consecutiveSprintSneakTicks = 0;
        this.consecutiveLowHungerSprintTicks = 0;
        this.fastPlaceBuffer = 0;
        this.bowDrawStartTime = 0;
        this.consumeStartTime = 0;
        this.timerBalance = 0.0;
        this.lastTargetCrosshairTime = 0;
        this.triggerBotBuffer = 0;
    }

    public void markGameModeChange() {
        markTeleport();
        this.speedBuffer = 0.0;
        this.airDistance = 0.0;
        this.timerBalance = 0.0;
        this.consecutiveSprintSneakTicks = 0;
        this.consecutiveLowHungerSprintTicks = 0;
    }

    public Location getLastLocation() { return lastLocation; }
    public Location getCurrentLocation() { return currentLocation; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public double getDeltaXZ() { return deltaXZ; }
    public double getLastDeltaX() { return lastDeltaX; }
    public double getLastDeltaY() { return lastDeltaY; }
    public double getLastDeltaZ() { return lastDeltaZ; }
    public double getLastDeltaXZ() { return lastDeltaXZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }
    public float getDeltaYaw() { return deltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }
    public float getLastDeltaYaw() { return lastDeltaYaw; }
    public float getLastDeltaPitch() { return lastDeltaPitch; }
    public boolean isOnGround() { return onGround; }
    public boolean isLastOnGround() { return lastOnGround; }
    public boolean isMathematicallyOnGround() { return mathematicallyOnGround; }
    public int getGroundTicks() { return groundTicks; }
    public int getAirTicks() { return airTicks; }
    public int getServerAirTicks() { return serverAirTicks; }
    public Vector getExpectedVelocity() { return expectedVelocity; }
    public int getVelocityTicks() { return velocityTicks; }
    public boolean isVelocityHandled() { return velocityHandled; }
    public void setVelocityHandled(boolean velocityHandled) { this.velocityHandled = velocityHandled; }
    public int getTeleportGraceTicks() { return teleportGraceTicks; }
    public int getRespawnGraceTicks() { return respawnGraceTicks; }
    public int getIceTicks() { return iceTicks; }
    public int getSlimeTicks() { return slimeTicks; }
    public int getHoneyTicks() { return honeyTicks; }
    public int getClimbableTicks() { return climbableTicks; }
    public int getWebTicks() { return webTicks; }
    public int getLiquidTicks() { return liquidTicks; }

    public boolean isOnIce() { return onIce; }
    public boolean isOnSlime() { return onSlime; }
    public boolean isOnHoney() { return onHoney; }
    public boolean isOnClimbable() { return onClimbable; }
    public boolean isInWeb() { return inWeb; }
    public boolean isInLiquid() { return inLiquid; }
    public boolean isOnLiquid() { return onLiquid; }
    public boolean isOnSoul() { return onSoul; }
    public boolean isOnPiston() { return onPiston; }

    public UUID getLastTargetUuid() { return lastTargetUuid; }
    public int getLastTargetEntityId() { return lastTargetEntityId; }
    public void setLastTarget(Entity target) {
        if (target != null) {
            this.lastTargetUuid = target.getUniqueId();
            this.lastTargetEntityId = target.getEntityId();
        } else {
            this.lastTargetUuid = null;
            this.lastTargetEntityId = 0;
        }
    }
    public int getConsecutiveLowHungerSprintTicks() { return consecutiveLowHungerSprintTicks; }
    public void setConsecutiveLowHungerSprintTicks(int ticks) { this.consecutiveLowHungerSprintTicks = ticks; }
    public int getFastPlaceBuffer() { return fastPlaceBuffer; }
    public void setFastPlaceBuffer(int fastPlaceBuffer) { this.fastPlaceBuffer = fastPlaceBuffer; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public float getPreAttackYaw() { return preAttackYaw; }
    public float getPreAttackPitch() { return preAttackPitch; }
    public long getPreAttackTime() { return preAttackTime; }
    public void setPreAttackRotation(float yaw, float pitch, long time) {
        this.preAttackYaw = yaw;
        this.preAttackPitch = pitch;
        this.preAttackTime = time;
    }
    public long getLastTargetCrosshairTime() { return lastTargetCrosshairTime; }
    public void setLastTargetCrosshairTime(long time) { this.lastTargetCrosshairTime = time; }
    public int getTriggerBotBuffer() { return triggerBotBuffer; }
    public void setTriggerBotBuffer(int buffer) { this.triggerBotBuffer = buffer; }

    public long getLastSwingTime() { return lastSwingTime; }
    public double getLastReach() { return lastReach; }
    public void setLastReach(double lastReach) { this.lastReach = lastReach; }
    public long getLastMovePacketTime() { return lastMovePacketTime; }
    public long getLastMovePacketDelta() { return lastMovePacketDelta; }
    public long getLastBlockPlaceTime() { return lastBlockPlaceTime; }
    public void setLastBlockPlaceTime(long lastBlockPlaceTime) { this.lastBlockPlaceTime = lastBlockPlaceTime; }
    public long getLastBlockBreakTime() { return lastBlockBreakTime; }
    public void setLastBlockBreakTime(long lastBlockBreakTime) { this.lastBlockBreakTime = lastBlockBreakTime; }
    public long getLastBowShootTime() { return lastBowShootTime; }
    public void setLastBowShootTime(long lastBowShootTime) { this.lastBowShootTime = lastBowShootTime; }
    public long getBowDrawStartTime() { return bowDrawStartTime; }
    public void setBowDrawStartTime(long bowDrawStartTime) { this.bowDrawStartTime = bowDrawStartTime; }
    public long getLastItemConsumeTime() { return lastItemConsumeTime; }
    public void setLastItemConsumeTime(long lastItemConsumeTime) { this.lastItemConsumeTime = lastItemConsumeTime; }
    public long getConsumeStartTime() { return consumeStartTime; }
    public void setConsumeStartTime(long consumeStartTime) { this.consumeStartTime = consumeStartTime; }
    public long getLastProjectileTime() { return lastProjectileTime; }
    public void setLastProjectileTime(long lastProjectileTime) { this.lastProjectileTime = lastProjectileTime; }
    public double getTimerBalance() { return timerBalance; }
    public void setTimerBalance(double timerBalance) { this.timerBalance = timerBalance; }
    public long getLastTimerCheckTime() { return lastTimerCheckTime; }
    public void setLastTimerCheckTime(long lastTimerCheckTime) { this.lastTimerCheckTime = lastTimerCheckTime; }
    public double getSpeedBuffer() { return speedBuffer; }
    public void setSpeedBuffer(double speedBuffer) { this.speedBuffer = speedBuffer; }
    public double getAirDistance() { return airDistance; }
    public void setAirDistance(double airDistance) { this.airDistance = airDistance; }
    public int getPitchLockTicks() { return pitchLockTicks; }
    public void setPitchLockTicks(int pitchLockTicks) { this.pitchLockTicks = pitchLockTicks; }
    public int getLinearAimTicks() { return linearAimTicks; }
    public void setLinearAimTicks(int linearAimTicks) { this.linearAimTicks = linearAimTicks; }
    public int getConsecutiveSprintSneakTicks() { return consecutiveSprintSneakTicks; }
    public void setConsecutiveSprintSneakTicks(int ticks) { this.consecutiveSprintSneakTicks = ticks; }
    public boolean isDebugWatched() { return debugWatched; }
    public void setDebugWatched(boolean debugWatched) { this.debugWatched = debugWatched; }
    public Map<String, Double> getViolations() { return Collections.unmodifiableMap(violations); }

    public String getClientBrand() {
        return clientBrand;
    }

    public boolean hasSuspiciousBrand() {
        return suspiciousBrand;
    }

    public void setClientBrand(String brand) {
        if (brand != null && !brand.isEmpty()) {
            this.clientBrand = brand.replaceAll("[^a-zA-Z0-9_\\- .:/+]", "").trim();
            this.suspiciousBrand = checkSuspiciousBrand(this.clientBrand);
        }
    }

    private boolean checkSuspiciousBrand(String brand) {
        if (brand == null || brand.isEmpty()) return false;
        String lower = brand.toLowerCase();
        return lower.contains("meteor")
                || lower.contains("wurst")
                || lower.contains("liquidbounce")
                || lower.contains("aristois")
                || lower.contains("inertia")
                || lower.contains("future")
                || lower.contains("rusherhack")
                || lower.contains("vape")
                || lower.contains("rise")
                || lower.contains("doomsday")
                || lower.contains("bleachhack")
                || lower.contains("boze")
                || lower.contains("sigma")
                || lower.contains("impact")
                || lower.contains("cheat");
    }

    public void recordCategoryViolation(CheckType type) {
        if (type != null) {
            recentCategoryViolations.put(type, TimeUtil.now());
        }
    }

    public int getActiveSuspiciousSignalsCount() {
        long now = TimeUtil.now();
        int count = 0;
        for (long timestamp : recentCategoryViolations.values()) {
            if (now - timestamp < 6000L) {
                count++;
            }
        }
        return count;
    }

    public void addRegisteredChannel(String channel) {
        if (channel != null && !channel.isEmpty() && registeredChannels.size() < 64) {
            registeredChannels.add(channel.toLowerCase().trim());
        }
    }

    public Set<String> getRegisteredChannels() {
        return Collections.unmodifiableSet(registeredChannels);
    }

    public boolean hasRecentViolation(CheckType type, long maxAgeMs) {
        if (type == null) return false;
        Long timestamp = recentCategoryViolations.get(type);
        return timestamp != null && (TimeUtil.now() - timestamp <= maxAgeMs);
    }

    public void setDetectedClient(String clientName, DetectionConfidence confidence, double score, List<String> signals) {
        this.detectedClientName = clientName;
        this.clientDetectionConfidence = confidence;
        this.clientConfidenceScore = score;
        this.clientSignals.clear();
        if (signals != null) {
            this.clientSignals.addAll(signals);
        }
    }

    public String getDetectedClientName() { return detectedClientName; }
    public DetectionConfidence getClientDetectionConfidence() { return clientDetectionConfidence; }
    public double getClientConfidenceScore() { return clientConfidenceScore; }
    public List<String> getClientSignals() { return Collections.unmodifiableList(clientSignals); }
    public InjectorDetectionResult getInjectorResult() { return injectorResult; }
    public void setInjectorResult(InjectorDetectionResult injectorResult) { this.injectorResult = injectorResult; }

    public DetectionConfidence calculateConfidence(CheckType currentType, double tps, int ping) {
        if (tps < 18.0 || ping > 250 || velocityTicks < 25) {
            return DetectionConfidence.LOW;
        }

        if (injectorResult != null && injectorResult.getConfidence() == DetectionConfidence.CRITICAL) {
            return DetectionConfidence.DEFINITIVE;
        }

        if (clientDetectionConfidence == DetectionConfidence.CRITICAL || clientDetectionConfidence == DetectionConfidence.DEFINITIVE) {
            return DetectionConfidence.DEFINITIVE;
        }

        if (clientDetectionConfidence == DetectionConfidence.HIGH) {
            return DetectionConfidence.HIGH;
        }

        int signals = getActiveSuspiciousSignalsCount();
        if (signals >= 3) {
            return DetectionConfidence.DEFINITIVE;
        }
        if (signals >= 2 || suspiciousBrand) {
            return DetectionConfidence.HIGH;
        }
        return DetectionConfidence.MEDIUM;
    }
}
