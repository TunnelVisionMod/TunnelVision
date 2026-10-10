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

	/** Corpse profit per [io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType] name. */
	@field:Expose
	var corpseProfitCoins: MutableMap<String, Double> = mutableMapOf()

	@field:Expose
	var corpseProfitCorpses: MutableMap<String, Int> = mutableMapOf()

	/** Frozen Corpse RNG meter XP since its last payout, so the payout is not counted twice. */
	@field:Expose
	var corpseMeterXp: Double = 0.0

	/** False until the RNG Meter menu has been read, and again after every Shattered Locket drop. */
	@field:Expose
	var corpseMeterSynced: Boolean = false

	/** Whether the meter is set to the Shattered Locket, null until the RNG Meter menu has said. */
	@field:Expose
	var corpseMeterLocketSelected: Boolean? = null
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
