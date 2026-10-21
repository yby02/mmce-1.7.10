/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 * 许可证全文见 jar 中的 LICENSE-gugu-utils.txt。
 */
package com.warmthdawn.mod.gugu_utils;

import com.warmthdawn.mod.gugu_utils.common.Constants;
import com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.BlockEnvironmentHatch;
import com.warmthdawn.mod.gugu_utils.modularmachenary.environment.TileEnvironmentHatch;
import com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.types.RequirementTypeEnvironment;
import com.warmthdawn.mod.gugu_utils.modularmachenary.tools.ItemRangedConstructTool;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.compat.registry.IRegistryNamed;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GuGu Utils 的 1.7.10 子集，随 MMCE 1.7.10 移植版一起打包：只包含环境传感器（gugu-utils:environmenthatch）
 * 与范围建造选择工具（gugu-utils:constructionranged）。
 * <p>
 * 单独作为一个模组容器（modid gugu-utils），是因为 1.7.10 的 FML 总以“当前活动模组”的 modid 作为注册名前缀：
 * 在这里注册才能得到与原模组相同的 ID，原整合包中写 gugu-utils:environmenthatch 的机器结构 JSON 与 /give 命令可直接使用。
 * 在 MMCE 之后加载；机器结构与配方在 MMCE 的 postInit 中读取，此时这里注册的方块与需求类型都已就绪。
 */
@Mod(modid = GuGuUtils.MODID, name = GuGuUtils.NAME, version = GuGuUtils.VERSION,
    dependencies = "required-after:" + ModularMachinery.MODID)
public class GuGuUtils {

    public static final String MODID   = "gugu-utils";
    public static final String NAME    = "GuGu Utils (MMCE 1.7.10 subset)";
    public static final String VERSION = "0.8.4-mmce-" + ModularMachinery.VERSION;

    public static final Logger log = LogManager.getLogger(NAME);

    public static Block blockEnvironmentHatch;
    public static Item  constructionTool;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 组件类型与需求类型：gugu-utils:environment
        MMCompoments.register();
        RequirementTypeEnvironment.register();

        // 环境传感器：gugu-utils:environmenthatch（物品形式与其他 MMCE 组件相同，带机器颜色）
        blockEnvironmentHatch = new BlockEnvironmentHatch();
        IRegistryNamed.setName(blockEnvironmentHatch, MODID, Constants.NAME_ENVIRONMENTHATCH);
        GameRegistry.registerBlock(blockEnvironmentHatch, ItemBlockMachineComponent.class, Constants.NAME_ENVIRONMENTHATCH);
        GameRegistry.registerTileEntity(TileEnvironmentHatch.class, Constants.TILE_ENVIRONMENTHATCH);

        // 范围建造选择工具：gugu-utils:constructionranged
        constructionTool = new ItemRangedConstructTool();
        IRegistryNamed.setName(constructionTool, MODID, Constants.NAME_RANGED_CONSTRUCTION_TOOL);
        GameRegistry.registerItem(constructionTool, Constants.NAME_RANGED_CONSTRUCTION_TOOL);

        if (event.getSide().isClient()) {
            GuGuUtilsClient.registerHandlers();
        }
        log.info("[gugu-utils] Registered {} and {}", Block.blockRegistry.getNameForObject(blockEnvironmentHatch),
            Item.itemRegistry.getNameForObject(constructionTool));
    }
}
