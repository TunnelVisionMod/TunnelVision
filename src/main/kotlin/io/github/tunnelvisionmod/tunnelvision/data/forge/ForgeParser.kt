package io.github.tunnelvisionmod.tunnelvision.data.forge

data class ForgeSlot(val slot: Int, val item: String, val isReady: Boolean)

/**
 * The Forges widget as a whole: what is cooking, and whether there is anywhere left to put
 * something. [openSlots] counts `EMPTY` lines; [lockedSlots] counts `LOCKED` ones, which are not
 * open - those need Quick Forge levels, not a free hand.
 */
data class ForgeStatus(val slots: List<ForgeSlot>, val openSlots: Int, val lockedSlots: Int) {
	/** No `EMPTY` line left, so nothing more can be started. */
	val isFull: Boolean get() = openSlots == 0
}

object ForgeParser {
	private const val TAB_HEADER = "Forges:"
	private const val EMPTY_SLOT = "EMPTY"
	private const val LOCKED_SLOT = "LOCKED"
	private val slotLine = Regex("""^(\d+)\) (.+)$""")
	private val occupiedSlot = Regex("""^(.+): (.+)$""")
	private val readyStates = setOf("ready!", "ready")

	/** Only the occupied slots, or null when the widget is not shown. */
	fun parseTab(lines: List<String>): List<ForgeSlot>? = read(lines)?.slots

	/**
	 * Null when the widget is not shown, and also when it is shown with no slot lines under it -
	 * unknown is not the same as full, and callers must not read "the forge is full" out of either.
	 */
	fun parseStatus(lines: List<String>): ForgeStatus? = read(lines)?.takeIf { it.sawSlot }?.status

	private class Read(val status: ForgeStatus, val sawSlot: Boolean) {
		val slots get() = status.slots
	}

	private fun read(lines: List<String>): Read? {
		val header = lines.indexOf(TAB_HEADER)
		if (header < 0) return null
		val slots = mutableListOf<ForgeSlot>()
		var open = 0
		var locked = 0
		var sawSlot = false
		for (line in lines.drop(header + 1)) {
			val (slot, content) = slotLine.matchEntire(line)?.destructured ?: break
			sawSlot = true
			val occupied = occupiedSlot.matchEntire(content)?.destructured
			when {
				occupied != null -> {
					val (item, state) = occupied
					slots += ForgeSlot(slot.toInt(), item.trimStart { !it.isLetterOrDigit() }, state.lowercase() in readyStates)
				}
				content.equals(EMPTY_SLOT, ignoreCase = true) -> open++
				content.equals(LOCKED_SLOT, ignoreCase = true) -> locked++
			}
		}
		return Read(ForgeStatus(slots, open, locked), sawSlot)
	}
}
