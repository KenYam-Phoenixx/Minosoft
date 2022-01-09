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

package de.bixilon.minosoft.protocol.packets.s2c.play.title

import de.bixilon.kutil.enums.EnumUtil
import de.bixilon.kutil.enums.ValuesEnum
import de.bixilon.minosoft.protocol.packets.factory.LoadPacket
import de.bixilon.minosoft.protocol.packets.factory.PacketDirection
import de.bixilon.minosoft.protocol.packets.factory.factories.PlayPacketFactory
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer

@LoadPacket(threadSafe = false)
object TitleS2CF : PlayPacketFactory {
    override val direction: PacketDirection = PacketDirection.SERVER_TO_CLIENT

    override fun createPacket(buffer: PlayInByteBuffer): TitleS2CP {
        return when (buffer.connection.registries.titleActionsRegistry[buffer.readVarInt()]!!) {
            TitleActions.TITLE_TEXT -> TitleTextS2CP(buffer)
            TitleActions.SUBTITLE -> SubtitleS2CP(buffer)
            TitleActions.HOTBAR_TEXT -> HotbarTextS2CP(buffer)
            TitleActions.TIMES -> TitleTimesS2CP(buffer)
            TitleActions.HIDE -> HideTitleS2CP(buffer)
            TitleActions.RESET -> ResetTitleS2CP(buffer)
        }
    }

    enum class TitleActions {
        TITLE_TEXT,
        SUBTITLE,
        HOTBAR_TEXT,
        TIMES,
        HIDE,
        RESET,
        ;

        companion object : ValuesEnum<TitleActions> {
            override val VALUES = values()
            override val NAME_MAP: Map<String, TitleActions> = EnumUtil.getEnumValues(VALUES)
        }
    }
}
