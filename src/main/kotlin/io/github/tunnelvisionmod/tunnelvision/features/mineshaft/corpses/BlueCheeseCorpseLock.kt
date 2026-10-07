package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.core.Feature
import io.github.tunnelvisionmod.tunnelvision.core.config.ConfigManager
import io.github.tunnelvisionmod.tunnelvision.core.events.EventBus
import io.github.tunnelvisionmod.tunnelvision.core.events.RightClickEvent
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSound
import io.github.tunnelvisionmod.tunnelvision.core.sound.TitleSounds
import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.utils.Debug
import io.github.tunnelvisionmod.tunnelvision.utils.SkyBlock
import io.github.tunnelvisionmod.tunnelvision.utils.Titles
import io.github.tunnelvisionmod.tunnelvision.utils.customData
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.EntityHitResult

object BlueCheeseCorpseLock : Feature {
	private const val UPGRADE_MODULE_KEY = "drill_part_upgrade_module"
	private const val WARNING_COOLDOWN_MS = 1000L

	private val config get() = ConfigManager.config.mineshaft.corpses.blueCheeseCorpseLock

	private var lastWarning = 0L

	override fun init() {
		EventBus.on<RightClickEvent> { onRightClick(it) }
	}

	private fun onRightClick(event: RightClickEvent) {
		if (!config.enabled || !SkyBlock.isInMineshaft) return
		val player = mc.player ?: return
		val corpse = targetedCorpse() ?: return
		if (player.mainHandItem.hasBlueCheese()) return
		if (!player.inventory.hasBlueCheeseDrill()) return
		event.cancel()
		warn(corpse)
	}

	private fun targetedCorpse(): CorpseType? {
		val stand = (mc.hitResult as? EntityHitResult)?.entity as? ArmorStand ?: return null
		return stand.corpseType()
	}

	private fun ItemStack.hasBlueCheese(): Boolean =
		!isEmpty && BlueCheese.isUpgradeModule(customData()?.getString(UPGRADE_MODULE_KEY)?.orElse(null))

	private fun Inventory.hasBlueCheeseDrill(): Boolean =
		(0 until Inventory.INVENTORY_SIZE).any { getItem(it).hasBlueCheese() }

	private fun warn(corpse: CorpseType) {
		val now = System.currentTimeMillis()
		if (now - lastWarning < WARNING_COOLDOWN_MS) return
		lastWarning = now
		Debug.log { "BlueCheeseCorpseLock: blocked looting $corpse corpse while not holding blue cheese drill" }
		if (config.showTitle) {
			Titles.show(Component.literal("Blue Cheese!").withStyle(ChatFormatting.RED), TitleSound.BLUE_CHEESE, 0, 20, 5)
		} else {
			TitleSounds.play(TitleSound.BLUE_CHEESE)
		}
	}
}
