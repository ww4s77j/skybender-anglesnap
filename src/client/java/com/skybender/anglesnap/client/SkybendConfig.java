package com.skybender.anglesnap.client;

import com.skybender.anglesnap.DialLayout;
import com.skybender.anglesnap.SkybenderAnglesnap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;

final class SkybendConfig {
	private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
	private static final Path LEGACY_FILE = CONFIG_DIR.resolve("skybender-anglesnap.properties");
	private static final Path PROFILE_DIR = CONFIG_DIR.resolve("skybender-anglesnap").resolve("profiles");
	private static final Path MIGRATION_MARKER = CONFIG_DIR.resolve("skybender-anglesnap").resolve("profiles-migrated");
	private static Path activeFile;
	private static String activeIdentity;
	private static String activeIdentityKey;
	private static boolean loading;
	private record ProfileIdentity(String stableKey, String exactIdentity, String displayName) {
	}

	private SkybendConfig() {
	}

	static void activate(Minecraft client) {
		save();
		loading = true;
		ProfileIdentity identity = profileIdentity(client);
		activeFile = identity == null ? null : PROFILE_DIR.resolve(safeFileName(identity.displayName()) + ".properties");
		activeIdentity = identity == null ? null : identity.exactIdentity();
		activeIdentityKey = identity == null ? null : identity.stableKey();
		resetSettings();
		if (activeFile == null) {
			loading = false;
			SkybenderAnglesnap.LOGGER.warn("Could not identify the current world or server; using temporary default settings");
			return;
		}

		Properties properties = new Properties();
		boolean legacyProfile = false;
		if (Files.exists(activeFile)) {
			properties = read(activeFile);
			if (!identity.stableKey().equals(properties.getProperty("profile.key"))) {
				SkybenderAnglesnap.LOGGER.warn("Profile identity in {} does not match the current world/server; ignoring it", activeFile);
				activeFile = null;
				activeIdentity = null;
				activeIdentityKey = null;
				loading = false;
				return;
			}
		} else {
			Path hashedFile = PROFILE_DIR.resolve(digest(identity.stableKey()) + ".properties");
			if (Files.exists(hashedFile)) {
				properties = read(hashedFile);
				legacyProfile = true;
			} else if (!Files.exists(MIGRATION_MARKER) && Files.exists(LEGACY_FILE)) {
				properties = read(LEGACY_FILE);
				legacyProfile = true;
			}
		}
		apply(properties);
		loading = false;
		save();
		markMigrationComplete();
		if (legacyProfile) {
			SkybenderAnglesnap.LOGGER.info("Migrated existing settings to readable profile {}", activeFile.getFileName());
		}
	}

	static void deactivate() {
		save();
		loading = true;
		activeFile = null;
		activeIdentity = null;
		activeIdentityKey = null;
		resetSettings();
		loading = false;
	}

	static void save() {
		if (loading || activeFile == null) {
			return;
		}

		Properties properties = new Properties();
		properties.setProperty("profile.identity", activeIdentity);
		properties.setProperty("profile.key", activeIdentityKey);
		properties.setProperty("skybend.configured", Boolean.toString(SkybendSettings.isSet()));
		properties.setProperty("skybend.sizeConfigured", Boolean.toString(SkybendSettings.hasSize()));
		properties.setProperty("skybend.originConfigured", Boolean.toString(SkybendSettings.hasOrigin()));
		properties.setProperty("skybend.size", Integer.toString(SkybendSettings.nukeSize()));
		properties.setProperty("skybend.originX", Integer.toString(SkybendSettings.originX()));
		properties.setProperty("skybend.originZ", Integer.toString(SkybendSettings.originZ()));
		properties.setProperty("overlay.enabled", Boolean.toString(DialOptions.enabled()));
		properties.setProperty("overlay.radius", Float.toString(DialOptions.radius()));
		properties.setProperty("overlay.scale", Float.toString(DialOptions.scale()));
		properties.setProperty("tps.override", Boolean.toString(ServerTps.isOverridden()));
		if (ServerTps.isOverridden()) {
			properties.setProperty("tps.value", Double.toString(ServerTps.tps()));
		}

		try {
			Files.createDirectories(activeFile.getParent());
			try (OutputStream output = Files.newOutputStream(activeFile)) {
				properties.store(output, "Skybender AngleSnap profile settings");
			}
		} catch (IOException exception) {
			SkybenderAnglesnap.LOGGER.warn("Could not save profile settings to {}", activeFile, exception);
		}
	}

	private static ProfileIdentity profileIdentity(Minecraft client) {
		String identity;
		String exactIdentity;
		String displayName;
		MinecraftServer integratedServer = client.getSingleplayerServer();
		if (integratedServer != null) {
			Path worldPath = integratedServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			identity = "singleplayer:" + worldPath;
			exactIdentity = worldPath.toString();
			Path worldName = worldPath.getFileName();
			displayName = "singleplayer-" + (worldName == null ? "world" : worldName);
		} else {
			ServerData server = client.getCurrentServer();
			if (server == null || server.ip == null || server.ip.isBlank()) {
				return null;
			}
			exactIdentity = server.ip.trim();
			identity = "multiplayer:" + exactIdentity.toLowerCase(Locale.ROOT);
			displayName = "multiplayer-" + exactIdentity;
		}
		return new ProfileIdentity(identity, exactIdentity, displayName);
	}

	private static void apply(Properties properties) {
		boolean legacyConfigured = Boolean.parseBoolean(properties.getProperty("skybend.configured", "false"));
		boolean sizeConfigured = Boolean.parseBoolean(properties.getProperty("skybend.sizeConfigured", Boolean.toString(legacyConfigured)));
		boolean originConfigured = Boolean.parseBoolean(properties.getProperty("skybend.originConfigured", Boolean.toString(legacyConfigured)));
		int size = integer(properties, "skybend.size", -1);
		if (sizeConfigured && size >= 1 && size <= 15) {
			SkybendSettings.setSize(size);
		}
		if (originConfigured) {
			SkybendSettings.setOrigin(
				integer(properties, "skybend.originX", 0),
				integer(properties, "skybend.originZ", 0));
		}

		DialOptions.setEnabled(Boolean.parseBoolean(properties.getProperty("overlay.enabled", "false")));
		DialOptions.setRadius(decimal(properties, "overlay.radius", DialLayout.DEFAULT_RADIUS));
		DialOptions.setScale(decimal(properties, "overlay.scale", 1.0f));
		if (Boolean.parseBoolean(properties.getProperty("tps.override", "false"))) {
			ServerTps.setOverride(decimal(properties, "tps.value", 20.0));
		}
	}

	private static void resetSettings() {
		SkybendSettings.reset();
		DialOptions.reset();
		ServerTps.clearOverride();
	}

	private static Properties read(Path file) {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		} catch (IOException exception) {
			SkybenderAnglesnap.LOGGER.warn("Could not read profile settings from {}", file, exception);
		}
		return properties;
	}

	private static void markMigrationComplete() {
		try {
			Files.createDirectories(MIGRATION_MARKER.getParent());
			Files.writeString(MIGRATION_MARKER, "complete", StandardCharsets.UTF_8);
		} catch (IOException exception) {
			SkybenderAnglesnap.LOGGER.warn("Could not write profile migration marker {}", MIGRATION_MARKER, exception);
		}
	}

	private static String digest(String identity) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static String safeFileName(String name) {
		StringBuilder safeName = new StringBuilder();
		for (byte value : name.getBytes(StandardCharsets.UTF_8)) {
			int character = value & 0xFF;
			if (character >= 'a' && character <= 'z'
				|| character >= 'A' && character <= 'Z'
				|| character >= '0' && character <= '9'
				|| character == ' ' || character == '.' || character == '_' || character == '-') {
				safeName.append((char) character);
			} else {
				safeName.append('%')
					.append("0123456789ABCDEF".charAt(character >>> 4))
					.append("0123456789ABCDEF".charAt(character & 0x0F));
			}
		}
		if (safeName.length() > 180) {
			return safeName.substring(0, 140) + "-" + digest(name).substring(0, 24);
		}
		return safeName.toString();
	}

	private static int integer(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static float decimal(Properties properties, String key, float fallback) {
		try {
			return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static double decimal(Properties properties, String key, double fallback) {
		try {
			return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}
}