package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.WarpRole
import io.github.tunnelvisionmod.tunnelvision.data.value.of

data class RouteChoice(val name: String, val part: GemstoneRoute.Part) {
	companion object {
		private const val CRYSTAL_SUFFIX = "_C"
		private const val CRYSTAL_ROUTE = "CRYSTAL"
		private const val SOLO_SUFFIX = "_SOLO"

		val extraNames = listOf(CRYSTAL_ROUTE, "JASP_1$SOLO_SUFFIX", "$CRYSTAL_ROUTE$SOLO_SUFFIX")

		fun of(mineshaftCode: String, role: WarpRole, exists: (String) -> Boolean): RouteChoice? {
			val name = if (mineshaftCode.endsWith(CRYSTAL_SUFFIX)) CRYSTAL_ROUTE else mineshaftCode
			if (!exists(name)) return null
			return when (role) {
				WarpRole.WARPED_SOMEONE -> RouteChoice(name, GemstoneRoute.Part.FIRST)
				WarpRole.WARPED_IN -> RouteChoice(name, GemstoneRoute.Part.SECOND)
				WarpRole.SOLO -> "$name$SOLO_SUFFIX".takeIf(exists)?.let { RouteChoice(it, GemstoneRoute.Part.ALL) }
					?: RouteChoice(name, GemstoneRoute.Part.ALL)
			}
		}
	}
}
