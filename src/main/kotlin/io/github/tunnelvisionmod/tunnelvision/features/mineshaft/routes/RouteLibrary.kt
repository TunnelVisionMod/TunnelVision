package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import net.fabricmc.loader.api.FabricLoader

object RouteLibrary {
	private const val RESOURCE = "/assets/tunnelvision/routes/"

	private val overrides get() = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/routes").toFile()
	private val cache = mutableMapOf<String, GemstoneRoute?>()

	val names: List<String>
		get() = (MineshaftType.entries.map { it.code } + RouteChoice.extraNames).filter { exists(it) }.distinct().sorted()

	fun exists(name: String): Boolean = override(name) != null || RouteLibrary::class.java.getResource("$RESOURCE$name.json") != null

	fun load(name: String): GemstoneRoute? {
		override(name)?.let { return GemstoneRoute.parse(it.readText()) }
		return cache.getOrPut(name) {
			RouteLibrary::class.java.getResourceAsStream("$RESOURCE$name.json")?.reader()?.use { GemstoneRoute.parse(it.readText()) }
		}
	}

	private fun override(name: String) =
		overrides.resolve("$name.json").takeIf { ConfigManager.config.dev.debugMode && it.isFile }
}
