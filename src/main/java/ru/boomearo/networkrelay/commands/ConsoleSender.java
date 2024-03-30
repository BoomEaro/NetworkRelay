package ru.boomearo.networkrelay.commands;

import lombok.RequiredArgsConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class ConsoleSender {

    private final Logger logger;

    public void sendMessage(String message) {
        this.logger.log(Level.INFO, message);
    }

    public void sendMessage(String message, Throwable throwable) {
        this.logger.log(Level.INFO, message, throwable);
    }

}
