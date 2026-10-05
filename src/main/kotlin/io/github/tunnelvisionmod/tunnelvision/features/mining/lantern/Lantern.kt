package io.github.tunnelvisionmod.tunnelvision.features.mining.lantern

enum class Lantern(val itemName: String) {
	DWARVEN("Dwarven Lantern"),
	MITHRIL("Mithril Lantern"),
	TITANIUM("Titanium Lantern"),
	GLACITE("Glacite Lantern"),
	WILL_O_WISP("Will-o'-wisp");

	companion object {
		private val despawnMessage = Regex("""^Your (.+) despawned\.$""")

		fun strongest(itemNames: List<String>): Lantern? = entries.lastOrNull { it.itemName in itemNames }

		fun fromDespawnMessage(message: String): Lantern? {
			val name = despawnMessage.matchEntire(message.trim())?.groupValues?.get(1) ?: return null
			return entries.firstOrNull { it.itemName == name }
		}
	}
}
