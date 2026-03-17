package com.xiaoqi;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public class CarpetVisualFixSettings {
	@Rule(
		categories = {RuleCategory.CLIENT}
	)
	public static boolean entityRenderingFix = false;
}
