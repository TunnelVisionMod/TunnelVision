package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes

import io.github.tunnelvisionmod.tunnelvision.data.value.of
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GemstoneRouteTest {
	private val json = """
		{"layout":"JASP_1","loop":true,"split":2,"start":[0,0,0],"waypoints":[
			{"x":1,"y":0,"z":0,"blocks":[[1,1,0]]},
			{"x":2,"y":0,"z":0,"blocks":[[2,1,0],[2,2,0]]},
			{"x":3,"y":0,"z":0,"blocks":[[3,1,0]]},
			{"x":4,"y":0,"z":0,"blocks":[[4,1,0]]}
		]}
	""".trimIndent()

	private fun List<RouteWaypoint>.xs() = map { it.pos.x }

	@Test
	fun `parses waypoints and their blocks`() {
		val route = GemstoneRoute.parse(json)
		assertEquals("JASP_1", route.layout)
		assertEquals(listOf(1, 2, 3, 4), route.waypoints.xs())
		assertEquals(listOf(Pos(2, 1, 0), Pos(2, 2, 0)), route.waypoints[1].blocks)
	}

	@Test
	fun `second half walks the loop backwards`() {
		val route = GemstoneRoute.parse(json)
		assertEquals(listOf(1, 2), route.part(GemstoneRoute.Part.FIRST).xs())
		assertEquals(listOf(4, 3), route.part(GemstoneRoute.Part.SECOND).xs())
		assertEquals(listOf(1, 2, 3, 4), route.part(GemstoneRoute.Part.ALL).xs())
	}

	@Test
	fun `centre is the vein block closest to the middle`() {
		val waypoint = RouteWaypoint(Pos(0, 0, 0), listOf(Pos(0, 5, 0), Pos(1, 5, 0), Pos(2, 5, 0), Pos(9, 5, 0)))
		assertEquals(Pos(2, 5, 0), waypoint.centre)
	}

	@Test
	fun `centre of a waypoint without vein blocks is its position`() {
		val waypoint = RouteWaypoint(Pos(-166, 5, -195))
		assertEquals(Pos(-166, 5, -195), waypoint.centre)
	}

	@Test
	fun `parses waypoints without vein blocks`() {
		val route = GemstoneRoute.parse("""
			{"layout":"JADE_1","split":null,"waypoints":[
				{"x":-166,"y":5,"z":-195,"blocks":[]},
				{"x":-176,"y":3,"z":-195}
			]}
		""".trimIndent())
		assertEquals(listOf(-166, -176), route.waypoints.xs())
		assertEquals(listOf(emptyList<Pos>(), emptyList<Pos>()), route.waypoints.map { it.blocks })
		assertEquals(Pos(-176, 3, -195), route.waypoints[1].centre)
	}

	@Test
	fun `route without split ignores parts`() {
		val route = GemstoneRoute.parse(json.replace("\"split\":2", "\"split\":null"))
		assertEquals(listOf(1, 2, 3, 4), route.part(GemstoneRoute.Part.SECOND).xs())
	}
}
