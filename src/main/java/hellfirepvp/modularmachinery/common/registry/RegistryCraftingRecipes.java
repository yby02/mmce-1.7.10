package hellfirepvp.modularmachinery.common.registry;

import cpw.mods.fml.common.registry.GameRegistry;
import hellfirepvp.modularmachinery.common.block.BlockCasing;
import hellfirepvp.modularmachinery.common.lib.BlocksMM;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * 1.7.10 移植：1.12.2 的 assets/modularmachinery/recipes/*.json（forge:ore_shaped）在 1.7.10 不会被加载，
 * 这里逐条用 {@link ShapedOreRecipe} 注册，图案、材料与产出数量与上游 JSON 一致。
 */
public final class RegistryCraftingRecipes {

    private RegistryCraftingRecipes() {
    }

    private static ItemStack casing(BlockCasing.CasingType type, int count) {
        return new ItemStack(BlocksMM.blockCasing, count, type.ordinal());
    }

    private static ItemStack stack(Block block, int meta) {
        return new ItemStack(block, 1, meta);
    }

    private static void shaped(ItemStack result, Object... recipe) {
        GameRegistry.addRecipe(new ShapedOreRecipe(result, recipe));
    }

    public static void register() {
        ItemStack plain = casing(BlockCasing.CasingType.PLAIN, 1);

        // modularium_ingot.json
        shaped(new ItemStack(ItemsMM.modularium, 5), "gig", "iri", "ror",
            'r', "dustRedstone", 'g', "ingotGold", 'i', "ingotIron", 'o', "dustGlowstone");
        // casing_plain.json
        shaped(casing(BlockCasing.CasingType.PLAIN, 2), " m ", "mrm", " m ",
            'r', "blockRedstone", 'm', ItemsMM.modularium);
        // casing_firebox.json
        shaped(casing(BlockCasing.CasingType.FIREBOX, 2), "rfr", "c c", "rir",
            'r', "dustRedstone", 'i', "ingotIron", 'c', plain, 'f', Items.flint);
        // casing_reinforced.json
        shaped(casing(BlockCasing.CasingType.REINFORCED, 4), "oco", "c c", "oco",
            'o', Blocks.obsidian, 'c', plain);
        // controller.json
        shaped(new ItemStack(BlocksMM.blockController), " d ", "rmr", "grg",
            'r', "blockRedstone", 'd', "gemDiamond", 'g', "ingotGold", 'm', plain);

        // energy_input_tiny.json / energy_input_small.json
        shaped(stack(BlocksMM.energyInputHatch, 0), " d ", "dmd", "ere",
            'r', "blockRedstone", 'e', Items.repeater, 'd', "dustRedstone", 'm', plain);
        shaped(stack(BlocksMM.energyInputHatch, 1), "ded", "rmr", "drd",
            'r', "blockRedstone", 'e', Items.repeater, 'd', "dustRedstone", 'm', stack(BlocksMM.energyInputHatch, 0));
        // energy_output_tiny.json / energy_output_small.json
        shaped(stack(BlocksMM.energyOutputHatch, 0), "ere", "dmd", " d ",
            'r', "blockRedstone", 'e', Items.repeater, 'd', "dustRedstone", 'm', plain);
        shaped(stack(BlocksMM.energyOutputHatch, 1), "drd", "rmr", "ded",
            'r', "blockRedstone", 'e', Items.repeater, 'd', "dustRedstone", 'm', stack(BlocksMM.energyOutputHatch, 0));

        // fluid_input_tiny.json / fluid_input_small.json
        shaped(stack(BlocksMM.fluidInputHatch, 0), "h", "m", "c",
            'c', Items.bucket, 'h', Blocks.hopper, 'm', plain);
        shaped(stack(BlocksMM.fluidInputHatch, 1), " h ", "mbm", "cmc",
            'c', Items.bucket, 'h', Blocks.hopper, 'm', ItemsMM.modularium, 'b', stack(BlocksMM.fluidInputHatch, 0));
        // fluid_output_tiny.json / fluid_output_small.json
        shaped(stack(BlocksMM.fluidOutputHatch, 0), "c", "m", "h",
            'c', Items.bucket, 'h', Blocks.hopper, 'm', plain);
        shaped(stack(BlocksMM.fluidOutputHatch, 1), "cmc", "mbm", " h ",
            'c', Items.bucket, 'h', Blocks.hopper, 'm', ItemsMM.modularium, 'b', stack(BlocksMM.fluidOutputHatch, 0));

        // item_input_tiny.json / item_input_small.json
        shaped(stack(BlocksMM.itemInputBus, 0), "h", "m", "c",
            'c', Blocks.chest, 'h', Blocks.hopper, 'm', plain);
        shaped(stack(BlocksMM.itemInputBus, 1), " h ", "mbm", "cmc",
            'c', Blocks.chest, 'h', Blocks.hopper, 'm', ItemsMM.modularium, 'b', stack(BlocksMM.itemInputBus, 0));
        // item_output_tiny.json / item_output_small.json
        shaped(stack(BlocksMM.itemOutputBus, 0), "c", "m", "h",
            'c', Blocks.chest, 'h', Blocks.hopper, 'm', plain);
        shaped(stack(BlocksMM.itemOutputBus, 1), "cmc", "mbm", " h ",
            'c', Blocks.chest, 'h', Blocks.hopper, 'm', ItemsMM.modularium, 'b', stack(BlocksMM.itemOutputBus, 0));
    }
}
