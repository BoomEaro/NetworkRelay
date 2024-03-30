package ru.boomearo.networkrelay.logger;

import com.google.common.base.Strings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.apache.logging.log4j.message.MessageFormatMessage;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class Log4JLogHandler extends Handler {

    private final Map<String, Logger> cache = new ConcurrentHashMap<>();

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        Logger logger = cache.computeIfAbsent(Strings.nullToEmpty(record.getLoggerName()), LogManager::getLogger);

        String message = record.getMessage();
        if (record.getResourceBundle() != null) {
            try {
                message = record.getResourceBundle().getString(message);
            } catch (MissingResourceException ignored) {
            }
        }

        final Level level = LevelTranslator.toLevel(record.getLevel());
        if (record.getParameters() != null && record.getParameters().length > 0) {
            logger.log(level, new MessageFormatMessage(message, record.getParameters()), record.getThrown());
        } else {
            logger.log(level, message, record.getThrown());
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
