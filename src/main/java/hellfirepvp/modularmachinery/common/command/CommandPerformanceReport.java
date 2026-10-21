package hellfirepvp.modularmachinery.common.command;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import javax.annotation.Nonnull;

import static github.kasuminova.mmce.common.concurrent.TaskExecutor.executedCount;
import static github.kasuminova.mmce.common.concurrent.TaskExecutor.taskUsedTime;
import static github.kasuminova.mmce.common.concurrent.TaskExecutor.totalExecuted;
import static github.kasuminova.mmce.common.concurrent.TaskExecutor.totalUsedTime;

public class CommandPerformanceReport extends CommandBase {
    private static final String LANG_KEY = "command.modularmachinery.performance_report";

    @Nonnull
    @Override
    public String getCommandName() {
        return "mm-performance_report";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Nonnull
    @Override
    public String getCommandUsage(@Nonnull ICommandSender sender) {
        return LANG_KEY;
    }

    @Override
    public void processCommand(@Nonnull ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            executedCount = 0;
            totalExecuted = 0;
            totalUsedTime = 0;
            taskUsedTime = 0;
            sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".reset"));
            return;
        }

        long executedAvgPerExecution = executedCount == 0 ? 0 : totalExecuted / executedCount;
        double usedTimeAvgPerExecution = executedCount == 0 ? 0 : (double) (totalUsedTime / executedCount) / 1000;
        double taskUsedTimeAvg = totalExecuted == 0 ? 0 : (double) (taskUsedTime / executedCount) / 1000;
        long usedTimeAvg = totalExecuted == 0 ? 0 : taskUsedTime / totalExecuted;

        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".title",
            EnumChatFormatting.GREEN + MiscUtils.formatDecimal(executedCount) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentText(""));

        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".total_executed",
            EnumChatFormatting.BLUE + MiscUtils.formatDecimal(totalExecuted) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".tasks_avg_per_execution",
            EnumChatFormatting.BLUE + String.valueOf(executedAvgPerExecution) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentText(""));

        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".total_used_time",
            EnumChatFormatting.BLUE + String.valueOf(totalUsedTime / 1000) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".used_time_avg_per_execution",
            EnumChatFormatting.YELLOW + String.format("%.2f", usedTimeAvgPerExecution) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentText(""));

        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".task_used_time",
            EnumChatFormatting.BLUE + MiscUtils.formatDecimal(((double) taskUsedTime / 1000L)) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentText(""));

        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".task_used_time_avg",
            EnumChatFormatting.YELLOW + String.format("%.2f", taskUsedTimeAvg) + EnumChatFormatting.RESET));
        sender.addChatMessage(new ChatComponentTranslation(LANG_KEY + ".used_time_avg",
            EnumChatFormatting.BLUE + String.valueOf(usedTimeAvg) + EnumChatFormatting.RESET));
    }
}
