package io.github.tunnelvisionmod.tunnelvision.core.update

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Checks a downloaded jar against its `.sig`. Only jars signed with a key from [ReleaseKeys] pass,
 * so a release uploaded by anyone without the private key - a hijacked GitHub account, a leaked
 * token, a tampered CI run - can never be installed by the updater.
 */
object UpdateVerifier {
	private const val ALGORITHM = "Ed25519"

	fun verify(data: ByteArray, signatureBase64: String, publicKeys: List<String> = ReleaseKeys.PUBLIC): Boolean {
		val signature = runCatching { Base64.getDecoder().decode(signatureBase64.trim()) }.getOrNull() ?: return false
		return publicKeys.any { key -> runCatching { verifyWith(key, data, signature) }.getOrDefault(false) }
	}

	private fun verifyWith(publicKey: String, data: ByteArray, signature: ByteArray): Boolean {
		val key = KeyFactory.getInstance(ALGORITHM).generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
		return Signature.getInstance(ALGORITHM).run {
			initVerify(key)
			update(data)
			verify(signature)
		}
	}
}
