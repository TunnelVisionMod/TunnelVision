package io.github.tunnelvisionmod.tunnelvision.core.update

/**
 * A release version, `1.4.0` or `1.4.0-beta`. A suffix sorts below the same version without one,
 * so a local `1.3.4-sounds` build is offered the real `1.3.4`.
 */
data class Version(val major: Int, val minor: Int, val patch: Int, val suffix: String? = null) : Comparable<Version> {
	override fun compareTo(other: Version): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
		.takeIf { it != 0 }
		?: when {
			suffix == other.suffix -> 0
			suffix == null -> 1
			other.suffix == null -> -1
			else -> suffix.compareTo(other.suffix)
		}

	override fun toString(): String = "$major.$minor.$patch" + (suffix?.let { "-$it" } ?: "")

	companion object {
		private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$""")

		fun parse(text: String): Version? = pattern.matchEntire(text.trim())?.destructured?.let { (major, minor, patch, suffix) ->
			Version(major.toInt(), minor.toInt(), patch.toInt(), suffix.ifEmpty { null })
		}
	}
}
