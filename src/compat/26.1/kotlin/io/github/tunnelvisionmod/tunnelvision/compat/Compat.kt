package io.github.tunnelvisionmod.tunnelvision.compat

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexFormat
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import net.minecraft.client.Camera
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.network.chat.Component

/** Minecraft 26.1 versions of the calls that moved in 26.2. */
object Compat {
	val screen: Screen? get() = mc.screen
	val chat: ChatComponent get() = mc.gui.chat
	val tabList: PlayerTabOverlay get() = mc.gui.tabList
	val toasts: ToastManager get() = mc.toastManager
	val camera: Camera get() = mc.gameRenderer.mainCamera

	fun setScreen(screen: Screen?) = mc.setScreen(screen)
	fun setTitleTimes(fadeIn: Int, stay: Int, fadeOut: Int) = mc.gui.setTimes(fadeIn, stay, fadeOut)
	fun setTitle(title: Component) = mc.gui.setTitle(title)
	fun setSubtitle(subtitle: Component) = mc.gui.setSubtitle(subtitle)

	fun quads(builder: RenderPipeline.Builder, format: VertexFormat): RenderPipeline.Builder =
		builder.withVertexFormat(format, VertexFormat.Mode.QUADS)

	fun transientBuffer(builder: RenderSetup.RenderSetupBuilder): RenderSetup.RenderSetupBuilder =
		builder.bufferSize(RenderType.TRANSIENT_BUFFER_SIZE)
}
