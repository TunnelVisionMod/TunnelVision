package io.github.tunnelvisionmod.tunnelvision.features.party.commands

enum class PartyCommand(private val aliases: Set<String>, private val usableBySelf: Boolean) {
	TRANSFER(setOf("pt", "ptme"), usableBySelf = false),
	WARP(setOf("w", "warp"), usableBySelf = true);

	fun canBeUsedBy(author: String, ownName: String): Boolean = usableBySelf || !author.equals(ownName, ignoreCase = true)

	fun hypixelCommand(author: String): String = when (this) {
		TRANSFER -> "party transfer $author"
		WARP -> "p warp"
	}

	companion object {
		private const val PREFIX = "!"

		fun parse(message: String): PartyCommand? {
			val label = message.trim().substringBefore(' ')
			if (!label.startsWith(PREFIX)) return null
			val name = label.removePrefix(PREFIX).lowercase()
			return entries.firstOrNull { name in it.aliases }
		}
	}
}
