package io.github.tunnelvisionmod.tunnelvision.data.party

data class PartyMessage(val author: String, val message: String)

object PartyChatParser {
	private val partyLine = Regex("""^Party > (?:.*? )?(\w{1,16}): (.*)$""")

	fun parse(text: String): PartyMessage? {
		val (author, message) = partyLine.matchEntire(text.trim())?.destructured ?: return null
		return PartyMessage(author, message)
	}
}
