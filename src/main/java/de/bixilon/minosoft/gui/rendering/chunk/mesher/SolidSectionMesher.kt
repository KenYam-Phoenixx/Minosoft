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

package de.bixilon.minosoft.gui.rendering.chunk.mesher

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kutil.cast.CastUtil.nullCast
import de.bixilon.kutil.observer.DataObserver.Companion.observe
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.direction.Directions.Companion.O_DOWN
import de.bixilon.minosoft.data.direction.Directions.Companion.O_EAST
import de.bixilon.minosoft.data.direction.Directions.Companion.O_NORTH
import de.bixilon.minosoft.data.direction.Directions.Companion.O_SOUTH
import de.bixilon.minosoft.data.direction.Directions.Companion.O_UP
import de.bixilon.minosoft.data.direction.Directions.Companion.O_WEST
import de.bixilon.minosoft.data.registries.blocks.state.BlockState
import de.bixilon.minosoft.data.registries.blocks.types.building.stone.Bedrock
import de.bixilon.minosoft.data.registries.blocks.types.fluid.FluidBlock
import de.bixilon.minosoft.data.registries.blocks.types.properties.offset.OffsetBlock
import de.bixilon.minosoft.data.world.chunk.ChunkSection
import de.bixilon.minosoft.data.world.chunk.chunk.Chunk
import de.bixilon.minosoft.data.world.chunk.light.SectionLight
import de.bixilon.minosoft.data.world.chunk.neighbours.ChunkNeighbours
import de.bixilon.minosoft.data.world.positions.BlockPosition
import de.bixilon.minosoft.data.world.positions.InSectionPosition
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.chunk.entities.BlockEntityRenderer
import de.bixilon.minosoft.gui.rendering.chunk.entities.renderer.RenderedBlockEntity
import de.bixilon.minosoft.gui.rendering.chunk.mesh.ChunkMeshes
import de.bixilon.minosoft.gui.rendering.light.ao.AmbientOcclusion
import de.bixilon.minosoft.gui.rendering.models.block.state.render.WorldRenderProps
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import java.util.*

class SolidSectionMesher(
    val context: RenderContext,
) {
    private val profile = context.session.profiles.block.rendering
    private val bedrock = context.session.registries.block[Bedrock]?.states?.default
    private val tints = context.tints
    private var fastBedrock = false
    private var ambientOcclusion = false

    init {
        val profile = context.session.profiles.rendering
        profile.performance::fastBedrock.observe(this, true) { this.fastBedrock = it }
        profile.light::ambientOcclusion.observe(this, true) { this.ambientOcclusion = it }
    }

    fun mesh(chunkPosition: Vec2i, sectionHeight: Int, chunk: Chunk, section: ChunkSection, neighbourChunks: Array<Chunk>, neighbours: Array<ChunkSection?>, mesh: ChunkMeshes) {
        val random = if (profile.antiMoirePattern) Random(0L) else null


        val isLowestSection = sectionHeight == chunk.minSection
        val isHighestSection = sectionHeight == chunk.maxSection
        val blocks = section.blocks
        val entities: ArrayList<BlockEntityRenderer<*>> = ArrayList(section.blockEntities.count)

        val tint = IntArray(1)
        var position = BlockPosition()
        var inSectionPosition = InSectionPosition(0, 0, 0)
        val neighbourBlocks: Array<BlockState?> = arrayOfNulls(Directions.SIZE)
        val light = ByteArray(Directions.SIZE + 1) // last index (6) for the current block

        val cameraOffset = context.camera.offset.offset

        val offsetX = chunkPosition.x * ProtocolDefinition.SECTION_WIDTH_X
        val offsetY = sectionHeight * ProtocolDefinition.SECTION_HEIGHT_Y
        val offsetZ = chunkPosition.y * ProtocolDefinition.SECTION_WIDTH_Z

        val floatOffset = FloatArray(3)

        val ao = if (ambientOcclusion) AmbientOcclusion(section) else null

        val props = WorldRenderProps(floatOffset, mesh, random, neighbourBlocks, light, ao)


        for (y in blocks.minPosition.y..blocks.maxPosition.y) {
            inSectionPosition = inSectionPosition.with(y = y)
            position = position.with(y = offsetY + y)
            floatOffset[1] = (position.y - cameraOffset.y).toFloat()
            val fastBedrock = y == 0 && isLowestSection && fastBedrock
            for (x in blocks.minPosition.x..blocks.maxPosition.x) {
                inSectionPosition = inSectionPosition.with(x = x)
                position = position.with(x = offsetX + x)
                floatOffset[0] = (position.x - cameraOffset.x).toFloat()
                for (z in blocks.minPosition.z..blocks.maxPosition.z) {
                    inSectionPosition = inSectionPosition.with(z = z)
                    val state = blocks[inSectionPosition] ?: continue
                    if (state.block is FluidBlock) continue // fluids are rendered in a different renderer

                    val model = state.block.model ?: state.model
                    val blockEntity = section.blockEntities[inSectionPosition]
                    val renderedBlockEntity = blockEntity?.nullCast<RenderedBlockEntity<*>>()
                    if (model == null && renderedBlockEntity == null) continue


                    light[SELF_LIGHT_INDEX] = section.light[inSectionPosition.index]
                    position = position.with(z = offsetZ + z)
                    floatOffset[2] = (position.z - cameraOffset.z).toFloat()

                    val maxHeight = chunk.light.heightmap[inSectionPosition.xz]
                    if (position.y >= maxHeight) {
                        light[SELF_LIGHT_INDEX] = (light[SELF_LIGHT_INDEX].toInt() or 0xF0).toByte()
                    }

                    checkDown(state, fastBedrock, inSectionPosition, isLowestSection, neighbourBlocks, neighbours, light, section, chunk)
                    checkUp(isHighestSection, inSectionPosition, neighbourBlocks, neighbours, light, section, chunk)

                    setZ(neighbourBlocks, inSectionPosition, neighbours, light, neighbourChunks, section, chunk)
                    setX(neighbourBlocks, inSectionPosition, neighbours, light, neighbourChunks, section, chunk)

                    // TODO: cull neighbours

                    if (position.y - 1 >= maxHeight) {
                        light[O_UP] = (light[O_UP].toInt() or 0xF0).toByte()
                        light[O_DOWN] = (light[O_DOWN].toInt() or 0xF0).toByte()
                    } else if (position.y + 1 >= maxHeight) {
                        light[O_UP] = (light[O_UP].toInt() or 0xF0).toByte()
                    }

                    var offset: Vec3? = null
                    if (state.block is OffsetBlock) {
                        offset = state.block.offsetModel(position)
                        floatOffset[0] += offset.x
                        floatOffset[1] += offset.y
                        floatOffset[2] += offset.z
                    }

                    ao?.clear()


                    val tints = tints.getBlockTint(state, chunk, x, position.y, z, tint)
                    var rendered = false
                    model?.render(props, position, state, blockEntity, tints)?.let { if (it) rendered = true }

                    renderedBlockEntity?.getRenderer(context, state, position, light[SELF_LIGHT_INDEX].toInt())?.let { rendered = true; entities += it }

                    if (offset != null) {
                        floatOffset[0] -= offset.x
                        floatOffset[1] -= offset.y
                        // z is automatically reset
                    }

                    if (rendered) {
                        mesh.addBlock(x, y, z)
                    }
                    if (Thread.interrupted()) throw InterruptedException()
                }
            }
        }
        mesh.blockEntities = entities
    }

    private inline fun checkDown(state: BlockState, fastBedrock: Boolean, position: InSectionPosition, lowest: Boolean, neighbourBlocks: Array<BlockState?>, neighbours: Array<ChunkSection?>, light: ByteArray, section: ChunkSection, chunk: Chunk) {
        if (position.y == 0) {
            if (fastBedrock && state === bedrock) {
                neighbourBlocks[O_DOWN] = bedrock
            } else {
                neighbourBlocks[O_DOWN] = neighbours[O_DOWN]?.blocks?.let { it[position.with(y = ProtocolDefinition.SECTION_MAX_Y)] }
                light[O_DOWN] = (if (lowest) chunk.light.bottom else neighbours[O_DOWN]?.light)?.get(position.with(y = ProtocolDefinition.SECTION_MAX_Y)) ?: 0x00
            }
        } else {
            neighbourBlocks[O_DOWN] = section.blocks[position.minusY()]
            light[O_DOWN] = section.light[position.minusY().index]
        }
    }

    fun checkUp(highest: Boolean, position: InSectionPosition, neighbourBlocks: Array<BlockState?>, neighbours: Array<ChunkSection?>, light: ByteArray, section: ChunkSection, chunk: Chunk) {
        if (position.y == ProtocolDefinition.SECTION_MAX_Y) {
            neighbourBlocks[O_UP] = neighbours[O_UP]?.blocks?.let { it[position.with(y = 0)] }
            light[O_UP] = (if (highest) chunk.light.top else neighbours[O_UP]?.light)?.get(position.with(y = 0)) ?: 0x00
        } else {
            neighbourBlocks[O_UP] = section.blocks[position.plusY()]
            light[O_UP] = section.light[position.plusY().index]
        }
    }

    private inline fun setZ(neighbourBlocks: Array<BlockState?>, position: InSectionPosition, neighbours: Array<ChunkSection?>, light: ByteArray, neighbourChunks: Array<Chunk>, section: ChunkSection, chunk: Chunk) {
        if (position.z == 0) {
            setNeighbour(neighbourBlocks, position.with(z = ProtocolDefinition.SECTION_MAX_Z), light, neighbours[O_NORTH], neighbourChunks[ChunkNeighbours.NORTH], O_NORTH)
            setNeighbour(neighbourBlocks, position.plusZ(), light, section, chunk, O_SOUTH)
        } else if (position.z == ProtocolDefinition.SECTION_MAX_Z) {
            setNeighbour(neighbourBlocks, position.minusZ(), light, section, chunk, O_NORTH)
            setNeighbour(neighbourBlocks, position.with(z = 0), light, neighbours[O_SOUTH], neighbourChunks[ChunkNeighbours.SOUTH], O_SOUTH)
        } else {
            setNeighbour(neighbourBlocks, position.minusZ(), light, section, chunk, O_NORTH)
            setNeighbour(neighbourBlocks, position.plusZ(), light, section, chunk, O_SOUTH)
        }
    }


    private inline fun setX(neighbourBlocks: Array<BlockState?>, position: InSectionPosition, neighbours: Array<ChunkSection?>, light: ByteArray, neighbourChunks: Array<Chunk>, section: ChunkSection, chunk: Chunk) {
        if (position.x == 0) {
            setNeighbour(neighbourBlocks, position.with(x = ProtocolDefinition.SECTION_MAX_X), light, neighbours[O_WEST], neighbourChunks[ChunkNeighbours.WEST], O_WEST)
            setNeighbour(neighbourBlocks, position.plusX(), light, section, chunk, O_EAST)
        } else if (position.x == ProtocolDefinition.SECTION_MAX_X) {
            setNeighbour(neighbourBlocks, position.with(x = 0), light, neighbours[O_EAST], neighbourChunks[ChunkNeighbours.EAST], O_EAST)
            setNeighbour(neighbourBlocks, position.minusX(), light, section, chunk, O_WEST)
        } else {
            setNeighbour(neighbourBlocks, position.minusX(), light, section, chunk, O_WEST)
            setNeighbour(neighbourBlocks, position.plusX(), light, section, chunk, O_EAST)
        }
    }

    private inline fun setNeighbour(neighbourBlocks: Array<BlockState?>, position: InSectionPosition, light: ByteArray, section: ChunkSection?, chunk: Chunk, ordinal: Int) {
        neighbourBlocks[ordinal] = section?.blocks?.let { it[position] }
        light[ordinal] = section?.light?.get(position) ?: 0x00
        if (position.y >= chunk.light.heightmap[position.xz]) {
            light[ordinal] = (light[ordinal].toInt() or SectionLight.SKY_LIGHT_MASK).toByte() // set sky light to 0x0F
        }
    }

    companion object {
        const val SELF_LIGHT_INDEX = 6 // after all directions
    }
}
