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

package de.bixilon.minosoft.terminal.commands.commands;

import de.bixilon.minosoft.Minosoft;
import de.bixilon.minosoft.data.commands.CommandArgumentNode;
import de.bixilon.minosoft.data.commands.CommandLiteralNode;
import de.bixilon.minosoft.data.commands.CommandNode;
import de.bixilon.minosoft.data.commands.parser.IntegerParser;
import de.bixilon.minosoft.data.commands.parser.properties.IntegerParserProperties;
import de.bixilon.minosoft.protocol.network.Connection;
import de.bixilon.minosoft.terminal.CLI;
import de.bixilon.minosoft.terminal.commands.exceptions.ConnectionNotFoundCommandParseException;

public class CommandConnection extends Command {

    @Override
    public CommandNode build(CommandNode parent) {
        parent.addChildren(
                new CommandLiteralNode("connection",
                        new CommandLiteralNode("list", (stack) -> {
                            if (Minosoft.CONNECTIONS.isEmpty()) {
                                print("You are not connected to any server!");
                                return;
                            }
                            print("List of connections:");
                            print("ID\t\t\tAddress\t\t\tAccount");
                            for (var entry : Minosoft.CONNECTIONS.entrySet()) {
                                print("[%d]\t\t\t%s\t\t\t%s", entry.getKey(), entry.getValue().getAddress(), entry.getValue().getPlayer().getAccount());
                            }
                        }),
                        new CommandLiteralNode("select", new CommandArgumentNode("connectionId", IntegerParser.INTEGER_PARSER, new IntegerParserProperties(0), (stack) -> {
                            int connectionId = stack.getInt(0);
                            Connection connection = Minosoft.CONNECTIONS.get(connectionId);
                            if (connection == null) {
                                throw new ConnectionNotFoundCommandParseException(stack, connectionId);
                            }
                            CLI.setCurrentConnection(connection);
                            print("Current connection changed %s", connection);
                        })),
                        new CommandLiteralNode("current", (stack) -> {
                            Connection connection = CLI.getCurrentConnection();
                            if (connection == null) {
                                print("No connection selected");
                                return;
                            }
                            print("Current connection: %s", connection);

                        })));
        return parent;
    }
}
