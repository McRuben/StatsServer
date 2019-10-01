package de.derrop.labymod.addons.server.function;
/*
 * Created by derrop on 29.09.2019
 */

public interface ThrowingFunction<I, O, T extends Throwable> {

    O apply(I input) throws T;

}
