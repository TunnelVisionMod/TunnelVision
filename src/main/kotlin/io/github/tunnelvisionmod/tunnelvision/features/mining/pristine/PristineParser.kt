package io.github.tunnelvisionmod.tunnelvision.features.mining.pristine

data class PristineProc(val gemstone: String, val amount: Int)

object PristineParser {
	const val PRISTINE_PREFIX = "PRISTINE!"

	// "PRISTINE! You found Flawed Ruby Gemstone x2!" - Hypixel puts a coloured marker between
	// "found" and the tier, which may or may not survive having the formatting stripped.
	private val procLine = Regex("""^PRISTINE! You found .*?Flawed (\w+) Gemstone x(\d+)!$""")

	fun parse(message: String): PristineProc? {
		val (gemstone, amount) = procLine.matchEntire(message.trim())?.destructured ?: return null
		return PristineProc(gemstone, amount.toIntOrNull() ?: return null)
	}

	/** True for any Pristine line, so an unparsed one can be told apart from an unrelated message. */
	fun isPristineMessage(message: String): Boolean = message.trimStart().startsWith(PRISTINE_PREFIX)
}
