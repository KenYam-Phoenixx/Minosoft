/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.terminal;

import de.bixilon.minosoft.data.commands.CommandRootNode;
import de.bixilon.minosoft.data.commands.CommandStringReader;
import de.bixilon.minosoft.data.commands.parser.exceptions.CommandParseException;
import de.bixilon.minosoft.data.commands.parser.exceptions.UnknownCommandParseException;
import de.bixilon.minosoft.protocol.network.Connection;
import de.bixilon.minosoft.terminal.commands.CommandStack;
import de.bixilon.minosoft.terminal.commands.commands.*;
import de.bixilon.minosoft.terminal.commands.exceptions.CLIException;
import de.bixilon.minosoft.util.CountUpAndDownLatch;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import javax.annotation.Nullable;
import java.io.IOException;

public class CLI {
    private static final CommandRootNode ROOT_NODE;
    private static Connection currentConnection;

    static {
        ROOT_NODE = new CommandRootNode();
        new CommandHelp().build(ROOT_NODE);
        new CommandConnection().build(ROOT_NODE);
        new CommandDisconnect().build(ROOT_NODE);
        new CommandSendChat().build(ROOT_NODE);
    }

    public static Connection getCurrentConnection() {
        return currentConnection;
    }

    public static void setCurrentConnection(@Nullable Connection connection) {
        currentConnection = connection;
    }

    public static void initialize() throws InterruptedException {
        CountUpAndDownLatch latch = new CountUpAndDownLatch(1);
        new Thread(() -> {
            try {
                TerminalBuilder builder = TerminalBuilder.builder();

                Terminal terminal = builder.build();
                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        //    .completer() // ToDo
                        .parser(new DefaultParser())
                        .build();


                latch.countDown();

                while (true) {
                    try {
                        String line;
                        try {
                            line = reader.readLine().trim();
                        } catch (UserInterruptException e) {
                            System.exit(0);
                            return;
                        }
                        terminal.flush();
                        if (line.isBlank()) {
                            continue;
                        }
                        ROOT_NODE.execute(currentConnection, new CommandStringReader(line), new CommandStack());


                    } catch (Exception exception) {
                        if (exception instanceof CommandParseException) {
                            Command.printError("--> " + exception.getMessage());
                            if (exception instanceof UnknownCommandParseException) {
                                Command.printError("Type help for a command list!");
                            }
                            continue;
                        }
                        if (exception instanceof CLIException) {
                            Command.printError("--> " + exception.getMessage());
                            continue;
                        }
                        exception.printStackTrace();
                        if (exception instanceof UserInterruptException) {
                            System.exit(0);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "CLI").start();
        latch.waitUntilZero();
    }
}
