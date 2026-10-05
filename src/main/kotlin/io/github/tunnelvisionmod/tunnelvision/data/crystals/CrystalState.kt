package io.github.tunnelvisionmod.tunnelvision.data.crystals

import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.DisconnectEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.data.forge.ForgeParser
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Storage
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.loreLines
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import io.github.tunnelvisionmod.tunnelvision.utils.removeFormatting
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

/**
 * Which crystals you carry and how full the forge is, kept up to date whether or not any feature
 * that shows them is enabled.
 */
object CrystalState {
	/**
	 * A forge slot can only be started from the Forge menus, so a Perfect gem that shows up with
	 * none of them open recently is the tab widget reloading, not a crystal being spent.
	 */
	private const val FORGE_MENU_WINDOW_TICKS = 200
	private val forgeMenuTitles = setOf("The Forge", "Forging", "Confirm Process")
	private const val FORGE_SELECT_PREFIX = "Select Process ("

	/** The HotM menu is the only complete source of which crystals you carry. */
	private const val HOTM_TITLE = "Heart of the Mountain"

	private val tracker = CrystalTracker()
	private val perfectGems = PerfectGemWatcher()

	/** Null while the Forges widget has not been seen on this island, so "unknown" never reads as "full". */
	var forgeFull: Boolean? = null
		private set

	/** True when every crystal is carried and the forge has no open slot. */
	val crystalsAndForgeFull: Boolean get() = tracker.hasAll && (forgeFull ?: Storage.data.forgeFull) == true

	val carried: Set<CrystalType> get() = tracker.carried

	/** False until /hotm has been read, so [carried] being empty may just mean unknown. */
	val known: Boolean get() = stateKnown

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
		EventBus.on<LocationChangedEvent> { forgeFull = null }
		EventBus.on<DisconnectEvent> {
			tracker.reset()
			forgeFull = null
			lastHotmStates = null
			lastForgeItems = null
			stateKnown = false
			testOverride = false
			perfectGems.reset()
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
}
