package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.Random;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;
    private boolean noslowSuccess = false;
    private long lastCheckTime = 0L;
    private boolean wasBlocking = false;
    private long lastBlockingTime = 0L;
    private static final long SPRINT_COOLDOWN_MS = 300L;
    private boolean isBlinking = false;
    private int blinkTimer = 0;
    private boolean slotSwapped = false; // 新增：槽位切换状态跟踪
    private int hypixelTestState = 0; // 新增：HypixelTest状态机 (0=空闲, 1=格挡中, 2=需要切换)

    // Properties
    public final ModeProperty swordMode;
    public final BooleanProperty onlyKillAuraAutoBlock;
    public final PercentProperty swordMotion;
    public final BooleanProperty swordSprint;
    public final IntProperty swordBlinkDelay;
    public final IntProperty swordBlinkDuration;
    public final BooleanProperty enableHypixelTest;
    public final IntProperty hypixelTestDelay;
    public final IntProperty hypixelTestDuration;
    public final ModeProperty foodMode;
    public final PercentProperty foodMotion;
    public final BooleanProperty foodSprint;
    public final IntProperty foodBlinkDelay;
    public final IntProperty foodBlinkDuration;
    public final ModeProperty bowMode;
    public final PercentProperty bowMotion;
    public final BooleanProperty bowSprint;
    public final IntProperty bowBlinkDelay;
    public final IntProperty bowBlinkDuration;
    public final BooleanProperty successDetection;
    public final BooleanProperty successMessage;

    public NoSlow() {
        super("NoSlow", "Allows you to move faster.", Category.MOVEMENT, 0, false, false);

        this.swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA", "BLINK", "HYPIXEL_TEST"});
        this.onlyKillAuraAutoBlock = new BooleanProperty("only-killaura-autoblock", false, () -> this.swordMode.getValue() != 0);
        this.swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() != 0);
        this.swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
        this.swordBlinkDelay = new IntProperty("sword-blink-delay", 1, 1, 10, () -> this.swordMode.getValue() == 2);
        this.swordBlinkDuration = new IntProperty("sword-blink-duration", 2, 1, 5, () -> this.swordMode.getValue() == 2);

        // HypixelTest专属设置
        this.enableHypixelTest = new BooleanProperty("enable-hypixel-test", true, () -> this.swordMode.getValue() == 3);
        this.hypixelTestDelay = new IntProperty("hypixel-test-delay", 1, 1, 10, () -> this.swordMode.getValue() == 3);
        this.hypixelTestDuration = new IntProperty("hypixel-test-duration", 2, 1, 5, () -> this.swordMode.getValue() == 3);

        this.foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT", "BLINK"});
        this.foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() != 0);
        this.foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
        this.foodBlinkDelay = new IntProperty("food-blink-delay", 2, 1, 10, () -> this.foodMode.getValue() == 3);
        this.foodBlinkDuration = new IntProperty("food-blink-duration", 1, 1, 5, () -> this.foodMode.getValue() == 3);

        this.bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "FLOAT", "BLINK"});
        this.bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() != 0);
        this.bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);
        this.bowBlinkDelay = new IntProperty("bow-blink-delay", 2, 1, 10, () -> this.bowMode.getValue() == 3);
        this.bowBlinkDuration = new IntProperty("bow-blink-duration", 1, 1, 5, () -> this.bowMode.getValue() == 3);

        this.successDetection = new BooleanProperty("success-detection", true,
                () -> this.swordMode.getValue() == 1 || this.swordMode.getValue() == 2 || this.swordMode.getValue() == 3);
        this.successMessage = new BooleanProperty("success-message", true,
                () -> this.successDetection.getValue());
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0
                && ItemUtil.isHoldingSword()
                && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    private boolean isKillAuraAutoBlocking() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura == null || !killAura.isEnabled()) {
            return false;
        }
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        int mode = killAura.autoBlock.getValue();
        if (mode == 0 || mode == 8) { // NONE or FAKE
            return false;
        }
        return killAura.isBlocking() || killAura.isPlayerBlocking();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return (this.foodMode.getValue() == 2 && ItemUtil.isEating())
                || (this.bowMode.getValue() == 2 && ItemUtil.isUsingBow());
    }

    public boolean isBlinkMode() {
        return (this.swordMode.getValue() == 2 && ItemUtil.isHoldingSword())
                || (this.foodMode.getValue() == 3 && ItemUtil.isEating())
                || (this.bowMode.getValue() == 3 && ItemUtil.isUsingBow());
    }

    public boolean isHypixelTestMode() {
        return this.swordMode.getValue() == 3 && ItemUtil.isHoldingSword();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return (this.isSwordActive() && this.swordSprint.getValue())
                || (this.isFoodActive() && this.foodSprint.getValue())
                || (this.isBowActive() && this.bowSprint.getValue());
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }

    // HypixelTest模式核心逻辑 - 槽位切换系统
    private void handleHypixelTest() {
        if (!this.enableHypixelTest.getValue() || !this.isHypixelTestMode()) {
            return;
        }

        boolean isBlocking = PlayerUtil.isUsingItem() && ItemUtil.isHoldingSword();

        switch (this.hypixelTestState) {
            case 0: // 空闲状态
                if (isBlocking) {
                    this.hypixelTestState = 1; // 进入格挡状态
                    this.lastBlockingTime = System.currentTimeMillis();
                }
                break;

            case 1: // 格挡中
                long blockTime = System.currentTimeMillis() - this.lastBlockingTime;
                if (blockTime >= this.hypixelTestDelay.getValue() * 50L) { // 转换为毫秒
                    // 到达延迟时间，准备切换槽位
                    this.hypixelTestState = 2;
                }
                break;

            case 2: // 需要切换槽位
                if (this.slotSwapped) {
                    // 已经切换过，恢复原始槽位
                    this.restoreOriginalSlot();
                    this.hypixelTestState = 0;
                } else {
                    // 切换到其他槽位
                    this.swapToOtherSlot();
                    this.hypixelTestState = 0;
                }
                break;
        }
    }

    private void swapToOtherSlot() {
        if (this.slotSwapped) {
            return;
        }

        int currentSlot = mc.thePlayer.inventory.currentItem;
        int nextSlot = this.getNextSlot(currentSlot);

        if (nextSlot != -1 && nextSlot != currentSlot) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(nextSlot));
            this.slotSwapped = true;

            if (this.successMessage.getValue()) {
                mc.thePlayer.addChatMessage(
                        new ChatComponentText("§a[NoSlow] §fSwitched to slot " + (nextSlot + 1))
                );
            }
        }
    }

    private void restoreOriginalSlot() {
        if (!this.slotSwapped) {
            return;
        }

        int originalSlot = this.getOriginalSlot();
        if (originalSlot != -1 && originalSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(originalSlot));
            this.slotSwapped = false;
        }
    }

    private int getNextSlot(int currentSlot) {
        // 简单的+1循环切换
        int nextSlot = (currentSlot + 1) % 9;

        // 确保切换的槽位有物品（避免切换到空槽位）
        for (int i = 0; i < 9; i++) {
            int testSlot = (currentSlot + i + 1) % 9;
            if (mc.thePlayer.inventory.getStackInSlot(testSlot) != null) {
                return testSlot;
            }
        }

        return nextSlot;
    }

    private int getOriginalSlot() {
        // 这里可以记录原始槽位，简化起见使用当前槽位的前一个
        int current = mc.thePlayer.inventory.currentItem;
        return (current - 1 + 9) % 9;
    }

    // Blink模式核心逻辑
    private boolean shouldBlink() {
        if (!this.isBlinkMode()) {
            return false;
        }
        ++this.blinkTimer;
        int delay = 2;
        int duration = 1;

        if (ItemUtil.isHoldingSword()) {
            delay = this.swordBlinkDelay.getValue();
            duration = this.swordBlinkDuration.getValue();
        } else if (ItemUtil.isEating()) {
            delay = this.foodBlinkDelay.getValue();
            duration = this.foodBlinkDuration.getValue();
        } else if (ItemUtil.isUsingBow()) {
            delay = this.bowBlinkDelay.getValue();
            duration = this.bowBlinkDuration.getValue();
        }

        int totalCycle = delay + duration;
        int currentPhase = this.blinkTimer % totalCycle;

        if (currentPhase < delay) {
            this.isBlinking = false;
            return false; // 延迟阶段：正常减速
        }
        this.isBlinking = true;
        return true; // Blink阶段：停止使用物品
    }

    private boolean checkNoSlowSuccess() {
        if (!(this.isEnabled() && this.isSwordActive() && this.successDetection.getValue())) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastCheckTime < 500L) {
            return this.noslowSuccess;
        }

        this.lastCheckTime = currentTime;
        boolean wasSprinting = mc.thePlayer.isSprinting();
        boolean isMoving = Math.abs(mc.thePlayer.movementInput.moveForward) > 0.1f
                || Math.abs(mc.thePlayer.movementInput.moveStrafe) > 0.1f;
        boolean newSuccessState = wasSprinting && isMoving && PlayerUtil.isUsingItem();

        if (newSuccessState != this.noslowSuccess && this.successMessage.getValue()) {
            if (newSuccessState) {
                mc.thePlayer.addChatMessage(
                        new ChatComponentText("§a[NoSlow] §fSuccess - Sword blocking without slowdown!")
                );
            } else {
                mc.thePlayer.addChatMessage(
                        new ChatComponentText("§c[NoSlow] §fFailed - Normal sword blocking slowdown")
                );
            }
        }

        this.noslowSuccess = newSuccessState;
        return this.noslowSuccess;
    }

    public boolean isNoSlowSuccess() {
        return this.checkNoSlowSuccess();
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            this.wasBlocking = false;
            return;
        }

        boolean isCurrentlyBlocking = this.isSwordActive() && PlayerUtil.isUsingItem();

        // HypixelTest模式处理
        if (this.isHypixelTestMode()) {
            this.handleHypixelTest();
        }

        // Blink模式处理
        if (this.isBlinkMode() && this.shouldBlink()) {
            if (this.isSwordActive()) {
                mc.thePlayer.stopUsingItem();
            }
            isCurrentlyBlocking = false;
            this.wasBlocking = false;
            return;
        }

        if (isCurrentlyBlocking) {
            this.wasBlocking = true;
            this.lastBlockingTime = System.currentTimeMillis();
        }

        boolean inSprintProtection = System.currentTimeMillis() - this.lastBlockingTime < SPRINT_COOLDOWN_MS;
        boolean playerWantsToSprint = mc.gameSettings.keyBindSprint.isKeyDown();

        if (this.isAnyActive() || inSprintProtection) {
            if (this.isSwordActive() || inSprintProtection) {
                this.checkNoSlowSuccess();
            }

            float multiplier = (float) this.getMotionMultiplier() / 100.0f;
            if (this.isAnyActive()) {
                mc.thePlayer.movementInput.moveForward *= multiplier;
                mc.thePlayer.movementInput.moveStrafe *= multiplier;
            }

            if ((this.canSprint() || inSprintProtection) && playerWantsToSprint
                    && mc.thePlayer.movementInput.moveForward > 0.1f) {
                mc.thePlayer.setSprinting(true);
            } else {
                mc.thePlayer.setSprinting(false);
            }
        } else {
            this.wasBlocking = false;
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }

        if (this.isSwordActive() && this.successDetection.getValue()) {
            this.checkNoSlowSuccess();
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }

    public String checkAndReturnStatus() {
        boolean success = this.checkNoSlowSuccess();
        return success ? "success" : "failed";
    }

    @Override
    public void onEnabled() {
        this.blinkTimer = 0;
        this.isBlinking = false;
        this.noslowSuccess = false;
        this.lastCheckTime = 0L;
        this.wasBlocking = false;
        this.lastBlockingTime = 0L;
        this.slotSwapped = false;
        this.hypixelTestState = 0;
    }

    @Override
    public void onDisabled() {
        this.blinkTimer = 0;
        this.isBlinking = false;
        this.noslowSuccess = false;
        this.lastCheckTime = 0L;
        this.wasBlocking = false;
        this.lastBlockingTime = 0L;
        this.slotSwapped = false;
        this.hypixelTestState = 0;

        // 确保恢复原始槽位
        if (this.slotSwapped) {
            this.restoreOriginalSlot();
        }

        if (mc.thePlayer != null) {
            mc.thePlayer.stopUsingItem();
        }
    }
}