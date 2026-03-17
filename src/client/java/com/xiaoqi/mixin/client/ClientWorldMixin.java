package com.xiaoqi.mixin.client;

import com.xiaoqi.CarpetVisualFixSettings;
import com.xiaoqi.DebugLogger;
import com.xiaoqi.LazyChunkHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
	@Inject(
		method = "tickEntity",
		at = @At("HEAD"),
		cancellable = true
	)
	private void onTickEntity(Entity entity, CallbackInfo ci) {
		if (entity instanceof PlayerEntity) {
			return;
		}
		
		boolean ruleEnabled = CarpetVisualFixSettings.entityRenderingFix;
		boolean inLazyChunk = LazyChunkHelper.isInLazyChunk(entity);
		
		if (entity instanceof TntEntity) {
			DebugLogger.log("TNT tick - RuleEnabled: " + ruleEnabled + ", InLazyChunk: " + inLazyChunk + 
				", Pos: " + entity.getX() + ", " + entity.getY() + ", " + entity.getZ() +
				", Chunk: " + entity.getChunkPos().x + "," + entity.getChunkPos().z);
		}
		
		if (!ruleEnabled) {
			return;
		}
		
		if (inLazyChunk) {
			DebugLogger.log("CANCELLED tick for: " + entity.getType().toString());
			ci.cancel();
		}
	}
}
