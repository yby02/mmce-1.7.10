/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.integration.crafttweaker;

import github.kasuminova.mmce.compat.ItemStacks;

import github.kasuminova.mmce.compat.crt.CraftTweakerAPI;
import github.kasuminova.mmce.compat.crt.ZenRegister;
import minetweaker.api.data.IData;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IngredientStack;
import minetweaker.api.liquid.ILiquidStack;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;
import minetweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFailureEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFailureEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeStartEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeTickEvent;
import github.kasuminova.mmce.common.util.concurrent.Action;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.PreparedRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementCatalyst;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluidPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGasPerTick;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementIngredientArray;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.AdvancedItemCheckerCT;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.helper.AdvancedItemModifierCT;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import github.kasuminova.mmce.common.integration.mekanism.MekanismZenSupport;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraft.item.ItemStack;
import github.kasuminova.mmce.compat.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import cpw.mods.fml.common.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RecipePrimer
 * Created by HellFirePvP
 * Date: 02.01.2018 / 18:18
 */
@ZenRegister
@ZenClass("mods.modularmachinery.RecipePrimer")
/*
 * 1.7.10 移植：Mekanism 气体方法。
 * - “气体注册名 + 数量”写法：addGasInput / addGasOutput / addGasPerTickInput / addGasPerTickOutput(String, int)，始终可用。
 * - <gas:...> 写法：1.7.10 的气体 ZenScript 类型由 ModTweaker2 提供（mods.mekanism.gas.IGasStack）。MMCE 不再自行注册同名
 *   ZenClass 与尖括号（会与 ModTweaker 冲突），而是按鸭子类型读取气体对象的 name / amount，因此 <gas:x> * n 可直接传入。
 * 这些方法以 @Optional.Method 软依赖 Mekanism，未装 Mekanism 时被剥离，调用它们的脚本文件会编译失败（其他脚本文件不受影响）。
 */
public class RecipePrimer implements PreparedRecipe {

    protected final ResourceLocation name, machineName;
    private final int tickTime, priority;
    private final boolean doesVoidPerTick;

    private final List<ComponentRequirement<?, ?>>                components           = new LinkedList<>();
    private final List<Action>                                    needAfterInitActions = new LinkedList<>();
    private final List<String>                                    toolTipList          = new ArrayList<>();
    private final Map<Class<?>, List<IEventHandler<RecipeEvent>>> recipeEventHandlers  = new HashMap<>();

    private boolean                    parallelized  = Config.recipeParallelizeEnabledByDefault;
    private boolean                    loadJEI       = true;
    private int                        maxThreads    = -1;
    private String                     threadName    = "";
    private ComponentRequirement<?, ?> lastComponent = null;

    /*
     * 1.7.10 移植：MineTweaker3 解析不到的尖括号引用（未安装模组的物品、拼错的名称等）只在 minetweaker.log 报
     * “Could not resolve <...>”，脚本照常运行、该处的值为 null；上游在报错信息里调用 input.getClass()，因此抛 NPE 并中断整个脚本文件。
     * 现在记下收到 null 的方法并报错，build() 时不注册这条配方（与 JSON 配方引用未知物品时整条跳过一致，避免缺一项原料的配方被注册），
     * 同一文件中的其他配方照常注册。
     */
    private final List<String> nullArguments = new ArrayList<>();

    public RecipePrimer(ResourceLocation registryName, ResourceLocation owningMachine, int tickTime, int configuredPriority, boolean doesVoidPerTick) {
        this.name = registryName;
        this.machineName = owningMachine;
        this.tickTime = tickTime;
        this.priority = configuredPriority;
        this.doesVoidPerTick = doesVoidPerTick;
    }

    @ZenMethod
    public RecipePrimer setParallelizeUnaffected(boolean unaffected) {
        if (lastComponent instanceof ComponentRequirement.Parallelizable parallelizable) {
            parallelizable.setParallelizeUnaffected(unaffected);
        } else if (lastComponent == null) {
            CraftTweakerAPI.logWarning("[ModularMachinery] setParallelizeUnaffected(bool) has no input / output to apply to!");
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] Target " + lastComponent.getClass() + " cannot be parallelized!");
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer setChance(float chance) {
        if (lastComponent != null) {
            if (lastComponent instanceof ComponentRequirement.ChancedRequirement chancedReq) {
                chancedReq.setChance(chance);
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] Cannot set chance for not-chance-based Component: " + lastComponent.getClass());
            }
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer setTag(String selectorTag) {
        if (lastComponent != null) {
            lastComponent.setTag(new ComponentSelectorTag(selectorTag));
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer setPreViewNBT(IData nbt) {
        if (lastComponent != null) {
            if (lastComponent instanceof RequirementItem reqItem) {
                reqItem.previewDisplayTag = CraftTweakerMC.getNBTCompound(nbt);
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] setPreViewNBT(IData nbt) only can be applied to `Item`!");
            }
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] setPreViewNBT(IData nbt) only can be applied to `Item`!");
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer setNBTChecker(AdvancedItemCheckerCT checker) {
        if (lastComponent != null) {
            if (lastComponent instanceof RequirementItem reqItem) {
                reqItem.setItemChecker((controller, stack) -> checker.isMatch(controller, CraftTweakerMC.getIItemStack(stack)));
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] setNBTChecker(AdvancedItemNBTChecker checker) only can be applied to `Item`!");
            }
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] setNBTChecker(AdvancedItemNBTChecker checker) only can be applied to `Item`!");
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addItemModifier(AdvancedItemModifierCT modifier) {
        if (lastComponent != null) {
            if (lastComponent instanceof RequirementItem reqItem) {
                reqItem.addItemModifier((controller, stack) -> CraftTweakerMC.getItemStack(modifier.apply(controller, CraftTweakerMC.getIItemStackMutable(stack))));
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] addItemModifier(AdvancedItemModifier checker) only can be applied to `Item`!");
            }
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] addItemModifier(AdvancedItemModifier checker) only can be applied to `Item`!");
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer setMinMaxAmount(int min, int max) {
        if (lastComponent != null) {
            if (lastComponent instanceof RequirementItem reqItem) {
                if (min < max) {
                    reqItem.minAmount = min;
                    reqItem.maxAmount = max;
                } else {
                    CraftTweakerAPI.logWarning("[ModularMachinery] `min` cannot larger than `max`!");
                }
            } else {
                CraftTweakerAPI.logWarning("[ModularMachinery] setMinMaxOutputAmount(int min, int max) only can be applied to `Item`!");
            }
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] setMinMaxOutputAmount(int min, int max) only can be applied to `Item`!");
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer consumeDurability(int durability) {
        if (lastComponent instanceof RequirementItem reqItem) {
            if (reqItem.getActionType() != IOType.INPUT) {
                CraftTweakerAPI.logWarning("[ModularMachinery] consumeDurability(int) only can be applied to item inputs!");
                return this;
            }
            int safeDurability = durability;
            if (durability < 0) {
                CraftTweakerAPI.logWarning("[ModularMachinery] consumeDurability(int) requires a non-negative durability value!");
                safeDurability = 0;
            }
            if (safeDurability > 0 && !reqItem.supportsDurability()) {
                CraftTweakerAPI.logWarning("[ModularMachinery] consumeDurability(int) only applies to items with durability; a component will be ignored.");
                reqItem.setConsumeDurability(0);
                return this;
            }
            reqItem.setConsumeDurability(safeDurability);
        } else {
            CraftTweakerAPI.logWarning("[ModularMachinery] consumeDurability(int) only can be applied to `Item`!");
        }
        return this;
    }

    /**
     * <p>为某个输入或输出设置特定触发时间。</p>
     * <p>注意：如果设置了触发时间，则配方在其他时间不会触发任何消耗或产出动作。</p>
     *
     * @param tickTime 触发的配方时间，实际触发时间受到配方修改器影响
     */
    @ZenMethod
    public RecipePrimer setTriggerTime(int tickTime) {
        if (lastComponent != null) {
            lastComponent.setTriggerTime(tickTime);
        }
        return this;
    }

    /**
     * 使触发时间可以被重复触发。
     *
     * @param repeatable true 为可重复，默认 false。
     */
    @ZenMethod
    public RecipePrimer setTriggerRepeatable(boolean repeatable) {
        if (lastComponent != null) {
            lastComponent.setTriggerRepeatable(repeatable);
        }
        return this;
    }

    /**
     * <p>使一个物品/流体等需求忽略输出检测，对一些大量输出不同种类物品等需求非常有用。</p>
     * <p>警告：如果忽略输出则有时可能会导致输出吞物品行为。</p>
     *
     * @param ignoreOutputCheck true 为忽略，默认为 false 不忽略。
     */
    @ZenMethod
    public RecipePrimer setIgnoreOutputCheck(boolean ignoreOutputCheck) {
        if (lastComponent != null) {
            lastComponent.setIgnoreOutputCheck(ignoreOutputCheck);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addRecipeTooltip(String... tooltips) {
        toolTipList.addAll(Arrays.asList(tooltips));
        return this;
    }

    @ZenMethod
    public RecipePrimer addSmartInterfaceDataInput(String typeStr, float minValue, float maxValue) {
        needAfterInitActions.add(() -> {
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(machineName);
            if (machine == null) {
                CraftTweakerAPI.logError("Could not find machine `" + machineName.toString() + "`!");
                return;
            }
            SmartInterfaceType type = machine.getSmartInterfaceType(typeStr);
            if (type == null) {
                CraftTweakerAPI.logError("SmartInterfaceType " + typeStr + " Not Found!");
                return;
            }
            appendComponent(new RequirementInterfaceNumInput(type, minValue, maxValue));
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addSmartInterfaceDataInput(String typeStr, float value) {
        return addSmartInterfaceDataInput(typeStr, value, value);
    }

    @ZenMethod
    public RecipePrimer addPreCheckHandler(IEventHandler<RecipeCheckEvent> handler) {
        addRecipeEventHandler(RecipeCheckEvent.class, event -> {
            if (event.phase != Phase.START) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addPostCheckHandler(IEventHandler<RecipeCheckEvent> handler) {
        addRecipeEventHandler(RecipeCheckEvent.class, event -> {
            if (event.phase != Phase.END) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    @Deprecated
    public RecipePrimer addCheckHandler(IEventHandler<RecipeCheckEvent> handler) {
        CraftTweakerAPI.logWarning("[ModularMachinery] Deprecated method addCheckHandler()! Consider using addPostCheckHandler()");
        addRecipeEventHandler(RecipeCheckEvent.class, handler);
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // EventHandlers
    //----------------------------------------------------------------------------------------------

    @ZenMethod
    public RecipePrimer addStartHandler(IEventHandler<RecipeStartEvent> handler) {
        addRecipeEventHandler(RecipeStartEvent.class, handler);
        return this;
    }

    @ZenMethod
    public RecipePrimer addPreTickHandler(IEventHandler<RecipeTickEvent> handler) {
        addRecipeEventHandler(RecipeTickEvent.class, event -> {
            if (event.phase != Phase.START) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addPostTickHandler(IEventHandler<RecipeTickEvent> handler) {
        addRecipeEventHandler(RecipeTickEvent.class, event -> {
            if (event.phase != Phase.END) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addFailureHandler(IEventHandler<RecipeFailureEvent> handler) {
        addRecipeEventHandler(RecipeFailureEvent.class, handler);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFinishHandler(IEventHandler<RecipeFinishEvent> handler) {
        addRecipeEventHandler(RecipeFinishEvent.class, handler);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFactoryStartHandler(IEventHandler<FactoryRecipeStartEvent> handler) {
        addRecipeEventHandler(FactoryRecipeStartEvent.class, handler);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFactoryPreTickHandler(IEventHandler<FactoryRecipeTickEvent> handler) {
        addRecipeEventHandler(FactoryRecipeTickEvent.class, event -> {
            if (event.phase != Phase.START) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addFactoryPostTickHandler(IEventHandler<FactoryRecipeTickEvent> handler) {
        addRecipeEventHandler(FactoryRecipeTickEvent.class, event -> {
            if (event.phase != Phase.END) {
                return;
            }
            handler.handle(event);
        });
        return this;
    }

    @ZenMethod
    public RecipePrimer addFactoryFailureHandler(IEventHandler<FactoryRecipeFailureEvent> handler) {
        addRecipeEventHandler(FactoryRecipeFailureEvent.class, handler);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFactoryFinishHandler(IEventHandler<FactoryRecipeFinishEvent> handler) {
        addRecipeEventHandler(FactoryRecipeFinishEvent.class, handler);
        return this;
    }

    private <H extends RecipeEvent> void addRecipeEventHandler(Class<H> hClass, IEventHandler<H> handler) {
        recipeEventHandlers.putIfAbsent(hClass, new ArrayList<>());
        recipeEventHandlers.get(hClass).add((IEventHandler<RecipeEvent>) handler);
    }

    //----------------------------------------------------------------------------------------------
    // General Input & Output
    //----------------------------------------------------------------------------------------------
    /**
     * 逐层拆开 {@link IIngredient} 的包装，返回最内层的真实类型。
     * <p>
     * 1.7.10 的 MineTweaker3 里 {@code <ore:x> * n} 得到的是 {@code IngredientStack}，而它的内部可能还是
     * {@code IngredientOreDict}（只实现 {@code IIngredient}，<b>不是</b> {@code IOreDictEntry}），
     * 因此只判断一层 {@code instanceof} 会漏掉这类写法并报 "Invalid input type"。
     * 这里沿 {@code getInternal()} 逐层向下，遇到 {@code IOreDictEntry} 或 {@code IItemStack} 立即返回。
     */
    private static Object unwrapIngredient(IIngredient ingredient) {
        Object current = ingredient;
        for (int depth = 0; depth < 8; depth++) {
            if (current instanceof IOreDictEntry || current instanceof IItemStack) {
                return current;
            }
            if (!(current instanceof IIngredient)) {
                return current;
            }
            Object internal = ((IIngredient) current).getInternal();
            if (internal == null || internal == current) {
                return current;
            }
            current = internal;
        }
        return current;
    }

    /** 该 {@link IIngredient} 最终是否指向一个矿物词典条目。 */
    private static boolean isOreDictIngredient(IIngredient ingredient) {
        return unwrapIngredient(ingredient) instanceof IOreDictEntry;
    }

    /** 该 {@link IIngredient} 最终是否指向一个具体物品。 */
    private static boolean isItemIngredient(IIngredient ingredient) {
        return unwrapIngredient(ingredient) instanceof IItemStack;
    }

    @ZenMethod
    public RecipePrimer addInput(IIngredient input) {
        if (isNullArgument(input, "addInput")) {
            return this;
        }
        if (isItemIngredient(input) || isOreDictIngredient(input)) {
            addItemInput(input);
        } else if (input instanceof ILiquidStack liquidStack) {
            addFluidInput(liquidStack);
        } else if (Mods.MEKANISM.isPresent() && MekanismZenSupport.isGas(input)) {
            MekanismZenSupport.addGas(this, input, IOType.INPUT);
        } else {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Invalid input type %s(%s)! Ignored.", input, input.getClass()));
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addInputs(IIngredient... inputs) {
        for (IIngredient input : inputs) {
            addInput(input);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addOutput(IIngredient output) {
        if (isNullArgument(output, "addOutput")) {
            return this;
        }
        if (isItemIngredient(output) || isOreDictIngredient(output)) {
            addItemOutput(output);
        } else if (output instanceof ILiquidStack) {
            addFluidOutput((ILiquidStack) output);
        } else if (Mods.MEKANISM.isPresent() && MekanismZenSupport.isGas(output)) {
            MekanismZenSupport.addGas(this, output, IOType.OUTPUT);
        } else {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Invalid output type %s(%s)! Ignored.", output, output.getClass()));
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addOutputs(IIngredient... outputs) {
        for (IIngredient output : outputs) {
            addOutput(output);
        }
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // Energy input & output
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public RecipePrimer addEnergyPerTickInput(long perTick) {
        requireEnergy(IOType.INPUT, perTick);
        return this;
    }

    @ZenMethod
    public RecipePrimer addEnergyPerTickOutput(long perTick) {
        requireEnergy(IOType.OUTPUT, perTick);
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // FLUID input & output
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public RecipePrimer addFluidInput(ILiquidStack fluid) {
        requireFluid(IOType.INPUT, fluid, false);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidInputs(ILiquidStack... fluids) {
        for (ILiquidStack fluid : fluids) {
            addFluidInput(fluid);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidPerTickInput(ILiquidStack fluid) {
        requireFluid(IOType.INPUT, fluid, true);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidPerTickInputs(ILiquidStack... fluids) {
        for (ILiquidStack fluid : fluids) {
            addFluidPerTickInput(fluid);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidOutput(ILiquidStack fluid) {
        requireFluid(IOType.OUTPUT, fluid, false);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidOutputs(ILiquidStack... fluids) {
        for (ILiquidStack fluid : fluids) {
            addFluidOutput(fluid);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidPerTickOutput(ILiquidStack fluid) {
        requireFluid(IOType.OUTPUT, fluid, true);
        return this;
    }

    @ZenMethod
    public RecipePrimer addFluidPerTickOutputs(ILiquidStack... fluids) {
        for (ILiquidStack fluid : fluids) {
            addFluidPerTickOutput(fluid);
        }
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // GAS input & output
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasInput(String gasName, int amount) {
        requireGas(IOType.INPUT, gasName, amount, false);
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasOutput(String gasName, int amount) {
        requireGas(IOType.OUTPUT, gasName, amount, false);
        return this;
    }

    // Gas Per Tick Input / Output

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickInput(String gasName, int amount) {
        requireGas(IOType.INPUT, gasName, amount, true);
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickOutput(String gasName, int amount) {
        requireGas(IOType.OUTPUT, gasName, amount, true);
        return this;
    }

    // 与上游相同的 <gas:...> 写法，含复数形式；addInput / addOutput 也会识别气体。
    // 气体对象由 ModTweaker2 提供（mods.mekanism.gas.IGasStack），这里按鸭子类型读取 name / amount，不绑定具体实现类。

    private RecipePrimer addGasFromIngredient(IIngredient gas, IOType ioType, boolean perTick, String method) {
        if (isNullArgument(gas, method)) {
            return this;
        }
        String name = MekanismZenSupport.gasName(gas);
        if (name == null) {
            CraftTweakerAPI.logError(String.format(
                "[ModularMachinery] %s: %s 不是 Mekanism 气体对象（缺少 name / amount）。需要 ModTweaker2 提供的 <gas:...>，或改用 \"气体名\", 数量 的写法。",
                method, gas));
            return this;
        }
        requireGas(ioType, name, MekanismZenSupport.gasAmount(gas), perTick);
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasInput(IIngredient gas) {
        return addGasFromIngredient(gas, IOType.INPUT, false, "addGasInput");
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasInputs(IIngredient... gases) {
        for (IIngredient gas : gases) {
            addGasInput(gas);
        }
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasOutput(IIngredient gas) {
        return addGasFromIngredient(gas, IOType.OUTPUT, false, "addGasOutput");
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasOutputs(IIngredient... gases) {
        for (IIngredient gas : gases) {
            addGasOutput(gas);
        }
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickInput(IIngredient gas) {
        return addGasFromIngredient(gas, IOType.INPUT, true, "addGasPerTickInput");
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickInputs(IIngredient... gases) {
        for (IIngredient gas : gases) {
            addGasPerTickInput(gas);
        }
        return this;
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickOutput(IIngredient gas) {
        return addGasFromIngredient(gas, IOType.OUTPUT, true, "addGasPerTickOutput");
    }

    @ZenMethod
    @Optional.Method(modid = "Mekanism")
    public RecipePrimer addGasPerTickOutputs(IIngredient... gases) {
        for (IIngredient gas : gases) {
            addGasPerTickOutput(gas);
        }
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // ITEM input
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public RecipePrimer addItemInput(IIngredient input) {
        if (isNullArgument(input, "addItemInput")) {
            return this;
        }
        Object core = unwrapIngredient(input);
        if (core instanceof IOreDictEntry oreDict) {
            requireItem(IOType.INPUT, oreDict.getName(), Math.max(1, input.getAmount()));
        } else if (core instanceof IItemStack stack) {
            int amount = input.getAmount();
            if (amount > 0 && amount != stack.getAmount()) {
                stack = stack.amount(amount);
            }
            requireItem(IOType.INPUT, stack);
        } else {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Invalid input type %s(%s)! Ignored.", input, input.getClass()));
        }

        return this;
    }

    @Deprecated
    @ZenMethod
    public RecipePrimer addItemInput(IOreDictEntry oreDict, int amount) {
        if (isNullArgument(oreDict, "addItemInput")) {
            return this;
        }
        requireItem(IOType.INPUT, oreDict.getName(), amount);
        CraftTweakerAPI.logWarning(String.format("[ModularMachinery] Deprecated method " +
                "`addItemInput(<ore:%s>, %s)`! Consider using `addItemInput(<ore:%s> * %s)`",
            oreDict.getName(), amount, oreDict.getName(), amount)
        );
        return this;
    }

    @ZenMethod
    public RecipePrimer addItemInputs(IIngredient... inputs) {
        for (IIngredient input : inputs) {
            addItemInput(input);
        }
        return this;
    }

    @ZenMethod
    public RecipePrimer addFuelItemInput(int requiredTotalBurnTime) {
        requireFuel(requiredTotalBurnTime);
        return this;
    }

    @ZenMethod
    public RecipePrimer addIngredientArrayInput(IngredientArrayPrimer ingredientArrayPrimer) {
        appendComponent(new RequirementIngredientArray(ingredientArrayPrimer.getIngredientStackList()));
        return this;
    }

    /**
     * <p>随机选择给定的一组物品中的其中一个输出。</p>
     * <p>虽然都使用 IngredientArrayPrimer 作为参数，但是输出的工作机制要<strong>稍有不同</strong>。</p>
     * <p>每个 Ingredient 的 chance 代表整个物品列表的随机选择权重，权重越高，输出概率越大。</p>
     * <p>同样，setMinMaxAmount() 也能够起作用。</p>
     */
    @ZenMethod
    public RecipePrimer addRandomItemOutput(IngredientArrayPrimer ingredientArrayPrimer) {
        appendComponent(new RequirementIngredientArray(ingredientArrayPrimer.getIngredientStackList(), IOType.OUTPUT));
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // ITEM output
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public RecipePrimer addItemOutput(IIngredient output) {
        if (isNullArgument(output, "addItemOutput")) {
            return this;
        }
        Object core = unwrapIngredient(output);
        if (core instanceof IOreDictEntry oreDict) {
            requireItem(IOType.OUTPUT, oreDict.getName(), Math.max(1, output.getAmount()));
        } else if (core instanceof IItemStack stack) {
            int amount = output.getAmount();
            if (amount > 0 && amount != stack.getAmount()) {
                stack = stack.amount(amount);
            }
            requireItem(IOType.OUTPUT, stack);
        } else {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Invalid output type %s(%s)! Ignored.", output, output.getClass()));
        }

        return this;
    }

    @Deprecated
    @ZenMethod
    public RecipePrimer addItemOutput(IOreDictEntry oreDict, int amount) {
        if (isNullArgument(oreDict, "addItemOutput")) {
            return this;
        }
        requireItem(IOType.OUTPUT, oreDict.getName(), amount);
        CraftTweakerAPI.logWarning(String.format("[ModularMachinery] Deprecated method " +
                "`addItemOutput(<ore:%s>, %s)`! Consider using `addItemOutput(<ore:%s> * %s)`",
            oreDict.getName(), amount, oreDict.getName(), amount)
        );
        return this;
    }

    @ZenMethod
    public RecipePrimer addItemOutputs(IIngredient... inputs) {
        for (IIngredient input : inputs) {
            addItemOutput(input);
        }

        return this;
    }

    //----------------------------------------------------------------------------------------------
    // Catalyst
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public RecipePrimer addCatalystInput(IIngredient input, String[] tooltips, RecipeModifier[] modifiers) {
        if (isNullArgument(input, "addCatalystInput")) {
            return this;
        }
        Object core = unwrapIngredient(input);
        if (core instanceof IOreDictEntry oreDict) {
            requireCatalyst(oreDict.getName(), Math.max(1, input.getAmount()), tooltips, modifiers);
        } else if (core instanceof IItemStack stack) {
            requireCatalyst(stack, tooltips, modifiers);
        } else {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Invalid input type %s(%s)! Ignored.", input, input.getClass()));
        }

        return this;
    }

    @ZenMethod
    public RecipePrimer addCatalystInput(IngredientArrayPrimer input, String[] tooltips, RecipeModifier[] modifiers) {
        requireCatalyst(input, tooltips, modifiers);
        return this;
    }

    //----------------------------------------------------------------------------------------------
    // Internals
    //----------------------------------------------------------------------------------------------
    private void requireEnergy(IOType ioType, long perTick) {
        appendComponent(new RequirementEnergy(ioType, perTick));
    }

    private void requireFluid(IOType ioType, ILiquidStack stack, boolean isPerTick) {
        if (isNullArgument(stack, "addFluid" + (isPerTick ? "PerTick" : "") + (ioType == IOType.INPUT ? "Input" : "Output"))) {
            return;
        }
        FluidStack mcFluid = CraftTweakerMC.getLiquidStack(stack);
        if (mcFluid == null) {
            CraftTweakerAPI.logError("[ModularMachinery] FluidStack not found/unknown fluid: " + stack.toString());
            return;
        }
        if (stack.getTag() != null) {
            mcFluid.tag = CraftTweakerMC.getNBTCompound(stack.getTag());
        }

        if (isPerTick) {
            appendComponent(new RequirementFluidPerTick(ioType, mcFluid));
        } else {
            appendComponent(new RequirementFluid(ioType, mcFluid));
        }
    }

    @Optional.Method(modid = "Mekanism")
    private void requireGas(IOType ioType, String gasName, int amount, boolean perTick) {
        Gas gas = GasRegistry.getGas(gasName);
        if (gas == null) {
            CraftTweakerAPI.logError("[ModularMachinery] GasStack not found/unknown gas: " + gasName);
            return;
        }
        GasStack gasStack = new GasStack(gas, Math.max(0, amount));
        if (perTick) {
            appendComponent(new RequirementGasPerTick(ioType, gasStack));
        } else {
            appendComponent(new RequirementGas(ioType, gasStack));
        }
    }

    private void requireFuel(int requiredTotalBurnTime) {
        appendComponent(new RequirementItem(IOType.INPUT, requiredTotalBurnTime));
    }

    private void requireItem(IOType ioType, IItemStack stack) {
        ItemStack mcStack = CraftTweakerMC.getItemStack(stack);
        if (ItemStacks.isEmpty(mcStack)) {
            CraftTweakerAPI.logError("[ModularMachinery] ItemStack not found/unknown item: " + stack.toString());
            return;
        }
        RequirementItem ri = new RequirementItem(ioType, mcStack);
        if (stack.getTag().length() > 0) {
            ri.tag = CraftTweakerMC.getNBTCompound(stack.getTag());
            ri.previewDisplayTag = CraftTweakerMC.getNBTCompound(stack.getTag());
        }
        appendComponent(ri);
    }

    private void requireItem(IOType ioType, String oreDictName, int amount) {
        appendComponent(new RequirementItem(ioType, oreDictName, amount));
    }

    private void requireCatalyst(String oreDictName, int amount, String[] tooltips, RecipeModifier[] modifiers) {
        RequirementCatalyst catalyst = new RequirementCatalyst(oreDictName, amount);
        for (String tooltip : tooltips) {
            catalyst.addTooltip(tooltip);
        }
        for (RecipeModifier modifier : modifiers) {
            if (modifier != null) {
                catalyst.addModifier(modifier);
            }
        }
        appendComponent(catalyst);
    }

    private void requireCatalyst(IItemStack stack, String[] tooltips, RecipeModifier[] modifiers) {
        ItemStack mcStack = CraftTweakerMC.getItemStack(stack);
        if (ItemStacks.isEmpty(mcStack)) {
            CraftTweakerAPI.logError("[ModularMachinery] ItemStack not found/unknown item: " + stack.toString());
            return;
        }
        RequirementCatalyst catalyst = new RequirementCatalyst(mcStack);
        for (String tooltip : tooltips) {
            catalyst.addTooltip(tooltip);
        }
        for (RecipeModifier modifier : modifiers) {
            if (modifier != null) {
                catalyst.addModifier(modifier);
            }
        }
        appendComponent(catalyst);
    }

    private void requireCatalyst(IngredientArrayPrimer ingredientArrayPrimer, String[] tooltips, RecipeModifier[] modifiers) {
        RequirementCatalyst catalyst = new RequirementCatalyst(ingredientArrayPrimer.getIngredientStackList());
        for (String tooltip : tooltips) {
            catalyst.addTooltip(tooltip);
        }
        for (RecipeModifier modifier : modifiers) {
            if (modifier != null) {
                catalyst.addModifier(modifier);
            }
        }
        appendComponent(catalyst);
    }

    public void appendComponent(ComponentRequirement<?, ?> component) {
        this.components.add(component);
        this.lastComponent = component;
    }

    private boolean isNullArgument(Object argument, String method) {
        if (argument != null) {
            return false;
        }
        nullArguments.add(method);
        // 后面的 setChance 等修饰方法不应作用到上一项
        lastComponent = null;
        CraftTweakerAPI.logError(String.format("[ModularMachinery] Recipe %s: %s(null) - the argument could not be resolved "
            + "(see \"Could not resolve <...>\" above: a mod that is not installed or a misspelled name). This recipe will not be registered.",
            name, method));
        return true;
    }

    //----------------------------------------------------------------------------------------------
    // build
    //----------------------------------------------------------------------------------------------
    @ZenMethod
    public void build() {
        if (!nullArguments.isEmpty()) {
            CraftTweakerAPI.logError(String.format("[ModularMachinery] Recipe %s was not registered: %d null argument(s) in %s.",
                name, nullArguments.size(), String.join(", ", nullArguments)));
            return;
        }
        RecipeRegistry.getRegistry().registerRecipeEarly(this);
    }

    @Override
    public String getFilePath() {
        return "";
    }

    @Override
    public ResourceLocation getRecipeRegistryName() {
        return name;
    }

    @Override
    public ResourceLocation getAssociatedMachineName() {
        return machineName;
    }

    @Override
    public ResourceLocation getParentMachineName() {
        return machineName;
    }

    //----------------------------------------------------------------------------------------------
    // lingering stats
    //----------------------------------------------------------------------------------------------

    @Override
    public int getTotalProcessingTickTime() {
        return tickTime;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean voidPerTickFailure() {
        return doesVoidPerTick;
    }

    @Override
    public List<ComponentRequirement<?, ?>> getComponents() {
        return components;
    }

    @Override
    public Map<Class<?>, List<IEventHandler<RecipeEvent>>> getRecipeEventHandlers() {
        return recipeEventHandlers;
    }

    @Override
    public List<String> getTooltipList() {
        return toolTipList;
    }

    @Override
    public boolean isParallelized() {
        return parallelized;
    }

    @ZenMethod
    public RecipePrimer setParallelized(boolean isParallelized) {
        this.parallelized = isParallelized;
        return this;
    }

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * 设置此配方在工厂中同时运行的数量是否不超过指定数值。
     */
    @ZenMethod
    public RecipePrimer setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    /**
     * 设置此配方只能被指定的核心线程执行。
     *
     * @param name 线程名
     */
    @ZenMethod
    public RecipePrimer setThreadName(String name) {
        this.threadName = name == null ? "" : name;
        return this;
    }

    @Override
    public void loadNeedAfterInitActions() {
        for (Action needAfterInitAction : needAfterInitActions) {
            needAfterInitAction.doAction();
        }
    }

    @Override
    public boolean getLoadJEI() {
        return loadJEI;
    }

    /**
     * <p>设置为false时不再为JEI注册对应配方</p>
     * <p>默认情况在为true</p>
     */
    @ZenMethod
    public RecipePrimer setLoadJEI(boolean load) {
        this.loadJEI = load;
        return this;
    }
}
