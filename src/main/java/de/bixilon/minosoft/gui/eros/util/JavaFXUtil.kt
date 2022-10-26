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

package de.bixilon.minosoft.gui.eros.util

import com.sun.javafx.util.WeakReferenceQueue
import de.bixilon.kutil.cast.CastUtil.unsafeCast
import de.bixilon.kutil.concurrent.pool.DefaultThreadPool
import de.bixilon.kutil.reflection.ReflectionUtil.setValue
import de.bixilon.kutil.url.URLUtil.toURL
import de.bixilon.minosoft.Minosoft
import de.bixilon.minosoft.config.profile.delegate.watcher.SimpleProfileDelegateWatcher.Companion.profileWatchFX
import de.bixilon.minosoft.config.profile.profiles.eros.ErosProfileManager
import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.gui.eros.controller.EmbeddedJavaFXController
import de.bixilon.minosoft.gui.eros.controller.JavaFXController
import de.bixilon.minosoft.gui.eros.controller.JavaFXWindowController
import de.bixilon.minosoft.util.DesktopUtil
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import javafx.application.HostServices
import javafx.application.Platform
import javafx.css.StyleableProperty
import javafx.fxml.FXMLLoader
import javafx.scene.*
import javafx.scene.control.Labeled
import javafx.scene.control.TableColumnBase
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File
import kotlin.reflect.jvm.javaField

object JavaFXUtil {
    private const val DEFAULT_STYLE = "resource:minosoft:eros/style.css"
    private val stages: WeakReferenceQueue<Stage> = WeakReferenceQueue()
    lateinit var JAVA_FX_THREAD: Thread
    lateinit var MINOSOFT_LOGO: Image
    lateinit var HOST_SERVICES: HostServices
    lateinit var BIXILON_LOGO: Group
    private var watchingTheme = false

    val THEME_ASSETS_MANAGER = Minosoft.MINOSOFT_ASSETS_MANAGER

    private fun startThemeWatcher() {
        if (watchingTheme) {
            return
        }

        ErosProfileManager.selected.theme::theme.profileWatchFX(this) {
            stages.cleanup()
            for (stage in stages.iterator().unsafeCast<Iterator<Stage?>>()) {
                stage ?: continue
                stage.scene.stylesheets.clear()
                stage.scene.stylesheets.add(DEFAULT_STYLE)
                stage.scene.stylesheets.add(getThemeURL(it))
            }
        }
        watchingTheme = true
    }

    private fun <T : JavaFXController> loadController(title: Any, fxmlLoader: FXMLLoader, parent: Parent, modality: Modality = Modality.WINDOW_MODAL): T {
        val stage = Stage()
        stage.initModality(modality)
        stage.title = Minosoft.LANGUAGE_MANAGER.translate(title).message
        stage.scene = Scene(parent)
        stage.icons.setAll(MINOSOFT_LOGO)

        stage.scene.stylesheets.add(DEFAULT_STYLE)
        val theme = ErosProfileManager.selected.theme.theme
        stage.scene.stylesheets.add(getThemeURL(theme))

        stages.cleanup()
        stages.add(stage)

        val controller: T = fxmlLoader.getController()

        if (controller is JavaFXWindowController) {
            controller.stage = stage
        }
        controller.postInit()

        return controller
    }

    fun <T : JavaFXController> openModal(title: Any, layout: ResourceLocation, controller: T? = null, modality: Modality = Modality.WINDOW_MODAL): T {
        startThemeWatcher()
        val fxmlLoader = FXMLLoader()
        controller?.let { fxmlLoader.setController(it) }
        val parent: Parent = fxmlLoader.load(Minosoft.MINOSOFT_ASSETS_MANAGER[layout])
        return loadController(title, fxmlLoader, parent, modality)
    }

    fun <T : JavaFXController> openModalAsync(title: Any, layout: ResourceLocation, controller: T? = null, modality: Modality = Modality.WINDOW_MODAL, callback: ((T) -> Unit)? = null) {
        DefaultThreadPool += add@{
            startThemeWatcher()
            val fxmlLoader = FXMLLoader()
            controller?.let { fxmlLoader.setController(it) }
            val parent: Parent = fxmlLoader.load(Minosoft.MINOSOFT_ASSETS_MANAGER[layout])

            if (callback == null) {
                return@add
            }

            runLater { callback(loadController(title, fxmlLoader, parent, modality)) }
        }
    }

    fun <T : EmbeddedJavaFXController<out Pane>> loadEmbeddedController(layout: ResourceLocation): T {
        val fxmlLoader = FXMLLoader()
        val pane = fxmlLoader.load<Pane>(Minosoft.MINOSOFT_ASSETS_MANAGER[layout])

        val controller = fxmlLoader.getController<T>()

        controller::root.javaField!!.setValue(controller, pane)
        controller.postInit()

        return controller
    }

    var TextFlow.text: Any?
        get() = TODO("Can not get the text of a TextFlow (yet)")
        set(value) {
            this.children.setAll(Minosoft.LANGUAGE_MANAGER.translate(value).javaFXText)
        }

    var TextField.placeholder: Any?
        get() = this.promptText
        set(value) {
            this.promptText = Minosoft.LANGUAGE_MANAGER.translate(value).message
        }

    var Labeled.ctext: Any?
        get() = this.text
        set(value) {
            this.text = Minosoft.LANGUAGE_MANAGER.translate(value).message
        }

    var TableColumnBase<*, *>.ctext: Any?
        get() = this.text
        set(value) {
            this.text = Minosoft.LANGUAGE_MANAGER.translate(value).message
        }

    var Text.ctext: Any?
        get() = this.text
        set(value) {
            this.text = Minosoft.LANGUAGE_MANAGER.translate(value).message
        }

    fun Text.hyperlink(link: String) {
        val url = link.toURL()
        this.setOnMouseClicked { DefaultThreadPool += { DesktopUtil.openURL(url) } }
        this.accessibleRole = AccessibleRole.HYPERLINK
        this.styleClass.setAll("hyperlink")
        this.clickable()
    }

    fun Text.file(path: File) {
        this.setOnMouseClicked { DefaultThreadPool += { DesktopUtil.openFile(path) } }
        this.accessibleRole = AccessibleRole.HYPERLINK
        this.styleClass.setAll("hyperlink")
        this.clickable()
    }

    fun Node.clickable() {
        this.styleClass.add("button")
        this.cursorProperty().unsafeCast<StyleableProperty<Cursor>>().applyStyle(null, Cursor.HAND)
    }

    fun runLater(runnable: Runnable) {
        if (Thread.currentThread() === JAVA_FX_THREAD) {
            runnable.run()
            return
        }

        Platform.runLater(runnable)
    }

    fun Stage.bringToFront() {
        isAlwaysOnTop = true
        this.requestFocus()
        this.toFront()
        isAlwaysOnTop = false
    }

    private fun getThemeURL(name: String): String {
        val path = "minosoft:eros/themes/$name.css"
        if (path.toResourceLocation() !in THEME_ASSETS_MANAGER) {
            throw Exception("Can not load theme: $name")
        }

        return "resource:$path"
    }
}
