package com.ferri.arnus.contacts.embeddiumCompatibility.mixin.vertex_format.entity;

import com.ferri.arnus.contacts.embeddiumCompatibility.impl.vertex_format.entity_xhfp.EntityVertex;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.ColorU8;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.common.ModelVertex;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedModelEncoder.class)
public class MixinModelVertex {
	@Inject(method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V", at = @At("HEAD"), cancellable = true)
	private static void redirect2(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int color, int light, int overlay, boolean todo, CallbackInfo ci) {
		if (shouldBeExtended()) {
			ci.cancel();
			EntityVertex.writeQuadVertices(writer, matrices, quad, light, overlay, color);
		}
	}

	/**
	 * @author IMS
	 * @reason Rewrite
	 */
	@Overwrite
	public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, float r, float g, float b, float a, float[] brightnessTable, boolean colorize, int[] light, int overlay) {
		Matrix3f matNormal = matrices.normal();
		Matrix4f matPosition = matrices.pose();
		MemoryStack stack = MemoryStack.stackPush();

		try {
			long buffer = stack.nmalloc(144);
			long ptr = buffer;
			int normal = MatrixHelper.transformNormal(matNormal, matrices.trustedNormals, quad.getLightFace());

			for (int i = 0; i < 4; ++i) {
				float x = quad.getX(i);
				float y = quad.getY(i);
				float z = quad.getZ(i);
				float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
				float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
				float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
				float brightness = brightnessTable[i];
				float fR;
				float fG;
				float fB;
				int color;
				if (colorize) {
					color = quad.getColor(i);
					float oR = ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color));
					float oG = ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color));
					float oB = ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color));
					fR = oR * brightness * r;
					fG = oG * brightness * g;
					fB = oB * brightness * b;
				} else {
					fR = brightness * r;
					fG = brightness * g;
					fB = brightness * b;
				}

				color = ColorABGR.pack(fR, fG, fB, a);
				ModelVertex.write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, light[i], normal);
				ptr += 36L;
			}

			writer.push(stack, buffer, 4, ModelVertex.FORMAT);
		} catch (Throwable var34) {
			if (stack != null) {
				try {
					stack.close();
				} catch (Throwable var33) {
					var34.addSuppressed(var33);
				}
			}

			throw var34;
		}

		if (stack != null) {
			stack.close();
		}

	}

	private static boolean shouldBeExtended() {
		return IrisApi.getInstance().isShaderPackInUse() && ImmediateState.renderWithExtendedVertexFormat;
	}
}