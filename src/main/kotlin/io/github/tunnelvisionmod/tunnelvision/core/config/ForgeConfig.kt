package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.tunnelvisionmod.tunnelvision.features.forge.crystals.CrystalNotificationsConfig
import io.github.tunnelvisionmod.tunnelvision.features.forge.notification.ForgeNotificationConfig

class ForgeConfig {
	@Expose
	@JvmField
	@ConfigOption(name = "Forge Notification", desc = "Get notified when something is done forging.")
	@Accordion
	var forgeNotification = ForgeNotificationConfig()

	@Expose
	@JvmField
	@ConfigOption(name = "Crystal Notifications", desc = "Notify you when a crystal is waiting to be forged.")
	@Accordion
	var crystalNotifications = CrystalNotificationsConfig()
}
