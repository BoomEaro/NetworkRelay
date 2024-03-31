package ru.boomearo.networkrelay.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

@RequiredArgsConstructor
@Log4j2
public class ConsoleSender {

    public void sendMessage(String message) {
        log.log(Level.INFO, message);
    }

    public void sendMessage(String message, Throwable throwable) {
        log.log(Level.INFO, message, throwable);
    }

}
