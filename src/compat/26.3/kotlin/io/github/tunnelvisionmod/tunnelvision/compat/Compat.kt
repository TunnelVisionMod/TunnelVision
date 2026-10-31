package io.github.tunnelvisionmod.tunnelvision.compat

import com.mojang.blaze3d.Blaze3D
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import java.nio.file.Path
import net.minecraft.client.Camera
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.network.chat.Component
import org.lwjgl.sdl.SDLDialog
import org.lwjgl.sdl.SDLMouse
import org.lwjgl.sdl.SDL_DialogFileCallback
import org.lwjgl.sdl.SDL_DialogFileFilter
import org.lwjgl.system.MemoryUtil

typealias RenderPipeline = com.mojang.renderpearl.api.pipeline.RenderPipeline
typealias RenderPipelineBuilder = com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder
typealias BlendFunction = com.mojang.renderpearl.api.pipeline.BlendFunction
typealias ColorTargetState = com.mojang.renderpearl.api.pipeline.ColorTargetState
typealias DepthStencilState = com.mojang.renderpearl.api.pipeline.DepthStencilState
typealias CompareOp = com.mojang.renderpearl.api.pipeline.CompareOp
typealias VertexFormat = com.mojang.renderpearl.api.vertex.VertexFormat

/** Minecraft 26.3 versions of the calls that differ between the supported versions. */
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

	fun quads(builder: RenderPipelineBuilder, format: VertexFormat): RenderPipelineBuilder =
		builder.withVertexBinding(0, format).withPrimitiveTopology(PrimitiveTopology.QUADS)

	fun transientBuffer(builder: RenderSetup.RenderSetupBuilder): RenderSetup.RenderSetupBuilder = builder

	fun isKeyDown(keyCode: Int): Boolean = when (keyCode) {
		in InputConstants.MOUSE_BUTTON_LEFT..InputConstants.MOUSE_BUTTON_8 ->
			SDLMouse.SDL_GetMouseState(null, null) and (1 shl (keyCode - 1)) != 0
		else -> keyCode > 0 && InputConstants.isKeyDown(keyCode)
	}

	fun openPath(path: Path) = Blaze3D.openPath(path)

	/** Shows the system file picker; [onPicked] runs on whatever thread SDL calls back on. */
	fun pickFile(title: String, extension: String, filterName: String, onPicked: (Path?) -> Unit) {
		val filters = SDL_DialogFileFilter.calloc(1)
		val name = MemoryUtil.memUTF8(filterName)
		val pattern = MemoryUtil.memUTF8(extension)
		filters[0].name(name).pattern(pattern)
		lateinit var callback: SDL_DialogFileCallback
		callback = SDL_DialogFileCallback.create { _, files, _ ->
			val first = if (files == 0L) 0L else MemoryUtil.memGetAddress(files)
			val picked = if (first == 0L) null else Path.of(MemoryUtil.memUTF8(first))
			filters.free()
			MemoryUtil.memFree(name)
			MemoryUtil.memFree(pattern)
			callback.free()
			onPicked(picked)
		}
		SDLDialog.SDL_ShowOpenFileDialog(callback, 0L, mc.window.handle(), filters, null as CharSequence?, false)
	}
}
