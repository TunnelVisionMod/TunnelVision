package io.github.tunnelvisionmod.tunnelvision.core.config

import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ConfigManager {
	private lateinit var managed: ManagedConfig<TunnelVisionConfig>

	val config: TunnelVisionConfig get() = managed.instance

	fun load() {
		val file = FabricLoader.getInstance().configDir.resolve("${TunnelVision.MOD_ID}/config.json").toFile()
		managed = ManagedConfig.create(file, TunnelVisionConfig::class.java) {
			loadFailed = { _, e -> TunnelVision.logger.error("Failed to load config", e) }
			saveFailed = { _, e -> TunnelVision.logger.error("Failed to save config", e) }
		}
	}

	fun save() = managed.saveToFile()

	fun createScreen(parent: Screen?): Screen =
		object : MoulConfigScreenComponent(Component.literal("TunnelVision"), GuiContext(GuiElementComponent(managed.getEditor())), parent) {
			override fun removed() {
				super.removed()
				save()
			}
		}

	fun openScreen() {
		TunnelVision.mc.schedule { Compat.setScreen(createScreen(null)) }
	}
}
