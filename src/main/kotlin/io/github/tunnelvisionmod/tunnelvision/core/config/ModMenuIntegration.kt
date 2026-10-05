package io.github.tunnelvisionmod.tunnelvision.core.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

object ModMenuIntegration : ModMenuApi {
	override fun getModConfigScreenFactory() = ConfigScreenFactory { ConfigManager.createScreen(it) }
}
