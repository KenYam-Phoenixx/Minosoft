/*
 * Minosoft
 * Copyright (C) 2020-2023 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.world.biome.source

import de.bixilon.minosoft.data.registries.biomes.Biome
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition

class XZBiomeArray(private val biomes: Array<Biome?>) : BiomeSource {

    init {
        check(biomes.size == ProtocolDefinition.SECTION_WIDTH_X * ProtocolDefinition.SECTION_WIDTH_Z) { "Biome array size does not match the xz block count!" }
    }

    override fun get(x: Int, y: Int, z: Int): Biome? {
        return biomes[(x and 0x0F) or ((z and 0x0F) shl 4)]
    }
}
