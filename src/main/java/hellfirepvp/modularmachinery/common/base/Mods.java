/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.base;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModAPIManager;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: Mods
 * Created by HellFirePvP
 * Date: 05.03.2019 / 13:46
 * <p>
 * 1.7.10 移植：
 * - 常量名与 1.12.2 保持一致，modid 改为 1.7.10 版本的实际 ID（多数为大小写不同，如 Mekanism、IC2）。
 * - 只在 1.12.2 存在、1.7.10 没有对应版本的模组（GTCEu、AE2-UEL、ZenUtils、星辉、自然灵气、GeckoLib 等）
 *   以 {@link #UNAVAILABLE} 标记，isPresent() 恒为 false，避免误触发为 1.12.2 编写的集成逻辑。
 * - RF 能量以 API 形式提供（CoFHAPI|energy），同时检查模组与 API 列表。
 */
public enum Mods {
    CRAFTTWEAKER("MineTweaker3"),
    NEI("NotEnoughItems"),
    WAILA("Waila"),
    JEI(Mods.UNAVAILABLE),
    GREGTECH("gregtech"),
    GREGTECHCEU(Mods.UNAVAILABLE),
    DRACONICEVOLUTION("DraconicEvolution"),
    REDSTONEFLUXAPI("CoFHAPI|energy"),
    MEKANISM("Mekanism"),
    IC2("IC2"),
    TOP(Mods.UNAVAILABLE),
    NUCLEARCRAFT_OVERHAULED(Mods.UNAVAILABLE),
    RESOURCELOADER(Mods.UNAVAILABLE),
    TX_LOADER(Mods.UNAVAILABLE),
    FLUX_NETWORKS(Mods.UNAVAILABLE),
    ZEN_UTILS(Mods.UNAVAILABLE),
    TCONSTRUCT("TConstruct"),
    AE2("appliedenergistics2"),
    AE2EL(Mods.UNAVAILABLE),
    BM2(Mods.UNAVAILABLE),
    TC6(Mods.UNAVAILABLE),
    TA(Mods.UNAVAILABLE),
    TAHUMIC_JEI(Mods.UNAVAILABLE),
    EXU2(Mods.UNAVAILABLE),
    ASTRAL_SORCERY(Mods.UNAVAILABLE),
    NATURESAURA(Mods.UNAVAILABLE),
    // Modular Magic 的 1.7.10 子集：植物魔法（GTNH）、神秘时代 4、血魔法 1（modid AWWayofTime）
    BOTANIA("Botania"),
    THAUMCRAFT("Thaumcraft"),
    BLOODMAGIC("AWWayofTime"),
    GECKOLIB(Mods.UNAVAILABLE),
    MBD(Mods.UNAVAILABLE),
    COMPONENT_MODEL_HIDER(Mods.UNAVAILABLE),
    AE2FCR("ae2fc"),
    MEKENG(Mods.UNAVAILABLE),
    LUMENIZED(Mods.UNAVAILABLE),
    ALFHEIM(Mods.UNAVAILABLE),
    THERMAL_EXPANSION("ThermalExpansion"),
    ENDERIO("EnderIO"),
    ;

    /** 标记 1.7.10 中不存在对应版本的模组。 */
    public static final String UNAVAILABLE = "";

    public final  String  modid;
    private final boolean loaded;

    Mods(String modName) {
        this.modid = modName;
        this.loaded = !modName.isEmpty() && (Loader.isModLoaded(modName) || ModAPIManager.INSTANCE.hasAPI(modName));
    }

    public boolean isPresent() {
        return loaded;
    }
}
