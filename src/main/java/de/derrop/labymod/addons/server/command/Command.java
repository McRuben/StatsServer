package de.derrop.labymod.addons.server.command;
/*
 * Created by derrop on 05.10.2019
 */

import lombok.*;

@Getter
@Setter
public abstract class Command {

    private String[] names;

    public Command(String... names) {
        this.names = names;
    }

    public abstract void execute(CommandSender sender, String label, String line, String[] args);

    public boolean canBeExecuted(CommandSender sender) {
        return true;
    }

}
