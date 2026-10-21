package ink.ikx.mmce.core;

import net.minecraftforge.common.config.Configuration;

/**
 * 机械自动组装配置（写在 modularmachinery.cfg 的 auto-assembly 分类）。
 * 1.7.10 移植：1.12.2 的 @Config 注解在 1.7.10 不存在，只保留 Configuration 读取。
 */
public class AssemblyConfig {

    /** 自动组装使用的物品，默认 minecraft:stick。 */
    public static String itemName = "minecraft:stick";

    public static int itemMeta = 0;

    /** 每隔多少 tick 放置一个方块。 */
    public static int tickBlock = 5;

    /** 是否要求背包中备齐全部方块才开始组装。 */
    public static boolean needAllBlocks = true;

    /** 是否跳过带 NBT 要求的方块。 */
    public static boolean skipBlockContainNBT = false;

    public static void loadFormConfig(Configuration config) {
        itemName = config.getString("itemName", "auto-assembly", "minecraft:stick",
            "Set the Item auto-assembly.");
        itemMeta = config.getInt("itemMeta", "auto-assembly", 0,
            0, 32767, "Set the Item's meta, e.g. 0");
        tickBlock = config.getInt("tickBlock", "auto-assembly", 5,
            1, 1000, "Set how many ticks to assemble the block once.");
        needAllBlocks = config.getBoolean("needAllBlocks", "auto-assembly", true,
            "Set the auto-assembly need all blocks must be in the inventory.");
        skipBlockContainNBT = config.getBoolean("skipBlockContainNBT", "auto-assembly", false,
            "Set whether to skip blocks containing NBTs.");
    }
}
