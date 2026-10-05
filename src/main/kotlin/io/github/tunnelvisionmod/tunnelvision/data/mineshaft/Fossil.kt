package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos
import io.github.tunnelvisionmod.tunnelvision.utils.render.VeinOutline
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3

/**
 * The fossil of the shaft you are in, from the known spot per layout, and how much of it is left.
 *
 * The fossil goes to whoever opened the mineshaft, so a warped-in player has none.
 */
object Fossil {
	private const val CHECK_EVERY_TICKS = 5
	private const val RADIUS = 12

	private val spots by lazy { MineshaftSpots.load() }
	private val isQuartz = mutableMapOf<Block, Boolean>()

	/** The fossil's spot, or null when there is none or it has been mined. */
	var pos: Pos? = null
		private set

	/** Outline of the fossil blocks still standing, empty until the area around it has loaded. */
	var edges: List<Pair<Vec3, Vec3>> = emptyList()
		private set

	/** True once some of it has been mined. */
	var miningStarted = false
		private set

	/** There is a fossil here that nobody has started mining yet. */
	val pending: Boolean get() = started && pos != null && !miningStarted

	private var started = false
	private var ticks = 0
	private var blocks: Set<BlockPos> = emptySet()

	fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<LocationChangedEvent> { reset() }
	}

	private fun onTick() {
		if (!SkyBlock.isInMineshaft) return
		val level = mc.level ?: return
		if (!started) start() else if (++ticks % CHECK_EVERY_TICKS == 0) check(level)
	}

	private fun start() {
		val type = MineshaftState.type ?: return
		started = true
		pos = if (MineshaftRole.isWarpedIn) null else spots.fossil(type)
		Debug.log { "Fossil: ${type.code} -> $pos" }
	}

	private fun check(level: ClientLevel) {
		val fossil = pos ?: return
		val centre = BlockPos(fossil.x, fossil.y, fossil.z)
		if (listOf(-RADIUS, RADIUS).any { dx -> listOf(-RADIUS, RADIUS).any { dz -> !level.isLoaded(centre.offset(dx, 0, dz)) } }) return
		if (blocks.isEmpty()) {
			blocks = find(level, centre)
			edges = blocks.outline()
			if (blocks.isNotEmpty()) Debug.log { "Fossil: ${blocks.size} blocks" }
			return
		}
		val left = blocks.filter { level.getBlockState(it).block.isQuartz() }.toSet()
		if (left.size == blocks.size) return
		if (!miningStarted) Debug.log { "Fossil: started mining" }
		miningStarted = true
		blocks = left
		edges = left.outline()
		if (left.isEmpty()) {
			Debug.log { "Fossil: mined" }
			pos = null
			miningStarted = false
		}
	}

	private fun find(level: ClientLevel, centre: BlockPos): Set<BlockPos> =
		BlockPos.betweenClosed(centre.offset(-RADIUS, -RADIUS, -RADIUS), centre.offset(RADIUS, RADIUS, RADIUS))
			.map { it.immutable() }
			.filter { it.closerThan(centre, RADIUS.toDouble()) && level.getBlockState(it).block.isQuartz() }
			.toSet()

	private fun Set<BlockPos>.outline() =
		VeinOutline.edges(map { Pos(it.x, it.y, it.z) }.toSet()).map { (a, b) -> a.toVec3() to b.toVec3() }

	private fun Pos.toVec3() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

	private fun Block.isQuartz(): Boolean = isQuartz.getOrPut(this) {
		val path = BuiltInRegistries.BLOCK.getKey(this).path
		"quartz" in path && "ore" !in path
	}

	private fun reset() {
		started = false
		ticks = 0
		pos = null
		blocks = emptySet()
		edges = emptyList()
		miningStarted = false
	}
}
