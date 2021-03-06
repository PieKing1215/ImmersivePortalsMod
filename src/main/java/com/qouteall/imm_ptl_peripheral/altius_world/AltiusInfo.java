package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.api.PortalAPI;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.List;

public class AltiusInfo {
    
    public final boolean loop;
    public final List<AltiusEntry> entries;
    
    public AltiusInfo(List<AltiusEntry> entries, boolean loop) {
        this.entries = entries;
        this.loop = loop;
    }
    
    public static void initializeFuseViewProperty(Portal portal) {
        if (portal.world.getDimension().hasSkyLight()) {
            if (portal.getNormal().y < 0) {
                portal.fuseView = true;
            }
        }
    }
    
    public static void createConnectionBetween(
        AltiusEntry a, AltiusEntry b
    ) {
        ServerWorld fromWorld = McHelper.getServerWorld(a.dimension);
        
        ServerWorld toWorld = McHelper.getServerWorld(b.dimension);
        
        boolean xorFlipped = a.flipped ^ b.flipped;
        
        VerticalConnectingPortal connectingPortal = VerticalConnectingPortal.createConnectingPortal(
            fromWorld,
            a.flipped ? VerticalConnectingPortal.ConnectorType.ceil :
                VerticalConnectingPortal.ConnectorType.floor,
            toWorld,
            b.scale / a.scale,
            xorFlipped,
            b.horizontalRotation - a.horizontalRotation
        );
        
        VerticalConnectingPortal reverse = PortalAPI.createReversePortal(connectingPortal);
        
        initializeFuseViewProperty(connectingPortal);
        initializeFuseViewProperty(reverse);
        
        PortalAPI.addGlobalPortal(fromWorld, connectingPortal);
        PortalAPI.addGlobalPortal(toWorld, reverse);
    }
    
    public void createPortals() {
        
        if (entries.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(new LiteralText(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        if (!McHelper.getGlobalPortals(McHelper.getServerWorld(entries.get(0).dimension)).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            entries.stream(),
            (before, after) -> {
                createConnectionBetween(before, after);
                return null;
            }
        ).forEach(k -> {
        });
        
        if (loop) {
            createConnectionBetween(entries.get(entries.size() - 1), entries.get(0));
        }
        
        McHelper.sendMessageToFirstLoggedPlayer(
            new TranslatableText("imm_ptl.dim_stack_initialized")
        );
    }
    
}
