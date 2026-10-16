package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.SerializedName

enum class LootMode(private val label: String) {
	LAPIS_ONLY("Lapis Only"),

	/** Saved as `NORMAL` before the rename, which would otherwise load as null. */
	@SerializedName(value = "ADAPTIVE", alternate = ["NORMAL"])
	ADAPTIVE("Adaptive"),

	@SerializedName(value = "CRYSTAL_HUNTER", alternate = ["GREEDY"])
	CRYSTAL_HUNTER("Crystal Hunter");

	override fun toString() = label
}
