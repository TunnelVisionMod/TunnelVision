package io.github.tunnelvisionmod.tunnelvision.features.forge.crystals

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudClickable
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalValue
import io.github.tunnelvisionmod.tunnelvision.data.crystals.ForgeInput
import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeParser
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.formatCoins
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvents

/**
 * Two notifications about crystals waiting to be forged, plus a widget listing what you carry:
 *
 * 1. Entering the Dwarven Mines while carrying a crystal shows `<Crystal> available` with `go to
 *    forge` under it - unless the forge is full, in which case there is nothing to go and do.
 * 2. Carrying every crystal while the forge is full sends `Crystals full` to chat once.
 *
 * What you carry comes from [CrystalState].
 */
object CrystalNotifications : Feature {
	/**
	 * The tab widgets are not populated on the tick the island changes, so the entry check waits
	 * for the forge widget to appear rather than reading an empty tab list.
	 */
	private const val ENTRY_TIMEOUT_TICKS = 100

	private val config get() = ConfigManager.config.forge.crystalNotifications

	private var entryPending = false
	private var entryTicks = 0
	private var fullMessageSent = false

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { onLocationChanged() }
		EventBus.on<DisconnectEvent> {
			entryPending = false
			fullMessageSent = false
		}
		HudManager.register(Widget)
	}

	private fun onLocationChanged() {
		if (SkyBlock.isInDwarvenMines) {
			entryPending = true
			entryTicks = 0
		} else {
			entryPending = false
		}
	}

	private fun onTick() {
		if (!SkyBlock.isOnSkyBlock) return
		if (entryPending) tryEntryNotification()
		checkCrystalsFull()
	}

	/** Lets `/tv crystal` re-run the entry notification without changing island. */
	fun replayEntryNotification() {
		entryPending = true
		entryTicks = 0
	}

	private fun tryEntryNotification() {
		// Wait for the forge widget: without it we cannot tell whether going to the forge is
		// pointless, and the whole point is to stay quiet when it is.
		val full = CrystalState.forgeFull
		if (full == null) {
			if (++entryTicks < ENTRY_TIMEOUT_TICKS) return
			Debug.log { "Crystals: no Forges widget after " + entryTicks + " ticks, skipping entry notification" }
			entryPending = false
			return
		}
		entryPending = false
		if (full) {
			Debug.log { "Crystals: forge full on entry, staying quiet" }
			return
		}
		val carried = CrystalState.carried
		if (carried.isEmpty()) return
		announceAvailable(carried)
	}

	private fun announceAvailable(carried: Set<CrystalType>) {
		if (!config.enabled || !config.availableTitle) return
		Debug.log { "Crystals: available " + carried.map { it.displayName } }
		Compat.setTitleTimes(0, 60, 10)
		// The crystals move to the subtitle: the title says what to do, the subtitle says with what.
		Compat.setSubtitle(crystalNames(carried))
		Compat.setTitle(Component.literal("Forge Crystal").withStyle(ChatFormatting.GOLD))
		playSound()
	}

	private fun checkCrystalsFull() {
		if (!CrystalState.crystalsAndForgeFull) {
			// Re-arm, so the message fires again the next time the condition comes back.
			fullMessageSent = false
			return
		}
		if (fullMessageSent) return
		fullMessageSent = true
		Debug.log { "Crystals: all crystals carried and forge full" }
		if (!config.enabled || !config.fullMessage) return
		ChatUtils.send(Component.literal("Crystals full").withStyle(ChatFormatting.AQUA))
		playSound()
	}

	private fun playSound() {
		if (!config.playSound) return
		mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
	}

	/** One crystal name, in its own colour. */
	private fun crystalName(crystal: CrystalType): MutableComponent =
		Component.literal(crystal.displayName).withStyle(crystal.color)

	/** The crystals as one component, each name in its own crystal colour. */
	private fun crystalNames(crystals: Collection<CrystalType>): MutableComponent {
		val text = Component.empty()
		for ((index, crystal) in crystals.withIndex()) {
			if (index > 0) text.append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
			text.append(Component.literal(crystal.displayName).withStyle(crystal.color))
		}
		return text
	}

	/**
	 * A crystal line, with what forging it is worth and a mark on the ones to forge now. The forge
	 * slot is the scarce resource, so the most valuable crystals go in first.
	 */
	private fun forgeLine(pick: ForgePick, input: ForgeInput?): Pair<Component, IntRange?> {
		val line = crystalName(pick.crystal)
		pick.value?.let { line.append(separator()).append(Component.literal(formatCoins(it)).withStyle(ChatFormatting.GOLD)) }
		var gems: IntRange? = null
		if (input != null) {
			line.append(separator())
			val from = mc.font.width(line)
			line.append(gemsText(input))
			gems = from until mc.font.width(line)
		}
		if (pick.forgeNow) line.append(separator()).append(Component.literal("forge").withStyle(ChatFormatting.GREEN))
		return line to gems
	}

	private fun separator(): Component = Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)

	private fun gemsText(input: ForgeInput): Component =
		Component.literal("${input.amount}x ${input.displayName}").withStyle(input.color)

	object Widget : HudWidget("crystals", "Crystals", HudPosition(0.02f, 0.45f)) {
		override val isEnabled get() = config.enabled && config.widget

		private var gemClicks: List<HudClickable> = emptyList()
		override val clickables get() = gemClicks

		override fun getLines(): List<Component> {
			gemClicks = emptyList()
			return buildLines()
		}

		private fun buildLines(): List<Component> {
			if (!SkyBlock.isOnMiningIsland) return emptyList()
			val carried = CrystalState.carried
			val header = Component.literal("Crystals (" + carried.size + "/" + CrystalType.entries.size + ")")
				.withStyle(ChatFormatting.AQUA)
			if (!CrystalState.known) {
				return listOf(header, Component.literal("open /hotm").withStyle(ChatFormatting.GRAY))
			}
			if (carried.isEmpty()) return listOf(header, Component.literal("none").withStyle(ChatFormatting.GRAY))
			// One crystal per line, so the widget stays narrow however many you are holding.
			if (!config.forgePriority) return listOf(header) + carried.map { crystalName(it) }
			// A missing Forges widget means unknown, not full, so nothing is marked to forge.
			val openSlots = ForgeParser.parseStatus(TabList.lines)?.openSlots ?: 0
			val worths = carried.associateWith { CrystalValue.worthOf(it) }
			val ranked = ForgePriority.rank(carried, openSlots) { worths[it]?.value }
			val lines = mutableListOf<Component>(header)
			val clicks = mutableListOf<HudClickable>()
			for (pick in ranked) {
				val input = if (config.forgeGems) worths[pick.crystal]?.input else null
				val (line, gems) = forgeLine(pick, input)
				if (input != null && gems != null) {
					val command = "bz ${input.displayName} ${pick.crystal.displayName} Gemstone"
					clicks += HudClickable(lines.size, gems.first, gems.last + 1, gemsText(input)) { mc.connection?.sendCommand(command) }
				}
				lines += line
			}
			gemClicks = clicks
			return lines
		}

		override fun getExampleLines() = listOf(
			Component.literal("Crystals (3/" + CrystalType.entries.size + ")").withStyle(ChatFormatting.AQUA),
			forgeLine(ForgePick(CrystalType.JASPER, 3_700_000.0, forgeNow = true), ForgeInput.FINE).first,
			crystalName(CrystalType.RUBY),
			crystalName(CrystalType.ONYX),
		)
	}
}
