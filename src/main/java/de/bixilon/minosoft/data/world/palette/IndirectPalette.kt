/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */
package de.bixilon.minosoft.data.world.palette

import de.bixilon.minosoft.config.StaticConfiguration
import de.bixilon.minosoft.data.mappings.blocks.BlockState
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.logging.Log

class IndirectPalette(
    override val bitsPerBlock: Int,
    buffer: PlayInByteBuffer,
) : Palette {
    private val connection = buffer.connection
    private var palette = buffer.readVarIntArray()

    override fun blockById(blockId: Int): BlockState? {
        var realBlockId = blockId
        if (realBlockId < palette.size) {
            realBlockId = palette[realBlockId]
        }
        val block = connection.mapping.getBlockState(realBlockId)

        if (StaticConfiguration.DEBUG_MODE && block == null && realBlockId != ProtocolDefinition.NULL_BLOCK_ID) {
            val blockName: String = if (connection.version.isFlattened()) {
                realBlockId.toString()
            } else {
                "(${realBlockId shr 4}:${realBlockId and 0x0F}"
            }
            Log.warn("Server sent unknown block: $blockName")
        }
        return block
    }
}
