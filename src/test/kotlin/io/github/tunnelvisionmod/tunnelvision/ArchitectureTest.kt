package io.github.tunnelvisionmod.tunnelvision

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Keeps features independent: a feature never reaches into another feature, and the shared `data`
 * layer never depends on a feature or its toggles. That is what lets every feature work with only
 * its own toggle on.
 */
class ArchitectureTest {
	private val pkg = "io.github.tunnelvisionmod.tunnelvision"
	private val root = File("src/main/kotlin/" + pkg.replace('.', '/'))
	private val importLine = Regex("""^import ${Regex.escape(pkg)}\.([\w.]+)$""", RegexOption.MULTILINE)
	private val configRead = Regex("""ConfigManager\.config\.([\w.]+)""")
	private val sharedSettings = listOf("general.bazaarPrice", "mineshaft.corpses.loot", "dev.")

	private fun sources(): List<Pair<List<String>, String>> =
		root.walkTopDown().filter { it.extension == "kt" }.map { file ->
			file.relativeTo(root).invariantSeparatorsPath.split('/') to file.readText()
		}.toList()

	private fun imports(text: String): List<List<String>> = importLine.findAll(text).map { it.groupValues[1].split('.') }.toList()

	/** `features/<group>/<feature>`, the unit that may share code freely. */
	private fun featureOf(path: List<String>): String? =
		if (path.firstOrNull() == "features" && path.size >= 3) path.take(3).joinToString("/") else null

	@Test
	fun `sources are found`() {
		assertTrue(sources().size > 50, "no sources under ${root.absolutePath}")
	}

	@Test
	fun `features do not import other features`() {
		val violations = sources().flatMap { (path, text) ->
			val own = featureOf(path) ?: return@flatMap emptyList()
			imports(text).mapNotNull { imported ->
				val other = featureOf(imported)
				if (other != null && other != own) "${path.joinToString("/")} imports ${imported.joinToString(".")}" else null
			}
		}
		assertEquals(emptyList<String>(), violations)
	}

	@Test
	fun `data does not import features`() {
		val violations = sources().filter { (path, _) -> path.first() == "data" }.flatMap { (path, text) ->
			imports(text).filter { it.first() == "features" }.map { "${path.joinToString("/")} imports ${it.joinToString(".")}" }
		}
		assertEquals(emptyList<String>(), violations)
	}

	@Test
	fun `data only reads shared settings`() {
		val violations = sources().filter { (path, _) -> path.first() == "data" }.flatMap { (path, text) ->
			configRead.findAll(text).map { it.groupValues[1] }
				.filter { read -> sharedSettings.none { read.startsWith(it) } }
				.map { "${path.joinToString("/")} reads $it" }
				.toList()
		}
		assertEquals(emptyList<String>(), violations)
	}

	@Test
	fun `titles only go through Titles`() {
		val direct = Regex("""Compat\.set(Title|Subtitle|TitleTimes)\(""")
		val allowed = setOf("utils/Titles.kt")
		val violations = sources().filter { (path, text) -> path.joinToString("/") !in allowed && direct.containsMatchIn(text) }
			.map { (path, _) -> "${path.joinToString("/")} shows a title without a TitleSound" }
		assertEquals(emptyList<String>(), violations)
	}
}
