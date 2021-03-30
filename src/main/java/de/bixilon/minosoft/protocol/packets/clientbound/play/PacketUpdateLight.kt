/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */
package de.bixilon.minosoft.protocol.packets.clientbound.play


import de.bixilon.minosoft.data.world.light.ChunkLightAccessor
import de.bixilon.minosoft.data.world.light.LightAccessor
import de.bixilon.minosoft.protocol.network.Connection
import de.bixilon.minosoft.protocol.packets.ClientboundPacket
import de.bixilon.minosoft.protocol.protocol.InByteBuffer
import de.bixilon.minosoft.protocol.protocol.ProtocolVersions
import de.bixilon.minosoft.util.KUtil
import de.bixilon.minosoft.util.chunk.LightUtil.readLightPacket
import de.bixilon.minosoft.util.logging.Log
import glm_.vec2.Vec2i
import java.util.*

class PacketUpdateLight(buffer: InByteBuffer) : ClientboundPacket() {
    val position: Vec2i
    var trustEdges: Boolean = false
        private set
    val lightAccessor: LightAccessor

    init {
        position = Vec2i(buffer.readVarInt(), buffer.readVarInt())

        if (buffer.versionId >= ProtocolVersions.V_1_16_PRE3) {
            trustEdges = buffer.readBoolean()
        }

        val skyLightMask: BitSet
        val blockLightMask: BitSet
        val emptySkyLightMask: BitSet
        val emptyBlockLightMask: BitSet

        if (buffer.versionId < ProtocolVersions.V_20W49A) {
            skyLightMask = KUtil.bitSetOf(buffer.readVarLong())
            blockLightMask = KUtil.bitSetOf(buffer.readVarLong())
            emptyBlockLightMask = KUtil.bitSetOf(buffer.readVarLong())
            emptySkyLightMask = KUtil.bitSetOf(buffer.readVarLong())
        } else {
            skyLightMask = BitSet.valueOf(buffer.readLongArray())
            blockLightMask = BitSet.valueOf(buffer.readLongArray())
            emptySkyLightMask = BitSet.valueOf(buffer.readLongArray())
            emptyBlockLightMask = BitSet.valueOf(buffer.readLongArray())
        }

        lightAccessor = readLightPacket(buffer, skyLightMask, blockLightMask, emptyBlockLightMask, emptySkyLightMask, buffer.connection.player.world.dimension!!)
    }

    override fun log() {
        Log.protocol("[IN] Received light update (position=%s)", position)
    }

    override fun handle(connection: Connection) {
        val chunk = connection.player.world.getOrCreateChunk(position)
        if (chunk.lightAccessor != null && chunk.lightAccessor is ChunkLightAccessor && lightAccessor is ChunkLightAccessor) {
            (chunk.lightAccessor as ChunkLightAccessor).merge(lightAccessor)
        } else {
            chunk.lightAccessor = lightAccessor
        }
        connection.renderer.renderWindow.worldRenderer.prepareChunk(position, chunk)
    }
}
