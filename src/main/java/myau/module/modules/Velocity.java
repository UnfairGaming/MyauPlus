package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.DelayModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorEntity;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import myau.util.TimerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

public class Velocity extends Module {

    // Properties
    public final ModeProperty mode = new ModeProperty("Mode", 0,
            new String[]{"Vanilla", "Jump", "Reduce", "Prediction"});

    // General Settings
    public final PercentProperty chance = new PercentProperty("Chance", 100, () -> mode.getValue() != 3);
    public final PercentProperty horizontal = new PercentProperty("Horizontal", 100);
    public final PercentProperty vertical = new PercentProperty("Vertical", 100);

    // Explosion Settings
    public final PercentProperty explosionHorizontal = new PercentProperty("ExplosionsHorizontal", 100);
    public final PercentProperty explosionVertical = new PercentProperty("ExplosionsVertical", 100);

    // Checks & Debug
    public final BooleanProperty fakeCheck = new BooleanProperty("FakeCheck", true);
    public final BooleanProperty debugLog = new BooleanProperty("DebugLog", true);

    // Reduce Mode Settings (Mode 2)
    public final BooleanProperty jumpReset = new BooleanProperty("JumpReset", true, () -> this.mode.getValue() == 2);
    public final IntProperty hurtTimeReduce = new IntProperty("HurtTime", 10, 1, 10, () -> this.mode.getValue() == 2);
    public final FloatProperty reduceFactor = new FloatProperty("ReduceFactor", 0.6F, 0.1F, 1.0F, () -> this.mode.getValue() == 2);

    // Jump Mode Settings (Mode 1)
    public final BooleanProperty useDelay = new BooleanProperty("UseDelay", false, () -> this.mode.getValue() == 1);

    // Prediction Mode Settings (Mode 3) - 严格按Rise参数
    public final BooleanProperty prediction = new BooleanProperty("Prediction", true, () -> this.mode.getValue() == 3);
    public final PercentProperty predictionFactor = new PercentProperty("Prediction Factor", 65, 1, 100, () -> this.mode.getValue() == 3 && prediction.getValue());
    public final IntProperty smoothness = new IntProperty("Smoothness", 3, 1, 10, () -> this.mode.getValue() == 3 && prediction.getValue());
    public final BooleanProperty autoReset = new BooleanProperty("Auto Reset", true, () -> this.mode.getValue() == 3 && prediction.getValue());
    public final BooleanProperty strict = new BooleanProperty("Strict", false, () -> this.mode.getValue() == 3 && prediction.getValue());
    public final BooleanProperty buffer = new BooleanProperty("Buffer", true, () -> this.mode.getValue() == 3);

    // Delay Settings for Prediction
    public final IntProperty delayTicks = new IntProperty("Delay Ticks", 2, 0, 5, () -> this.mode.getValue() == 3);
    public final PercentProperty delayChance = new PercentProperty("Delay Chance", 100, () -> this.mode.getValue() == 3 && delayTicks.getValue() > 0);

    // Internal State
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean reverseFlag = false;
    private boolean delayActive = false;
    private boolean jumpFlag = false;

    private long lastAttackTime = 0L;
    private long reverseStartTime = 0L;

    // Prediction Mode State
    private final ArrayList<Vec3> motionHistory = new ArrayList<>();
    private final ArrayList<Vec3> velocityHistory = new ArrayList<>();
    private Vec3 predictedVelocity = new Vec3(0, 0, 0);
    private Vec3 lastPredictedMotion = new Vec3(0, 0, 0);
    private Vec3 velocityBuffer = new Vec3(0, 0, 0);
    private int predictionTicks = 0;
    private double predictionAccuracy = 1.0;
    private double interpolationFactor = 0.0;
    private boolean isPredicting = false;
    private int ticksSinceLastVelocity = 0;

    // 计时器
    private final TimerUtil velocityTimer = new TimerUtil();

    public Velocity() {
        super("Velocity", "Allows you to modify knockback.", Category.COMBAT, 0, false, false);
    }

    // --- Helper Methods ---

    private boolean isInLiquidOrWeb() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean isInCombat() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.target != null) {
            return true;
        }
        return System.currentTimeMillis() - this.lastAttackTime < 3000L;
    }

    private void debug(String message) {
        if (this.debugLog.getValue()) {
            ChatUtil.sendFormatted(Myau.clientName + " " + message + "&r");
        }
    }

    private void applyMotion(KnockbackEvent event, double hPct, double vPct) {
        if (hPct != 100) {
            event.setX(event.getX() * hPct / 100.0);
            event.setZ(event.getZ() * hPct / 100.0);
        }
        if (vPct != 100) {
            event.setY(event.getY() * vPct / 100.0);
        }
    }

    // --- Rise Prediction Mode Methods ---

    private void initPrediction() {
        motionHistory.clear();
        velocityHistory.clear();
        predictedVelocity = new Vec3(0, 0, 0);
        lastPredictedMotion = new Vec3(0, 0, 0);
        velocityBuffer = new Vec3(0, 0, 0);
        predictionTicks = 0;
        predictionAccuracy = 1.0;
        interpolationFactor = 0.0;
        isPredicting = false;
        ticksSinceLastVelocity = 0;
        velocityTimer.reset();
    }

    private void addMotionData(Vec3 motion) {
        if (motionHistory.size() >= 15) {
            motionHistory.remove(0);
        }
        motionHistory.add(motion);
    }

    private void addVelocityData(Vec3 velocity) {
        if (velocityHistory.size() >= 10) {
            velocityHistory.remove(0);
        }
        velocityHistory.add(velocity);
    }

    private Vec3 calculateWeightedPrediction() {
        if (motionHistory.size() < 3) {
            return new Vec3(0, 0, 0);
        }

        int size = motionHistory.size();
        double x = 0, y = 0, z = 0;
        double totalWeight = 0;

        for (int i = 0; i < size; i++) {
            double weight = (i + 1) / (double) size;
            Vec3 data = motionHistory.get(i);
            x += data.xCoord * weight;
            y += data.yCoord * weight;
            z += data.zCoord * weight;
            totalWeight += weight;
        }

        if (totalWeight > 0) {
            x /= totalWeight;
            y /= totalWeight;
            z /= totalWeight;
        }

        return new Vec3(x, y, z);
    }

    private Vec3 calculateLinearPrediction() {
        if (motionHistory.size() < 2) {
            return new Vec3(0, 0, 0);
        }

        int lastIndex = motionHistory.size() - 1;
        Vec3 lastMotion = motionHistory.get(lastIndex);
        Vec3 prevMotion = motionHistory.get(lastIndex - 1);

        double trendX = lastMotion.xCoord - prevMotion.xCoord;
        double trendY = lastMotion.yCoord - prevMotion.yCoord;
        double trendZ = lastMotion.zCoord - prevMotion.zCoord;

        double factor = predictionFactor.getValue() / 100.0;
        if (strict.getValue()) {
            factor *= 0.8;
        }

        return new Vec3(
                lastMotion.xCoord + trendX * factor,
                lastMotion.yCoord + trendY * factor,
                lastMotion.zCoord + trendZ * factor
        );
    }

    private void updatePrediction() {
        ticksSinceLastVelocity++;

        if (motionHistory.isEmpty()) {
            return;
        }

        Vec3 weightedPred = calculateWeightedPrediction();
        Vec3 linearPred = calculateLinearPrediction();

        double blendFactor = 0.6;
        double x = weightedPred.xCoord * blendFactor + linearPred.xCoord * (1 - blendFactor);
        double y = weightedPred.yCoord * blendFactor + linearPred.yCoord * (1 - blendFactor);
        double z = weightedPred.zCoord * blendFactor + linearPred.zCoord * (1 - blendFactor);

        predictedVelocity = new Vec3(x, y, z);

        if (smoothness.getValue() > 1) {
            double smoothFactor = 1.0 / (smoothness.getValue() * 2);
            predictedVelocity = lerpVec3(lastPredictedMotion, predictedVelocity, smoothFactor);
        }

        lastPredictedMotion = predictedVelocity;

        if (!velocityHistory.isEmpty()) {
            Vec3 lastVelocity = velocityHistory.get(velocityHistory.size() - 1);
            double dx = predictedVelocity.xCoord - lastVelocity.xCoord;
            double dy = predictedVelocity.yCoord - lastVelocity.yCoord;
            double dz = predictedVelocity.zCoord - lastVelocity.zCoord;
            double error = Math.sqrt(dx * dx + dy * dy + dz * dz);

            predictionAccuracy = 1.0 / (1.0 + error);

            if (autoReset.getValue() && error > 2.0) {
                debug("[Prediction] Large error detected, resetting...");
                initPrediction();
            }
        }

        predictionTicks++;
    }

    private Vec3 lerpVec3(Vec3 start, Vec3 end, double factor) {
        if (factor < 0) factor = 0;
        if (factor > 1) factor = 1;

        return new Vec3(
                start.xCoord + (end.xCoord - start.xCoord) * factor,
                start.yCoord + (end.yCoord - start.yCoord) * factor,
                start.zCoord + (end.zCoord - start.zCoord) * factor
        );
    }

    private Vec3 multiplyVec3(Vec3 vec) {
        return new Vec3(vec.xCoord * 0.8, vec.yCoord * 0.8, vec.zCoord * 0.8);
    }

    private Vec3 addVec3(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.xCoord + vec2.xCoord, vec1.yCoord + vec2.yCoord, vec1.zCoord + vec2.zCoord);
    }

    private Vec3 subtractVec3(Vec3 vec1, Vec3 vec2) {
        return new Vec3(vec1.xCoord - vec2.xCoord, vec1.yCoord - vec2.yCoord, vec1.zCoord - vec2.zCoord);
    }

    private double lengthVec3(Vec3 vec) {
        return Math.sqrt(vec.xCoord * vec.xCoord + vec.yCoord * vec.yCoord + vec.zCoord * vec.zCoord);
    }

    private void applyVelocityBuffer() {
        if (!buffer.getValue() || (velocityBuffer.xCoord == 0 && velocityBuffer.yCoord == 0 && velocityBuffer.zCoord == 0)) {
            return;
        }

        if (mc.thePlayer != null && predictionTicks < 5) {
            double bufferFactor = 0.5;
            mc.thePlayer.motionX += velocityBuffer.xCoord * bufferFactor;
            mc.thePlayer.motionY += velocityBuffer.yCoord * bufferFactor;
            mc.thePlayer.motionZ += velocityBuffer.zCoord * bufferFactor;

            velocityBuffer = multiplyVec3(velocityBuffer);
            if (lengthVec3(velocityBuffer) < 0.01) {
                velocityBuffer = new Vec3(0, 0, 0);
            }
        }
    }

    private boolean shouldDelayVelocity() {
        if (delayTicks.getValue() <= 0 || delayChance.getValue() <= 0) {
            return false;
        }

        // 更新计数器
        delayChanceCounter += delayChance.getValue();

        // Rise的逻辑：当计数器达到或超过100时触发
        if (delayChanceCounter < 100) {
            return false;
        }

        // 触发后重置计数器
        delayChanceCounter = delayChanceCounter % 100;

        // 检查其他条件
        if (isInLiquidOrWeb()) {
            return false;
        }

        if (mc.thePlayer != null && !mc.thePlayer.onGround) {
            return false;
        }

        return System.currentTimeMillis() - lastAttackTime >= 200;

        // 所有条件都满足
    }

    // --- Events ---

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            return;
        }

        if (this.pendingExplosion) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.applyMotion(event, this.explosionHorizontal.getValue(), this.explosionVertical.getValue());
            return;
        }

        if (!this.allowNext && this.fakeCheck.getValue()) {
            this.allowNext = true;
            event.setCancelled(true);
            return;
        }
        this.allowNext = true;

        int modeIndex = this.mode.getValue();

        switch (modeIndex) {
            case 0: // Vanilla
                handleVanillaMode(event);
                break;
            case 1: // Jump
                handleJumpMode(event);
                break;
            case 2: // Reduce
                handleReduceMode(event);
                break;
            case 3: // Prediction
                handlePredictionMode(event);
                break;
        }
    }

    private void handleVanillaMode(KnockbackEvent event) {
        this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
        if (this.chanceCounter >= 100) {
            this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
        }
    }

    private void handleJumpMode(KnockbackEvent event) {
        this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
        if (this.chanceCounter >= 100) {
            if (event.getY() > 0.0) {
                this.jumpFlag = true;
            }
            this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
        }
    }

    private void handleReduceMode(KnockbackEvent event) {
        if (!isInCombat()) {
            return;
        }

        if (this.jumpReset.getValue() && event.getY() > 0.0) {
            this.jumpFlag = true;
        }

        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
    }

    private void handlePredictionMode(KnockbackEvent event) {
        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());

        if (!prediction.getValue()) {
            return;
        }

        Vec3 currentMotion = new Vec3(event.getX(), event.getY(), event.getZ());
        addMotionData(currentMotion);

        if (isPredicting && velocityTimer.hasTimeElapsed(50)) {
            updatePrediction();

            if (lengthVec3(predictedVelocity) > 0.1) {
                double applyFactor = predictionAccuracy * 0.7 + 0.3;

                interpolationFactor = Math.min(1.0, interpolationFactor + 0.2);

                Vec3 smoothed = lerpVec3(currentMotion, predictedVelocity,
                        interpolationFactor * applyFactor * (smoothness.getValue() / 10.0));

                event.setX(smoothed.xCoord);
                event.setY(smoothed.yCoord);
                event.setZ(smoothed.zCoord);

                if (buffer.getValue()) {
                    Vec3 diff = subtractVec3(smoothed, currentMotion);
                    velocityBuffer = addVec3(velocityBuffer, diff);
                }

                if (debugLog.getValue()) {
                    debug(String.format("[Prediction] Acc:%.2f Pred:%.2f,%.2f,%.2f",
                            predictionAccuracy, smoothed.xCoord, smoothed.yCoord, smoothed.zCoord));
                }
            }
        }

        if (event.getY() > 0.15) {
            this.jumpFlag = true;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    handleVelocityPacket(event, packet);
                }
            }
            else if (event.getPacket() instanceof S19PacketEntityStatus) {
                S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                Entity entity = packet.getEntity(mc.theWorld);
                if (entity == mc.thePlayer && packet.getOpCode() == 2) {
                    this.allowNext = false;
                    debug("[Velocity] Fake KB detected");
                }
            }
            else if (event.getPacket() instanceof S27PacketExplosion) {
                handleExplosionPacket(event, (S27PacketExplosion) event.getPacket());
            }
        }
        else if (event.getType() == EventType.SEND && !event.isCancelled()) {
            if (event.getPacket() instanceof C02PacketUseEntity) {
                C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
                if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    this.lastAttackTime = System.currentTimeMillis();
                }
            }
        }
    }

    private void handleVelocityPacket(PacketEvent event, S12PacketEntityVelocity packet) {
        int modeIndex = this.mode.getValue();

        Vec3 velocity = new Vec3(
                packet.getMotionX() / 8000.0,
                packet.getMotionY() / 8000.0,
                packet.getMotionZ() / 8000.0
        );

        if (modeIndex == 3) {
            addVelocityData(velocity);

            if (prediction.getValue()) {
                isPredicting = true;
                interpolationFactor = 0.0;
                velocityTimer.reset();
                ticksSinceLastVelocity = 0;

                if (shouldDelayVelocity()) {
                    Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                    Myau.delayManager.delayedPacket.offer(packet);
                    event.setCancelled(true);

                    this.reverseFlag = true;
                    this.reverseStartTime = System.currentTimeMillis();

                    Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);

                    debug("[Prediction] Delaying velocity packet");
                    delayChanceCounter = 0;
                    return;
                }
            }
        }
        else if (modeIndex == 1 && this.useDelay.getValue()) {
            if (!mc.thePlayer.onGround) {
                Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Myau.delayManager.delayedPacket.offer(packet);
                event.setCancelled(true);
            }
        }

        debug(String.format("Velocity (mode:%d x:%.2f y:%.2f z:%.2f)",
                modeIndex,
                packet.getMotionX() / 8000.0,
                packet.getMotionY() / 8000.0,
                packet.getMotionZ() / 8000.0));
    }

    private void handleExplosionPacket(PacketEvent event, S27PacketExplosion packet) {
        if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
            this.pendingExplosion = true;
            if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.POST) return;

        int modeIndex = this.mode.getValue();

        if (modeIndex == 2) {
            if (this.reduceFactor.getValue() < 1.0F && mc.thePlayer.hurtTime == this.hurtTimeReduce.getValue() &&
                    System.currentTimeMillis() - this.lastAttackTime <= 8000L) {
                mc.thePlayer.motionX *= this.reduceFactor.getValue();
                mc.thePlayer.motionZ *= this.reduceFactor.getValue();
            }
        }

        if (modeIndex == 3) {
            if (prediction.getValue() && isPredicting) {
                updatePrediction();

                applyVelocityBuffer();

                if (ticksSinceLastVelocity > 20) {
                    isPredicting = false;
                    if (autoReset.getValue()) {
                        initPrediction();
                    }
                }
            }

            if (this.reverseFlag) {
                boolean shouldRelease = false;

                if (delayTicks.getValue() >= 1 && delayTicks.getValue() <= 3) {
                    long requiredDelay = delayTicks.getValue() == 1 ? 50L :
                            (delayTicks.getValue() == 2 ? 80L : 100L);
                    if (System.currentTimeMillis() - this.reverseStartTime >= requiredDelay) {
                        shouldRelease = true;
                    }
                } else {
                    shouldRelease = Myau.delayManager.isDelay() >= delayTicks.getValue();
                }

                if (shouldRelease) {
                    Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    this.reverseFlag = false;
                    Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    debug("[Prediction] Released delayed packet");
                }
            }
        }

        if (this.delayActive) {
            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
            this.delayActive = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() &&
                    !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if (this.mode.getValue() == 3 && debugLog.getValue()) {
                    debug("[Prediction] Jump reset applied");
                }
            }
        }

        if (this.mode.getValue() == 3 && prediction.getValue() && mc.thePlayer != null) {
            Vec3 currentMotion = new Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
            if (lengthVec3(currentMotion) > 0.01) {
                addMotionData(currentMotion);
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        resetState();
    }

    private void resetState() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.delayActive = false;
        this.reverseStartTime = 0L;
        this.jumpFlag = false;

        if (this.mode.getValue() == 3) {
            initPrediction();
        }
    }

    @Override
    public void onEnabled() {
        resetState();
        this.lastAttackTime = 0L;

        if (this.mode.getValue() == 3) {
            initPrediction();
            debug("[Prediction] Mode enabled");
        }
    }

    @Override
    public void onDisabled() {
        resetState();
        if (Myau.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Myau.delayManager.delayedPacket.clear();
        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (this.mode.getValue() == 3 && prediction.getValue()) {
            return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName) +
                    " PF:" + predictionFactor.getValue()};
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}