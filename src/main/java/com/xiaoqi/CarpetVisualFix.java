package com.xiaoqi;

import java.util.Map;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import net.fabricmc.api.ModInitializer;

public class CarpetVisualFix implements ModInitializer, CarpetExtension {
	public static final String MOD_ID = "carpet-visualfix";
	public static final String VERSION = "1.0.0";

	@Override
	public void onInitialize() {
		CarpetServer.manageExtension(this);
	}

	@Override
	public void onGameStarted() {
		CarpetServer.settingsManager.parseSettingsClass(CarpetVisualFixSettings.class);
	}

	@Override
	public String version() {
		return MOD_ID;
	}

	@Override
	public Map<String, String> canHasTranslations(String lang) {
		if ("zh_cn".equals(lang)) {
			return Map.of(
				"carpet.category.client", "客户端",
				"carpet.rule.entityRenderingFix.desc", "修复了弱加载区域内实体的动画"
			);
		}
		return Map.of(
			"carpet.category.client", "client",
			"carpet.rule.entityRenderingFix.desc", "Fixes entity animations in lazy-loaded chunks"
		);
	}
}
