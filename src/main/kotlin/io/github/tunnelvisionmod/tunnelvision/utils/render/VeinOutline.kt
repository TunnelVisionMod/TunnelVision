package io.github.tunnelvisionmod.tunnelvision.utils.render

object VeinOutline {
	private val axes = listOf(Pos(1, 0, 0), Pos(0, 1, 0), Pos(0, 0, 1))

	fun edges(blocks: Set<Pos>): List<Pair<Pos, Pos>> {
		val edges = mutableListOf<Pair<Pos, Pos>>()
		for (block in blocks) {
			for (axis in axes) {
				val (u, v) = axes.filter { it != axis }
				for (su in 0..1) for (sv in 0..1) {
					val du = if (su == 1) u else -u
					val dv = if (sv == 1) v else -v
					val first = block + du in blocks
					val second = block + dv in blocks
					val diagonal = block + du + dv in blocks
					if ((first || second) && !(first && second && !diagonal)) continue
					val corner = block + u * su + v * sv
					edges += corner to corner + axis
				}
			}
		}
		return edges
	}

	private operator fun Pos.plus(other: Pos) = Pos(x + other.x, y + other.y, z + other.z)
	private operator fun Pos.unaryMinus() = Pos(-x, -y, -z)
	private operator fun Pos.times(factor: Int) = Pos(x * factor, y * factor, z * factor)
}
