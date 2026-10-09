package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RngMeterParserTest {
	@Test
	fun `any RNG meter menu is recognised by its title`() {
		assertTrue(RngMeterParser.isMeterMenu("Frozen Corpse RNG Meter"))
		assertTrue(RngMeterParser.isMeterMenu("  Mineshaft RNG Meter  "))
		assertFalse(RngMeterParser.isMeterMenu("Heart of the Mountain"))
		assertFalse(RngMeterParser.isMeterMenu("RNG Meter Settings"))
	}

	@Test
	fun `reads progress and target off the lore line`() {
		assertEquals(
			MeterReading(1_250_000.0, 2_500_000.0),
			RngMeterParser.parse("Frozen Corpse XP: 1,250,000/2,500,000"),
		)
	}

	@Test
	fun `accepts abbreviated numbers`() {
		assertEquals(MeterReading(1_200_000.0, 2_500_000.0), RngMeterParser.parse("Frozen Corpse XP: 1.2M/2.5M"))
		assertEquals(MeterReading(250_000.0, 2_500_000.0), RngMeterParser.parse("Frozen Corpse XP: 250k/2.5M"))
	}

	@Test
	fun `another meter's line is not ours`() {
		assertNull(RngMeterParser.parse("Slayer XP: 120,000/150,000"))
		assertNull(RngMeterParser.parse("Dungeon Score: 300/1,000"))
		assertNull(RngMeterParser.parse("Frozen Corpses:"))
	}

	@Test
	fun `finds the line anywhere in the lore`() {
		val lore = listOf("Shattered Pendant", "", "Frozen Corpse XP: 0/2,500,000", "Click to select!")
		assertEquals(MeterReading(0.0, 2_500_000.0), RngMeterParser.parseLore(lore))
		assertNull(RngMeterParser.parseLore(listOf("Nothing to see", "here")))
	}

	/**
	 * The menu lists every drop with its own target, and the first row read back 12,000 XP instead
	 * of the Pendant's 2,500,000. The stored XP is shared, so only the target was wrong.
	 */
	@Test
	fun `the Pendant's row wins over whatever is listed first`() {
		val menu = listOf(
			"Goblin Egg" to listOf("Frozen Corpse XP: 1,250,000/12,000"),
			"Shattered Pendant" to listOf("Frozen Corpse XP: 1,250,000/2,500,000"),
		)
		assertEquals(MeterReading(1_250_000.0, 2_500_000.0), RngMeterParser.parseMenu(menu))
	}

	@Test
	fun `falls back to any row when the Pendant is not listed`() {
		val menu = listOf("Goblin Egg" to listOf("Frozen Corpse XP: 900/12,000"))
		assertEquals(MeterReading(900.0, 12_000.0), RngMeterParser.parseMenu(menu))
	}

	@Test
	fun `a menu with no corpse rows reads nothing`() {
		assertNull(RngMeterParser.parseMenu(listOf("Close" to listOf("To go back"))))
	}

	@Test
	fun `a zero target is refused rather than dividing by it`() {
		assertNull(RngMeterParser.parse("Frozen Corpse XP: 10/0"))
	}

	@Test
	fun `parses the number formats SkyBlock writes`() {
		assertEquals(2_500_000.0, RngMeterParser.parseAmount("2,500,000"))
		assertEquals(2_500_000.0, RngMeterParser.parseAmount("2.5M"))
		assertEquals(1_000.0, RngMeterParser.parseAmount("1k"))
		assertEquals(42.0, RngMeterParser.parseAmount("42"))
		assertNull(RngMeterParser.parseAmount("lots"))
	}
}
