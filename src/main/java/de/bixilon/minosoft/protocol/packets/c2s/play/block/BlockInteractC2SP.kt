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
package de.bixilon.minosoft.protocol.packets.c2s.play.block

import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.minosoft.data.container.stack.ItemStack
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.entities.entities.player.Hands
import de.bixilon.minosoft.data.world.positions.BlockPosition
import de.bixilon.minosoft.protocol.packets.c2s.PlayC2SPacket
import de.bixilon.minosoft.protocol.protocol.ProtocolVersions
import de.bixilon.minosoft.protocol.protocol.ProtocolVersions.V_15W31A
import de.bixilon.minosoft.protocol.protocol.buffers.play.PlayOutByteBuffer
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType

data class BlockInteractC2SP(
    val position: BlockPosition?,
    val direction: Directions?,
    val cursorPosition: Vec3?,
    val item: ItemStack?,
    val hand: Hands,
    val insideBlock: Boolean,
    val sequence: Int = 1,
) : PlayC2SPacket {

    override fun write(buffer: PlayOutByteBuffer) {
        if (buffer.versionId >= V_15W31A && (position == null || direction == null || cursorPosition == null)) {
            throw NullPointerException()
        }
        if (buffer.versionId >= ProtocolVersions.V_19W03A) {
            buffer.writeVarInt(hand.ordinal)
        }
        if (buffer.versionId < ProtocolVersions.V_14W04A) {
            buffer.writeByteBlockPosition(position ?: BlockPosition(-1, -1, -1))
        } else {
            buffer.writeBlockPosition(position ?: BlockPosition(-1, -1, -1))
        }
        if (buffer.versionId < V_15W31A) {
            buffer.writeByte(direction?.ordinal ?: -1)
            buffer.writeItemStack(item)
        } else {
            buffer.writeVarInt(direction?.ordinal ?: -1)
            if (buffer.versionId < ProtocolVersions.V_19W03A) {
                buffer.writeVarInt(hand.ordinal)
            }
        }

        if (buffer.versionId < ProtocolVersions.V_16W39C) {
            if (cursorPosition == null) {
                buffer.writeByte(0); buffer.writeByte(0); buffer.writeByte(0)
            } else {
                buffer.writeByte((cursorPosition.x * 15.0f).toInt())
                buffer.writeByte((cursorPosition.y * 15.0f).toInt())
                buffer.writeByte((cursorPosition.z * 15.0f).toInt())
            }
        } else {
            buffer.writeVec3f(cursorPosition!!)
        }

        if (buffer.versionId >= ProtocolVersions.V_19W03A) {
            buffer.writeBoolean(insideBlock)
        }
        if (buffer.versionId >= ProtocolVersions.V_22W11A) {
            buffer.writeVarInt(sequence)
        }
    }

    override fun log(reducedLog: Boolean) {
        Log.log(LogMessageType.NETWORK_OUT, LogLevels.VERBOSE) { "Block interact (position=$position, direction=$direction, cursor=$cursorPosition, hand=$hand, insideBlock=$insideBlock, sequence=$sequence)" }
    }
}
