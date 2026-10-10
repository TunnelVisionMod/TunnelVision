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
	fun `the Locket row says whether it is selected`() {
		val selected = listOf("Shattered Locket" to listOf("Frozen Corpse XP: 2,600,000/2,500,000", "Click to deselect!"))
		val other = listOf("Shattered Locket" to listOf("Frozen Corpse XP: 2,600,000/2,500,000", "Click to select!"))
		val silent = listOf("Shattered Locket" to listOf("Frozen Corpse XP: 2,600,000/2,500,000"))
		assertEquals(true, RngMeterParser.parseMenu(selected)?.locketSelected)
		assertEquals(false, RngMeterParser.parseMenu(other)?.locketSelected)
		assertNull(RngMeterParser.parseMenu(silent)?.locketSelected)
	}

	/** The hub's Frozen Corpse tooltip with nothing selected, as Hypixel writes it. */
	@Test
	fun `the hub says when no drop is selected`() {
		val hub = listOf(
			"Frozen Corpses RNG Meter" to listOf(
				"Your Frozen Corpse RNG Meter fills",
				"is full!",
				"",
				"You don't have an RNG drop",
				"selected. Choose one to start",
				"progressing towards it!",
				"",
				"Stored Frozen Corpse XP: 1,752,200",
				"",
				"Click to view!",
			),
		)
		assertEquals(MeterReading(1_752_200.0, null, locketSelected = false), RngMeterParser.parseMenu(hub))
	}

	/** With the Locket selected the hub swaps the stored XP for a progress bar. */
	@Test
	fun `the hub names the selected drop and reads its progress bar`() {
		val hub = listOf(
			"Frozen Corpses RNG Meter" to listOf(
				"Vanguard Corpse: 25,000 XP",
				"Selected Drop",
				"Shattered Locket",
				"",
				"Progress: 70.4%",
				"━━━━━━━━━━━━━━━━━━━━ 1,760,450/2.5M",
				"",
				"Click to view!",
			),
		)
		assertEquals(MeterReading(1_760_450.0, 2_500_000.0, locketSelected = true), RngMeterParser.parseMenu(hub))
	}

	@Test
	fun `another category's progress bar is not ours`() {
		val hub = listOf("Catacombs RNG Meter" to listOf("Selected Drop", "Necron's Handle", "━━━━ 400/1,000"))
		assertNull(RngMeterParser.parseMenu(hub))
	}

	@Test
	fun `selecting a drop in chat is recognised`() {
		assertEquals(true, RngMeterParser.selectedFromChat("You set your Frozen Corpses RNG Meter to drop Shattered Locket!"))
		assertEquals(false, RngMeterParser.selectedFromChat("You set your Frozen Corpses RNG Meter to drop Caged Wisp!"))
		assertNull(RngMeterParser.selectedFromChat("You set your Catacombs RNG Meter to drop Necron's Handle!"))
		assertEquals(false, RngMeterParser.selectedFromChat("You reset your selected drop for your Frozen Corpses RNG Meter!"))
		assertNull(RngMeterParser.selectedFromChat("You reset your selected drop for your Catacombs RNG Meter!"))
	}

	/** In game the drop is a Shattered Locket, whatever its id says. */
	@Test
	fun `the row is found under its Locket name`() {
		val menu = listOf(
			"Goblin Egg" to listOf("Frozen Corpse XP: 1,250,000/12,000"),
			"Shattered Locket" to listOf("Frozen Corpse XP: 1,250,000/2,500,000"),
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
