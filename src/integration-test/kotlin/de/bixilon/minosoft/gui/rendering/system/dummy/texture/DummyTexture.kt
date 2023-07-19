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

package de.bixilon.minosoft.gui.rendering.system.dummy.texture

import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureFormats
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureStates
import de.bixilon.minosoft.gui.rendering.system.base.texture.TextureTransparencies
import de.bixilon.minosoft.gui.rendering.system.base.texture.array.TextureArrayProperties
import de.bixilon.minosoft.gui.rendering.system.base.texture.data.TextureData
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.Texture
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.TextureRenderData
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.memory.TextureGenerator
import de.bixilon.minosoft.gui.rendering.textures.properties.ImageProperties

class DummyTexture : Texture {
    override var array = TextureArrayProperties(Vec2(), 1, Vec2())
    override var state: TextureStates = TextureStates.DECLARED
    override var size: Vec2i = Vec2i(1, 1)
    override val transparency: TextureTransparencies get() = TextureTransparencies.OPAQUE
    override var properties: ImageProperties = ImageProperties()
    override var renderData: TextureRenderData = DummyTextureRenderData
    override lateinit var data: TextureData
    override var mipmaps: Boolean = false

    override fun load(context: RenderContext) {
        data = TextureData(size, TextureFormats.RGBA8, TextureGenerator.allocate(size, TextureFormats.RGBA8))
    }
}
