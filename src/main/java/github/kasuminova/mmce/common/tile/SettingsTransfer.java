package github.kasuminova.mmce.common.tile;

import net.minecraft.nbt.NBTTagCompound;

/** 可用 AE2 内存卡复制 / 粘贴设置的 ME 部件（潜行右键复制，右键粘贴）。 */
public interface SettingsTransfer {
    NBTTagCompound downloadSettings();

    void uploadSettings(NBTTagCompound settings);
}
