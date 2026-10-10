package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.config.LootMode
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.data.value.CorpseValue
import io.github.tunnelvisionmod.tunnelvision.data.value.ShaftVerdict
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.TabList
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import net.minecraft.world.entity.player.Inventory

/**
 * What is still left to do in the shaft you are in: the corpses worth looting, the fossil and the
 * crystal of a crystal shaft. Worked out on demand from the tab list and your inventory.
 */
object MineshaftTodoState {
	private const val CRYSTAL_SUFFIX = "_C"

	private val loot get() = ConfigManager.config.mineshaft.corpses.loot

	/** [corpsesKnown] is false while the Frozen Corpses tab widget is missing, so "unknown" never reads as "done". */
	class State(val todos: List<Todo>, val corpsesKnown: Boolean) {
		val done: Boolean get() = corpsesKnown && MineshaftTodos.isDone(todos)
	}

	val current: State?
		get() {
			if (!SkyBlock.isInMineshaft) return null
			val unlooted = CorpseLoot.parseUnlooted(TabList.lines)
			val todos = MineshaftTodos.list(
				unlooted?.let { CorpseLoot.toLoot(it, currentRule()) } ?: emptyMap(),
				carriedItemNames(),
				Fossil.pending,
				MineshaftTodos.crystalToGrab(crystalShaft(), CrystalState.known, CrystalState.carried),
				warpedIn = MineshaftRole.isWarpedIn,
			)
			return State(todos, corpsesKnown = unlooted != null)
		}

	val done: Boolean get() = current?.done == true

	/**
	 * Your crystals are unknown until /hotm has been opened, and here that changes the list: Greedy
	 * loots by whether your crystals are full, and a crystal shaft has a crystal to grab.
	 */
	val needsHotm: Boolean get() = !CrystalState.known && (loot.lootMode == LootMode.GREEDY || crystalShaft() != null)

	private fun currentRule(): LootRule = CorpseLoot.rule(
		mode = loot.lootMode,
		crystalsFull = CrystalState.corpseCrystalsCarried,
		shouldMine = ShaftVerdict.current()?.shouldMine,
		openVanguards = CorpseValue.opensVanguards(
			loot.openVanguards,
			ConfigManager.config.general.bazaarPrice,
			if (CrystalState.known) CrystalState.carried else emptySet(),
		),
	)

	private fun crystalShaft(): CrystalType? =
		MineshaftState.type?.takeIf { it.code.endsWith(CRYSTAL_SUFFIX) }?.let { CrystalType.byDisplayName(it.displayName) }

	private fun carriedItemNames(): Set<String> {
		val inventory = mc.player?.inventory ?: return emptySet()
		return (0 until Inventory.INVENTORY_SIZE).map { inventory.getItem(it).plainName() }.toSet()
	}
}
