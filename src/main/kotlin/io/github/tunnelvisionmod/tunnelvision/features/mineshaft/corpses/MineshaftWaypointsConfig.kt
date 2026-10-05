package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftWaypointsConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Show waypoints for corpses and the fossil.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Corpse Spots", desc = "Mark every spot a corpse can spawn. Found corpses get colored. A spot disappears once checked, a corpse once looted.")
	@ConfigEditorBoolean
	var corpseSpots = true

	@Expose
	@JvmField
	@ConfigOption(name = "Remove When To-Dos Done", desc = "Hide all corpse waypoints once your to-dos are done. §7Uses §eLoot§7.")
	@ConfigEditorBoolean
	var removeWhenTodosDone = true

	@Expose
	@JvmField
	@ConfigOption(name = "Fossil", desc = "Mark the fossil and outline it until the last block is mined.")
	@ConfigEditorBoolean
	var fossil = true

	@Expose
	@JvmField
	@ConfigOption(name = "Labels", desc = "Show a name and distance above each waypoint.")
	@ConfigEditorBoolean
	var labels = true
}
