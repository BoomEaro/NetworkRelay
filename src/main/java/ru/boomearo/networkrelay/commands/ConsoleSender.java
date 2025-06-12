package ru.boomearo.networkrelay.commands;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@Log4j2
public class ConsoleSender {

    public void sendMessage(@NonNull String message) {
        log.log(Level.INFO, message);
    }

    public void sendMessage(@NonNull String message, @Nullable Throwable throwable) {
        log.log(Level.INFO, message, throwable);
    }

}
