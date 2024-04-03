package ru.boomearo.networkrelay.commands.app;

import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;
import ru.boomearo.networkrelay.netty.*;

import java.util.List;

public class CommandConn extends CommandNodeApp {

    public CommandConn(CommandNodeApp root) {
        super(root, "conn");
    }

    @Override
    public List<String> getDescription(ConsoleSender consoleSender) {
        return List.of("conn - display current active connections");
    }

    @Override
    public void onExecute(ConsoleSender sender, String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        sender.sendMessage(SEP);
        sender.sendMessage("TCP: Opened Upstream: " + TcpRelayUpstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("TCP: Opened Downstream: " + TcpRelayDownstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("UDP: Opened Downstream: " + UdpRelayDownstreamHandler.OPENED_CONNECTIONS.sum());
        sender.sendMessage("Total Upstream read bytes: " + bytesIntoHumanReadable(StatisticsUpstreamHandler.TOTAL_READ.sum()));
        sender.sendMessage("Total Upstream write bytes: " + bytesIntoHumanReadable(StatisticsUpstreamHandler.TOTAL_WRITE.sum()));
        sender.sendMessage("Total Downstream read bytes: " + bytesIntoHumanReadable(StatisticsDownstreamHandler.TOTAL_READ.sum()));
        sender.sendMessage("Total Downstream write bytes: " + bytesIntoHumanReadable(StatisticsDownstreamHandler.TOTAL_WRITE.sum()));
        sender.sendMessage(SEP);
    }

    private static String bytesIntoHumanReadable(long bytes) {
        return bytesIntoHumanReadable(bytes, false);
    }

    private static String bytesIntoHumanReadable(long bytes, boolean ki) {
        if (ki) {
            bytes = bytes * 8;
        }

        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + (ki ? " Bi" : " B");
        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + (ki ? " KiB" : " KB");
        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + (ki ? " MiB" : " MB");
        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return (bytes / gigabyte) + (ki ? " GiB" : " GB");
        } else if (bytes >= terabyte) {
            return (bytes / terabyte) + (ki ? " TiB" : " TB");
        } else {
            return bytes + " Bites";
        }
    }
}
