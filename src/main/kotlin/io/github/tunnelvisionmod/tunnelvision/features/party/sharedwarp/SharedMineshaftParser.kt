package io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType

data class SharedMineshaft(val type: MineshaftType, val corpses: String?)

object SharedMineshaftParser {
	private val shareText = Regex("""Mineshafttype: (\w+)(?:, Corpses: (.+))?$""")

	fun parse(message: String): SharedMineshaft? {
		val match = shareText.find(message.trim()) ?: return null
		val type = MineshaftType.entries.firstOrNull { it.code == match.groupValues[1] } ?: return null
		return SharedMineshaft(type, match.groupValues[2].takeIf { it.isNotEmpty() })
	}
}
