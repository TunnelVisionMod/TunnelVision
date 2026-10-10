package io.github.tunnelvisionmod.tunnelvision.features.mining.effects

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EffectParserTest {
	@Test
	fun `parses effect with colon and minutes`() {
		assertEquals(
			listOf(TabEffect(MiningEffect.COLD_RESISTANCE, 12 * 60, 60)),
			EffectParser.parseTab(listOf("Cold Resistance IV: 12m")),
		)
	}

	@Test
	fun `parses effect without colon and with seconds`() {
		assertEquals(
			listOf(TabEffect(MiningEffect.FILET_O_FORTUNE, 58 * 60 + 4, 1)),
			EffectParser.parseTab(listOf("Filet O' Fortune I 58m 4s")),
		)
	}

	@Test
	fun `parses hours`() {
		assertEquals(
			listOf(TabEffect(MiningEffect.COLD_RESISTANCE, 3600 + 5 * 60, 60)),
			EffectParser.parseTab(listOf("Cold Resistance IV: 1h 5m")),
		)
	}

	@Test
	fun `ignores other effects and lines`() {
		val lines = listOf("Active Effects", "You have 3 non-god effects.", "Smoldering Polarization I: 58s", "Cold Resistance III: 4m")
		assertEquals(emptyList<TabEffect>(), EffectParser.parseTab(lines))
	}

	@Test
	fun `gained message`() {
		assertEquals(
			MiningEffect.COLD_RESISTANCE,
			EffectParser.parseGainedMessage("BUFF! You have gained Cold Resistance IV! Press TAB or type /effects to view your active effects!"),
		)
	}

	@Test
	fun `other gained message`() {
		assertNull(EffectParser.parseGainedMessage("BUFF! You have gained Mushed Glowy Tonic I!"))
	}

	@Test
	fun `effects menu title`() {
		assertTrue(EffectParser.isEffectsMenu("Active Effects"))
		assertTrue(EffectParser.isEffectsMenu("(2/3) Active Effects"))
		assertFalse(EffectParser.isEffectsMenu("SkyBlock Menu"))
	}

	@Test
	fun `effects menu item with minutes and seconds`() {
		assertEquals(
			TabEffect(MiningEffect.COLD_RESISTANCE, 12 * 60 + 34, 1),
			EffectParser.parseMenuItem("Cold Resistance IV", listOf("Grants +5 Cold Resistance.", "", "Remaining: 12:34")),
		)
	}

	@Test
	fun `effects menu item with hours`() {
		assertEquals(
			TabEffect(MiningEffect.FILET_O_FORTUNE, 3600 + 5 * 60 + 9, 1),
			EffectParser.parseMenuItem("Filet O' Fortune I", listOf("Remaining: 01:05:09")),
		)
	}

	@Test
	fun `effects menu reads the fiesta flask with or without a level`() {
		assertEquals(TabEffect(MiningEffect.FIESTA_FLASK, 59 * 60 + 4, 1), EffectParser.parseMenuItem("Fiesta Flask", listOf("Remaining: 59:04")))
		assertEquals(TabEffect(MiningEffect.FIESTA_FLASK, 59 * 60 + 4, 1), EffectParser.parseMenuItem("Fiesta Flask I", listOf("Remaining: 59:04")))
	}

	@Test
	fun `effects menu ignores other items`() {
		assertNull(EffectParser.parseMenuItem("Smoldering Polarization I", listOf("Remaining: 12:34")))
		assertNull(EffectParser.parseMenuItem("Cold Resistance IV", listOf("Click to view!")))
	}

	@Test
	fun `potion lore duration`() {
		val lore = listOf("Cold Resistance IV (1:00:00)", "Gain +10 Cold Resistance.", "", "UNCOMMON")
		assertEquals(MiningEffect.COLD_RESISTANCE to 3600, EffectParser.parsePotionLore(lore))
	}

	@Test
	fun `potion lore with minutes`() {
		assertEquals(MiningEffect.COLD_RESISTANCE to 180, EffectParser.parsePotionLore(listOf("Cold Resistance IV (3:00)")))
	}

	@Test
	fun `potion lore of other potion`() {
		assertNull(EffectParser.parsePotionLore(listOf("Speed VIII (43:00)", "Grants +40 Speed.")))
	}
}
