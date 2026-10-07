package io.github.tunnelvisionmod.tunnelvision.core.sound

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Copies chosen sounds into the sounds folder, so a setting keeps working after the original is
 * moved or deleted.
 *
 * Choosing the same file twice reuses the copy; a different file with a name already taken gets
 * `-2`, `-3` and so on, so one alert's sound never silently replaces another's.
 */
object SoundFiles {
	const val EXTENSION = "ogg"

	fun isOgg(path: Path): Boolean = path.fileName.toString().substringAfterLast('.', "").equals(EXTENSION, ignoreCase = true)

	/** Copies [source] into [folder] and returns the name it was stored under. */
	fun import(source: Path, folder: Path): String {
		Files.createDirectories(folder)
		val base = source.fileName.toString().substringBeforeLast('.')
		var index = 1
		while (true) {
			val name = if (index == 1) "$base.$EXTENSION" else "$base-$index.$EXTENSION"
			val target = folder.resolve(name)
			if (!Files.exists(target)) {
				Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)
				return name
			}
			if (Files.mismatch(source, target) == -1L) return name
			index++
		}
	}
}
