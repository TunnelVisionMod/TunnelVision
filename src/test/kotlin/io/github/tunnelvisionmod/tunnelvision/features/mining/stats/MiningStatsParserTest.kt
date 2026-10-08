package io.github.tunnelvisionmod.tunnelvision.features.mining.stats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MiningStatsParserTest {
	@Test
	fun `sky mall from chat`() {
		assertEquals(SkyMallBuff.MINING_SPEED, MiningStatsParser.parseSkyMallChat("New buff: Gain +100⸕ Mining Speed."))
		assertEquals(SkyMallBuff.MINING_FORTUNE, MiningStatsParser.parseSkyMallChat("New buff: Gain +50☘ Mining Fortune."))
		assertEquals(SkyMallBuff.EXTRA_POWDER, MiningStatsParser.parseSkyMallChat("New buff: Gain +15% more Powder while mining."))
		assertEquals(SkyMallBuff.ABILITY_COOLDOWN, MiningStatsParser.parseSkyMallChat("New buff: -20% Pickaxe Ability cooldowns."))
		assertEquals(SkyMallBuff.GOBLIN_CHANCE, MiningStatsParser.parseSkyMallChat("New buff: 10x chance to find Golden and Diamond Goblins."))
		assertEquals(SkyMallBuff.TITANIUM, MiningStatsParser.parseSkyMallChat("New buff: Gain 5x Titanium drops."))
	}

	@Test
	fun `sky mall ignores other new buffs`() {
		assertNull(MiningStatsParser.parseSkyMallChat("New buff: Gain +5% ∮ Sweep."))
		assertNull(MiningStatsParser.parseSkyMallChat("New day! Your Sky Mall buff changed!"))
	}

	@Test
	fun `sky mall from hotm item`() {
		val lore = listOf("Every SkyBlock day, you receive a random", "buff in the Dwarven Mines.", "", "Your Current Effect", "■ Gain +100⸕ Mining Speed.", "", "Click to toggle")
		assertEquals(SkyMallBuff.MINING_SPEED, MiningStatsParser.parseSkyMallItem(lore))
	}

	@Test
	fun `sky mall item without current effect`() {
		assertNull(MiningStatsParser.parseSkyMallItem(listOf("Every SkyBlock day, you receive a random", "Requires Tier 2")))
	}

	@Test
	fun `fortunate freezing with bonus`() {
		assertEquals(
			ActiveMiningEvent.FortunateFreezing(4),
			MiningStatsParser.parseMiningEvent(listOf("Cold: -3❄", "Event: FORTUNATE FREEZING", "Event Bonus: +4☘")),
		)
	}

	@Test
	fun `fortunate freezing without bonus line`() {
		assertEquals(ActiveMiningEvent.FortunateFreezing(null), MiningStatsParser.parseMiningEvent(listOf("Event: FORTUNATE FREEZING")))
	}

	@Test
	fun `better together with nearby players`() {
		assertEquals(ActiveMiningEvent.BetterTogether(3), MiningStatsParser.parseMiningEvent(listOf("Event: BETTER TOGETHER", "Nearby Players: 3")))
		assertEquals(ActiveMiningEvent.BetterTogether(5), MiningStatsParser.parseMiningEvent(listOf("Event: BETTER TOGETHER", "Nearby Players: 5 MAX")))
		assertEquals(ActiveMiningEvent.BetterTogether(null), MiningStatsParser.parseMiningEvent(listOf("Event: BETTER TOGETHER", "Nearby Players: N/A")))
	}

	@Test
	fun `better together bonus per nearby player`() {
		assertEquals(750, ActiveMiningEvent.BetterTogether(3).speedBonus)
		assertEquals(60, ActiveMiningEvent.BetterTogether(3).fortuneBonus)
		assertEquals(1250, ActiveMiningEvent.BetterTogether(5).speedBonus)
		assertEquals(0, ActiveMiningEvent.BetterTogether(0).fortuneBonus)
		assertNull(ActiveMiningEvent.BetterTogether(null).speedBonus)
	}

	@Test
	fun `other events are ignored`() {
		assertNull(MiningStatsParser.parseMiningEvent(listOf("Event: 2X POWDER", "Event Bonus: +4☘")))
		assertNull(MiningStatsParser.parseMiningEvent(listOf("Event: GONE WITH THE WIND")))
		assertNull(MiningStatsParser.parseMiningEvent(listOf("Cold: -3❄", "Nearby Players: 3")))
	}

	@Test
	fun `skyblock day`() {
		assertEquals(0L, SkyBlockTime.day(SkyBlockTime.EPOCH_MS))
		assertEquals(0L, SkyBlockTime.day(SkyBlockTime.EPOCH_MS + SkyBlockTime.DAY_MS - 1))
		assertEquals(1L, SkyBlockTime.day(SkyBlockTime.EPOCH_MS + SkyBlockTime.DAY_MS))
	}
}
