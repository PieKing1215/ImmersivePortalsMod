package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationServer;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdRecord;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.network.ImplRemoteProcedureCall;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.UUID;

public class MyNetwork {
    public static final Identifier id_stcRedirected =
        new Identifier("imm_ptl", "rd");
    public static final Identifier id_stcDimSync =
        new Identifier("imm_ptl", "dim_sync");
    public static final Identifier id_ctsTeleport =
        new Identifier("imm_ptl", "teleport");
    public static final Identifier id_stcCustom =
        new Identifier("imm_ptl", "stc_custom");
    public static final Identifier id_stcSpawnEntity =
        new Identifier("imm_ptl", "spawn_entity");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("imm_ptl", "dim_confirm");
    public static final Identifier id_stcUpdateGlobalPortal =
        new Identifier("imm_ptl", "upd_glb_ptl");
    public static final Identifier id_ctsPlayerAction =
        new Identifier("imm_ptl", "player_action");
    public static final Identifier id_ctsRightClick =
        new Identifier("imm_ptl", "right_click");
    public static final Identifier id_stcRemote =
        new Identifier("imm_ptl", "remote_stc");
    public static final Identifier id_ctsRemote =
        new Identifier("imm_ptl", "remote_cts");
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsTeleport,
            (server, player, handler, buf, responseSender) -> {
                processCtsTeleport(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsPlayerAction,
            (server, player, handler, buf, responseSender) -> {
                processCtsPlayerAction(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsRightClick,
            (server, player, handler, buf, responseSender) -> {
                processCtsRightClick(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsRemote,
            (server, player, handler, buf, responseSender) -> {
                McHelper.executeOnServerThread(
                    ImplRemoteProcedureCall.serverReadFunctionAndArguments(player, buf)
                );
            }
        );
        
    }
    
    public static Packet createRedirectedMessage(
        RegistryKey<World> dimension,
        Packet packet
    ) {
        int messageType = 0;
        try {
            messageType = NetworkState.PLAY.getPacketId(NetworkSide.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, dimension, false);
        
        buf.writeInt(messageType);
        
        try {
            packet.write(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        return new CustomPayloadS2CPacket(id_stcRedirected, buf);
    }
    
    public static Packet createDimSync() {
        Validate.notNull(DimensionIdRecord.serverRecord);
        
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        CompoundTag idMapTag = DimensionIdRecord.recordToTag(DimensionIdRecord.serverRecord);
        buf.writeCompoundTag(idMapTag);
        
        CompoundTag typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo();
        buf.writeCompoundTag(typeMapTag);
        
        return new CustomPayloadS2CPacket(id_stcDimSync, buf);
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        Packet packet
    ) {
        player.networkHandler.sendPacket(createRedirectedMessage(dimension, packet));
    }
    
    public static Packet createStcDimensionConfirm(
        RegistryKey<World> dimensionType,
        Vec3d pos
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionType, false);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new CustomPayloadS2CPacket(id_stcDimensionConfirm, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static Packet createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getEntityId());
        DimId.writeWorldId(
            buf, entity.world.getRegistryKey(),
            entity.world.isClient
        );
        CompoundTag tag = new CompoundTag();
        entity.toTag(tag);
        buf.writeCompoundTag(tag);
        return new CustomPayloadS2CPacket(id_stcSpawnEntity, buf);
    }
    
    public static Packet createGlobalPortalUpdate(
        GlobalPortalStorage storage
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, storage.world.get().getRegistryKey(), false);
        buf.writeCompoundTag(storage.toTag(new CompoundTag()));
        
        return new CustomPayloadS2CPacket(id_stcUpdateGlobalPortal, buf);
    }
    
    private static void processCtsTeleport(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        UUID portalEntityId = buf.readUuid();
        
        McHelper.executeOnServerThread(() -> {
            Global.serverTeleportationManager.onPlayerTeleportedInClient(
                player,
                dim,
                posBefore,
                portalEntityId
            );
        });
    }
    
    private static void processCtsPlayerAction(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket();
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ModMain.serverTaskList.addTask(() -> {
            BlockManipulationServer.processBreakBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
    private static void processCtsRightClick(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket();
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ModMain.serverTaskList.addTask(() -> {
            BlockManipulationServer.processRightClickBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
}
