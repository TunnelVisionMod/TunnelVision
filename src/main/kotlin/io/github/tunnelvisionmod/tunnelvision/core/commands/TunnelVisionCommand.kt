package io.github.tunnelvisionmod.tunnelvision.core.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.hud.HudManager
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.core.update.Updater
import io.github.tunnelvisionmod.tunnelvision.data.bazaar.PriceHistory
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalState
import io.github.tunnelvisionmod.tunnelvision.data.crystals.CrystalType
import io.github.tunnelvisionmod.tunnelvision.features.forge.crystals.CrystalNotifications
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses.CorpseTracker
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.GemstoneRoute
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.RouteLibrary
import io.github.tunnelvisionmod.tunnelvision.features.mineshaft.routes.RouteRunner
import io.github.tunnelvisionmod.tunnelvision.utils.ChatUtils
import io.github.tunnelvisionmod.tunnelvision.utils.formatCoins
import io.github.tunnelvisionmod.tunnelvision.utils.formatPrice
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component

object TunnelVisionCommand {
	fun register() {
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			for (name in listOf("tunnelvision", "tv")) {
				dispatcher.register(
					literal(name)
						.executes {
							ConfigManager.openScreen()
							1
						}
						.then(literal("hud").executes {
							HudManager.openEditor()
							1
						})
						.then(literal("sounds").executes {
							TitleSounds.openScreen()
							1
						})
						.then(
							literal("update")
								.executes {
									Updater.command()
									1
								}
								.then(literal("now").executes {
									Updater.installNow()
									1
								})
								.then(literal("onexit").executes {
									Updater.installOnExit()
									1
								}),
						)
						.then(literal("prices").executes {
							PriceHistory.refresh(announce = true)
							1
						})
						.then(crystalCommand())
						.then(routeCommand())
						.then(corpsesCommand())
				)
			}
		}
	}

	/** `/tv corpses` reports the running corpse profit, `/tv corpses reset` clears it. */
	private fun corpsesCommand() = literal("corpses")
		.executes {
			showCorpseProfit()
			1
		}
		.then(literal("reset").executes {
			CorpseTracker.reset()
			ChatUtils.send(Component.literal("Corpse profit reset.").withStyle(ChatFormatting.GREEN))
			1
		})
		.then(
			literal("meter").then(
				argument("xp", DoubleArgumentType.doubleArg(0.0)).executes { context ->
					val xp = DoubleArgumentType.getDouble(context, "xp")
					CorpseTracker.setMeter(xp)
					ChatUtils.send(
						Component.literal("RNG meter set to " + formatPrice(xp) + " / " + formatPrice(CorpseTracker.meterNeeded))
							.withStyle(ChatFormatting.GREEN),
					)
					1
				},
			),
		)

	private fun showCorpseProfit() {
		val total = CorpseTracker.overall
		if (total.corpses == 0) {
			ChatUtils.send(Component.literal("No corpses looted yet.").withStyle(ChatFormatting.GRAY))
			return
		}
		val color = if (total.coins < 0) ChatFormatting.RED else ChatFormatting.GREEN
		ChatUtils.send(
			Component.literal(total.corpses.toString() + " corpses: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(formatCoins(total.coins)).withStyle(color))
				.append(
					Component.literal(
						"  RNG meter " + formatPrice(CorpseTracker.meterProgress) + " / " + formatPrice(CorpseTracker.meterNeeded),
					).withStyle(ChatFormatting.DARK_GRAY),
				),
		)
	}

	/**
	 * Crystals are rare enough that waiting to find one is no way to test the notifications, so
	 * this pretends you carry them. `/tv crystal all`, `/tv crystal none`, `/tv crystal <name>` to
	 * toggle one, `/tv crystal off` to go back to the real data, and `/tv crystal entry` to replay the
	 * entry notification.
	 */
	private fun crystalCommand() = literal("crystal")
		.requires { ConfigManager.config.dev.debugMode }
		.executes {
			showCarried()
			1
		}
		.then(literal("all").executes {
			CrystalState.setCarriedForTesting(CrystalType.entries.associateWith { true })
			showCarried()
			1
		})
		.then(literal("off").executes {
			CrystalState.endTesting()
			ChatUtils.send(Component.literal("Crystal test mode off, the tab list and /hotm are used again.").withStyle(ChatFormatting.GREEN))
			1
		})
		.then(literal("none").executes {
			CrystalState.setCarriedForTesting(CrystalType.entries.associateWith { false })
			showCarried()
			1
		})
		.then(literal("entry").executes {
			CrystalNotifications.replayEntryNotification()
			ChatUtils.send(Component.literal("Replaying the entry notification.").withStyle(ChatFormatting.GREEN))
			1
		})
		.also { node ->
			for (crystal in CrystalType.entries) {
				node.then(literal(crystal.displayName.lowercase()).executes {
					val carried = crystal !in CrystalState.carried
					CrystalState.setCarriedForTesting(mapOf(crystal to carried))
					showCarried()
					1
				})
			}
		}

	/**
	 * Previews a gemstone route in any world, so routes can be walked in downloaded mineshaft saves.
	 * In debug mode `config/tunnelvision/routes/<layout>.json` overrides the bundled route.
	 * `first`/`second` show one half of a split route, `skip`/`back` move the target and `off` hides it.
	 */
	private fun routeCommand() = literal("route")
		.requires { ConfigManager.config.dev.debugMode }
		.then(literal("off").executes {
			RouteRunner.stop()
			1
		})
		.then(literal("skip").executes {
			RouteRunner.step(1)
			1
		})
		.then(literal("back").executes {
			RouteRunner.step(-1)
			1
		})
		.then(argument("layout", StringArgumentType.word())
			.suggests { _, builder -> SharedSuggestionProvider.suggest(RouteLibrary.names, builder) }
			.executes { startRoute(it, GemstoneRoute.Part.ALL) }
			.then(literal("first").executes { startRoute(it, GemstoneRoute.Part.FIRST) })
			.then(literal("second").executes { startRoute(it, GemstoneRoute.Part.SECOND) }))

	private fun startRoute(context: CommandContext<FabricClientCommandSource>, part: GemstoneRoute.Part): Int {
		val name = StringArgumentType.getString(context, "layout")
		val route = RouteLibrary.load(name)
		if (route == null) {
			ChatUtils.send(Component.literal("No route called $name").withStyle(ChatFormatting.RED))
			return 1
		}
		val waypoints = route.part(part)
		RouteRunner.start(waypoints) { ConfigManager.config.dev.debugMode }
		ChatUtils.send(Component.literal("$name (${part.name.lowercase()}): ${waypoints.size} waypoints, ${waypoints.sumOf { it.blocks.size }} gems").withStyle(ChatFormatting.GREEN))
		return 1
	}

	private fun showCarried() {
		val carried = CrystalState.carried
		val text = Component.literal("Crystals (" + carried.size + "/" + CrystalType.entries.size + "): ")
			.withStyle(ChatFormatting.AQUA)
		if (carried.isEmpty()) {
			text.append(Component.literal("none").withStyle(ChatFormatting.GRAY))
		} else {
			for ((index, crystal) in carried.withIndex()) {
				if (index > 0) text.append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
				text.append(Component.literal(crystal.displayName).withStyle(crystal.color))
			}
		}
		ChatUtils.send(text)
	}
}
