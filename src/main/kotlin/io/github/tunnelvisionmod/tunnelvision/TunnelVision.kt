package io.github.tunnelvisionmod.tunnelvision

import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.commands.TunnelVisionCommand
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.EventHooks
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.data.bazaar.Bazaar
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.Fossil
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftRole
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.data.value.ShaftVerdict
import io.github.tunnelvisionmod.tunnelvision.features.forge.crystals.CrystalNotifications
import io.github.tunnelvisionmod.tunnelvision.features.forge.notification.ForgeNotification
import io.github.tunnelvisionmod.tunnelvision.features.mining.lantern.LanternReminder
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.BlueCheeseCorpseLock
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.MineshaftWaypoints
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.cutloose.CutLooseTracker
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.GemstoneRoutes
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.RouteRunner
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.type.MineshaftTypeAnnouncer
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value.MineshaftTodoWidget
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.value.MineshaftValueAlert
import io.github.tunnelvisionmod.tunnelvision.features.mining.effects.MiningEffects
import io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe.PickaxeAbility
import io.github.tunnelvisionmod.tunnelvision.features.mining.pristine.HidePristineMessages
import io.github.tunnelvisionmod.tunnelvision.features.mining.pristine.WrongGearWarning
import io.github.tunnelvisionmod.tunnelvision.features.mining.stats.MiningStats
import io.github.tunnelvisionmod.tunnelvision.features.party.commands.PartyCommands
import io.github.tunnelvisionmod.tunnelvision.features.party.partyshare.MineshaftPartyShare
import io.github.tunnelvisionmod.tunnelvision.features.party.sharedwarp.SharedMineshaftWarp
import io.github.tunnelvisionmod.tunnelvision.utils.Sidebar
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

object TunnelVision : ClientModInitializer {
	const val MOD_ID = "tunnelvision"
	val logger = LoggerFactory.getLogger(MOD_ID)
	val mc: Minecraft get() = Minecraft.getInstance()

	// Event handlers run in registration order, so data is up to date before any feature reads it,
	// and the Mineshaft Type title is up before the Lantern Reminder adds its subtitle.
	private val features: List<Feature> = listOf(
		MineshaftTypeAnnouncer,
		PickaxeAbility,
		ForgeNotification,
		CrystalNotifications,
		MiningEffects,
		BlueCheeseCorpseLock,
		CutLooseTracker,
		MineshaftWaypoints,
		MineshaftPartyShare,
		WrongGearWarning,
		HidePristineMessages,
		PartyCommands,
		SharedMineshaftWarp,
		MiningStats,
		LanternReminder,
		MineshaftValueAlert,
		MineshaftTodoWidget,
		RouteRunner,
		GemstoneRoutes,
	)

	override fun onInitializeClient() {
		ConfigManager.load()
		HudManager.load()
		TitleSounds.load()
		Storage.load()
		EventHooks.register()
		SkyBlock.register()
		TabList.register()
		Sidebar.register()

		Bazaar.init()
		MineshaftState.init()
		MineshaftRole.init()
		CrystalState.init()
		ShaftVerdict.init()
		Fossil.init()

		features.forEach { it.init() }
		TunnelVisionCommand.register()
		logger.info("TunnelVision initialized")
	}
}
