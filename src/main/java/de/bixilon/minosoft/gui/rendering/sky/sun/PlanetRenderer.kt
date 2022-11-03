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

package de.bixilon.minosoft.gui.rendering.sky.sun

import de.bixilon.kotlinglm.func.rad
import de.bixilon.kotlinglm.mat4x4.Mat4
import de.bixilon.kotlinglm.vec2.Vec2
import de.bixilon.kotlinglm.vec3.Vec3
import de.bixilon.kotlinglm.vec4.Vec4
import de.bixilon.kutil.watcher.DataWatcher.Companion.observe
import de.bixilon.minosoft.data.world.time.WorldTime
import de.bixilon.minosoft.gui.rendering.sky.SkyChildRenderer
import de.bixilon.minosoft.gui.rendering.sky.SkyRenderer
import de.bixilon.minosoft.gui.rendering.system.base.BlendingFunctions
import de.bixilon.minosoft.gui.rendering.system.base.RenderingCapabilities
import de.bixilon.minosoft.gui.rendering.system.base.texture.texture.AbstractTexture
import de.bixilon.minosoft.util.KUtil.minosoft

abstract class PlanetRenderer(
    protected val sky: SkyRenderer,
) : SkyChildRenderer {
    protected abstract val texture: AbstractTexture
    protected val shader = sky.renderWindow.renderSystem.createShader(minosoft("weather/planet"))
    private val mesh = PlanetMesh(sky.renderWindow)
    protected var day = -1L
    protected var matrix = Mat4()
    private var matrixUpdate = true
    protected var modifier = 0.0f
    protected var intensity = -1.0f

    override fun init() {
        shader.load()
    }

    private fun prepareMesh() {
        mesh.addYQuad(
            start = Vec2(-0.15f, -0.15f),
            y = 1f,
            end = Vec2(+0.15f, +0.15f),
            vertexConsumer = { position, uv ->
                mesh.addVertex(
                    position = position,
                    texture = texture,
                    uv = uv,
                )
            }
        )

        mesh.load()
    }

    override fun postInit() {
        prepareMesh()
        sky.renderWindow.textureManager.staticTextures.use(shader)
        sky::matrix.observe(this) { calculateMatrix(it) }
    }

    protected abstract fun calculateAngle(): Float
    protected abstract fun calculateIntensity(): Float


    private fun calculateMatrix(base: Mat4) {
        val matrix = Mat4(base)

        matrix.rotateAssign(calculateAngle().rad, Vec3(0, 0, -1))
        matrix.translateAssign(Vec3(0.0f, -0.01f, 0.0f)) // prevents face fighting

        matrix.translateAssign(Vec3(0.0f, -modifier, 0.0f)) // moves the planet closer to the player (appears appears bigger)


        this.matrix = matrix
        this.matrixUpdate = true
    }

    protected abstract fun calculateModifier(day: Long): Float

    override fun onTimeUpdate(time: WorldTime) {
        if (this.day != time.day) {
            this.day = time.day
            modifier = calculateModifier(time.day)
        }
        calculateMatrix(sky.matrix)
    }

    override fun draw() {
        shader.use()
        if (matrixUpdate) {
            shader.setMat4("uMatrix", matrix)

            val intensity = calculateIntensity()
            if (this.intensity != intensity) {
                shader.setVec4("uTintColor", Vec4(1.0f, 1.0f, 1.0f, intensity))
                this.intensity = intensity
            }
            this.matrixUpdate = false
        }

        sky.renderSystem.enable(RenderingCapabilities.BLENDING)
        sky.renderSystem.setBlendFunction(BlendingFunctions.SOURCE_ALPHA, BlendingFunctions.ONE, BlendingFunctions.ONE, BlendingFunctions.ZERO)

        mesh.draw()
        sky.renderSystem.resetBlending()
    }
}
