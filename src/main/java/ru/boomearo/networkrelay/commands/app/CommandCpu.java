package ru.boomearo.networkrelay.commands.app;

import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.List;

public class CommandCpu extends CommandNodeApp {

    public CommandCpu(CommandNodeApp root) {
        super(root, "cpu");
    }

    @Override
    public List<String> getDescription(ConsoleSender consoleSender) {
        return List.of("cpu - display current memory and cpu usage");
    }

    @Override
    public void onExecute(ConsoleSender sender, String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        long maxHeapMem = (Runtime.getRuntime().maxMemory() / 1024 / 1024);
        long totalHeapMem = (Runtime.getRuntime().totalMemory() / 1024 / 1024);
        long freeHeapMem = (Runtime.getRuntime().freeMemory() / 1024 / 1024);
        long usedHeapMem = totalHeapMem - freeHeapMem;

        long usedDirectMem = 0;
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for (BufferPoolMXBean pool : pools) {
            usedDirectMem = usedDirectMem + pool.getMemoryUsed();
        }

        usedDirectMem = usedDirectMem / 1024 / 1024;

        long totalUsed = usedHeapMem + usedDirectMem;

        sender.sendMessage(SEP);
        sender.sendMessage("Processor usage: " + getProcessorUsage(true) + " (threads: " + Runtime.getRuntime().availableProcessors() + ")");
        sender.sendMessage("Process usage: " + getProcessorUsage(false) + " (threads: " + ManagementFactory.getThreadMXBean().getThreadCount() + ")");
        sender.sendMessage("Maximum heap: " + maxHeapMem + " MB");
        sender.sendMessage("Total heap: " + totalHeapMem + " MB");
        sender.sendMessage("Free heap: " + freeHeapMem + " MB");
        sender.sendMessage("Used heap: " + usedHeapMem + " MB");
        sender.sendMessage("Used direct memory: " + usedDirectMem + " MB");
        sender.sendMessage("Total used: " + totalUsed + " MB");
        sender.sendMessage(SEP);
    }

    private static String getProcessorUsage(boolean system) {
        String usage;
        double cpuUsage;
        try {
            com.sun.management.OperatingSystemMXBean systemBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            cpuUsage = ((system ? systemBean.getCpuLoad() : systemBean.getProcessCpuLoad()) * 100);
        } catch (Throwable t) {
            return "Not available: " + t.getMessage();
        }
        if (cpuUsage < 0) {
            usage = "Not available";
        } else {
            usage = getCpuFormat(cpuUsage, new DecimalFormat("#.#"));
        }

        return usage;
    }

    private static String getCpuFormat(double cpu, DecimalFormat df) {
        if (cpu <= 15) {
            return df.format(cpu) + "%";
        }
        if (cpu <= 45) {
            return df.format(cpu) + "%";
        }
        if (cpu <= 75) {
            return df.format(cpu) + "%";
        }
        if (cpu <= 100) {
            return df.format(cpu) + "%";
        }
        return df.format(cpu) + "%";
    }

}
