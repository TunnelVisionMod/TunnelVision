package io.github.tunnelvisionmod.tunnelvision.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import io.github.tunnelvisionmod.tunnelvision.TunnelVision
import io.github.tunnelvisionmod.tunnelvision.TunnelVision.mc
import io.github.tunnelvisionmod.tunnelvision.compat.Compat
import kotlin.math.max
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object WorldRender {
	private const val LINE_WIDTH = 2f
	private const val LABEL_BASE_SCALE = 0.03f
	private const val LABEL_SCALE_DISTANCE = 8.0
	private const val LABEL_BACKGROUND = 0x90000000.toInt()
	private const val OPAQUE = 0xFF000000.toInt()

	private val linesThroughWallsPipeline: RenderPipeline = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(TunnelVision.MOD_ID, "pipeline/lines_through_walls"))
			.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.build()
	)

	private val filledThroughWallsPipeline: RenderPipeline = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(TunnelVision.MOD_ID, "pipeline/filled_through_walls"))
			.let { Compat.quads(it, DefaultVertexFormat.POSITION_COLOR) }
			.withCull(false)
			.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
			.build()
	)

	private val linesThroughWalls: RenderType = RenderType.create(
		"${TunnelVision.MOD_ID}_lines_through_walls",
		RenderSetup.builder(linesThroughWallsPipeline).let { Compat.transientBuffer(it) }.createRenderSetup()
	)

	private val filledThroughWalls: RenderType = RenderType.create(
		"${TunnelVision.MOD_ID}_filled_through_walls",
		RenderSetup.builder(filledThroughWallsPipeline).let { Compat.transientBuffer(it) }.sortOnUpload().createRenderSetup()
	)

	private val camera get() = Compat.camera.position()

	fun outline(context: LevelRenderContext, box: AABB, color: Int, throughWalls: Boolean) {
		val renderType = if (throughWalls) linesThroughWalls else RenderTypes.lines()
		val shifted = box.move(camera.reverse())
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), renderType) { pose, buffer ->
			val x0 = shifted.minX.toFloat()
			val y0 = shifted.minY.toFloat()
			val z0 = shifted.minZ.toFloat()
			val x1 = shifted.maxX.toFloat()
			val y1 = shifted.maxY.toFloat()
			val z1 = shifted.maxZ.toFloat()
			fun line(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float) {
				val nx = bx - ax
				val ny = by - ay
				val nz = bz - az
				buffer.lineVertex(pose, ax, ay, az, color, nx, ny, nz)
				buffer.lineVertex(pose, bx, by, bz, color, nx, ny, nz)
			}
			line(x0, y0, z0, x1, y0, z0); line(x0, y0, z1, x1, y0, z1); line(x0, y1, z0, x1, y1, z0); line(x0, y1, z1, x1, y1, z1)
			line(x0, y0, z0, x0, y1, z0); line(x1, y0, z0, x1, y1, z0); line(x0, y0, z1, x0, y1, z1); line(x1, y0, z1, x1, y1, z1)
			line(x0, y0, z0, x0, y0, z1); line(x1, y0, z0, x1, y0, z1); line(x0, y1, z0, x0, y1, z1); line(x1, y1, z0, x1, y1, z1)
		}
	}

	fun lines(context: LevelRenderContext, segments: List<Pair<Vec3, Vec3>>, color: Int, throughWalls: Boolean) {
		if (segments.isEmpty()) return
		val renderType = if (throughWalls) linesThroughWalls else RenderTypes.lines()
		val cameraPos = camera
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), renderType) { pose, buffer ->
			for ((from, to) in segments) {
				val a = from.subtract(cameraPos)
				val b = to.subtract(cameraPos)
				val nx = (b.x - a.x).toFloat()
				val ny = (b.y - a.y).toFloat()
				val nz = (b.z - a.z).toFloat()
				buffer.lineVertex(pose, a.x.toFloat(), a.y.toFloat(), a.z.toFloat(), color, nx, ny, nz)
				buffer.lineVertex(pose, b.x.toFloat(), b.y.toFloat(), b.z.toFloat(), color, nx, ny, nz)
			}
		}
	}

	fun filled(context: LevelRenderContext, box: AABB, color: Int) {
		val shifted = box.move(camera.reverse())
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), filledThroughWalls) { pose, buffer ->
			val x0 = shifted.minX.toFloat()
			val y0 = shifted.minY.toFloat()
			val z0 = shifted.minZ.toFloat()
			val x1 = shifted.maxX.toFloat()
			val y1 = shifted.maxY.toFloat()
			val z1 = shifted.maxZ.toFloat()
			fun quad(vararg corners: Float) {
				for (i in corners.indices step 3) buffer.addVertex(pose, corners[i], corners[i + 1], corners[i + 2]).setColor(color)
			}
			quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1)
			quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0)
			quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0)
			quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1)
			quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0)
			quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1)
		}
	}

	fun label(context: LevelRenderContext, pos: Vec3, text: Component, color: Int) {
		val cameraPos = camera
		val scale = LABEL_BASE_SCALE * max(1.0, cameraPos.distanceTo(pos) / LABEL_SCALE_DISTANCE).toFloat()
		val poseStack = context.poseStack()
		poseStack.pushPose()
		poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
		poseStack.mulPose(Compat.camera.rotation())
		poseStack.scale(scale, -scale, scale)
		val chars = text.visualOrderText
		context.submitNodeCollector().submitText(
			poseStack, -mc.font.width(chars) / 2f, 0f, chars, true, Font.DisplayMode.SEE_THROUGH,
			LightCoordsUtil.FULL_BRIGHT, color or OPAQUE, LABEL_BACKGROUND, 0,
		)
		poseStack.popPose()
	}

	private fun VertexConsumer.lineVertex(pose: PoseStack.Pose, x: Float, y: Float, z: Float, color: Int, nx: Float, ny: Float, nz: Float) {
		addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH)
	}
}
