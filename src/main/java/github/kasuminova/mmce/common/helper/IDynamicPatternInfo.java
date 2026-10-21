package github.kasuminova.mmce.common.helper;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.mmce.compat.EnumFacing;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

@ZenRegister
@ZenClass("mods.modularmachinery.IDynamicPatternInfo")
public interface IDynamicPatternInfo {

    DynamicPattern getPattern();

    EnumFacing getMatchFacing();

    /**
     * 获取动态结构的方向，通常情况下对应控制器方向。
     *
     * @return 方向（全小写）
     */
    @ZenGetter("facing")
    String getFacing();

    /**
     * 获取的动态结构的名称，对应 JSON 文件中的 "name"。
     *
     * @return 名称
     */
    @ZenGetter("name")
    String getPatternName();

    /**
     * 获取机械的动态结构的大小。
     * 不是设置中的大小。
     *
     * @return 已形成结构中的动态结构的大小。
     */
    @ZenGetter("size")
    int getSize();

    /**
     * 获取动态结构的最小大小。
     *
     * @return 大小
     */
    @ZenGetter("minSize")
    int getMinSize();

    /**
     * 获取动态结构的最大大小。
     *
     * @return 大小
     */
    @ZenGetter("maxSize")
    int getMaxSize();

}
