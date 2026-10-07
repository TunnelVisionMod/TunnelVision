package io.github.tunnelvisionmod.tunnelvision.core.update

import io.github.tunnelvisionmod.tunnelvision.update.TunnelVisionUpdateHelper
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class UpdateTest {
	@TempDir
	lateinit var temp: Path

	@Test
	fun `versions compare numerically and a suffix sorts first`() {
		assertTrue(Version.parse("1.10.0")!! > Version.parse("1.9.9")!!)
		assertTrue(Version.parse("1.3.4")!! > Version.parse("1.3.4-sounds")!!)
		assertTrue(Version.parse("v1.4.0")!! > Version.parse("1.3.4")!!)
		assertEquals(Version(1, 4, 0), Version.parse("v1.4.0"))
		assertEquals("1.4.0-beta", Version.parse("1.4.0-beta").toString())
		assertNull(Version.parse("dev"))
	}

	private fun release(tag: String, assets: List<String>, draft: Boolean = false, prerelease: Boolean = false, repo: String = UpdateChecker.REPO) = """
		{"tag_name": "$tag", "draft": $draft, "prerelease": $prerelease, "assets": [
			${assets.joinToString(",") { """{"name": "$it", "browser_download_url": "https://github.com/$repo/releases/download/$tag/$it"}""" }}
		]}
	""".trimIndent()

	private val both = listOf(
		"TunnelVision-v1.4.0-26.1.2.jar", "TunnelVision-v1.4.0-26.1.2.jar.sig",
		"TunnelVision-v1.4.0-26.2.jar", "TunnelVision-v1.4.0-26.2.jar.sig",
	)

	@Test
	fun `picks the jar for this minecraft version`() {
		val parsed = UpdateChecker.parseLatest(release("v1.4.0", both), "26.2")
		assertNotNull(parsed)
		assertEquals("TunnelVision-v1.4.0-26.2.jar", parsed!!.jarName)
		assertTrue(parsed.signatureUrl.endsWith("TunnelVision-v1.4.0-26.2.jar.sig"))
	}

	@Test
	fun `an unsigned release is ignored`() {
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", listOf("TunnelVision-v1.4.0-26.2.jar")), "26.2"))
	}

	@Test
	fun `drafts, prereleases and other repositories are ignored`() {
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, draft = true), "26.2"))
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, prerelease = true), "26.2"))
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, repo = "evil/TunnelVision"), "26.2"))
	}

	@Test
	fun `only newer releases are offered`() {
		val release = UpdateChecker.parseLatest(release("v1.4.0", both), "26.2")
		assertNotNull(UpdateChecker.newer(release, Version.parse("1.3.4")))
		assertNull(UpdateChecker.newer(release, Version.parse("1.4.0")))
		assertNull(UpdateChecker.newer(release, Version.parse("1.5.0")))
	}

	@Test
	fun `signatures only verify with the right key and the exact bytes`() {
		val generator = KeyPairGenerator.getInstance("Ed25519")
		val ours = generator.generateKeyPair()
		val theirs = generator.generateKeyPair()
		val jar = "jar bytes".toByteArray()
		val signature = Signature.getInstance("Ed25519").run {
			initSign(ours.private)
			update(jar)
			Base64.getEncoder().encodeToString(sign())
		}
		val ourKey = Base64.getEncoder().encodeToString(ours.public.encoded)
		val theirKey = Base64.getEncoder().encodeToString(theirs.public.encoded)
		assertTrue(UpdateVerifier.verify(jar, signature, listOf(ourKey)))
		assertTrue(UpdateVerifier.verify(jar, signature, listOf(theirKey, ourKey)))
		assertFalse(UpdateVerifier.verify(jar, signature, listOf(theirKey)))
		assertFalse(UpdateVerifier.verify("tampered".toByteArray(), signature, listOf(ourKey)))
		assertFalse(UpdateVerifier.verify(jar, "not base64!", listOf(ourKey)))
		assertFalse(UpdateVerifier.verify(jar, signature, emptyList()))
	}

	@Test
	fun `the helper swaps the jar and leaves exactly one`() {
		val mods = Files.createDirectories(temp.resolve("mods"))
		val old = Files.writeString(mods.resolve("TunnelVision-v1.3.4-26.1.2.jar"), "old")
		val downloaded = Files.writeString(temp.resolve("TunnelVision-v1.4.0-26.1.2.jar"), "new")
		val target = mods.resolve("TunnelVision-v1.4.0-26.1.2.jar")
		val hash = TunnelVisionUpdateHelper.sha256("new".toByteArray())
		assertEquals("ok 1.4.0", TunnelVisionUpdateHelper.install(downloaded, old, target, "1.4.0", hash))
		assertEquals(listOf("TunnelVision-v1.4.0-26.1.2.jar"), Files.list(mods).use { files -> files.map { it.fileName.toString() }.toList() })
		assertEquals("new", Files.readString(target))
	}

	@Test
	fun `the helper refuses a jar that changed after verification`() {
		val mods = Files.createDirectories(temp.resolve("mods"))
		val old = Files.writeString(mods.resolve("TunnelVision-v1.3.4-26.1.2.jar"), "old")
		val downloaded = Files.writeString(temp.resolve("TunnelVision-v1.4.0-26.1.2.jar"), "swapped")
		val hash = TunnelVisionUpdateHelper.sha256("new".toByteArray())
		val result = TunnelVisionUpdateHelper.install(downloaded, old, mods.resolve("TunnelVision-v1.4.0-26.1.2.jar"), "1.4.0", hash)
		assertTrue(result.startsWith("failed 1.4.0"), result)
		assertEquals(listOf("TunnelVision-v1.3.4-26.1.2.jar"), Files.list(mods).use { files -> files.map { it.fileName.toString() }.toList() })
	}

	@Test
	fun `the helper restores the old jar when the new one is missing`() {
		val mods = Files.createDirectories(temp.resolve("mods"))
		val old = Files.writeString(mods.resolve("TunnelVision-v1.3.4-26.1.2.jar"), "old")
		val result = TunnelVisionUpdateHelper.install(temp.resolve("missing.jar"), old, mods.resolve("TunnelVision-v1.4.0-26.1.2.jar"), "1.4.0", "x")
		assertTrue(result.startsWith("failed 1.4.0"), result)
		assertEquals(listOf("TunnelVision-v1.3.4-26.1.2.jar"), Files.list(mods).use { files -> files.map { it.fileName.toString() }.toList() })
		assertEquals("old", Files.readString(old))
	}
}
