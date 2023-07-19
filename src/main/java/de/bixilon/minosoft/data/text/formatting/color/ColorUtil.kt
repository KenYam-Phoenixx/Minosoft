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

package de.bixilon.minosoft.data.text.formatting.color

object ColorUtil {

    fun mixColors(vararg colors: Int): Int {
        var red = 0
        var green = 0
        var blue = 0

        for (color in colors) {
            red += color shr 16 and 0xFF
            green += color shr 8 and 0xFF
            blue += color and 0xFF
        }

        return ((red / colors.size) shl 16) or ((green / colors.size) shl 8) or (blue / colors.size)
    }

    fun Float.asGray(): Int {
        val color = (this * RGBColor.COLOR_FLOAT_DIVIDER).toInt()
        return color shl 16 or color shl 8 or color
    }


    fun rgba8ToRgba2(rgba8: Int): Byte {
        val red = (rgba8 shr 24 + 6) and 0x03
        val green = (rgba8 shr 16 + 6) and 0x03
        val blue = (rgba8 shr 8 + 6) and 0x03
        val alpha = (rgba8 shr 0 + 6) and 0x03

        return ((red shl 6) or (green shl 4) or (blue shl 2) or alpha).toByte()
    }
}
