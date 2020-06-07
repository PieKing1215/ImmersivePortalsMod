package com.qouteall.immersive_portals.ducks;

import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.chunk.ChunkManager;

public interface IEWorld {
    void setChunkManager(ChunkManager manager);
    
    MutableWorldProperties myGetProperties();
}
