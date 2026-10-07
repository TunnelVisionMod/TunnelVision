package io.github.tunnelvisionmod.tunnelvision.core.sound

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SoundFilesTest {
	@TempDir
	lateinit var temp: Path

	private fun file(name: String, content: String): Path =
		temp.resolve("picked").also { Files.createDirectories(it) }.resolve(name).also { Files.writeString(it, content) }

	private val folder get() = temp.resolve("sounds")

	@Test
	fun `only ogg files are accepted`() {
		assertTrue(SoundFiles.isOgg(Path.of("alarm.ogg")))
		assertTrue(SoundFiles.isOgg(Path.of("ALARM.OGG")))
		assertFalse(SoundFiles.isOgg(Path.of("alarm.mp3")))
		assertFalse(SoundFiles.isOgg(Path.of("alarm")))
	}

	@Test
	fun `copies into the sounds folder`() {
		val name = SoundFiles.import(file("alarm.ogg", "a"), folder)
		assertEquals("alarm.ogg", name)
		assertEquals("a", Files.readString(folder.resolve(name)))
	}

	@Test
	fun `the same file twice reuses the copy`() {
		val source = file("alarm.ogg", "a")
		assertEquals("alarm.ogg", SoundFiles.import(source, folder))
		assertEquals("alarm.ogg", SoundFiles.import(source, folder))
		assertEquals(1, Files.list(folder).use { it.count() })
	}

	@Test
	fun `a different file with a taken name gets a suffix`() {
		SoundFiles.import(file("alarm.ogg", "a"), folder)
		val second = SoundFiles.import(file("alarm.ogg", "b"), folder)
		assertEquals("alarm-2.ogg", second)
		assertEquals("a", Files.readString(folder.resolve("alarm.ogg")))
		assertEquals("b", Files.readString(folder.resolve("alarm-2.ogg")))
	}

	@Test
	fun `picking a file already in the sounds folder keeps it`() {
		val name = SoundFiles.import(file("alarm.ogg", "a"), folder)
		assertEquals(name, SoundFiles.import(folder.resolve(name), folder))
	}

	@Test
	fun `every title sound has a unique name`() {
		val names = TitleSound.entries.map { it.displayName }
		assertEquals(names.size, names.toSet().size)
	}
}
