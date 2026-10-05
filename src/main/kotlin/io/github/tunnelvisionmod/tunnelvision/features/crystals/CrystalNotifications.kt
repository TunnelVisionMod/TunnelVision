package io.github.tunnelvisionmod.tunnelvision.features.crystals

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.features.forge.ForgeParser
import io.github.tunnelvisionmod.tunnelvision.utils.formatCoins
import io.github.tunnelvisionmod.tunnelvision.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.hud.HudPosition
import io.github.tunnelvisionmod.tunnelvision.hud.HudWidget
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
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
 * [crystalsAndForgeFull] is the same condition as (2), exposed for other features to read.
 */
object CrystalNotifications {
	/**
	 * The tab widgets are not populated on the tick the island changes, so the entry check waits
	 * for the forge widget to appear rather than reading an empty tab list.
	 */
	private const val ENTRY_TIMEOUT_TICKS = 100

	/**
	 * A forge slot can only be started from the Forge menus, so a Perfect gem that shows up with
	 * none of them open recently is the tab widget reloading, not a crystal being spent.
	 */
	private const val FORGE_MENU_WINDOW_TICKS = 200
	private val forgeMenuTitles = setOf("The Forge", "Forging", "Confirm Process")
	private const val FORGE_SELECT_PREFIX = "Select Process ("

	/** The HotM menu is the only complete source of which crystals you carry. */
	private const val HOTM_TITLE = "Heart of the Mountain"

	private val config get() = ConfigManager.config.general.crystalNotifications
	private val tracker = CrystalTracker()
	private val perfectGems = PerfectGemWatcher()

	/** Null while the Forges widget has not been seen, so "unknown" never reads as "full". */
	private var forgeFull: Boolean? = null

	/** True when every crystal is carried and the forge has no open slot. For other features. */
	val crystalsAndForgeFull: Boolean get() = tracker.hasAll && (forgeFull ?: Storage.data.forgeFull) == true

	/** The crystals you are carrying, for other features. */
	val carriedCrystals: Set<CrystalType> get() = tracker.carried

	/** False until /hotm has been read, so [carriedCrystals] being empty may just mean unknown. */
	val crystalsKnown: Boolean get() = stateKnown

	private var entryPending = false
	private var entryTicks = 0
	private var fullMessageSent = false
	private var lastHotmStates: Map<CrystalType, Boolean>? = null
	private var lastForgeItems: List<Pair<Int, String>>? = null
	private var ticksSinceMenu = FORGE_MENU_WINDOW_TICKS + 1

	/**
	 * The menu has to have been read once before anything we say about crystals is meaningful.
	 * Set by [readHotmMenu], and by the dev command so it can be tested without one.
	 */
	private var stateKnown = false

	private var testOverride = false

	fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<LocationChangedEvent> { onLocationChanged() }
		EventBus.on<DisconnectEvent> {
			tracker.reset()
			forgeFull = null
			lastHotmStates = null
			lastForgeItems = null
			stateKnown = false
			testOverride = false
			perfectGems.reset()
			entryPending = false
			fullMessageSent = false
		}
		HudManager.register(Widget)
	}

	private fun onLocationChanged() {
		forgeFull = null
		if (SkyBlock.isInDwarvenMines) {
			entryPending = true
			entryTicks = 0
		} else {
			entryPending = false
		}
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!SkyBlock.isOnSkyBlock) return
		// Somebody typing "jasper crystal" in party chat must not hand you a crystal.
		if (CrystalParser.isPlayerChat(event.text)) return
		CrystalParser.parseChatConsumed(event.text)?.let { crystal ->
			if (tracker.consumed(crystal)) Debug.log { "Crystals: spent " + crystal.displayName + " (chat)" }
			return
		}
		CrystalParser.parseChatGained(event.text)?.let { crystal ->
			if (tracker.gained(crystal)) Debug.log { "Crystals: picked up " + crystal.displayName + " (chat)" }
			return
		}
		// A line that mentions a crystal but does not parse is exactly the wording we need.
		if (event.text.contains("crystal", ignoreCase = true)) {
			Debug.log { "Crystals: unparsed crystal line: " + event.text }
		}
	}

	private fun onTick() {
		if (!SkyBlock.isOnSkyBlock) return

		if (testOverride && !ConfigManager.config.dev.debugMode) endTesting()
		if (!testOverride) readHotmMenu()
		val menuTitle = (Compat.screen as? AbstractContainerScreen<*>)?.title?.string?.removeFormatting()?.trim()
		val inForgeMenu = menuTitle != null && (menuTitle in forgeMenuTitles || menuTitle.startsWith(FORGE_SELECT_PREFIX))
		ticksSinceMenu = if (inForgeMenu) 0 else (ticksSinceMenu + 1).coerceAtMost(FORGE_MENU_WINDOW_TICKS + 1)
		readForge()

		if (entryPending) tryEntryNotification()
		checkCrystalsFull()
	}

	/**
	 * The Heart of the Mountain menu lists every crystal in the lore of one item, so whatever it
	 * says replaces what chat and the forge could tell us.
	 */
	private fun readHotmMenu() {
		val screen = Compat.screen as? AbstractContainerScreen<*> ?: return
		if (HOTM_TITLE !in screen.title.string.removeFormatting()) return
		val items = screen.menu.slots.map { it.item }.filter { !it.isEmpty }
		val lore = items.firstOrNull { it.plainName() == CrystalParser.HOTM_ITEM }?.loreLines()
			?: items.map { it.loreLines() }.firstOrNull { CrystalParser.parseHotmLore(it).isNotEmpty() }
			?: return
		val states = CrystalParser.parseHotmLore(lore)
		if (states.isEmpty()) return
		if (states != lastHotmStates) {
			lastHotmStates = states
			Debug.log { "Crystals: HotM says " + states.map { (c, held) -> c.displayName + "=" + held } }
		}
		stateKnown = true
		tracker.apply(states)
	}

	/**
	 * Pretends a crystal was picked up or spent, so the notifications and the widget can be tested
	 * without going and finding one. Driven by `/tv crystal`.
	 */
	fun endTesting() {
		testOverride = false
		Debug.log { "Crystals: test mode off, using real data again" }
	}

	fun setCarriedForTesting(states: Map<CrystalType, Boolean>) {
		stateKnown = true
		testOverride = true
		tracker.apply(states)
		Debug.log { "Crystals: test state " + tracker.carried.map { it.displayName } }
	}

	/** Lets `/tv crystal` re-run the entry notification without changing island. */
	fun replayEntryNotification() {
		entryPending = true
		entryTicks = 0
	}

	/**
	 * Only how full the forge is. What it is *cooking* says nothing about what you carry: a
	 * `Perfect Ruby Gemstone` in a slot was made from a crystal already spent, and reading it as
	 * "you have no Ruby crystal" wrongly strips the one you are holding now.
	 */
	private fun readForge() {
		val status = ForgeParser.parseStatus(TabList.lines) ?: return
		val items = status.slots.map { it.slot to it.item }
		if (items != lastForgeItems) {
			lastForgeItems = items
			Debug.log { "Crystals: forge slots $items" }
		}
		for (crystal in perfectGems.newPerfectGems(status.slots)) {
			if (ticksSinceMenu > FORGE_MENU_WINDOW_TICKS) {
				Debug.log { "Crystals: perfect " + crystal.displayName + " appeared in the forge without the Forge menu, ignored" }
				continue
			}
			val spent = tracker.consumed(crystal)
			Debug.log { "Crystals: perfect " + crystal.displayName + " started in the forge, " + if (spent) "spent" else "was not carried" }
		}
		val full = status.isFull
		forgeFull = full
		if (Storage.data.forgeFull != full) {
			Storage.data.forgeFull = full
			Storage.save()
		}
	}

	private fun tryEntryNotification() {
		// Wait for the forge widget: without it we cannot tell whether going to the forge is
		// pointless, and the whole point is to stay quiet when it is.
		val full = forgeFull
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
		val carried = tracker.carried
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
		if (!crystalsAndForgeFull) {
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
	private fun forgeLine(pick: ForgePick): Component {
		val line = crystalName(pick.crystal)
		pick.value?.let { line.append(Component.literal(" " + formatCoins(it)).withStyle(ChatFormatting.DARK_GRAY)) }
		if (pick.forgeNow) line.append(Component.literal(" forge").withStyle(ChatFormatting.GREEN))
		return line
	}

	object Widget : HudWidget("crystals", "Crystals", HudPosition(0.02f, 0.45f)) {
		override val isEnabled get() = config.enabled && config.widget

		override fun getLines(): List<Component> {
			if (!SkyBlock.isOnMiningIsland) return emptyList()
			val carried = tracker.carried
			val header = Component.literal("Crystals (" + carried.size + "/" + CrystalType.entries.size + ")")
				.withStyle(ChatFormatting.AQUA)
			if (!stateKnown) {
				return listOf(header, Component.literal("open /hotm").withStyle(ChatFormatting.GRAY))
			}
			if (carried.isEmpty()) return listOf(header, Component.literal("none").withStyle(ChatFormatting.GRAY))
			// One crystal per line, so the widget stays narrow however many you are holding.
			if (!config.forgePriority) return listOf(header) + carried.map { crystalName(it) }
			// A missing Forges widget means unknown, not full, so nothing is marked to forge.
			val openSlots = ForgeParser.parseStatus(TabList.lines)?.openSlots ?: 0
			val ranked = ForgePriority.rank(carried, openSlots) { CrystalValue.of(it) }
			return listOf(header) + ranked.map { forgeLine(it) }
		}

		override fun getExampleLines() = listOf(
			Component.literal("Crystals (3/" + CrystalType.entries.size + ")").withStyle(ChatFormatting.AQUA),
			crystalName(CrystalType.JASPER),
			crystalName(CrystalType.RUBY),
			crystalName(CrystalType.ONYX),
		)
	}
}
