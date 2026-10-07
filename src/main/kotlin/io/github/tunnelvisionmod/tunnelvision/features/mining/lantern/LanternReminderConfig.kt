package io.github.tunnelvisionmod.tunnelvision.features.mining.lantern

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LanternReminderConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Remind you to place your lantern and warn when it expires.")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Entry Reminder", desc = "Remind you to place your lantern when you enter.")
	@ConfigEditorBoolean
	var entryReminder = true

	@Expose
	@JvmField
	@ConfigOption(name = "Expired Alert", desc = "Show a title when your lantern despawns.")
	@ConfigEditorBoolean
	var expiredAlert = true

}
