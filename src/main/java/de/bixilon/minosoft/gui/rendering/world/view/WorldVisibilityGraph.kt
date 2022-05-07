/*
 * Minosoft
 * Copyright (C) 2020-2022 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.world.view

import de.bixilon.kotlinglm.func.common.clamp
import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kotlinglm.vec3.Vec3i
import de.bixilon.kutil.concurrent.lock.simple.SimpleLock
import de.bixilon.kutil.time.TimeUtil
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.registries.AABB
import de.bixilon.minosoft.data.world.Chunk
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.camera.Camera
import de.bixilon.minosoft.gui.rendering.util.VecUtil.chunkPosition
import de.bixilon.minosoft.gui.rendering.util.VecUtil.plus
import de.bixilon.minosoft.gui.rendering.util.VecUtil.sectionHeight
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2iUtil.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3iUtil.EMPTY
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.chunk.ChunkUtil.isInViewDistance
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import it.unimi.dsi.fastutil.ints.IntOpenHashSet

class WorldVisibilityGraph(
    private val renderWindow: RenderWindow,
    private val camera: Camera,
) {
    private val connection = renderWindow.connection
    private val frustum = camera.matrixHandler.frustum
    private var cameraChunkPosition = Vec2i.EMPTY
    private var cameraSectionHeight = 0
    private var viewDistance = connection.world.view.viewDistance

    var minSection = 0
    var maxSection = 16
    var maxIndex = 15

    private var visibilityLock = SimpleLock()
    private var visibilities: HashMap<Vec2i, BooleanArray> = HashMap()

    // check for view distance (hide chunks that are far away)
    // check if direction is non-negative (i.e. basic frustum culling)
    // always show current chunk section
    // check occlusion culling
    // real frustum culling


    // occlusion culling
    // always show current section

    init {
        calculateGraph()
    }

    fun isInViewDistance(chunkPosition: Vec2i): Boolean {
        return chunkPosition.isInViewDistance(connection.world.view.viewDistance, cameraChunkPosition)
    }

    fun isChunkVisible(chunkPosition: Vec2i): Boolean {
        if (!isInViewDistance(chunkPosition)) {
            return false
        }
        visibilityLock.acquire()
        val visible = visibilities[chunkPosition]
        visibilityLock.release()

        // ToDo: basic frustum culling
        return visible?.isNotEmpty() ?: false
    }

    fun isAABBVisible(aabb: AABB): Boolean {
        val chunkPositions: MutableSet<Vec2i> = HashSet()
        val sectionIndices = IntOpenHashSet()
        for (position in aabb.blockPositions) {
            chunkPositions += position.chunkPosition
            sectionIndices += position.sectionHeight - minSection
        }
        var visible = false
        visibilityLock.acquire()
        chunkPositions@ for (chunkPosition in chunkPositions) {
            val visibility = this.visibilities[chunkPosition] ?: continue
            for (index in sectionIndices.intIterator()) {
                if (index < 0 || index > maxIndex) {
                    visible = true // ToDo: Not 100% correct, image looking from >maxIndex to < 0
                    break@chunkPositions
                }
                if (visibility[index]) {
                    visible = true
                    break@chunkPositions
                }
            }
        }
        visibilityLock.release()

        if (!visible) {
            return false
        }

        return frustum.containsAABB(aabb)
    }

    fun isSectionVisible(chunkPosition: Vec2i, sectionHeight: Int, minPosition: Vec3i = DEFAULT_MIN_POSITION, maxPosition: Vec3i = DEFAULT_MAX_POSITION, checkChunk: Boolean = true): Boolean {
        if (checkChunk && !isChunkVisible(chunkPosition)) {
            return false
        }
        if (chunkPosition == cameraChunkPosition && sectionHeight == cameraSectionHeight) { // ToDo: Remove duplicated chunk position check
            return true
        }

        if (!frustum.containsChunk(chunkPosition, sectionHeight, minPosition, maxPosition)) {
            return false
        }
        visibilityLock.acquire()
        val visible = this.visibilities[chunkPosition]?.get(sectionHeight - minSection)
        visibilityLock.release()

        return visible == true
    }

    fun updateCamera(chunkPosition: Vec2i, sectionHeight: Int) {
        if (this.cameraChunkPosition == chunkPosition && this.cameraSectionHeight == sectionHeight) {
            return
        }
        this.cameraChunkPosition = chunkPosition
        this.cameraSectionHeight = sectionHeight
        this.minSection = connection.world.dimension?.lowestSection ?: 0
        this.maxSection = connection.world.dimension?.highestSection ?: 16
        this.maxIndex = maxSection - minSection - 1
        calculateGraph()
    }

    private fun checkSection(graph: MutableMap<Vec2i, IntOpenHashSet>, chunkPosition: Vec2i, sectionIndex: Int, chunk: Chunk, visibilities: IntOpenHashSet, direction: Directions, directionVector: Vec3i, steps: Int, ignoreVisibility: Boolean) {
        if (steps > 64 * 3) {
            Log.log(LogMessageType.OTHER, LogLevels.WARN) { "Potential stack overflow: $chunkPosition:$sectionIndex $direction $directionVector" }
            return
        }
        if ((direction == Directions.UP && sectionIndex >= maxIndex) || (direction == Directions.DOWN && sectionIndex < 0)) {
            return
        }
        if (!isInViewDistance(chunkPosition)) {
            return
        }
        val inverted = direction.inverted
        val nextStep = steps + 1

        if (ignoreVisibility) {
            visibilities += createVisibilityStatus(sectionIndex, Directions.DOWN, Directions.DOWN)
        }

        if (sectionIndex > 0 && directionVector.y <= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.DOWN) != true)) {
            val visibility = createVisibilityStatus(sectionIndex - 1, inverted, Directions.DOWN)
            if (visibilities.add(visibility)) {
                checkSection(graph, chunkPosition, sectionIndex - 1, chunk, visibilities, Directions.DOWN, directionVector.modify(1, -1), nextStep, false)
            }
        }
        if (sectionIndex < maxIndex && directionVector.y >= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.UP) != true)) {
            val visibility = createVisibilityStatus(sectionIndex + 1, inverted, Directions.UP)
            if (visibilities.add(visibility)) {
                checkSection(graph, chunkPosition, sectionIndex + 1, chunk, visibilities, Directions.UP, directionVector.modify(1, 1), nextStep, false)
            }
        }


        if (directionVector.x <= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.NORTH) != true)) {
            val nextPosition = chunkPosition + direction
            val nextChunk = connection.world.chunks.original[nextPosition]
            if (nextChunk != null) {
                val nextVisibilities = graph.getOrPut(nextPosition) { IntOpenHashSet() }
                if (nextVisibilities.add(createVisibilityStatus(sectionIndex, inverted, Directions.NORTH))) {
                    checkSection(graph, nextPosition, sectionIndex, nextChunk, nextVisibilities, Directions.NORTH, directionVector.modify(0, -1), nextStep, false)
                }
            }
        }
        if (directionVector.x >= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.SOUTH) != true)) {
            val nextPosition = chunkPosition + direction
            val nextChunk = connection.world.chunks.original[nextPosition]
            if (nextChunk != null) {
                val nextVisibilities = graph.getOrPut(nextPosition) { IntOpenHashSet() }
                if (nextVisibilities.add(createVisibilityStatus(sectionIndex, inverted, Directions.SOUTH))) {
                    checkSection(graph, nextPosition, sectionIndex, nextChunk, nextVisibilities, Directions.SOUTH, directionVector.modify(0, 1), nextStep, false)
                }
            }
        }

        if (directionVector.z <= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.WEST) != true)) {
            val nextPosition = chunkPosition + direction
            val nextChunk = connection.world.chunks.original[nextPosition]
            if (nextChunk != null) {
                val nextVisibilities = graph.getOrPut(nextPosition) { IntOpenHashSet() }
                if (nextVisibilities.add(createVisibilityStatus(sectionIndex, inverted, Directions.WEST))) {
                    checkSection(graph, nextPosition, sectionIndex, nextChunk, nextVisibilities, Directions.WEST, directionVector.modify(2, -1), nextStep, false)
                }
            }
        }

        if (directionVector.z >= 0 && (ignoreVisibility || chunk.sections?.getOrNull(sectionIndex)?.blocks?.isOccluded(inverted, Directions.EAST) != true)) {
            val nextPosition = chunkPosition + direction
            val nextChunk = connection.world.chunks.original[nextPosition]
            if (nextChunk != null) {
                val nextVisibilities = graph.getOrPut(nextPosition) { IntOpenHashSet() }
                if (nextVisibilities.add(createVisibilityStatus(sectionIndex, inverted, Directions.EAST))) {
                    checkSection(graph, nextPosition, sectionIndex, nextChunk, nextVisibilities, Directions.EAST, directionVector.modify(2, 1), nextStep, false)
                }
            }
        }
    }

    private fun Vec3i.modify(axis: Int, value: Int): Vec3i {
        val array = this.array
        if (array[axis] == value) {
            return this
        }
        val ret = Vec3i(this)
        ret[axis] = value
        return ret
    }

    private fun calculateGraph() {
        connection.world.chunks.lock.acquire()
        val start = TimeUtil.nanos
        println("Calculating graph...")

        val chunkPosition = cameraChunkPosition
        val sectionHeight = cameraSectionHeight
        val cameraSectionIndex = (sectionHeight - minSection).clamp(-1, maxIndex)
        this.viewDistance = connection.world.view.viewDistance

        val chunk = connection.world.chunks.original[chunkPosition]
        if (chunk == null) {
            connection.world.chunks.lock.release()
            return
        }

        val graph: MutableMap<Vec2i, IntOpenHashSet> = HashMap()

        for (direction in Directions.VALUES) {
            val nextPosition = chunkPosition + direction
            val nextChunk = connection.world.chunks.original[nextPosition] ?: continue
            val nextVisibility = graph.getOrPut(nextPosition) { IntOpenHashSet() }
            checkSection(graph, nextPosition, cameraSectionIndex, nextChunk, nextVisibility, direction, direction.vector, 0, true)
        }

        updateVisibilityGraph(graph)



        println("Done in ${(TimeUtil.nanos - start) / 1000}")

        connection.world.chunks.lock.release()
    }

    private fun updateVisibilityGraph(graph: MutableMap<Vec2i, IntOpenHashSet>) {
        visibilityLock.lock()
        this.visibilities.clear()


        for ((position, statuses) in graph) {
            val singleVisibility = this.visibilities.getOrPut(position) { BooleanArray(maxIndex + 1) }
            for (status in statuses.intIterator()) {
                val sectionHeight = (status shr 6) + minSection
                if (sectionHeight < 0 || sectionHeight > maxIndex) {
                    continue
                }
                if (singleVisibility[sectionHeight]) {
                    continue
                }

                singleVisibility[sectionHeight] = true
            }
        }

        visibilityLock.unlock()
    }

    private fun createVisibilityStatus(sectionIndex: Int, `in`: Directions, out: Directions): Int {
        val preferIn = `in`.ordinal < out.ordinal

        return (sectionIndex and 0xFFFF shl 6) or ((if (preferIn) `in` else `out`).ordinal shl 3) or (if (preferIn) out else `in`).ordinal
    }

    companion object {
        private val DEFAULT_MIN_POSITION = Vec3i.EMPTY
        private val DEFAULT_MAX_POSITION = Vec3i(ProtocolDefinition.SECTION_WIDTH_X, ProtocolDefinition.SECTION_HEIGHT_Y, ProtocolDefinition.SECTION_WIDTH_Z)
    }
}
