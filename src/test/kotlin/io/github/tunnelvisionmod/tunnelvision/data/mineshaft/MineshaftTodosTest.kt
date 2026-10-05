package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineshaftTodosTest {
	@Test
	fun `lists corpses, the fossil and the crystal in order`() {
		val todos = MineshaftTodos.list(
			mapOf(CorpseType.UMBER to 1, CorpseType.LAPIS to 2),
			setOf("Umber Key"),
			fossilPending = true,
			crystalToGrab = CrystalType.RUBY,
			warpedIn = false,
		)
		assertEquals(
			listOf(
				Todo.Corpse(CorpseType.LAPIS, 2, hasKey = true),
				Todo.Corpse(CorpseType.UMBER, 1, hasKey = true),
				Todo.Fossil,
				Todo.GrabCrystal(CrystalType.RUBY),
			),
			todos,
		)
	}

	@Test
	fun `done when nothing is left`() {
		assertTrue(MineshaftTodos.isDone(MineshaftTodos.list(emptyMap(), emptySet(), fossilPending = false, crystalToGrab = null, warpedIn = false)))
	}

	@Test
	fun `corpses without their key do not hold it up`() {
		val todos = MineshaftTodos.list(mapOf(CorpseType.TUNGSTEN to 1, CorpseType.VANGUARD to 1), emptySet(), fossilPending = false, crystalToGrab = null, warpedIn = false)
		assertTrue(MineshaftTodos.isDone(todos))
	}

	@Test
	fun `a lootable corpse, the fossil or the crystal keeps it open`() {
		assertFalse(MineshaftTodos.isDone(MineshaftTodos.list(mapOf(CorpseType.LAPIS to 1), emptySet(), fossilPending = false, crystalToGrab = null, warpedIn = false)))
		assertFalse(MineshaftTodos.isDone(MineshaftTodos.list(emptyMap(), emptySet(), fossilPending = true, crystalToGrab = null, warpedIn = false)))
		assertFalse(MineshaftTodos.isDone(MineshaftTodos.list(emptyMap(), emptySet(), fossilPending = false, crystalToGrab = CrystalType.OPAL, warpedIn = false)))
	}

	@Test
	fun `a warped-in player is not asked for the fossil or the crystal`() {
		val todos = MineshaftTodos.list(
			mapOf(CorpseType.LAPIS to 1),
			emptySet(),
			fossilPending = true,
			crystalToGrab = CrystalType.RUBY,
			warpedIn = true,
		)
		assertEquals(listOf(Todo.Corpse(CorpseType.LAPIS, 1, hasKey = true)), todos)
	}

	@Test
	fun `a warped-in player is done once the corpses are looted`() {
		val todos = MineshaftTodos.list(emptyMap(), emptySet(), fossilPending = true, crystalToGrab = CrystalType.RUBY, warpedIn = true)
		assertTrue(MineshaftTodos.isDone(todos))
	}

	@Test
	fun `only asks for a crystal you are known not to carry`() {
		assertEquals(CrystalType.JASPER, MineshaftTodos.crystalToGrab(CrystalType.JASPER, crystalsKnown = true, carried = setOf(CrystalType.RUBY)))
		assertNull(MineshaftTodos.crystalToGrab(CrystalType.JASPER, crystalsKnown = true, carried = setOf(CrystalType.JASPER)))
		assertNull(MineshaftTodos.crystalToGrab(CrystalType.JASPER, crystalsKnown = false, carried = emptySet()))
		assertNull(MineshaftTodos.crystalToGrab(null, crystalsKnown = true, carried = emptySet()))
	}
}
