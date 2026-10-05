package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ChatReceivedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.LocationChangedEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.WorldRenderEvent
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.Fossil
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftState
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftSpots
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftTodoState
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Visibility
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos
import io.github.tunnelvisionmod.tunnelvision.utils.render.WorldRender
import kotlin.math.roundToInt
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object MineshaftWaypoints : Feature {
	private const val CHECK_EVERY_TICKS = 5
	private const val SPOT_RANGE = 40.0
	private const val CLOSE_ENOUGH = 4.0
	private const val CORPSE_AT_SPOT = 3.0
	private const val VISIBLE_CHECKS_NEEDED = 2

	private const val SPOT_COLOR = 0xFFFFFFFF.toInt()
	private const val FOSSIL_COLOR = 0xFFB040FF.toInt()
	private const val FILL_ALPHA = 0x50

	private val lootMessage = Regex("""^(LAPIS|UMBER|TUNGSTEN|VANGUARD) CORPSE LOOT!""")

	private val config get() = ConfigManager.config.mineshaft.corpses.mineshaftWaypoints
	private val spots by lazy { MineshaftSpots.load() }
	private val state = WaypointState()

	private val visibleChecks = mutableMapOf<Pos, Int>()

	private var started = false
	private var todosDone = false
	private var ticks = 0

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<ChatReceivedEvent> { onChat(it) }
		EventBus.on<LocationChangedEvent> { reset() }
		EventBus.on<WorldRenderEvent> { onRender(it.context) }
	}

	private fun onTick() {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		val level = mc.level ?: return
		if (!started) start() else if (++ticks % CHECK_EVERY_TICKS == 0) {
			updateTodosDone()
			if (!todosDone) {
				checkSpots(level)
				checkCorpses(level)
				checkAllCorpsesFound()
			}
		}
	}

	private fun start() {
		val type = MineshaftState.type ?: return
		started = true
		state.start(if (config.corpseSpots) spots.corpseSpots(type) else emptyList())
		Debug.log { "MineshaftWaypoints: ${type.code} with ${state.possibleSpots.size} spots" }
	}

	private fun updateTodosDone() {
		val done = config.removeWhenTodosDone && MineshaftTodoState.done
		if (done != todosDone) Debug.log { "MineshaftWaypoints: to-dos ${if (done) "done, hiding corpse waypoints" else "open again"}" }
		todosDone = done
	}

	private fun checkSpots(level: ClientLevel) {
		val eye = mc.player?.eyePosition ?: return
		for (spot in state.possibleSpots.toList()) {
			val centre = spot.centre()
			val distance = eye.distanceTo(centre)
			if (distance > SPOT_RANGE) continue
			val visible = distance <= CLOSE_ENOUGH || Visibility.canSeeAny(spot.samples())
			val count = if (visible) (visibleChecks[spot] ?: 0) + 1 else 0
			visibleChecks[spot] = count
			if (count < VISIBLE_CHECKS_NEEDED) continue
			val corpse = corpseStands(level).firstOrNull { it.position().distanceTo(centre) <= CORPSE_AT_SPOT }
			Debug.log { "MineshaftWaypoints: checked spot $spot -> ${corpse?.corpseType() ?: "empty"}" }
			state.onSpotSeen(spot, corpse?.let { CorpseWaypoint(it.corpseType()!!, it.blockPosition().toPos()) })
		}
	}

	private fun checkCorpses(level: ClientLevel) {
		val eye = mc.player?.eyePosition ?: return
		for (stand in corpseStands(level)) {
			if (stand.position().distanceTo(eye) > SPOT_RANGE) continue
			if (!Visibility.canSeeAny(listOf(stand.position().add(0.0, 1.0, 0.0), stand.eyePosition))) continue
			state.onCorpseSeen(CorpseWaypoint(stand.corpseType()!!, stand.blockPosition().toPos()))
		}
	}

	private fun checkAllCorpsesFound() {
		if (state.possibleSpots.isEmpty()) return
		state.onCorpseTotal(MineshaftState.corpseCount)
		if (state.possibleSpots.isEmpty()) Debug.log { "MineshaftWaypoints: all ${MineshaftState.corpseCount} corpses found, cleared remaining spots" }
	}

	private fun onChat(event: ChatReceivedEvent) {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		val type = lootMessage.find(event.text.trim())?.groupValues?.get(1)?.let { CorpseType.valueOf(it) } ?: return
		val player = mc.player?.blockPosition()?.toPos() ?: return
		Debug.log { "MineshaftWaypoints: looted $type" }
		state.onLooted(type, player)
	}

	private fun onRender(context: LevelRenderContext) {
		if (!config.enabled || !SkyBlock.isInMineshaft || !started) return
		val eye = mc.player?.eyePosition ?: return
		if (!todosDone) renderCorpses(context, eye)
		if (!config.fossil) return
		Fossil.pos?.let { fossil ->
			WorldRender.lines(context, Fossil.edges, FOSSIL_COLOR, throughWalls = false)
			if (Fossil.miningStarted) return@let
			val box = AABB(fossil.toBlockPos())
			WorldRender.outline(context, box, FOSSIL_COLOR, throughWalls = true)
			label(context, box, "Fossil", FOSSIL_COLOR, eye)
		}
	}

	private fun renderCorpses(context: LevelRenderContext, eye: Vec3) {
		for (spot in state.possibleSpots) {
			val box = spot.box()
			WorldRender.outline(context, box, SPOT_COLOR, throughWalls = true)
			label(context, box, "Possible Corpse", SPOT_COLOR, eye)
		}
		for (corpse in state.corpses) {
			val box = corpse.pos.box()
			val color = corpse.type.color()
			WorldRender.filled(context, box, color.withAlpha(FILL_ALPHA))
			WorldRender.outline(context, box, color, throughWalls = true)
			label(context, box, "${corpse.type.displayName()} Corpse", color, eye)
		}
	}

	private fun label(context: LevelRenderContext, box: AABB, name: String, color: Int, eye: Vec3) {
		if (!config.labels) return
		val pos = Vec3(box.center.x, box.maxY + 0.5, box.center.z)
		WorldRender.label(context, pos, Component.literal("$name · ${eye.distanceTo(pos).roundToInt()}m"), color)
	}

	private fun corpseStands(level: ClientLevel): List<ArmorStand> =
		level.entitiesForRendering().filterIsInstance<ArmorStand>().filter { it.corpseType() != null }

	private fun reset() {
		started = false
		todosDone = false
		ticks = 0
		state.reset()
		visibleChecks.clear()
	}

	private fun Pos.toBlockPos() = BlockPos(x, y, z)
	private fun BlockPos.toPos() = Pos(x, y, z)
	private fun Pos.centre() = Vec3(x + 0.5, y + 1.0, z + 0.5)
	private fun Pos.box() = AABB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 2.0, z + 1.0)
	private fun Pos.samples() = listOf(centre(), Vec3(x + 0.5, y + 0.2, z + 0.5), Vec3(x + 0.5, y + 1.8, z + 0.5))

	private fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)

	private fun CorpseType.color(): Int = when (this) {
		CorpseType.LAPIS -> 0xFF5555FF.toInt()
		CorpseType.UMBER -> 0xFFFFAA00.toInt()
		CorpseType.TUNGSTEN -> 0xFFAAAAAA.toInt()
		CorpseType.VANGUARD -> 0xFF55FFFF.toInt()
	}

	private fun CorpseType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}
