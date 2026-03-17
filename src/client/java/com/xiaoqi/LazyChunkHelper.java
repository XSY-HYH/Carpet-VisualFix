package com.xiaoqi;

import carpet.CarpetSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public class LazyChunkHelper {
	public static boolean isInLazyChunk(Entity entity) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		ClientPlayerEntity player = client.player;
		
		if (world == null || player == null || entity.getWorld() != world) {
			return false;
		}
		
		ChunkPos entityChunkPos = entity.getChunkPos();
		ChunkPos playerChunkPos = player.getChunkPos();
		
		int simulationDistance = getSimulationDistance(world);
		
		int dx = Math.abs(entityChunkPos.x - playerChunkPos.x);
		int dz = Math.abs(entityChunkPos.z - playerChunkPos.z);
		
		boolean inSimulationDistance = dx <= simulationDistance && dz <= simulationDistance;
		
		WorldChunk chunk = world.getChunkManager().getWorldChunk(entityChunkPos.x, entityChunkPos.z);
		boolean chunkLoaded = chunk != null;
		
		boolean playerCanLoadChunks = canPlayerLoadChunks(player);
		boolean result = (!inSimulationDistance || !playerCanLoadChunks) && chunkLoaded;
		
		if (entity instanceof net.minecraft.entity.TntEntity) {
			DebugLogger.log("LazyChunkHelper - EntityChunk: " + entityChunkPos.x + "," + entityChunkPos.z +
				", PlayerChunk: " + playerChunkPos.x + "," + playerChunkPos.z +
				", dx: " + dx + ", dz: " + dz +
				", SimDist: " + simulationDistance +
				", InSimDist: " + inSimulationDistance +
				", ChunkLoaded: " + chunkLoaded +
				", PlayerCanLoadChunks: " + playerCanLoadChunks +
				", Result: " + result);
		}
		
		return result;
	}
	
	private static boolean canPlayerLoadChunks(ClientPlayerEntity player) {
		if (player.isCreative() && !CarpetSettings.creativePlayersLoadChunks) {
			return false;
		}
		return true;
	}
	
	private static int getSimulationDistance(ClientWorld world) {
		return MinecraftClient.getInstance().options.getSimulationDistance().getValue();
	}
}
