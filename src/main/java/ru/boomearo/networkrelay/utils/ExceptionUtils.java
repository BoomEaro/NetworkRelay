package ru.boomearo.networkrelay.utils;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.net.SocketException;

@UtilityClass
public class ExceptionUtils {

    public static void formatException(Logger logger, String message, Throwable throwable) {
        if (throwable instanceof ReadTimeoutException || throwable instanceof SocketException) {
            logger.log(Level.WARN, message + ": " + throwable.getMessage());
            return;
        }

        logger.log(Level.ERROR, message, throwable);
    }

}
