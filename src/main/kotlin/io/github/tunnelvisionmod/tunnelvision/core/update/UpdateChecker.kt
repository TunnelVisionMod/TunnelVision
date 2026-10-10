package io.github.tunnelvisionmod.tunnelvision.core.update

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** A published release that carries a jar for this Minecraft version, and its page on GitHub. */
data class Release(val version: Version, val pageUrl: String)

/**
 * Reads GitHub's "latest release" answer. Drafts and prereleases never count, and a release only
 * counts if it carries the jar for this Minecraft version, so nobody is told about an update they
 * cannot install.
 */
object UpdateChecker {
	const val REPO = "TunnelVisionMod/TunnelVision"
	const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
	private const val PAGE_PREFIX = "https://github.com/$REPO/releases/"

	fun jarName(version: Version, minecraft: String) = "TunnelVision-v$version-$minecraft.jar"

	fun parseLatest(json: String, minecraft: String): Release? {
		val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
		if (root.bool("draft") || root.bool("prerelease")) return null
		val version = root.get("tag_name")?.asString?.let(Version::parse) ?: return null
		val page = root.get("html_url")?.asString?.takeIf { it.startsWith(PAGE_PREFIX) } ?: return null
		val jarName = jarName(version, minecraft)
		val hasJar = root.getAsJsonArray("assets")?.any { it.asJsonObject.get("name")?.asString == jarName } == true
		return Release(version, page).takeIf { hasJar }
	}

	/** The release if it is strictly newer than what is running - never a downgrade. */
	fun newer(release: Release?, current: Version?): Release? =
		release?.takeIf { current == null || it.version > current }

	private fun JsonObject.bool(name: String) = get(name)?.takeIf { it.isJsonPrimitive }?.asBoolean == true
}
