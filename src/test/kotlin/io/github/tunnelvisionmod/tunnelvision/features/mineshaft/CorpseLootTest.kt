package io.github.tunnelvisionmod.tunnelvision.features.mineshaft

import io.github.tunnelvisionmod.tunnelvision.config.LootMode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CorpseLootTest {
	@Test
	fun `lapis only always means lapis`() {
		assertEquals(LootRule.LAPIS, CorpseLoot.rule(LootMode.LAPIS_ONLY, crystalsFull = false, shouldMine = true, openVanguards = true))
		assertEquals(LootRule.LAPIS, CorpseLoot.rule(LootMode.LAPIS_ONLY, crystalsFull = true, shouldMine = false, openVanguards = true))
	}

	@Test
	fun `greedy loots everything while crystals are not full`() {
		assertEquals(LootRule.ALL, CorpseLoot.rule(LootMode.GREEDY, crystalsFull = false, shouldMine = false, openVanguards = true))
		assertEquals(LootRule.ALL, CorpseLoot.rule(LootMode.GREEDY, crystalsFull = false, shouldMine = null, openVanguards = true))
	}

	@Test
	fun `greedy with crystals full loots everything only in a shaft worth mining`() {
		assertEquals(LootRule.ALL, CorpseLoot.rule(LootMode.GREEDY, crystalsFull = true, shouldMine = true, openVanguards = true))
		assertEquals(LootRule.LAPIS_AND_VANGUARD, CorpseLoot.rule(LootMode.GREEDY, crystalsFull = true, shouldMine = false, openVanguards = true))
		assertEquals(LootRule.LAPIS_AND_VANGUARD, CorpseLoot.rule(LootMode.GREEDY, crystalsFull = true, shouldMine = null, openVanguards = true))
	}

	@Test
	fun `normal loots everything only in a shaft worth mining, crystals or not`() {
		for (crystalsFull in listOf(false, true)) {
			assertEquals(LootRule.ALL, CorpseLoot.rule(LootMode.NORMAL, crystalsFull, shouldMine = true, openVanguards = true))
			assertEquals(LootRule.LAPIS_AND_VANGUARD, CorpseLoot.rule(LootMode.NORMAL, crystalsFull, shouldMine = false, openVanguards = true))
			assertEquals(LootRule.LAPIS_AND_VANGUARD, CorpseLoot.rule(LootMode.NORMAL, crystalsFull, shouldMine = null, openVanguards = true))
		}
	}

	@Test
	fun `parses unlooted corpses per type`() {
		val tab = listOf("Area: Mineshaft", "Frozen Corpses:", "Lapis: NOT LOOTED", "Umber: LOOTED", "Lapis: NOT LOOTED", "Vanguard: NOT LOOTED", "Powders:")
		assertEquals(mapOf(CorpseType.LAPIS to 2, CorpseType.VANGUARD to 1), CorpseLoot.parseUnlooted(tab))
	}

	@Test
	fun `everything looted`() {
		assertEquals(emptyMap<CorpseType, Int>(), CorpseLoot.parseUnlooted(listOf("Frozen Corpses:", "Lapis: LOOTED", "Powders:")))
	}

	@Test
	fun `no corpse widget`() {
		assertNull(CorpseLoot.parseUnlooted(listOf("Area: Mineshaft", "Powders:")))
	}

	@Test
	fun `filters corpses by rule`() {
		val unlooted = mapOf(CorpseType.LAPIS to 2, CorpseType.UMBER to 1, CorpseType.TUNGSTEN to 1, CorpseType.VANGUARD to 1)
		assertEquals(unlooted, CorpseLoot.toLoot(unlooted, LootRule.ALL))
		assertEquals(mapOf(CorpseType.LAPIS to 2), CorpseLoot.toLoot(unlooted, LootRule.LAPIS))
		assertEquals(mapOf(CorpseType.LAPIS to 2, CorpseType.VANGUARD to 1), CorpseLoot.toLoot(unlooted, LootRule.LAPIS_AND_VANGUARD))
	}

	@Test
	fun `keys needed per corpse`() {
		assertNull(CorpseType.LAPIS.keyName)
		assertEquals("Umber Key", CorpseType.UMBER.keyName)
		assertEquals("Tungsten Key", CorpseType.TUNGSTEN.keyName)
		assertEquals("Skeleton Key", CorpseType.VANGUARD.keyName)
	}

	@Test
	fun `vanguards drop out of every rule when we do not open them`() {
		for (crystalsFull in listOf(false, true)) {
			for (shouldMine in listOf(true, false, null)) {
				for (mode in LootMode.entries) {
					val rule = CorpseLoot.rule(mode, crystalsFull, shouldMine, openVanguards = false)
					assertFalse(rule.includes(CorpseType.VANGUARD), "$mode $crystalsFull $shouldMine gave $rule")
				}
			}
		}
	}

	@Test
	fun `not opening vanguards still loots everything else in a shaft worth mining`() {
		val rule = CorpseLoot.rule(LootMode.NORMAL, crystalsFull = false, shouldMine = true, openVanguards = false)
		assertEquals(LootRule.ALL_BUT_VANGUARD, rule)
		for (type in listOf(CorpseType.LAPIS, CorpseType.UMBER, CorpseType.TUNGSTEN)) {
			assertTrue(rule.includes(type), type.tabName)
		}
	}

	@Test
	fun `greedy without vanguards loots everything else`() {
		assertEquals(
			LootRule.ALL_BUT_VANGUARD,
			CorpseLoot.rule(LootMode.GREEDY, crystalsFull = false, shouldMine = false, openVanguards = false),
		)
	}
}
