package io.github.tunnelvisionmod.tunnelvision.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Swaps the mod jar once Minecraft has exited, because Windows keeps the running jar locked.
 *
 * Started by the updater as its own process, from a copy outside the mods folder, with plain Java only:
 * the Kotlin runtime is a mod too and not on this classpath. The old jar is renamed to a .bak first and
 * only deleted once the new one is in place, so whatever fails, the mods folder ends up with exactly one
 * working jar.
 *
 * The downloaded jar must still have the SHA-256 the game recorded when it verified the signature, so a
 * file swapped in between is never installed.
 *
 * Arguments: game pid, downloaded jar, old jar, target jar, result file, version, SHA-256 of the jar.
 */
public final class TunnelVisionUpdateHelper {
	private static final long RETRY_MS = 30_000;
	private static final long RETRY_STEP_MS = 500;

	private TunnelVisionUpdateHelper() {
	}

	public static void main(String[] args) throws Exception {
		long pid = Long.parseLong(args[0]);
		ProcessHandle.of(pid).ifPresent(game -> game.onExit().join());
		String result = install(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), args[5], args[6]);
		Path resultFile = Path.of(args[4]);
		Files.createDirectories(resultFile.getParent());
		Files.writeString(resultFile, result);
	}

	public static String sha256(byte[] data) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String install(Path downloaded, Path oldJar, Path target, String version, String expectedHash) throws InterruptedException {
		try {
			if (!sha256(Files.readAllBytes(downloaded)).equals(expectedHash)) {
				return "failed " + version + " the downloaded jar changed after it was verified";
			}
		} catch (IOException e) {
			return "failed " + version + " the downloaded jar is gone (" + e + ")";
		}
		Path backup = oldJar.resolveSibling(oldJar.getFileName() + ".bak");
		IOException lastError = null;
		long deadline = System.currentTimeMillis() + RETRY_MS;
		while (true) {
			try {
				Files.move(oldJar, backup, StandardCopyOption.REPLACE_EXISTING);
				break;
			} catch (IOException e) {
				lastError = e;
				if (System.currentTimeMillis() > deadline) return "failed " + version + " the old jar stayed locked (" + lastError + ")";
				Thread.sleep(RETRY_STEP_MS);
			}
		}
		try {
			Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			try {
				Files.move(backup, oldJar, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException restore) {
				return "failed " + version + " and the old jar is now " + backup.getFileName() + " - rename it back to .jar (" + e + ")";
			}
			return "failed " + version + " could not place the new jar (" + e + ")";
		}
		try {
			Files.deleteIfExists(backup);
		} catch (IOException ignored) {
			// A leftover .bak is not loaded by Fabric, so the update still counts.
		}
		return "ok " + version;
	}
}
