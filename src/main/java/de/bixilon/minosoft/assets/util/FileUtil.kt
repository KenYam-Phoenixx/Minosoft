/*
 * Minosoft
 * Copyright (C) 2020-2024 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.assets.util

import com.github.luben.zstd.ZstdInputStream
import de.bixilon.minosoft.terminal.RunConfiguration
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

object FileUtil {

    fun safeReadFile(path: Path, compressed: Boolean = true): InputStream? {
        return safeReadFile(path.toFile(), compressed)
    }

    fun safeReadFile(file: File, compressed: Boolean = true): InputStream? {
        if (!file.exists()) {
            return null
        }
        return readFile(file, compressed)
    }

    fun readFile(file: File, compressed: Boolean = true): InputStream {
        var stream: InputStream = BufferedInputStream(FileInputStream(file))
        if (compressed) {
            stream = ZstdInputStream(stream)
        }

        return stream
    }

    fun readFile(path: Path, compressed: Boolean = true): InputStream {
        return readFile(path.toFile(), compressed)
    }

    fun createTempFile(): File {
        return Files.createTempFile(RunConfiguration.TEMPORARY_FOLDER, "", "").toFile()
    }
}
