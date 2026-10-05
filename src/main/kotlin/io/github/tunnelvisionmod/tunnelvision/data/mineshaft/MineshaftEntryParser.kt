package io.github.tunnelvisionmod.tunnelvision.data.mineshaft

/**
 * Hypixel announces every arrival in a mineshaft that was already open, whoever sent the warp: the
 * guest's name to the host, and your own name to you when you are the one being warped in. Walking
 * into a fresh mineshaft announces nothing, so the name alone tells the two sides apart.
 */
object MineshaftEntryParser {
	private val entryLine = Regex("""^⛏ (\w{1,16}) entered the mineshaft!$""")

	fun playerEntered(text: String): String? = entryLine.matchEntire(text.trim())?.groupValues?.get(1)
}
