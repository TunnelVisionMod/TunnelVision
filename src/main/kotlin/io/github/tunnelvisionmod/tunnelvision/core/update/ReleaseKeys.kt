package io.github.tunnelvisionmod.tunnelvision.core.update

/**
 * Public halves of the keys allowed to sign releases, base64 X.509. `./gradlew generateReleaseKey`
 * adds new ones here. With none listed, the updater refuses every download.
 */
object ReleaseKeys {
	val PUBLIC: List<String> = listOf(
		"MCowBQYDK2VwAyEAquNWF4ZYOOKq013nDKgXyOP/OQczu4syhLXzYQUBnnA=",
	)
}
