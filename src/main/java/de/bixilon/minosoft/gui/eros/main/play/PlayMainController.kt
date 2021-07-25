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

package de.bixilon.minosoft.gui.eros.main.play

import de.bixilon.minosoft.Minosoft
import de.bixilon.minosoft.gui.eros.controller.EmbeddedJavaFXController
import de.bixilon.minosoft.gui.eros.main.play.server.Refreshable
import de.bixilon.minosoft.gui.eros.main.play.server.ServerListController
import de.bixilon.minosoft.gui.eros.main.play.server.type.ServerType
import de.bixilon.minosoft.gui.eros.main.play.server.type.ServerTypeCardController
import de.bixilon.minosoft.gui.eros.modding.invoker.JavaFXEventInvoker
import de.bixilon.minosoft.gui.eros.util.JavaFXUtil
import de.bixilon.minosoft.gui.eros.util.JavaFXUtil.text
import de.bixilon.minosoft.modding.event.events.LANServerDiscoverEvent
import de.bixilon.minosoft.protocol.protocol.LANServerListener
import de.bixilon.minosoft.util.KUtil.asResourceLocation
import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid

class PlayMainController : EmbeddedJavaFXController<Pane>() {
    @FXML
    private lateinit var playTypeContentFX: Pane

    @FXML
    private lateinit var playTypeListViewFX: ListView<ServerType>

    @FXML
    private lateinit var refreshPaneFX: AnchorPane


    private lateinit var currentController: EmbeddedJavaFXController<*>

    override fun init() {
        playTypeListViewFX.setCellFactory { ServerTypeCardController.build() }
        playTypeListViewFX.items += ServerType(FontAwesomeSolid.SERVER, "Custom", "0 Servers", "") {
            return@ServerType JavaFXUtil.loadEmbeddedController<ServerListController>(ServerListController.LAYOUT).apply {
                servers = Minosoft.config.config.server.entries.values
                refreshList()
            }
        }
        playTypeListViewFX.items += ServerType(FontAwesomeSolid.NETWORK_WIRED, "LAN", "12 Servers", "") {
            return@ServerType JavaFXUtil.loadEmbeddedController<ServerListController>(ServerListController.LAYOUT).apply {
                readOnly = true
                customRefresh = {
                    LANServerListener.SERVERS.clear()
                    refreshList()
                }
                servers = LANServerListener.SERVERS.values
                Minosoft.GLOBAL_EVENT_MASTER.registerEvent(JavaFXEventInvoker.of<LANServerDiscoverEvent> { refreshList() }) // ToDo: Unregister event when hiding pane
                refreshList()
            }
        }

        playTypeListViewFX.selectionModel.selectedItemProperty().addListener { _, _, new ->
            currentController = new.content()
            playTypeContentFX.children.setAll(currentController.root)
        }

        playTypeListViewFX.selectionModel.select(0)

        ServerTypeCardController.build().apply {
            refreshPaneFX.children.setAll(root)
            iconFX.iconLiteral = "fas-sync-alt"
            iconFX.isVisible = true

            headerFX.text = REFRESH_HEADER
            text1FX.text = REFRESH_TEXT1
            text2FX.text = REFRESH_TEXT2

            root.setOnMouseClicked {
                val currentController = currentController
                if (currentController is Refreshable) {
                    currentController.refresh()
                }
            }
        }
    }

    companion object {
        private val REFRESH_HEADER = "minosoft:server_list.refresh.header".asResourceLocation()
        private val REFRESH_TEXT1 = "minosoft:server_list.refresh.text1".asResourceLocation()
        private val REFRESH_TEXT2 = "minosoft:server_list.refresh.text2".asResourceLocation()
    }
}
