package io.github.tunnelvisionmod.tunnelvision.features.mineshaft.corpses

import io.github.tunnelvisionmod.tunnelvision.data.mineshaft.CorpseType
import io.github.tunnelvisionmod.tunnelvision.utils.plainName
import io.github.tunnelvisionmod.tunnelvision.utils.skyblockId
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

fun ArmorStand.corpseType(): CorpseType? {
	if (isInvisible) return null
	val helmet = getItemBySlot(EquipmentSlot.HEAD)
	if (helmet.isEmpty) return null
	return CorpseType.fromHelmet(helmet.skyblockId(), helmet.plainName())
}
