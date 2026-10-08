package io.github.tunnelvisionmod.tunnelvision.features.mining.stats

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MayhemBuff
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftMayhem
import io.github.tunnelvisionmod.tunnelvision.data.value.ColdResistance
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.Sidebar
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component

object MiningStats : Feature {
	private const val HOTM_TITLE = "Heart of the Mountain"
	private const val SKY_MALL_ITEM = "Sky Mall"

	private val config get() = ConfigManager.config.mining.miningStats

	private val todaysSkyMall: SkyMallBuff?
		get() {
			val data = Storage.data
			if (data.skyMallDay != SkyBlockTime.day(System.currentTimeMillis())) return null
			return data.skyMallBuff?.let { name -> SkyMallBuff.entries.firstOrNull { it.name == name } }
		}

	override fun init() {
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<ClientTickEvent> { onTick() }
		HudManager.register(Widget)
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		for (line in event.text.lines().map { it.trim() }) {
			MiningStatsParser.parseSkyMallChat(line)?.let { setSkyMall(it, "chat") }
		}
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isOnSkyBlock) return
		val screen = Compat.screen as? AbstractContainerScreen<*> ?: return
		if (HOTM_TITLE !in screen.title.string.removeFormatting()) return
		val item = screen.menu.slots.map { it.item }.firstOrNull { !it.isEmpty && it.plainName() == SKY_MALL_ITEM } ?: return
		val buff = MiningStatsParser.parseSkyMallItem(item.loreLines()) ?: return
		if (buff != todaysSkyMall) setSkyMall(buff, "/hotm")
	}

	private fun setSkyMall(buff: SkyMallBuff, source: String) {
		Debug.log { "MiningStats: Sky Mall $buff via $source" }
		Storage.data.skyMallBuff = buff.name
		Storage.data.skyMallDay = SkyBlockTime.day(System.currentTimeMillis())
		Storage.save()
	}

	private fun line(label: String, value: String, valueColor: ChatFormatting = ChatFormatting.WHITE): Component =
		Component.literal("$label: ").withStyle(ChatFormatting.AQUA).append(Component.literal(value).withStyle(valueColor))

	private fun eventLine(event: ActiveMiningEvent): Component = when (event) {
		is ActiveMiningEvent.FortunateFreezing -> line("Freezing", event.fortuneBonus?.let { "+$it ☘" } ?: "active", ChatFormatting.GOLD)
		is ActiveMiningEvent.BetterTogether -> line("Together", betterTogetherText(event), ChatFormatting.LIGHT_PURPLE)
	}

	private fun betterTogetherText(event: ActiveMiningEvent.BetterTogether): String = when (event.nearbyPlayers) {
		null -> "-"
		0 -> "no one nearby"
		else -> "+${event.speedBonus} ⸕ +${event.fortuneBonus} ☘"
	}

	private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

	object Widget : HudWidget("mining_stats", "Mining Stats", HudPosition(0.02f, 0.25f)) {
		override val isEnabled get() = config.enabled

		override fun getLines(): List<Component> {
			if (!SkyBlock.isOnMiningIsland) return emptyList()
			val lines = mutableListOf<Component>()
			if (config.skyMall) {
				lines += todaysSkyMall?.let { line("Sky Mall", it.displayName) }
					?: line("Sky Mall", "unknown (open /hotm)", ChatFormatting.GRAY)
			}
			if (config.miningEvent) MiningStatsParser.parseMiningEvent(Sidebar.lines)?.let { lines += eventLine(it) }
			if (!SkyBlock.isInMineshaft) return lines
			if (config.mayhem) MineshaftMayhem.buff?.let { lines += line("Mayhem", it.displayName) }
			if (config.coldResistance) {
				lines += ColdResistance.parse(TabList.lines)?.let { line("Cold Resistance", "${formatNumber(it)} ❄") }
					?: line("Cold Resistance", "add it to the Stats widget in /widget", ChatFormatting.GRAY)
			}
			return lines
		}

		override fun getExampleLines() = listOf(
			line("Sky Mall", SkyMallBuff.MINING_SPEED.displayName),
			line("Mayhem", MayhemBuff.MINING_FORTUNE.displayName),
			eventLine(ActiveMiningEvent.FortunateFreezing(4)),
			line("Cold Resistance", "25 ❄"),
		)
	}
}
