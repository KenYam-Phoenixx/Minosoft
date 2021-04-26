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

package de.bixilon.minosoft.gui.rendering.hud.nodes.chat


import de.bixilon.minosoft.config.config.game.controls.KeyBindingsNames
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.gui.rendering.font.Font
import de.bixilon.minosoft.gui.rendering.hud.HUDRenderer
import de.bixilon.minosoft.gui.rendering.hud.elements.input.TextField
import de.bixilon.minosoft.gui.rendering.hud.elements.input.TextFieldProperties
import de.bixilon.minosoft.gui.rendering.hud.nodes.HUDElement
import de.bixilon.minosoft.gui.rendering.hud.nodes.primitive.ImageNode
import de.bixilon.minosoft.gui.rendering.hud.nodes.properties.NodeSizing
import de.bixilon.minosoft.gui.rendering.hud.nodes.properties.Spacing
import de.bixilon.minosoft.gui.rendering.util.abstractions.ScreenResizeCallback
import de.bixilon.minosoft.util.MMath
import glm_.vec2.Vec2i

class ChatBoxHUDElement(hudRenderer: HUDRenderer) : HUDElement(hudRenderer), ScreenResizeCallback {
    private lateinit var inputField: TextField
    private var inputFieldBackground = ImageNode(hudRenderer.renderWindow, sizing = NodeSizing(margin = Spacing(left = 1, right = 1)), textureLike = hudRenderer.renderWindow.WHITE_TEXTURE, z = 0, tintColor = RenderConstants.TEXT_BACKGROUND_COLOR)

    override fun init() {
        inputField = TextField(
            renderWindow = hudRenderer.renderWindow,
            properties = TextFieldProperties(
                maxLength = 256,
                submitCloses = true,
                onSubmit = { hudRenderer.renderWindow.connection.sender.sendChatMessage(it) },
                onClose = { closeChat() },
            ))

        layout.addChild(Vec2i(0, 0), inputField)
        inputField.apply()

        hudRenderer.renderWindow.inputHandler.registerKeyCallback(KeyBindingsNames.OPEN_CHAT) {
            openChat()
        }
    }

    override fun onScreenResize(screenDimensions: Vec2i) {
        layout.sizing.minSize.x = screenDimensions.x
        inputFieldBackground.sizing.forceSize = Vec2i(screenDimensions.x - 2, MMath.clamp(inputField.textElement.getProperties.lines, 1, Int.MAX_VALUE) * (Font.CHAR_HEIGHT + RenderConstants.TEXT_LINE_PADDING)) // 2 pixels for margin
        layout.sizing.maxSize.x = screenDimensions.x
        layout.sizing.validate()
        layout.apply()
    }

    fun openChat() {
        layout.addChild(Vec2i(0, 0), inputFieldBackground)
        hudRenderer.renderWindow.inputHandler.currentKeyConsumer = inputField
    }

    fun closeChat() {
        layout.removeChild(inputFieldBackground)
        hudRenderer.renderWindow.inputHandler.currentKeyConsumer = null
    }
}
