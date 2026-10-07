package io.github.tunnelvisionmod.tunnelvision.core.update

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** A published release with the jar for this Minecraft version and its signature. */
data class Release(val version: Version, val jarName: String, val jarUrl: String, val signatureUrl: String)

/**
 * Reads GitHub's "latest release" answer. Drafts and prereleases never count, and a release only
 * counts if it carries both the jar for this Minecraft version and its `.sig`, downloaded from our
 * own repository.
 */
object UpdateChecker {
	const val REPO = "TunnelVisionMod/TunnelVision"
	const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
	private const val DOWNLOAD_PREFIX = "https://github.com/$REPO/releases/download/"

	fun jarName(version: Version, minecraft: String) = "TunnelVision-v$version-$minecraft.jar"

	fun parseLatest(json: String, minecraft: String): Release? {
		val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
		if (root.bool("draft") || root.bool("prerelease")) return null
		val version = root.get("tag_name")?.asString?.let(Version::parse) ?: return null
		val jarName = jarName(version, minecraft)
		val assets = root.getAsJsonArray("assets")?.mapNotNull { asset ->
			val obj = asset.asJsonObject
			val name = obj.get("name")?.asString ?: return@mapNotNull null
			val url = obj.get("browser_download_url")?.asString ?: return@mapNotNull null
			name to url
		}?.toMap() ?: return null
		val jarUrl = assets[jarName]?.takeIf { it.startsWith(DOWNLOAD_PREFIX) } ?: return null
		val signatureUrl = assets["$jarName.sig"]?.takeIf { it.startsWith(DOWNLOAD_PREFIX) } ?: return null
		return Release(version, jarName, jarUrl, signatureUrl)
	}

	/** The release if it is strictly newer than what is running - never a downgrade. */
	fun newer(release: Release?, current: Version?): Release? =
		release?.takeIf { current == null || it.version > current }

	private fun JsonObject.bool(name: String) = get(name)?.takeIf { it.isJsonPrimitive }?.asBoolean == true
}
