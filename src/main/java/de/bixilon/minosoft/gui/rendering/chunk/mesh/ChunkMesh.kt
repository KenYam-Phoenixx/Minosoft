/*
 * Minosoft
 * Copyright (C) 2020-2025 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.chunk.mesh

import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.system.base.MeshUtil.buffer
import de.bixilon.minosoft.gui.rendering.system.base.texture.shader.ShaderTexture
import de.bixilon.minosoft.gui.rendering.util.mesh.Mesh
import de.bixilon.minosoft.gui.rendering.util.mesh.MeshStruct
import de.bixilon.minosoft.gui.rendering.util.mesh.uv.PackedUV

class ChunkMesh(context: RenderContext, initialCacheSize: Int) : Mesh(context, ChunkMeshStruct, initialCacheSize = initialCacheSize), BlockVertexConsumer, Comparable<ChunkMesh> {
    var distance: Float = 0.0f // Used for sorting

    override val order = context.system.quadOrder

    override fun addVertex(position: FloatArray, uv: Vec2, texture: ShaderTexture, tintColor: Int, lightIndex: Int) {
        data.ensureSize(ChunkMeshStruct.FLOATS_PER_VERTEX)
        val transformedUV = texture.transformUV(uv).array
        data.add(position)
        data.add(PackedUV.pack(transformedUV[0], transformedUV[1]))
        data.add(
            texture.shaderId.buffer(),
            (((lightIndex shl 24) or tintColor).buffer())
        )
    }

    override inline fun addVertex(x: Float, y: Float, z: Float, u: Float, v: Float, textureId: Float, lightTint: Float) {
        data.add(
            x, y, z,
            PackedUV.pack(u, v),
            textureId, lightTint,
        )
    }

    override inline fun addVertex(x: Float, y: Float, z: Float, uv: Float, textureId: Float, lightTint: Float) {
        data.add(
            x, y, z,
            uv,
            textureId, lightTint,
        )
    }

    override fun compareTo(other: ChunkMesh): Int {
        if (distance < other.distance) return -1
        if (distance > other.distance) return 1
        return 0
    }

    data class ChunkMeshStruct(
        val position: Vec3,
        val uv: PackedUV,
        val indexLayerAnimation: Int,
        val lightTint: Int,
    ) {
        companion object : MeshStruct(ChunkMeshStruct::class)
    }
}
