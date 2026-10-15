package ru.boomearo.networkrelay.commands;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@Slf4j
public class ConsoleSender {

    public void sendMessage(@NonNull String message) {
        log.info(message);
    }

    public void sendMessage(@NonNull String message, @Nullable Throwable throwable) {
        log.info(message, throwable);
    }

}
