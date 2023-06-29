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

package de.bixilon.minosoft.gui.rendering.renderer.renderer

import de.bixilon.kutil.latch.AbstractLatch
import de.bixilon.minosoft.gui.rendering.RenderContext
import de.bixilon.minosoft.gui.rendering.system.base.PolygonModes
import de.bixilon.minosoft.gui.rendering.system.base.RenderSystem
import de.bixilon.minosoft.gui.rendering.system.base.buffer.frame.Framebuffer

interface Renderer {
    val context: RenderContext
    val renderSystem: RenderSystem
    val framebuffer: Framebuffer?
        get() = context.framebuffer.world.framebuffer
    val polygonMode: PolygonModes
        get() = context.framebuffer.world.polygonMode

    fun preAsyncInit(latch: AbstractLatch) = Unit
    fun init(latch: AbstractLatch) = Unit
    fun asyncInit(latch: AbstractLatch) = Unit
    fun postInit(latch: AbstractLatch) = Unit
    fun postAsyncInit(latch: AbstractLatch) = Unit

    fun prePrepareDraw() = Unit
    fun postPrepareDraw() = Unit
}
