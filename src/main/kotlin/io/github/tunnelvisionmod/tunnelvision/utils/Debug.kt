package io.github.tunnelvisionmod.tunnelvision.utils

import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager

object Debug {
	fun log(message: () -> String) {
		if (ConfigManager.config.dev.debugMode) TunnelVision.logger.info("[Debug] ${message()}")
	}
}
