package io.github.tunnelvisionmod.tunnelvision.compat

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexFormat
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import net.minecraft.client.Camera
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.network.chat.Component

/** Minecraft 26.2 versions of the calls that moved since 26.1. */
object Compat {
	val screen: Screen? get() = mc.gui.screen()
	val chat: ChatComponent get() = mc.gui.hud.chat
	val tabList: PlayerTabOverlay get() = mc.gui.hud.tabList
	val toasts: ToastManager get() = mc.gui.toastManager()
	val camera: Camera get() = mc.gameRenderer.mainCamera()

	fun setScreen(screen: Screen?) = mc.gui.setScreen(screen)
	fun setTitleTimes(fadeIn: Int, stay: Int, fadeOut: Int) = mc.gui.hud.setTimes(fadeIn, stay, fadeOut)
	fun setTitle(title: Component) = mc.gui.hud.setTitle(title)
	fun setSubtitle(subtitle: Component) = mc.gui.hud.setSubtitle(subtitle)

	fun quads(builder: RenderPipeline.Builder, format: VertexFormat): RenderPipeline.Builder =
		builder.withVertexBinding(0, format).withPrimitiveTopology(PrimitiveTopology.QUADS)

	fun transientBuffer(builder: RenderSetup.RenderSetupBuilder): RenderSetup.RenderSetupBuilder = builder
}
