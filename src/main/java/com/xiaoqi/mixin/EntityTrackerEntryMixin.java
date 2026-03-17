package com.xiaoqi.mixin;

import com.xiaoqi.CarpetVisualFixSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
	@Shadow @Final private Entity entity;
	@Shadow @Final private ServerWorld world;
	
	@Inject(
		method = "tick",
		at = @At("HEAD"),
		cancellable = true
	)
	private void onTick(CallbackInfo ci) {
		if (!CarpetVisualFixSettings.entityRenderingFix) {
			return;
		}
		
		if (isInLazyChunk()) {
			ci.cancel();
		}
	}
	
	private boolean isInLazyChunk() {
		ChunkPos chunkPos = entity.getChunkPos();
		ThreadedAnvilChunkStorage chunkLoadingManager = world.getChunkManager().threadedAnvilChunkStorage;
		ChunkHolder chunkHolder = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).invokeGetCurrentChunkHolder(chunkPos.toLong());
		if (chunkHolder == null) {
			return false;
		}
		int level = chunkHolder.getLevel();
		ChunkLevelType levelType = ChunkLevels.getType(level);
		return levelType == ChunkLevelType.INACCESSIBLE;
	}
}
