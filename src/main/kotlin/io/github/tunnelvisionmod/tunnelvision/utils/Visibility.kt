package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.tan
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object Visibility {
	private const val HIT_TOLERANCE = 1.0
	private const val FOV_MARGIN = 0.9

	fun isOnScreen(target: Vec3): Boolean {
		val player = mc.player ?: return false
		val direction = target.subtract(player.eyePosition).normalize()
		val verticalHalfFov = Math.toRadians(mc.options.fov().get() / 2.0)
		val aspect = mc.window.width.toDouble() / mc.window.height.coerceAtLeast(1)
		val horizontalHalfFov = atan(tan(verticalHalfFov) * aspect)
		return direction.dot(player.getViewVector(1f)) >= cos(horizontalHalfFov * FOV_MARGIN)
	}

	fun hasLineOfSight(target: Vec3): Boolean {
		val player = mc.player ?: return false
		val level = mc.level ?: return false
		val hit = level.clip(ClipContext(player.eyePosition, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
		return hit.type == HitResult.Type.MISS || hit.location.distanceTo(target) <= HIT_TOLERANCE
	}

	fun canSeeAny(targets: List<Vec3>): Boolean = targets.any { isOnScreen(it) && hasLineOfSight(it) }
}
