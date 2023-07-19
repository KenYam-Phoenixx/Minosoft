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

package de.bixilon.minosoft.gui.rendering.textures

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.data.registries.identified.ResourceLocation
import de.bixilon.minosoft.data.text.formatting.color.ColorUtil
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureFormats
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureTransparencies
import de.bixilon.minosoft.gui.rendering.system.base.texture.array.StaticTextureArray
import de.bixilon.minosoft.gui.rendering.system.base.texture.data.TextureData
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.Texture
import de.bixilon.minosoft.gui.rendering.world.mesh.SingleWorldMesh
import de.bixilon.minosoft.gui.rendering.world.mesh.WorldMesh
import de.bixilon.minosoft.util.KUtil.getRGBA
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import de.matthiasmann.twl.utils.PNGDecoder
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

object TextureUtil {

    fun ResourceLocation.texture(): ResourceLocation {
        var path = ""

        if (!this.path.startsWith("textures/")) {
            path += "textures/"
        }
        path += this.path

        if (!path.contains(".")) {
            // ending
            path += ".png"
        }

        return "$namespace:$path".toResourceLocation()
    }

    fun TextureTransparencies.getMesh(mesh: WorldMesh): SingleWorldMesh {
        return when (this) {
            TextureTransparencies.OPAQUE -> mesh.opaqueMesh
            TextureTransparencies.TRANSLUCENT -> mesh.translucentMesh
            TextureTransparencies.TRANSPARENT -> mesh.transparentMesh
        }!!
    }

    fun resolveTextures(textureArray: StaticTextureArray, textures: Map<String, String>): Map<String, Texture> {
        val resolvedTextures: MutableMap<String, Texture> = mutableMapOf()

        fun resolveTexture(key: String, value: String): Texture {
            resolvedTextures[key]?.let { return it }

            val variable = value.removePrefix("#")
            var texture: Texture? = null
            if (variable.length != value.length) {
                // resolve variable first
                texture = resolveTexture(variable, textures[variable]!!)
            }

            if (texture == null) {
                texture = textureArray.createTexture(value.toResourceLocation().texture())
            }

            resolvedTextures[key] = texture
            return texture
        }



        for ((key, value) in textures) {
            resolveTexture(key, value)
        }
        return resolvedTextures
    }

    /**
     * Other way of reading a texture if the first (preferred one) method does not work
     * Only happens to some weird textures (seen in PureBDCraft)
     * Ignores the alpha channel
     */
    private fun InputStream.readTexture2(format: TextureFormats): TextureData {
        val image: BufferedImage = ImageIO.read(this)
        val rgb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

        val byteOutput = ByteArrayOutputStream()
        val dataOutput = DataOutputStream(byteOutput)

        for (color in rgb) {
            val rgba = (color shl 8) or 0xFF
            when (format) {
                TextureFormats.RGBA8 -> dataOutput.writeInt(rgba)
                TextureFormats.RGBA2 -> dataOutput.writeByte(ColorUtil.rgba8ToRgba2(rgba).toInt())
            }
        }

        val buffer = MemoryUtil.memAlloc(byteOutput.size())
        buffer.put(byteOutput.toByteArray())

        return TextureData(Vec2i(image.width, image.height), format, buffer)
    }

    fun InputStream.readTexture(format: TextureFormats): TextureData {
        try {
            val decoder = PNGDecoder(this)
            val data = BufferUtils.createByteBuffer(decoder.width * decoder.height * PNGDecoder.Format.RGBA.numComponents)
            decoder.decode(data, decoder.width * PNGDecoder.Format.RGBA.numComponents, PNGDecoder.Format.RGBA)


            val converted = when (format) {
                TextureFormats.RGBA8 -> data
                TextureFormats.RGBA2 -> data.copyRgba8ToRgba2()
            }
            return TextureData(Vec2i(decoder.width, decoder.height), format, converted)
        } catch (_: Throwable) {
        }
        this.reset()
        return readTexture2(format)
    }


    private fun ByteBuffer.copyRgba8ToRgba2(): ByteBuffer {
        val next = BufferUtils.createByteBuffer(this.limit() / 4)

        for (index in 0..next.limit()) {
            val rgba = this.getRGBA(index)
            next.put(index, ColorUtil.rgba8ToRgba2(rgba))
        }

        return next
    }
}
