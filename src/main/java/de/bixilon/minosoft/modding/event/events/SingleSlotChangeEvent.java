/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.modding.event.events;

import de.bixilon.minosoft.data.inventory.ItemStack;
import de.bixilon.minosoft.protocol.network.connection.PlayConnection;
import de.bixilon.minosoft.protocol.packets.s2c.play.PacketSetSlot;

public class SingleSlotChangeEvent extends PlayConnectionEvent {
    private final byte windowId;
    private final short slotId;
    private final ItemStack itemStack;

    public SingleSlotChangeEvent(PlayConnection connection, byte windowId, short slotId, ItemStack itemStack) {
        super(connection);
        this.windowId = windowId;
        this.slotId = slotId;
        this.itemStack = itemStack;
    }

    public SingleSlotChangeEvent(PlayConnection connection, PacketSetSlot pkg) {
        super(connection);
        this.windowId = pkg.getWindowId();
        this.slotId = pkg.getSlotId();
        this.itemStack = pkg.getSlot();
    }

    public byte getWindowId() {
        return this.windowId;
    }

    public short getSlotId() {
        return this.slotId;
    }

    public ItemStack getSlot() {
        return this.itemStack;
    }
}
