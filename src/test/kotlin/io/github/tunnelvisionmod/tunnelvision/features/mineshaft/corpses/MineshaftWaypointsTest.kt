package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftSpots
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.MineshaftType
import io.github.tunnelvisionmod.tunnelvision.data.value.of
import io.github.tunnelvisionmod.tunnelvision.utils.render.Pos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineshaftWaypointsTest {
	private val json = """
		{
			"corpses": {
				"UMBE_1": [[-172, 16, -186], [-116, 9, -167]],
				"CRYSTAL": [[1, 2, 3]],
				"LITT_L": [[4, 5, 6]]
			},
			"fossils": {
				"TUNG_1": [-176, 11, -170]
			}
		}
	""".trimIndent()
	private val spots = MineshaftSpots.parse(json)

	@Test
	fun `loads corpse spots per type`() {
		assertEquals(listOf(Pos(-172, 16, -186), Pos(-116, 9, -167)), spots.corpseSpots(MineshaftType.UMBER))
		assertEquals(listOf(Pos(4, 5, 6)), spots.corpseSpots(MineshaftType.LITTLEFOOTS_DEN))
	}

	@Test
	fun `every crystal mineshaft uses the shared crystal spots`() {
		assertEquals(listOf(Pos(1, 2, 3)), spots.corpseSpots(MineshaftType.RUBY_CRYSTAL))
		assertEquals(listOf(Pos(1, 2, 3)), spots.corpseSpots(MineshaftType.OPAL_CRYSTAL))
	}

	@Test
	fun `unknown type has no spots`() {
		assertEquals(emptyList<Pos>(), spots.corpseSpots(MineshaftType.TOPAZ_2))
	}

	@Test
	fun `loads fossils`() {
		assertEquals(Pos(-176, 11, -170), spots.fossil(MineshaftType.TUNGSTEN))
		assertNull(spots.fossil(MineshaftType.UMBER))
	}

	private fun state() = WaypointState().apply { start(listOf(Pos(0, 0, 0), Pos(20, 0, 0))) }

	@Test
	fun `seen spot without corpse disappears`() {
		val state = state()
		state.onSpotSeen(Pos(0, 0, 0), corpse = null)
		assertEquals(listOf(Pos(20, 0, 0)), state.possibleSpots)
		assertEquals(emptyList<CorpseWaypoint>(), state.corpses)
	}

	@Test
	fun `seen spot with corpse becomes corpse waypoint`() {
		val state = state()
		state.onSpotSeen(Pos(0, 0, 0), corpse = CorpseWaypoint(CorpseType.UMBER, Pos(1, 0, 0)))
		assertEquals(listOf(Pos(20, 0, 0)), state.possibleSpots)
		assertEquals(listOf(CorpseWaypoint(CorpseType.UMBER, Pos(1, 0, 0))), state.corpses)
	}

	@Test
	fun `corpse seen elsewhere is added once and clears nearby spot`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(21, 0, 0)))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(21, 0, 1)))
		assertEquals(listOf(Pos(0, 0, 0)), state.possibleSpots)
		assertEquals(1, state.corpses.size)
	}

	@Test
	fun `loot removes closest corpse of that type`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(0, 0, 0)))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(4, 0, 0)))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.UMBER, Pos(1, 0, 0)))
		state.onLooted(CorpseType.LAPIS, player = Pos(3, 0, 0))
		assertEquals(
			listOf(CorpseWaypoint(CorpseType.LAPIS, Pos(0, 0, 0)), CorpseWaypoint(CorpseType.UMBER, Pos(1, 0, 0))),
			state.corpses,
		)
	}

	@Test
	fun `loot far away removes nothing`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(0, 0, 0)))
		state.onLooted(CorpseType.LAPIS, player = Pos(30, 0, 0))
		assertEquals(1, state.corpses.size)
	}

	@Test
	fun `looted corpse is not added again when seen`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(0, 0, 0)))
		state.onLooted(CorpseType.LAPIS, player = Pos(1, 0, 0))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(0, 0, 0)))
		assertTrue(state.corpses.isEmpty())
	}

	@Test
	fun `corpse looted before it was seen is not added`() {
		val state = state()
		state.onLooted(CorpseType.UMBER, player = Pos(10, 0, 0))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.UMBER, Pos(12, 0, 0)))
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(11, 0, 0)))
		assertEquals(listOf(CorpseWaypoint(CorpseType.LAPIS, Pos(11, 0, 0))), state.corpses)
	}

	@Test
	fun `remaining spots disappear once all corpses are found`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(100, 0, 0)))
		state.onCorpseTotal(2)
		assertEquals(2, state.possibleSpots.size)
		state.onCorpseSeen(CorpseWaypoint(CorpseType.UMBER, Pos(200, 0, 0)))
		state.onCorpseTotal(2)
		assertTrue(state.possibleSpots.isEmpty())
	}

	@Test
	fun `looted corpses count as found`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(100, 0, 0)))
		state.onLooted(CorpseType.LAPIS, player = Pos(100, 0, 0))
		state.onLooted(CorpseType.UMBER, player = Pos(200, 0, 0))
		state.onCorpseTotal(2)
		assertTrue(state.possibleSpots.isEmpty())
	}

	@Test
	fun `unknown total keeps spots`() {
		val state = state()
		state.onCorpseSeen(CorpseWaypoint(CorpseType.LAPIS, Pos(100, 0, 0)))
		state.onCorpseTotal(null)
		assertEquals(2, state.possibleSpots.size)
	}

	@Test
	fun `reset clears everything`() {
		val state = state()
		state.reset()
		assertTrue(state.possibleSpots.isEmpty() && state.corpses.isEmpty())
	}
}
