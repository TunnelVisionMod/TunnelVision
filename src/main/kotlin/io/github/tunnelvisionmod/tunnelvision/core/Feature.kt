package io.github.tunnelvisionmod.tunnelvision.core

/**
 * A toggleable feature. Features only read from `data`, `core` and `utils`, never from another
 * feature, so each one works with only its own toggle on (checked by ArchitectureTest).
 */
interface Feature {
	fun init()
}
