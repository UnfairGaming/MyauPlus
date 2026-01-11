package myau.module.modules;

import java.awt.Color;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;

import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.event.types.EventType;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BackTrack extends Module {

    // 设置项
    public final IntProperty latency = new IntProperty("Latency delay", 200, 1, 1000);
    public final FloatProperty enemyDistance = new FloatProperty("Enemy distance", 6.0F, 3.1F, 6.0F);
    public final BooleanProperty onlyCombat = new BooleanProperty("Only during combat", true);
    public final BooleanProperty predictPosition = new BooleanProperty("Render prediction", true);
    public final BooleanProperty useThemeColor = new BooleanProperty("Use theme color", false);
    public final IntProperty boxColor = new IntProperty("Box color [H]", 0, 0, 360);
    public final BooleanProperty disableOnWorldChange = new BooleanProperty("Disable on world change", false);
    public final BooleanProperty disableOnDisconnect = new BooleanProperty("Disable on disconnect", false);

    // 内部变量
    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private Vec3 vec3, lastVec3;
    private EntityPlayer target;
    private int attackTicks;

    // 新增：用于ESP平滑移动的历史位置队列
    private final LinkedList<Vec3> positionHistory = new LinkedList<>();
    private final int maxHistorySize = 10;
    private Vec3 lastRenderedPos = null;

    // 新增：用于检测快速移动的变量
    private final LinkedList<Vec3> recentPositions = new LinkedList<>();
    private static final int FAST_MOVE_CHECK_TICKS = 5;
    private static final double FAST_MOVE_THRESHOLD = 5.0;

    // 新增：模块状态标志
    private boolean moduleActive = false;

    public BackTrack() {
        super("BackTrack", "Delays packets to hit past positions", Category.COMBAT, 0, false, false);
    }

    @Override
    public String[] getSuffix() {
        if (!isEnabled()) {
            return new String[]{""};
        }
        return new String[]{latency.getValue() + "ms"};
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        if (mc.thePlayer == null) {
            toggle();
            return;
        }
        packetQueue.clear();
        positionHistory.clear();
        recentPositions.clear();
        vec3 = lastVec3 = null;
        lastRenderedPos = null;
        target = null;
        attackTicks = 0;
        moduleActive = true;
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        moduleActive = false;
        if (mc.thePlayer == null) return;
        // 在禁用时释放所有被延迟的数据包
        releaseAllImmediately();
        positionHistory.clear();
        recentPositions.clear();
        target = null;
        vec3 = lastVec3 = null;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;

        // 只有模块开启时才处理
        if (!moduleActive) {
            return;
        }

        // 更新攻击计时
        attackTicks++;

        try {
            if (target != null) {
                // 新增：检测快速移动
                Vec3 currentPos = target.getPositionVector();
                recentPositions.add(new Vec3(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord));

                if (recentPositions.size() > FAST_MOVE_CHECK_TICKS) {
                    recentPositions.removeFirst();
                }

                // 检查快速移动
                if (recentPositions.size() == FAST_MOVE_CHECK_TICKS) {
                    Vec3 oldestPos = recentPositions.getFirst();
                    double distanceMoved = oldestPos.distanceTo(currentPos);

                    if (distanceMoved > FAST_MOVE_THRESHOLD) {
                        target = null;
                        vec3 = lastVec3 = null;
                        positionHistory.clear();
                        recentPositions.clear();
                        releaseAllImmediately();
                        return;
                    }
                }

                // 更新位置历史用于平滑
                positionHistory.add(new Vec3(currentPos.xCoord, currentPos.yCoord, currentPos.zCoord));
                if (positionHistory.size() > maxHistorySize) {
                    positionHistory.removeFirst();
                }

                // 检查超时或距离
                if (attackTicks > 7 || vec3.distanceTo(mc.thePlayer.getPositionVector()) > enemyDistance.getValue()) {
                    target = null;
                    vec3 = lastVec3 = null;
                    positionHistory.clear();
                    recentPositions.clear();
                    releaseAllImmediately();
                }
                lastVec3 = vec3;
            }
        } catch (Exception ignored) {}

        // 处理队列释放
        long maxDelay = latency.getValue();
        while (!packetQueue.isEmpty()) {
            TimedPacket timedPacket = packetQueue.peek();

            // 检查时间是否到达
            if (System.currentTimeMillis() - timedPacket.timestamp >= maxDelay) {
                packetQueue.poll();
                processPacketLocally(timedPacket.packet);
            } else {
                break;
            }
        }

        // 更新位置向量
        if (packetQueue.isEmpty() && target != null) {
            vec3 = target.getPositionVector();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        // 只有模块开启时才渲染
        if (!moduleActive || !this.predictPosition.getValue()) {
            return;
        }

        if (this.target != null && this.vec3 != null && this.lastVec3 != null) {

            // 获取颜色
            Color color;
            if (useThemeColor.getValue()) {
                // 使用主题颜色，如果没有就用红色
                color = new Color(0xFFFF0000);
            } else {
                color = Color.getHSBColor((boxColor.getValue() % 360) / 360.0f, 1.0f, 1.0f);
            }

            // 获取平滑位置
            Vec3 renderPos = getSmoothedPosition(event.getPartialTicks());

            // 创建 AABB 并偏移
            float size = target.getCollisionBorderSize();
            AxisAlignedBB aabb = new AxisAlignedBB(
                    renderPos.xCoord - (double) target.width / 2.0,
                    renderPos.yCoord,
                    renderPos.zCoord - (double) target.width / 2.0,
                    renderPos.xCoord + (double) target.width / 2.0,
                    renderPos.yCoord + (double) target.height,
                    renderPos.zCoord + (double) target.width / 2.0
            ).expand(size, size, size).offset(
                    -mc.getRenderManager().viewerPosX,
                    -mc.getRenderManager().viewerPosY,
                    -mc.getRenderManager().viewerPosZ
            );

            // 渲染状态设置
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

            // 绘制盒子
            RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());

            // 恢复渲染状态
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

            lastRenderedPos = renderPos;
        }
    }

    /**
     * 获取平滑的位置（使用历史位置进行插值）
     */
    private Vec3 getSmoothedPosition(float partialTicks) {
        if (positionHistory.isEmpty()) {
            return new Vec3(
                    lastVec3.xCoord + (vec3.xCoord - lastVec3.xCoord) * partialTicks,
                    lastVec3.yCoord + (vec3.yCoord - lastVec3.yCoord) * partialTicks,
                    lastVec3.zCoord + (vec3.zCoord - lastVec3.zCoord) * partialTicks
            );
        }

        // 使用历史位置进行加权平均
        double totalWeight = 0;
        double x = 0, y = 0, z = 0;

        for (int i = 0; i < positionHistory.size(); i++) {
            double weight = (i + 1) / (double) positionHistory.size();
            Vec3 pos = positionHistory.get(i);

            x += pos.xCoord * weight;
            y += pos.yCoord * weight;
            z += pos.zCoord * weight;
            totalWeight += weight;
        }

        // 添加当前位置（最高权重）
        double currentWeight = 1.5;
        x += vec3.xCoord * currentWeight;
        y += vec3.yCoord * currentWeight;
        z += vec3.zCoord * currentWeight;
        totalWeight += currentWeight;

        // 计算加权平均值
        x /= totalWeight;
        y /= totalWeight;
        z /= totalWeight;

        return new Vec3(x, y, z);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        // 关键修改：只有在模块开启时才处理数据包
        if (!moduleActive) {
            return;
        }

        if (event.isCancelled()) return;
        Packet<?> p = event.getPacket();

        // 接收包处理 (Server -> Client)
        if (event.getType() == EventType.RECEIVE) {
            try {
                if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) {
                    packetQueue.clear();
                    positionHistory.clear();
                    recentPositions.clear();
                    return;
                }

                if (target == null) {
                    return;
                }

                // 检查是否是我们目标的包 - 只拦截玩家位置相关的包
                boolean shouldIntercept = false;

                if (p instanceof S14PacketEntity) {
                    S14PacketEntity wrapper = (S14PacketEntity) p;
                    Entity entity = wrapper.getEntity(mc.theWorld);
                    if (entity != null && entity.getEntityId() == target.getEntityId()) {
                        vec3 = vec3.addVector(
                                wrapper.func_149062_c() / 32.0D,
                                wrapper.func_149061_d() / 32.0D,
                                wrapper.func_149064_e() / 32.0D
                        );
                        shouldIntercept = true;
                    }
                } else if (p instanceof S18PacketEntityTeleport) {
                    S18PacketEntityTeleport wrapper = (S18PacketEntityTeleport) p;
                    if (wrapper.getEntityId() == target.getEntityId()) {
                        vec3 = new Vec3(wrapper.getX() / 32.0D, wrapper.getY() / 32.0D, wrapper.getZ() / 32.0D);
                        shouldIntercept = true;
                    }
                } else if (p instanceof S13PacketDestroyEntities) {
                    S13PacketDestroyEntities wrapper = (S13PacketDestroyEntities) p;
                    for (int id : wrapper.getEntityIDs()) {
                        if (id == target.getEntityId()) {
                            target = null;
                            vec3 = lastVec3 = null;
                            positionHistory.clear();
                            recentPositions.clear();
                            releaseAllImmediately();
                            return;
                        }
                    }
                }

                // 只有目标玩家的位置包才拦截
                if (shouldIntercept) {
                    packetQueue.add(new TimedPacket(p, System.currentTimeMillis()));
                    event.setCancelled(true);
                }
            } catch (Exception ignored) {}
        }

        // 发送包处理 (Client -> Server)
        else if (event.getType() == EventType.SEND) {
            if (p instanceof C02PacketUseEntity) {
                C02PacketUseEntity wrapper = (C02PacketUseEntity) p;

                if (onlyCombat.getValue() && wrapper.getAction() != C02PacketUseEntity.Action.ATTACK)
                    return;

                Entity entity = wrapper.getEntityFromWorld(mc.theWorld);
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;

                    if (target != null && player.getEntityId() == target.getEntityId()) {
                        attackTicks = 0;
                        return;
                    }

                    target = player;
                    vec3 = lastVec3 = player.getPositionVector();
                    positionHistory.clear();
                    recentPositions.clear();
                    positionHistory.add(new Vec3(vec3.xCoord, vec3.yCoord, vec3.zCoord));
                    recentPositions.add(new Vec3(vec3.xCoord, vec3.yCoord, vec3.zCoord));
                    attackTicks = 0;
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        if (disableOnWorldChange.getValue() && moduleActive) {
            toggle();
        }
    }

    private void releaseAllImmediately() {
        if (!packetQueue.isEmpty()) {
            // 创建临时队列以避免并发修改异常
            Queue<TimedPacket> tempQueue = new ConcurrentLinkedQueue<>();
            tempQueue.addAll(packetQueue);
            packetQueue.clear();

            // 按顺序处理数据包
            while (!tempQueue.isEmpty()) {
                TimedPacket tp = tempQueue.poll();
                try {
                    processPacketLocallyImmediately(tp.packet);
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        }
    }

    private void processPacketLocallyImmediately(Packet<?> packet) {
        try {
            if (mc.theWorld == null || mc.getNetHandler() == null) {
                return;
            }

            // 直接发送数据包，不进行任何处理
            if (packet instanceof Packet) {
                Packet rawPacket = (Packet) packet;
                rawPacket.processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    private void processPacketLocally(Packet<?> packet) {
        try {
            if (mc.theWorld == null || mc.getNetHandler() == null) {
                return;
            }

            // 安全处理数据包
            if (packet instanceof Packet) {
                Packet rawPacket = (Packet) packet;
                rawPacket.processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // 内部类
    private static class TimedPacket {
        public final Packet<?> packet;
        public final long timestamp;
        public TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}