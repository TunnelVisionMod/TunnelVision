package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.ClientTickEvent
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.WorldRenderEvent
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos
import io.github.tunnelvisionmod.tunnelvision.utils.render.WorldRender
import kotlin.math.roundToInt
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object RouteRunner : Feature {
	private const val CROSSHAIR_DISTANCE = 2.0
	private const val CHECK_EVERY_TICKS = 4
	private const val TARGET_COLOR = 0xFF55FF55.toInt()
	private const val PREVIOUS_COLOR = 0xFFAAAAAA.toInt()
	private const val FILL_ALPHA = 0x50
	private const val PREVIOUS_FILL_ALPHA = 0x28

	private var waypoints: List<RouteWaypoint> = emptyList()
	private var index = 0
	private var ticks = 0
	private var isActive: () -> Boolean = { false }

	private val config get() = ConfigManager.config.mineshaft.gemstones.gemstoneRoutes

	val isRunning: Boolean get() = waypoints.isNotEmpty()

	override fun init() {
		EventBus.on<ClientTickEvent> { onTick() }
		EventBus.on<WorldRenderEvent> { onRender(it.context) }
	}

	fun start(waypoints: List<RouteWaypoint>, isActive: () -> Boolean) {
		this.waypoints = waypoints
		this.isActive = isActive
		index = 0
	}

	fun stop() {
		waypoints = emptyList()
		index = 0
	}

	fun step(by: Int) {
		if (isRunning) index = (index + by).coerceIn(0, waypoints.size)
	}

	private fun onTick() {
		if (!isRunning || !isActive() || ++ticks % CHECK_EVERY_TICKS != 0) return
		val level = mc.level ?: return
		val eye = mc.player?.eyePosition ?: return
		while (index < waypoints.size) {
			if (!waypoints[index].reached(level, eye)) break
			if (++index == waypoints.size) ChatUtils.send(Component.literal("Route done").withStyle(ChatFormatting.GRAY))
		}
	}

	private fun onRender(context: LevelRenderContext) {
		if (!isRunning || !isActive()) return
		val player = mc.player ?: return
		val target = waypoints.getOrNull(index) ?: return
		waypoints.getOrNull(index - 1)?.let { renderWaypoint(context, it, index - 1, PREVIOUS_COLOR, PREVIOUS_FILL_ALPHA, player.eyePosition) }
		val box = renderWaypoint(context, target, index, TARGET_COLOR, FILL_ALPHA, player.eyePosition)
		val crosshair = Compat.camera.position().add(player.getViewVector(1f).scale(CROSSHAIR_DISTANCE))
		WorldRender.lines(context, listOf(crosshair to box.center), TARGET_COLOR, throughWalls = true)
	}

	private fun renderWaypoint(context: LevelRenderContext, waypoint: RouteWaypoint, at: Int, color: Int, fillAlpha: Int, eye: Vec3): AABB {
		val box = waypoint.centre.blockBox()
		WorldRender.filled(context, box, color.withAlpha(fillAlpha))
		WorldRender.outline(context, box, color, throughWalls = true)
		val labelPos = Vec3(box.center.x, box.maxY + 0.5, box.center.z)
		val gems = if (waypoint.blocks.isEmpty()) "" else "${waypoint.blocks.size} gems · "
		val text = "#${at + 1}/${waypoints.size} · $gems${eye.distanceTo(labelPos).roundToInt()}m"
		WorldRender.label(context, labelPos, Component.literal(text), color)
		return box
	}

	/** A vein waypoint is done once it is mined out or within reach, one without vein blocks once you get close to it. */
	private fun RouteWaypoint.reached(level: ClientLevel, eye: Vec3): Boolean {
		val distance = config.advanceDistance.toDouble()
		if (blocks.isEmpty()) return listOf(pos).within(eye, distance)
		val remaining = blocks.filter { !level.getBlockState(it.toBlockPos()).isAir }
		return remaining.isEmpty() || remaining.within(eye, distance)
	}

	private fun List<Pos>.within(eye: Vec3, range: Double) = any { AABB(it.toBlockPos()).distanceToSqr(eye) <= range * range }
	private fun Pos.blockBox() = AABB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
	private fun Pos.toBlockPos() = BlockPos(x, y, z)
	private fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)
}
