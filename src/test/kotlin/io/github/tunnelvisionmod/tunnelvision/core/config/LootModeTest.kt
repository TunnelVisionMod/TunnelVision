package io.github.tunnelvisionmod.tunnelvision.core.config

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LootModeTest {
	private val gson = Gson()

	@Test
	fun `modes saved under their old names still load`() {
		assertEquals(LootMode.ADAPTIVE, gson.fromJson("\"NORMAL\"", LootMode::class.java))
		assertEquals(LootMode.CRYSTAL_HUNTER, gson.fromJson("\"GREEDY\"", LootMode::class.java))
	}

	@Test
	fun `modes are saved under their new names`() {
		assertEquals("\"ADAPTIVE\"", gson.toJson(LootMode.ADAPTIVE))
		assertEquals("\"CRYSTAL_HUNTER\"", gson.toJson(LootMode.CRYSTAL_HUNTER))
	}
}
