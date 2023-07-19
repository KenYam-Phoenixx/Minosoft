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

package de.bixilon.minosoft.gui.rendering.system.base.texture.texture.file

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.assets.AssetsManager
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureFormats
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureStates
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureTransparencies
import de.bixilon.minosoft.gui.rendering.system.base.texture.array.TextureArrayProperties
import de.bixilon.minosoft.gui.rendering.system.base.texture.data.MipmapTextureData
import de.bixilon.minosoft.gui.rendering.system.base.texture.data.TextureData
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.TextureRenderData
import de.bixilon.minosoft.gui.rendering.textures.TextureUtil.readTexture
import de.bixilon.minosoft.gui.rendering.textures.properties.ImageProperties
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import java.io.FileNotFoundException


class PNGTexture(
    override val resourceLocation: ResourceLocation,
    override var mipmaps: Boolean = true,
    val format: TextureFormats = TextureFormats.RGBA8,
) : FileTexture {
    override lateinit var renderData: TextureRenderData

    override lateinit var array: TextureArrayProperties
    override var state: TextureStates = TextureStates.DECLARED
        private set
    override lateinit var size: Vec2i
        private set
    override lateinit var transparency: TextureTransparencies
        private set
    override lateinit var properties: ImageProperties


    override lateinit var data: TextureData


    @Synchronized
    override fun load(assetsManager: AssetsManager) {
        if (state == TextureStates.LOADED) {
            return
        }

        var data = try {
            assetsManager[resourceLocation].readTexture(format)
        } catch (error: Throwable) {
            state = TextureStates.ERRORED
            Log.log(LogMessageType.RENDERING, LogLevels.WARN) { "Can not load texture $resourceLocation: $error" }
            if (error !is FileNotFoundException) {
                Log.log(LogMessageType.RENDERING, LogLevels.VERBOSE) { error }
            }
            assetsManager[RenderConstants.DEBUG_TEXTURE_RESOURCE_LOCATION].readTexture(format)
        }
        data.buffer.rewind()
        if (mipmaps) data = MipmapTextureData(data.size, format, data.buffer)

        this.size = data.size
        transparency = TextureTransparencies.OPAQUE
        for (i in 0 until data.buffer.limit() / 4) {
            val alpha = data.buffer[i * 4 + 3].toInt() and 0xFF
            if (alpha == 0x00) {
                transparency = TextureTransparencies.TRANSPARENT
            } else if (alpha < 0xFF) {
                transparency = TextureTransparencies.TRANSLUCENT
                break
            }
        }

        this.data = data

        properties.postInit(this)

        state = TextureStates.LOADED
    }


    override fun toString(): String {
        return resourceLocation.toString()
    }
}
