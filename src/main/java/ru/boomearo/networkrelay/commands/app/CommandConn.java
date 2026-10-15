package ru.boomearo.networkrelay.commands.app;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;
import ru.boomearo.networkrelay.netty.*;

import java.text.DecimalFormat;
import java.util.List;

public class CommandConn extends CommandNodeApp {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public CommandConn(CommandNodeApp root) {
        super(root, "conn");
    }

    @Nullable
    @Override
    public List<String> getDescription(@NonNull ConsoleSender consoleSender) {
        return List.of("conn - display current active connections");
    }

    @Override
    public void onExecute(@NonNull ConsoleSender sender, @NonNull String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        sender.sendMessage(SEPARATOR);
        sender.sendMessage("TCP: Opened Upstream: " + TcpRelayUpstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("TCP: Opened Downstream: " + TcpRelayDownstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("UDP: Opened Downstream: " + UdpRelayDownstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("Total Upstream read bytes: " + bytesIntoHumanReadable(StatisticsUpstreamHandler.TOTAL_READ.sum()));
        sender.sendMessage("Total Upstream write bytes: " + bytesIntoHumanReadable(StatisticsUpstreamHandler.TOTAL_WRITE.sum()));
        sender.sendMessage("Total Downstream read bytes: " + bytesIntoHumanReadable(StatisticsDownstreamHandler.TOTAL_READ.sum()));
        sender.sendMessage("Total Downstream write bytes: " + bytesIntoHumanReadable(StatisticsDownstreamHandler.TOTAL_WRITE.sum()));
        sender.sendMessage(SEPARATOR);
    }

    @NonNull
    private static String bytesIntoHumanReadable(double bytes) {
        double kilobyte = 1024;
        double megabyte = kilobyte * 1024;
        double gigabyte = megabyte * 1024;
        double terabyte = gigabyte * 1024;

        if (bytes >= 0 && bytes < kilobyte) {
            return bytes + " B";
        } else if (bytes >= kilobyte && bytes < megabyte) {
            return DECIMAL_FORMAT.format(bytes / kilobyte) + " KB";
        } else if (bytes >= megabyte && bytes < gigabyte) {
            return DECIMAL_FORMAT.format(bytes / megabyte) + " MB";
        } else if (bytes >= gigabyte && bytes < terabyte) {
            return DECIMAL_FORMAT.format(bytes / gigabyte) + " GB";
        } else if (bytes >= terabyte) {
            return DECIMAL_FORMAT.format(bytes / terabyte) + " TB";
        } else {
            return bytes + " bytes";
        }
    }
}
