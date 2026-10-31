package io.github.tunnelvisionmod.tunnelvision.compat

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.platform.InputConstants
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import java.nio.file.Path
import net.minecraft.client.Camera
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs

typealias RenderPipeline = com.mojang.blaze3d.pipeline.RenderPipeline
typealias RenderPipelineBuilder = com.mojang.blaze3d.pipeline.RenderPipeline.Builder
typealias BlendFunction = com.mojang.blaze3d.pipeline.BlendFunction
typealias ColorTargetState = com.mojang.blaze3d.pipeline.ColorTargetState
typealias DepthStencilState = com.mojang.blaze3d.pipeline.DepthStencilState
typealias CompareOp = com.mojang.blaze3d.platform.CompareOp
typealias VertexFormat = com.mojang.blaze3d.vertex.VertexFormat

/** Minecraft 26.2 versions of the calls that differ between the supported versions. */
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
		in 0..GLFW.GLFW_MOUSE_BUTTON_LAST -> GLFW.glfwGetMouseButton(mc.window.handle(), keyCode) == GLFW.GLFW_PRESS
		else -> keyCode > 0 && InputConstants.isKeyDown(mc.window, keyCode)
	}

	fun openPath(path: Path) = Util.getPlatform().openPath(path)

	/** Shows the system file picker; [onPicked] runs on a background thread since tinyfd blocks. */
	fun pickFile(title: String, extension: String, filterName: String, onPicked: (Path?) -> Unit) {
		Thread({
			val picked = MemoryStack.stackPush().use { stack ->
				val filters = stack.mallocPointer(1)
				filters.put(stack.UTF8("*.$extension"))
				filters.flip()
				TinyFileDialogs.tinyfd_openFileDialog(title, null, filters, filterName, false)?.let { Path.of(it) }
			}
			onPicked(picked)
		}, "TunnelVision file picker").apply { isDaemon = true }.start()
	}
}
