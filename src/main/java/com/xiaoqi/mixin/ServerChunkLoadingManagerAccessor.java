package com.xiaoqi.mixin;

import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.server.world.ChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ServerChunkLoadingManagerAccessor {
	@Invoker("getCurrentChunkHolder")
	ChunkHolder invokeGetCurrentChunkHolder(long pos);
}
