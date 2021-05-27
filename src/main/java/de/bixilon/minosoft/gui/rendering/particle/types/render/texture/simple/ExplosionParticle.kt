/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.particle.types.render.texture.simple

import de.bixilon.minosoft.data.mappings.ResourceLocation
import de.bixilon.minosoft.data.mappings.particle.data.ParticleData
import de.bixilon.minosoft.data.text.RGBColor.Companion.asGray
import de.bixilon.minosoft.gui.rendering.particle.ParticleFactory
import de.bixilon.minosoft.gui.rendering.particle.ParticleRenderer
import de.bixilon.minosoft.protocol.network.connection.PlayConnection
import de.bixilon.minosoft.util.KUtil.asResourceLocation
import de.bixilon.minosoft.util.KUtil.ticks
import glm_.vec3.Vec3

class ExplosionParticle(connection: PlayConnection, particleRenderer: ParticleRenderer, position: Vec3, data: ParticleData, val power: Float = 1.0f) : SimpleTextureParticle(connection, particleRenderer, position, data) {

    init {
        maxAge = (6 + random.nextInt(4)).ticks
        val gray = random.nextFloat() * 0.6f + 0.4f
        color = gray.asGray()
        scale = 2.0f * (power - gray * 0.5f)
    }

    companion object : ParticleFactory<ExplosionParticle> {
        override val RESOURCE_LOCATION: ResourceLocation = "minecraft:explosion".asResourceLocation()

        override fun build(connection: PlayConnection, particleRenderer: ParticleRenderer, position: Vec3, velocity: Vec3, data: ParticleData): ExplosionParticle {
            return ExplosionParticle(connection, particleRenderer, position, data, velocity.x)
        }
    }
}
