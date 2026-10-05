package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

object ForgeSummary {
	fun grouped(items: List<String>): List<String> =
		items.groupingBy { it }.eachCount().map { (item, count) -> if (count > 1) "$item ×$count" else item }

	fun subtitle(items: List<String>): String {
		val groups = grouped(items)
		return if (groups.size == 1) groups.single() else "${items.size} items ready"
	}
}
