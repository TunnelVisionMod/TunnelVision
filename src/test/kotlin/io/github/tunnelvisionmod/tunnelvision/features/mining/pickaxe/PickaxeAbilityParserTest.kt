package io.github.tunnelvisionmod.tunnelvision.features.mining.pickaxe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PickaxeAbilityParserTest {
	private fun tab(vararg abilityLines: String) = listOf("Area: Dwarven Mines", "Pickaxe Ability:") + abilityLines + listOf("Powders:")

	@Test
	fun `tab cooldown in seconds`() {
		assertEquals(TabAbility("Mining Speed Boost", 42), PickaxeAbilityParser.parseTab(tab("Mining Speed Boost: 42s")))
	}

	@Test
	fun `tab cooldown in minutes and seconds`() {
		assertEquals(TabAbility("Pickobulus", 78), PickaxeAbilityParser.parseTab(tab("Pickobulus: 1m 18s")))
	}

	@Test
	fun `tab available`() {
		assertEquals(TabAbility("Pickobulus", null), PickaxeAbilityParser.parseTab(tab("Pickobulus: Available")))
	}

	@Test
	fun `tab short cooldown`() {
		assertEquals(TabAbility("Maniac Miner", 5), PickaxeAbilityParser.parseTab(tab("Maniac Miner: 5s")))
	}

	@Test
	fun `tab without widget`() {
		assertNull(PickaxeAbilityParser.parseTab(listOf("Area: Hub", "Profile: Apple")))
	}

	@Test
	fun `tab widget header as last line`() {
		assertNull(PickaxeAbilityParser.parseTab(listOf("Area: Dwarven Mines", "Pickaxe Ability:")))
	}

	@Test
	fun `tab widget with unexpected line`() {
		assertNull(PickaxeAbilityParser.parseTab(tab("Mithril Powder: 1,234")))
	}

	@Test
	fun `used message`() {
		assertEquals("Mining Speed Boost", PickaxeAbilityParser.parseUsedMessage("You used your Mining Speed Boost Pickaxe Ability!"))
	}

	@Test
	fun `unrelated chat message`() {
		assertNull(PickaxeAbilityParser.parseUsedMessage("Your Mining Speed Boost has expired!"))
	}

	@Test
	fun `available message`() {
		assertEquals("Pickobulus", PickaxeAbilityParser.parseAvailableMessage("Pickobulus is now available!"))
		assertNull(PickaxeAbilityParser.parseAvailableMessage("You used your Pickobulus Pickaxe Ability!"))
	}

	@Test
	fun `lore cooldown`() {
		val lore = listOf("Mining Speed: +1,000", "", "Ability: Mining Speed Boost  RIGHT CLICK", "Grants +200% Mining Speed for 15s.", "Cooldown: 120s", "", "LEGENDARY DRILL")
		assertEquals(120, PickaxeAbilityParser.parseLoreCooldown(lore))
	}

	@Test
	fun `lore without cooldown`() {
		assertNull(PickaxeAbilityParser.parseLoreCooldown(listOf("Mining Speed: +500", "RARE PICKAXE")))
	}

	@Test
	fun `mining tools`() {
		assertTrue(PickaxeAbilityParser.isMiningTool(listOf("Mining Speed: +1,000", "LEGENDARY DRILL")))
		assertTrue(PickaxeAbilityParser.isMiningTool(listOf("Mining Speed: +300", "EPIC PICKAXE")))
		assertTrue(PickaxeAbilityParser.isMiningTool(listOf("Mining Speed: +900", "MYTHIC DUNGEON GAUNTLET")))
	}

	@Test
	fun `non mining tools`() {
		assertFalse(PickaxeAbilityParser.isMiningTool(listOf("Damage: +100", "LEGENDARY SWORD")))
		assertFalse(PickaxeAbilityParser.isMiningTool(listOf("Breaking Power 4", "COMMON PICKAXE")))
		assertFalse(PickaxeAbilityParser.isMiningTool(emptyList()))
	}
}
