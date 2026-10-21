/*
 * 移植自 gugu-utils（https://github.com/ParaParty/gugu-utils，MIT License，Copyright (c) 2020 派瑞派对）。
 */
package com.warmthdawn.mod.gugu_utils;

import com.warmthdawn.mod.gugu_utils.handler.ClientEventHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;

/** 客户端初始化（单独成类，专用服务器上不加载客户端类）。 */
@SideOnly(Side.CLIENT)
final class GuGuUtilsClient {

    private GuGuUtilsClient() {
    }

    static void registerHandlers() {
        ClientEventHandler handler = new ClientEventHandler();
        FMLCommonHandler.instance().bus().register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }
}
