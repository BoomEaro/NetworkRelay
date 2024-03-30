package ru.boomearo.networkrelay.utils;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.experimental.UtilityClass;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

@UtilityClass
public class ExceptionUtils {

    public static void formatExceptionLogger(Logger logger, String message, Throwable throwable) {
        if (throwable instanceof ReadTimeoutException || throwable instanceof SocketException) {
            logger.log(Level.WARNING, message + ": " + throwable.getMessage());
            return;
        }

        logger.log(Level.SEVERE, message, throwable);
    }

}
