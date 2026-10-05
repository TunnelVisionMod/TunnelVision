package io.github.tunnelvisionmod.tunnelvision.utils.render

import kotlin.math.sqrt

data class Pos(val x: Int, val y: Int, val z: Int) {
	fun distanceTo(other: Pos): Double {
		val dx = (x - other.x).toDouble()
		val dy = (y - other.y).toDouble()
		val dz = (z - other.z).toDouble()
		return sqrt(dx * dx + dy * dy + dz * dz)
	}
}
