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

package de.bixilon.minosoft.protocol.packets.clientbound.play;

import de.bixilon.minosoft.logging.Log;
import de.bixilon.minosoft.protocol.packets.ClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.InByteBuffer;

import static de.bixilon.minosoft.protocol.protocol.ProtocolVersions.V_17W47A;

public class PacketCraftRecipeResponse extends ClientboundPacket {
    byte windowId;
    int recipeId;
    String recipeName;

    @Override
    public boolean read(InByteBuffer buffer) {
        this.windowId = buffer.readByte();
        if (buffer.getVersionId() < V_17W47A) { // ToDo: was this really in 346?
            this.recipeId = buffer.readVarInt();
        } else {
            this.recipeName = buffer.readString();
        }
        return true;
    }

    @Override
    public void log() {
        Log.protocol(String.format("[IN] Received Crafting recipe response (windowId=%d, recipeId=%d)", this.windowId, this.recipeId));
    }

    public byte getWindowId() {
        return this.windowId;
    }

    public int getRecipeId() {
        return this.recipeId;
    }
}
