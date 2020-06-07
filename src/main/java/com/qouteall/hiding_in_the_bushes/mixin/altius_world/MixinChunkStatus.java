package com.qouteall.hiding_in_the_bushes.mixin.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.structure.StructureManager;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    //vanilla feature generation is not thread safe
    
    private static ReentrantLock featureGenLock;
    
    @Redirect(
        method = "method_12151",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;generateFeatures(Lnet/minecraft/world/ChunkRegion;Lnet/minecraft/world/gen/StructureAccessor;)V"
        )
    )
    private static void redirectGenerateFeatures(
        ChunkGenerator chunkGenerator,
        ChunkRegion chunkRegion,
        StructureAccessor accessor
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            chunkGenerator.generateFeatures(chunkRegion,accessor);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s %d %d",
                chunkRegion.getWorld().getRegistryKey(),
                chunkRegion.getCenterChunkX(),
                chunkRegion.getCenterChunkZ()
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "method_16556",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;setStructureStarts(Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureManager;J)V"
        )
    )
    private static void redirectSetStructureStarts(
        StructureAccessor accessor,
        BiomeAccess biomeAccess,
        Chunk chunk,
        ChunkGenerator generator,
        StructureManager manager,
        long seed
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            generator.setStructureStarts(
                accessor, biomeAccess, chunk, generator, manager, seed
            );
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "method_16565",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;addStructureReferences(Lnet/minecraft/world/IWorld;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V"
        )
    )
    private static void redirectAddStructureReference(
        ChunkGenerator chunkGenerator, WorldAccess world, StructureAccessor structureAccessor, Chunk chunk
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            chunkGenerator.addStructureReferences(world, structureAccessor, chunk);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    private static boolean getShouldLock() {
        return AltiusInfo.isAltius();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
