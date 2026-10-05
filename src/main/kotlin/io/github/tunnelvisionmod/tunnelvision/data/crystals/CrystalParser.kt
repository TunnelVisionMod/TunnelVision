package io.github.tunnelvisionmod.tunnelvision.data.crystals

/** One crystal and whether you are carrying it. */
data class CrystalReading(val crystal: CrystalType, val carried: Boolean)

/**
 * Reads which crystals you are carrying, from two sources:
 *
 * - the `Crystal Hollows Crystals` item in the Heart of the Mountain menu, whose lore lists every
 *   crystal and is the only complete source, and
 * - the chat line you get when one drops, which works while no menu is open.
 *
 * The `Crystals:` tab widget is deliberately ignored: it only lists a few crystals and lags behind
 * the forge, putting spent crystals back.
 *
 * The lore looks like this, with the crystals we care about in the second block:
 *
 * ```
 * Your Crystal Nucleus
 * Jade  X Not Found
 * Topaz V Placed
 *
 * Your Other Crystals
 * Jasper X Not Found
 * Ruby V Found
 * ```
 *
 * The tick and cross are decoration, so a line is read as its leading name plus a state, and only
 * a state that clearly says otherwise counts as not carried. `Not Found` is checked before `Found`
 * for the obvious reason.
 */
object CrystalParser {
	/** The Heart of the Mountain item whose lore lists every crystal. */
	const val HOTM_ITEM = "Crystal Hollows Crystals"

	/** Spending wording, which has to be ruled out before a line counts as picking one up. */
	private const val CRYSTAL_WORD = " crystal"

	/**
	 * A line somebody typed, rather than one the server generated - `[534] [MVP+] ImNeppy: Hello`,
	 * `Party > Bob: jasper crystal`, or plain `ImNeppy: Hello`.
	 *
	 * Both the level and the rank are optional, since a player can hide the level and need not have
	 * a rank, so the only reliable part is `<name>:` after any bracketed tags. Anyone could
	 * otherwise hand you a crystal just by typing its name.
	 */
	private val playerChat = Regex("""^(?:[A-Za-z-]+ > )?(?:\[[^\]]*\] )*\w{1,16}(?: \[[^\]]*\])?: """)

	/** Spending one - forging with it, or placing it in the nucleus. Both mean it is gone. */
	private val chatConsumed = Regex("""You (?:placed|used|consumed|spent|forged)(?: your)? (?:a |an )?(\w+) Crystal""", RegexOption.IGNORE_CASE)

	/** Wording that means the crystal is NOT in hand. Everything else counts as carried. */
	private val missingStates = listOf("not found", "not placed", "missing", "none", "undiscovered")

	/**
	 * One `Jasper X Not Found` style line from the menu lore. Null when
	 * the line does not start with a crystal name.
	 */
	fun parseCrystalLine(line: String): CrystalReading? {
		val trimmed = line.trim()
		val name = trimmed.takeWhile { it.isLetter() }
		if (name.isEmpty()) return null
		val crystal = CrystalType.byDisplayName(name) ?: return null
		// The rest of the line is the state. A bare name with nothing after it tells us nothing,
		// so treat it as not carried rather than inventing a crystal.
		val state = trimmed.drop(name.length).trim()
		if (state.isEmpty()) return null
		return CrystalReading(crystal, !isMissing(state))
	}

	/**
	 * Every crystal named anywhere in the menu item's lore. The nucleus crystals listed above ours
	 * are not in [CrystalType] and fall out on their own.
	 */
	fun parseHotmLore(lore: List<String>): Map<CrystalType, Boolean> {
		val states = linkedMapOf<CrystalType, Boolean>()
		for (line in lore) {
			val reading = parseCrystalLine(line) ?: continue
			states[reading.crystal] = reading.carried
		}
		return states
	}

	/** True for a line somebody typed, which must never move crystal state. */
	fun isPlayerChat(message: String): Boolean = playerChat.containsMatchIn(message.trim())

	/**
	 * The crystal you just picked up. The drop line is the crystal name on its own, indented, so
	 * the whole line has to match rather than merely containing a name - that is what keeps a
	 * sentence mentioning a crystal from counting as finding one.
	 *
	 * Both `Jasper` and `Jasper Crystal` are accepted, with trailing punctuation ignored.
	 */
	fun parseChatGained(message: String): CrystalType? {
		val line = message.trim().trimEnd('!', '.', '✖', '✔').trim()
		return CrystalType.entries.firstOrNull {
			line.equals(it.displayName, ignoreCase = true) ||
				line.equals(it.displayName + CRYSTAL_WORD, ignoreCase = true)
		}
	}

	/** The crystal you just spent, if this line says you did. */
	fun parseChatConsumed(message: String): CrystalType? =
		chatConsumed.find(message)?.groupValues?.get(1)?.let { CrystalType.byDisplayName(it) }

	private fun isMissing(text: String): Boolean {
		val cleaned = text.lowercase()
		return missingStates.any { cleaned.contains(it) }
	}
}
