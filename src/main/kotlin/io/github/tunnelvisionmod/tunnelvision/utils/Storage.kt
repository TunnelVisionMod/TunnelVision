package io.github.tunnelvisionmod.tunnelvision.utils

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.managed.ManagedDataFile
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import net.fabricmc.loader.api.FabricLoader

class StorageData {
	@field:Expose
	var skyMallBuff: String? = null

	@field:Expose
	var skyMallDay: Long = -1

	@field:Expose
	var forgeFull: Boolean? = null

	@field:Expose
	var coldResistancePeaks: MutableList<Double> = mutableListOf()
}

object Storage {
	private lateinit var file: ManagedDataFile<StorageData>

	val data: StorageData get() = file.instance

	fun load() {
		val path = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/storage.json").toFile()
		file = ManagedDataFile.create(path, StorageData::class.java) {
			loadFailed = { _, e -> TunnelVision.logger.error("Failed to load storage", e) }
			saveFailed = { _, e -> TunnelVision.logger.error("Failed to save storage", e) }
		}
	}

	fun save() = file.saveToFile()
}
