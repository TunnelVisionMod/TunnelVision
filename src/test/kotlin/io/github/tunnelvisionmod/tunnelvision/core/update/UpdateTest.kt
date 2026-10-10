package io.github.tunnelvisionmod.tunnelvision.core.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateTest {
	@Test
	fun `versions compare numerically and a suffix sorts first`() {
		assertTrue(Version.parse("1.10.0")!! > Version.parse("1.9.9")!!)
		assertTrue(Version.parse("1.3.4")!! > Version.parse("1.3.4-sounds")!!)
		assertTrue(Version.parse("v1.4.0")!! > Version.parse("1.3.4")!!)
		assertEquals(Version(1, 4, 0), Version.parse("v1.4.0"))
		assertEquals("1.4.0-beta", Version.parse("1.4.0-beta").toString())
		assertNull(Version.parse("dev"))
	}

	private fun release(
		tag: String,
		assets: List<String>,
		draft: Boolean = false,
		prerelease: Boolean = false,
		page: String = "https://github.com/${UpdateChecker.REPO}/releases/tag/$tag",
	) = """
		{"tag_name": "$tag", "html_url": "$page", "draft": $draft, "prerelease": $prerelease, "assets": [
			${assets.joinToString(",") { """{"name": "$it"}""" }}
		]}
	""".trimIndent()

	private val both = listOf("TunnelVision-v1.4.0-26.1.2.jar", "TunnelVision-v1.4.0-26.2.jar")

	@Test
	fun `links the release page when it has a jar for this minecraft version`() {
		val parsed = UpdateChecker.parseLatest(release("v1.4.0", both), "26.2")
		assertNotNull(parsed)
		assertEquals(Version(1, 4, 0), parsed!!.version)
		assertEquals("https://github.com/${UpdateChecker.REPO}/releases/tag/v1.4.0", parsed.pageUrl)
	}

	@Test
	fun `a release without a jar for this minecraft version is ignored`() {
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", listOf("TunnelVision-v1.4.0-26.1.2.jar")), "26.2"))
	}

	@Test
	fun `drafts, prereleases and pages outside the repository are ignored`() {
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, draft = true), "26.2"))
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, prerelease = true), "26.2"))
		assertNull(UpdateChecker.parseLatest(release("v1.4.0", both, page = "https://example.com/v1.4.0"), "26.2"))
	}

	@Test
	fun `only newer releases are offered`() {
		val release = UpdateChecker.parseLatest(release("v1.4.0", both), "26.2")
		assertNotNull(UpdateChecker.newer(release, Version.parse("1.3.4")))
		assertNull(UpdateChecker.newer(release, Version.parse("1.4.0")))
		assertNull(UpdateChecker.newer(release, Version.parse("1.5.0")))
	}
}
