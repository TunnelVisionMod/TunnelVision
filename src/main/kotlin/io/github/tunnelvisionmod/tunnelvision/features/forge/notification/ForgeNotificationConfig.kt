package io.github.tunnelvisionmod.tunnelvision.features.forge.notification

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForgeNotificationConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Enabled", desc = "Notify you when something is done forging. §bRequires the Forges tab widget (/widget).")
	@ConfigEditorBoolean
	var enabled = false

	@Expose
	@JvmField
	@ConfigOption(name = "Ready Title", desc = "Show a title with the finished items.")
	@ConfigEditorBoolean
	var showTitle = true

	@Expose
	@JvmField
	@ConfigOption(name = "Chat Message", desc = "Send a chat message listing the finished items.")
	@ConfigEditorBoolean
	var sendChat = true

}
