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
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends Module {

    // Properties
    public final ModeProperty mode = new ModeProperty("Mode", 2, new String[]{"Vanilla", "Jump", "Prediction", "Reduce"});

    // General Settings
    public final PercentProperty chance = new PercentProperty("Chance", 100, () -> mode.getValue() != 2);
    public final PercentProperty horizontal = new PercentProperty("Horizontal", 0);
    public final PercentProperty vertical = new PercentProperty("Vertical", 0);

    // Explosion Settings
    public final PercentProperty explosionHorizontal = new PercentProperty("ExplosionsHorizontal", 0);
    public final PercentProperty explosionVertical = new PercentProperty("ExplosionsVertical", 0);

    // Checks & Debug
    public final BooleanProperty fakeCheck = new BooleanProperty("FakeCheck", true);
    public final BooleanProperty debugLog = new BooleanProperty("DebugLog", true);

    // Prediction Mode Settings (Mode 2)
    public final IntProperty delayTicks = new IntProperty("DelayTicks", 1, 1, 20, () -> this.mode.getValue() == 2);
    public final PercentProperty delayChance = new PercentProperty("DelayChange", 100, () -> this.mode.getValue() == 2);
    public final BooleanProperty jumpReset = new BooleanProperty("JumpReset", true, () -> this.mode.getValue() == 2 || this.mode.getValue() == 3);
    public final IntProperty hurt = new IntProperty("ReduceHurtTime", 10, 1, 10, () -> this.mode.getValue() == 2);
    public final FloatProperty astolftor = new FloatProperty("ReduceFactor", 0.6F, 0.1F, 1.0F, () -> this.mode.getValue() == 2);
    public final BooleanProperty test = new BooleanProperty("Test", true, () -> this.mode.getValue() == 2);

    // Jump Mode Settings (Mode 1)
    public final BooleanProperty userDp = new BooleanProperty("UserDelay", false, () -> this.mode.getValue() == 1);

    // Internal State
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean reverseFlag = false;
    private boolean delayActive = false;
    private boolean jumpFlag = false;

    private long lastAttackTime = 0L;
    private long blinkStartTime = 0L;
    private long reverseStartTime = 0L;

    public Velocity() {
        super("Velocity", "Allows you to modify knockback.", Category.COMBAT, 0, false, false);
    }

    // --- Helper Methods ---

    private boolean isInLiquidOrWeb() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.thePlayer.onGround && (killAura == null || !killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    /**
     * 判断是否处于战斗状态
     * 用于 Reduce 模式
     */
    private boolean isInCombat() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        // 如果 KillAura 开启且有目标，视为战斗中
        if (killAura != null && killAura.isEnabled() && killAura.target != null) {
            return true;
        }
        // 或者距离上次攻击时间在 3秒 (3000ms) 内
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

    // --- Events ---

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.resetState();
            return;
        }

        // 处理爆炸
        if (this.pendingExplosion) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.applyMotion(event, this.explosionHorizontal.getValue(), this.explosionVertical.getValue());
            return;
        }

        // 假 KB 检查 (S19 Status Check)
        if (!this.allowNext && this.fakeCheck.getValue()) {
            this.allowNext = true;
            return;
        }
        this.allowNext = true;

        int modeIndex = this.mode.getValue();

        switch (modeIndex) {
            case 0: // Vanilla
            case 1: // Jump
                this.handleStandardModes(event, modeIndex == 1);
                break;

            case 2: // Prediction
                this.handlePredictionMode(event);
                break;

            case 3: // Reduce
                this.handleReduceMode(event);
                break;
        }
    }

    private void handleStandardModes(KnockbackEvent event, boolean isJumpMode) {
        this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
        if (this.chanceCounter >= 100) {
            if (isJumpMode && event.getY() > 0.0) {
                this.jumpFlag = true;
                // Jump mode keeps full motion usually, or modified
                this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
            } else {
                this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
            }
        }
    }

    private void handlePredictionMode(KnockbackEvent event) {
        if (this.jumpReset.getValue() && event.getY() > 0.0) {
            this.jumpFlag = true;
            debug("[Prediction] jr!");
        }
        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
    }

    private void handleReduceMode(KnockbackEvent event) {
        // 核心修改：只在战斗中应用
        if (!isInCombat()) {
            return;
        }

        if (this.jumpReset.getValue() && event.getY() > 0.0) {
            this.jumpFlag = true;
            debug(String.format("[Reduce] JumpReset triggered (Y=%.2f)", event.getY()));
        }

        // 应用减少
        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());

        if (this.debugLog.getValue()) {
            debug("[Reduce] Reduce " + this.horizontal.getValue() + "%");
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
                }
            }
            else if (event.getPacket() instanceof S27PacketExplosion) {
                handleExplosionPacket(event, (S27PacketExplosion) event.getPacket());
            }
        }
        else if (event.getType() == EventType.SEND && !event.isCancelled()) {
            // 记录攻击时间
            if (this.mode.getValue() == 2 || this.mode.getValue() == 3) {
                if (event.getPacket() instanceof C02PacketUseEntity) {
                    C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
                    if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                        this.lastAttackTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void handleVelocityPacket(PacketEvent event, S12PacketEntityVelocity packet) {
        // Prediction Mode Logic
        if (this.mode.getValue() == 2) {
            LongJump longJump = (LongJump) Myau.moduleManager.modules.get(LongJump.class);
            boolean canStartJump = longJump != null && longJump.isEnabled() && longJump.canStartJump();

            // 如果不在液体、不在网里、没有爆炸、不是假KB、没有正在跳跃
            if (!this.reverseFlag && !this.isInLiquidOrWeb() && !this.pendingExplosion &&
                    !(this.allowNext && this.fakeCheck.getValue()) && !canStartJump) {

                this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();

                if (this.delayChanceCounter >= 100) {
                    Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                    Myau.delayManager.delayedPacket.offer(packet);
                    event.setCancelled(true);

                    this.reverseFlag = true;
                    this.reverseStartTime = System.currentTimeMillis();

                    if (this.test.getValue()) {
                        this.blinkStartTime = System.currentTimeMillis();
                        Myau.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    }
                    this.delayChanceCounter = 0;
                    return;
                }
            }
            debug(String.format("Velocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                    mc.thePlayer.ticksExisted, packet.getMotionX() / 8000.0, packet.getMotionY() / 8000.0, packet.getMotionZ() / 8000.0));
        }
        // Jump Mode User Delay Logic
        else if (this.mode.getValue() == 1 && this.userDp.getValue() && !mc.thePlayer.onGround) {
            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
            Myau.delayManager.delayedPacket.offer(packet);
            event.setCancelled(true);
            debug("[Jump] air delay!");
        }
        else {
            debug(String.format("Velocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                    mc.thePlayer.ticksExisted, packet.getMotionX() / 8000.0, packet.getMotionY() / 8000.0, packet.getMotionZ() / 8000.0));
        }
    }

    private void handleExplosionPacket(PacketEvent event, S27PacketExplosion packet) {
        // 如果有任何方向的推力
        if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
            this.pendingExplosion = true;
            // 如果设置为 0%，直接取消包，不再走 Knockback 事件
            if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                event.setCancelled(true);
            }
            debug(String.format("Explosion (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                    mc.thePlayer.ticksExisted,
                    mc.thePlayer.motionX + packet.func_149149_c(),
                    mc.thePlayer.motionY + packet.func_149144_d(),
                    mc.thePlayer.motionZ + packet.func_149147_e()));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.POST || this.mode.getValue() != 2) return;

        // Prediction Reduce Logic
        if (this.astolftor.getValue() < 1.0F && mc.thePlayer.hurtTime == this.hurt.getValue() &&
                System.currentTimeMillis() - this.lastAttackTime <= 8000L) {

            mc.thePlayer.motionX *= this.astolftor.getValue();
            mc.thePlayer.motionZ *= this.astolftor.getValue();
            debug("[Prediction] reduce!");
        }

        // Release Delayed Packet Logic
        if (this.reverseFlag) {
            boolean shouldRelease = false;
            int delayValue = this.delayTicks.getValue();

            if (delayValue >= 1 && delayValue <= 3) {
                long requiredDelay = delayValue == 1 ? 60L : (delayValue == 2 ? 95L : 100L);
                if (System.currentTimeMillis() - this.reverseStartTime >= requiredDelay) {
                    shouldRelease = true;
                }
            } else {
                shouldRelease = this.canDelay() || this.isInLiquidOrWeb() || Myau.delayManager.isDelay() >= delayValue;
            }

            if (shouldRelease) {
                Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.reverseFlag = false;
                Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
            }
        }

        if (this.delayActive) {
            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
            this.delayActive = false;
        }

        if (this.test.getValue()) {
            Myau.blinkManager.setBlinkState(System.currentTimeMillis() - this.blinkStartTime < 95L, BlinkModules.BLINK);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() &&
                    !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {

                mc.thePlayer.movementInput.jump = true;
                debug("[Prediction/Jump] jr successfully!");
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.resetState();
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
    }

    @Override
    public void onEnabled() {
        this.resetState();
        this.lastAttackTime = 0L;
        this.blinkStartTime = System.currentTimeMillis();
    }

    @Override
    public void onDisabled() {
        this.resetState();
        if (Myau.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Myau.delayManager.delayedPacket.clear();
        Myau.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        // 如果想要优化 CaseFormat 调用，可以缓存，但这里不频繁调用所以问题不大
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}