package com.skybender.anglesnap.client;

/**
 * Passiveively stored payload settings from /skybend set.
 */
public final class SkybendSettings {
	private static boolean sizeSet;
	private static boolean originSet;
	private static int nukeSize;
	private static int originX;
	private static int originZ;

	private SkybendSettings() {
	}

	public static void set(int nukeSize, int originX, int originZ) {
		SkybendSettings.nukeSize = nukeSize;
		SkybendSettings.originX = originX;
		SkybendSettings.originZ = originZ;
		sizeSet = true;
		originSet = true;
		SkybendConfig.save();
	}

	public static void setSize(int nukeSize) {
		SkybendSettings.nukeSize = nukeSize;
		sizeSet = true;
		SkybendConfig.save();
	}

	public static void setOrigin(int originX, int originZ) {
		SkybendSettings.originX = originX;
		SkybendSettings.originZ = originZ;
		originSet = true;
		SkybendConfig.save();
	}

	public static boolean isSet() {
		return sizeSet && originSet;
	}

	public static boolean hasSize() {
		return sizeSet;
	}

	public static boolean hasOrigin() {
		return originSet;
	}

	static void reset() {
		sizeSet = false;
		originSet = false;
		nukeSize = 0;
		originX = 0;
		originZ = 0;
	}

	public static int nukeSize() {
		return nukeSize;
	}

	public static int originX() {
		return originX;
	}

	public static int originZ() {
		return originZ;
	}
}
