package io.github.tunnelvisionmod.tunnelvision.data.crystals

import io.github.tunnelvisionmod.tunnelvision.data.bazaar.Bazaar
import net.minecraft.ChatFormatting

/** The gems a Perfect is forged from: 400 Fine on a buy order, or 5 Flawless insta-bought. */
enum class ForgeInput(val displayName: String, val amount: Int, val color: ChatFormatting) {
	FINE("Fine", 400, ChatFormatting.BLUE),
	FLAWLESS("Flawless", 5, ChatFormatting.DARK_PURPLE),
}

/** What forging a crystal is worth, and which gems are the cheaper way to get there. */
data class CrystalWorth(val value: Double, val input: ForgeInput)

/**
 * What a Gemstone Crystal is worth.
 *
 * A crystal has no sale price of its own - it is untradeable and exists only to forge a Perfect. So
 * it is worth the Perfect it unlocks less the 5 Flawless that Perfect would otherwise cost, taking
 * the cheaper of buy ordering 400 Fine (80 per Flawless) or insta-buying the 5 Flawless.
 */
object CrystalValue {
	const val FINE_PER_FLAWLESS = 80
	const val FLAWLESS_PER_PERFECT = 5

	/** Coins forging [crystal] is worth, or null while a price it needs is missing. */
	fun of(crystal: CrystalType): Double? = worthOf(crystal)?.value

	fun worthOf(crystal: CrystalType): CrystalWorth? {
		val gem = crystal.displayName.uppercase()
		return worth(
			perfect = Bazaar.price("PERFECT_${gem}_GEM")?.sellOffer,
			fineBuyOrder = Bazaar.price("FINE_${gem}_GEM")?.instantSell,
			flawlessInstaBuy = Bazaar.price("FLAWLESS_${gem}_GEM")?.sellOffer,
		)
	}

	fun worth(perfect: Double?, fineBuyOrder: Double?, flawlessInstaBuy: Double?): CrystalWorth? {
		if (perfect == null) return null
		val viaFine = fineBuyOrder?.times(FINE_PER_FLAWLESS)
		val input = when {
			viaFine == null && flawlessInstaBuy == null -> return null
			flawlessInstaBuy == null -> ForgeInput.FINE
			viaFine == null -> ForgeInput.FLAWLESS
			viaFine <= flawlessInstaBuy -> ForgeInput.FINE
			else -> ForgeInput.FLAWLESS
		}
		val flawless = if (input == ForgeInput.FINE) viaFine!! else flawlessInstaBuy!!
		return CrystalWorth(perfect - FLAWLESS_PER_PERFECT * flawless, input)
	}
}
