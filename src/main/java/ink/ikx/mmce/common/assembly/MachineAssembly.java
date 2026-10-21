package ink.ikx.mmce.common.assembly;

import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.IBlockState;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.Tuple;
import github.kasuminova.mmce.compat.Worlds;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.network.PktAssemblyReport;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import ink.ikx.mmce.common.utils.StructureIngredient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 机械自动组装：按结构定义逐个放置方块，消耗玩家背包中的物品（流体方块消耗流体容器中的流体）。
 * <p>
 * 1.7.10 移植：
 * - 背包是 ItemStack[]，数量减到 0 的格子置为 null（1.7.10 不会自动清除空栈）；
 * - 流体来源为 IFluidContainerItem（与上游一样不支持桶）；
 * - 放置后触发 Forge 的 BlockEvent.PlaceEvent，被取消时用快照恢复并退还物品；
 * - 写入方块 NBT 时补上目标坐标（1.7.10 的 TileEntity#readFromNBT 会读取 x/y/z）。
 */
public class MachineAssembly {
    private final World               world;
    private final BlockPos            ctrlPos;
    private final EntityPlayer        player;
    private       StructureIngredient ingredient;

    public MachineAssembly(final World world, final BlockPos ctrlPos, final EntityPlayer player, final StructureIngredient ingredient) {
        this.world = world;
        this.ctrlPos = ctrlPos;
        this.player = player;
        this.ingredient = ingredient;
    }

    // ---------------- 背包工具 ----------------

    private static ItemStack[] copyInventory(EntityPlayer player) {
        ItemStack[] main = player.inventory.mainInventory;
        ItemStack[] copy = new ItemStack[main.length];
        for (int i = 0; i < main.length; i++) {
            copy[i] = ItemStacks.copy(main[i]);
        }
        return copy;
    }

    public static boolean consumeInventoryItem(final ItemStack required, final ItemStack[] inventory) {
        for (int i = 0; i < inventory.length; i++) {
            ItemStack invStack = inventory[i];
            if (!ItemStacks.isEmpty(invStack) && ItemUtils.matchStacks(invStack, required) && invStack.stackSize >= ItemStacks.getCount(required)) {
                invStack.stackSize -= ItemStacks.getCount(required);
                if (invStack.stackSize <= 0) {
                    inventory[i] = null;
                }
                return true;
            }
        }
        return false;
    }

    /** 背包中可提供流体的容器（IFluidContainerItem，单个物品）。 */
    private static List<ItemStack> fluidContainers(final ItemStack[] inventory) {
        List<ItemStack> out = new ArrayList<>();
        for (final ItemStack stack : inventory) {
            if (!ItemStacks.isEmpty(stack) && stack.getItem() instanceof IFluidContainerItem && stack.stackSize == 1) {
                out.add(stack);
            }
        }
        return out;
    }

    public static boolean consumeInventoryFluid(final FluidStack required, final List<ItemStack> containers) {
        for (final ItemStack stack : containers) {
            IFluidContainerItem item = (IFluidContainerItem) stack.getItem();
            FluidStack contained = item.getFluid(stack);
            if (contained == null || !contained.isFluidEqual(required) || contained.amount < required.amount) {
                continue;
            }
            item.drain(stack, required.amount, true);
            return true;
        }
        return false;
    }

    public static boolean fillInventoryFluid(final FluidStack required, final List<ItemStack> containers) {
        for (final ItemStack stack : containers) {
            IFluidContainerItem item = (IFluidContainerItem) stack.getItem();
            if (item.fill(stack, required.copy(), false) >= required.amount) {
                item.fill(stack, required.copy(), true);
                return true;
            }
        }
        return false;
    }

    // ---------------- 材料匹配 ----------------

    public static List<StructureIngredient.FluidIngredient> buildFluidIngredients(final ItemStack[] inventory,
                                                                                  final List<StructureIngredient.FluidIngredient> fluidIngredients) {
        List<StructureIngredient.FluidIngredient> result = new ArrayList<>();
        List<ItemStack> containers = fluidContainers(inventory);

        Iterator<StructureIngredient.FluidIngredient> iter = fluidIngredients.iterator();
        fluidIngredient:
        while (iter.hasNext()) {
            final StructureIngredient.FluidIngredient fluidIngredient = iter.next();
            for (final Tuple<FluidStack, IBlockState> tuple : fluidIngredient.ingredientList()) {
                if (!consumeInventoryFluid(tuple.getFirst(), containers)) {
                    continue;
                }
                result.add(new StructureIngredient.FluidIngredient(fluidIngredient.pos(), Collections.singletonList(tuple)));
                iter.remove();
                continue fluidIngredient;
            }
        }
        return result;
    }

    public static List<StructureIngredient.ItemIngredient> buildItemIngredients(final ItemStack[] inventory,
                                                                                final List<StructureIngredient.ItemIngredient> itemIngredients) {
        List<StructureIngredient.ItemIngredient> result = new ArrayList<>();
        Iterator<StructureIngredient.ItemIngredient> iter = itemIngredients.iterator();
        itemIngredient:
        while (iter.hasNext()) {
            StructureIngredient.ItemIngredient ingredient = iter.next();
            for (final Tuple<ItemStack, IBlockState> tuple : ingredient.ingredientList()) {
                if (!consumeInventoryItem(tuple.getFirst(), inventory)) {
                    continue;
                }
                result.add(new StructureIngredient.ItemIngredient(ingredient.pos(), Collections.singletonList(tuple), ingredient.nbt()));
                iter.remove();
                continue itemIngredient;
            }
        }
        return result;
    }

    public static void searchAndRemoveContainItem(final ItemStack[] inventory,
                                                  final List<StructureIngredient.ItemIngredient> itemIngredients) {
        Iterator<StructureIngredient.ItemIngredient> iter = itemIngredients.iterator();
        itemIngredient:
        while (iter.hasNext()) {
            final StructureIngredient.ItemIngredient ingredient = iter.next();
            for (final Tuple<ItemStack, IBlockState> tuple : ingredient.ingredientList()) {
                ItemStack required = tuple.getFirst();
                if (ItemStacks.isEmpty(required) || consumeInventoryItem(required, inventory)) {
                    iter.remove();
                    continue itemIngredient;
                }
            }
        }
    }

    public static void searchAndRemoveContainFluid(final ItemStack[] inventory,
                                                   final List<StructureIngredient.FluidIngredient> fluidIngredients) {
        List<ItemStack> containers = fluidContainers(inventory);
        Iterator<StructureIngredient.FluidIngredient> iter = fluidIngredients.iterator();
        fluidIngredient:
        while (iter.hasNext()) {
            final StructureIngredient.FluidIngredient fluidIngredient = iter.next();
            for (final Tuple<FluidStack, IBlockState> tuple : fluidIngredient.ingredientList()) {
                if (consumeInventoryFluid(tuple.getFirst(), containers)) {
                    iter.remove();
                    continue fluidIngredient;
                }
            }
        }
    }

    /** 背包中是否备齐全部材料；缺少时把缺少的材料清单发给玩家。 */
    public static boolean checkAllItems(EntityPlayer player, StructureIngredient ingredient) {
        ItemStack[] inventory = copyInventory(player);
        List<StructureIngredient.ItemIngredient> itemIngredientList = ingredient.itemIngredient();
        List<StructureIngredient.FluidIngredient> fluidIngredientList = ingredient.fluidIngredient();
        searchAndRemoveContainItem(inventory, itemIngredientList);
        searchAndRemoveContainFluid(inventory, fluidIngredientList);

        if (itemIngredientList.isEmpty() && fluidIngredientList.isEmpty()) {
            return true;
        }
        sendReport(player, getItemStackIngList(itemIngredientList), getFluidStackIngList(fluidIngredientList));
        return false;
    }

    private static void sendReport(EntityPlayer player, List<List<ItemStack>> items, List<List<FluidStack>> fluids) {
        if (player instanceof EntityPlayerMP) {
            ModularMachinery.NET_CHANNEL.sendTo(new PktAssemblyReport(items, fluids), (EntityPlayerMP) player);
        }
    }

    private static List<List<FluidStack>> getFluidStackIngList(final List<StructureIngredient.FluidIngredient> fluidIngredientList) {
        List<List<FluidStack>> out = new ArrayList<>();
        fluidIng:
        for (final StructureIngredient.FluidIngredient ingredient : fluidIngredientList) {
            if (ingredient.ingredientList().isEmpty()) {
                continue;
            }
            List<FluidStack> stacks = new ArrayList<>();
            for (Tuple<FluidStack, IBlockState> tuple : ingredient.ingredientList()) {
                stacks.add(tuple.getFirst().copy());
            }
            if (stacks.size() == 1) {
                FluidStack ing = stacks.get(0);
                for (final List<FluidStack> existing : out) {
                    if (existing.size() == 1 && ing.isFluidEqual(existing.get(0))) {
                        existing.get(0).amount += 1000;
                        continue fluidIng;
                    }
                }
            }
            out.add(stacks);
        }
        return out;
    }

    private static List<List<ItemStack>> getItemStackIngList(final List<StructureIngredient.ItemIngredient> itemIngredientList) {
        List<List<ItemStack>> out = new ArrayList<>();
        itemIng:
        for (final StructureIngredient.ItemIngredient itemIng : itemIngredientList) {
            if (itemIng.ingredientList().isEmpty()) {
                continue;
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (Tuple<ItemStack, IBlockState> tuple : itemIng.ingredientList()) {
                stacks.add(ItemStacks.copy(tuple.getFirst()));
            }
            if (stacks.size() == 1) {
                ItemStack ing = stacks.get(0);
                for (final List<ItemStack> existing : out) {
                    if (existing.size() == 1 && ItemUtils.matchStacks(ing, existing.get(0))) {
                        ItemStacks.grow(existing.get(0), 1);
                        continue itemIng;
                    }
                }
            }
            List<ItemStack> filtered = new ArrayList<>();
            filter:
            for (final ItemStack stack : stacks) {
                for (final ItemStack f : filtered) {
                    if (ItemUtils.matchStacks(stack, f)) {
                        continue filter;
                    }
                }
                filtered.add(stack);
            }
            out.add(filtered);
        }
        return out;
    }

    // ---------------- 放置 ----------------

    public static boolean replaceCheck(final BlockPos realPos, final World world, final EntityPlayer player) {
        String posToString = MiscUtils.posToString(realPos);
        if (realPos.getY() < 0 || realPos.getY() >= world.getHeight()) {
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.too_high", posToString));
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.skipped", posToString));
            return false;
        }
        Block block = world.getBlock(realPos.getX(), realPos.getY(), realPos.getZ());
        if (Worlds.isAirBlock(world, realPos) || block instanceof IPlantable || block instanceof BlockLiquid || block instanceof IFluidBlock) {
            return true;
        }
        player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.cannot_replace", posToString));
        player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.skipped", posToString));
        return false;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getCtrlPos() {
        return ctrlPos;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public StructureIngredient getIngredient() {
        return ingredient;
    }

    /** consumeInventory 为 false 时只在背包副本上预演，用来确定每个位置最终使用哪种方块。 */
    public void buildIngredients(boolean consumeInventory) {
        ItemStack[] inventory = consumeInventory ? player.inventory.mainInventory : copyInventory(player);
        List<StructureIngredient.ItemIngredient> newItems = buildItemIngredients(inventory, ingredient.itemIngredient());
        List<StructureIngredient.FluidIngredient> newFluids = buildFluidIngredients(inventory, ingredient.fluidIngredient());
        ingredient = new StructureIngredient(newItems, newFluids);
    }

    public boolean isCompleted() {
        return ingredient.itemIngredient().isEmpty() && ingredient.fluidIngredient().isEmpty();
    }

    public boolean isControllerInvalid() {
        TileEntity te = Worlds.getTileEntity(world, ctrlPos);
        return !(te instanceof TileMultiblockMachineController);
    }

    public void assembly(boolean consumeInventory) {
        List<StructureIngredient.ItemIngredient> itemIngredient = ingredient.itemIngredient();
        List<StructureIngredient.FluidIngredient> fluidIngredient = ingredient.fluidIngredient();
        if (!itemIngredient.isEmpty()) {
            assemblyItemBlocks(consumeInventory, itemIngredient);
        } else if (!fluidIngredient.isEmpty()) {
            assemblyFluidBlocks(consumeInventory, fluidIngredient);
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public void assemblyCreative() {
        for (final StructureIngredient.ItemIngredient itemIngredient : ingredient.itemIngredient()) {
            List<Tuple<ItemStack, IBlockState>> list = itemIngredient.ingredientList();
            if (!list.isEmpty()) {
                placeWithNbt(ctrlPos.add(itemIngredient.pos()), list.get(0).getSecond(), itemIngredient.nbt());
            }
        }
        for (final StructureIngredient.FluidIngredient fluidIngredient : ingredient.fluidIngredient()) {
            List<Tuple<FluidStack, IBlockState>> list = fluidIngredient.ingredientList();
            if (!list.isEmpty()) {
                Worlds.setBlockState(world, ctrlPos.add(fluidIngredient.pos()), list.get(0).getSecond());
            }
        }
        world.playSoundEffect(ctrlPos.getX() + 0.5, ctrlPos.getY() + 0.5, ctrlPos.getZ() + 0.5, "dig.stone", 1.0F, 1.0F);
        player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.success"));
    }

    /** 放置方块并触发 Forge 放置事件；被取消时恢复原方块并返回 false。 */
    private boolean placeAndFireEvent(BlockPos realPos, IBlockState state) {
        BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(world, realPos.getX(), realPos.getY(), realPos.getZ());
        Worlds.setBlockState(world, realPos, state);
        BlockEvent.PlaceEvent event = ForgeEventFactory.onPlayerBlockPlace(player, snapshot, ForgeDirection.UP);
        if (event.isCanceled()) {
            snapshot.restore(true, false);
            return false;
        }
        return true;
    }

    private void placeWithNbt(BlockPos realPos, IBlockState state, NBTTagCompound nbt) {
        Worlds.setBlockState(world, realPos, state);
        applyNbt(realPos, state, nbt);
    }

    private void applyNbt(BlockPos realPos, IBlockState state, NBTTagCompound nbt) {
        TileEntity te = Worlds.getTileEntity(world, realPos);
        if (te == null || nbt == null) {
            return;
        }
        NBTTagCompound tag = (NBTTagCompound) nbt.copy();
        tag.setInteger("x", realPos.getX());
        tag.setInteger("y", realPos.getY());
        tag.setInteger("z", realPos.getZ());
        try {
            te.readFromNBT(tag);
        } catch (Exception e) {
            ModularMachinery.log.warn("Failed to apply NBT to TileEntity!", e);
            world.removeTileEntity(realPos.getX(), realPos.getY(), realPos.getZ());
            TileEntity fresh = state.getBlock().createTileEntity(world, state.getMetadata());
            if (fresh != null) {
                world.setTileEntity(realPos.getX(), realPos.getY(), realPos.getZ(), fresh);
            }
        }
    }

    private void assemblyItemBlocks(final boolean consumeInventory, final List<StructureIngredient.ItemIngredient> itemIngredient) {
        Iterator<StructureIngredient.ItemIngredient> iterator = itemIngredient.iterator();
        StructureIngredient.ItemIngredient ingredient = iterator.next();
        BlockPos realPos = ctrlPos.add(ingredient.pos());
        iterator.remove();

        if (!replaceCheck(realPos, world, player)) {
            return;
        }
        Tuple<ItemStack, IBlockState> tuple = ingredient.ingredientList().get(0);
        ItemStack required = tuple.getFirst();
        IBlockState state = tuple.getSecond();

        if (consumeInventory && !consumeInventoryItem(required, player.inventory.mainInventory)) {
            String posToString = MiscUtils.posToString(realPos);
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.missing", posToString));
            List<List<ItemStack>> missing = new ArrayList<>();
            missing.add(Collections.singletonList(required));
            sendReport(player, missing, new ArrayList<>());
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.skipped", posToString));
            return;
        }

        if (!placeAndFireEvent(realPos, state)) {
            if (consumeInventory) {
                player.inventory.addItemStackToInventory(ItemStacks.copy(required));
            }
            return;
        }
        world.playSoundEffect(realPos.getX() + 0.5, realPos.getY() + 0.5, realPos.getZ() + 0.5,
            state.getBlock().stepSound.func_150496_b(), 1.0F, 1.0F);
        applyNbt(realPos, state, ingredient.nbt());
    }

    private void assemblyFluidBlocks(final boolean consumeInventory, final List<StructureIngredient.FluidIngredient> fluidIngredient) {
        Iterator<StructureIngredient.FluidIngredient> iterator = fluidIngredient.iterator();
        StructureIngredient.FluidIngredient ingredient = iterator.next();
        BlockPos realPos = ctrlPos.add(ingredient.pos());
        iterator.remove();

        if (!replaceCheck(realPos, world, player)) {
            return;
        }
        Tuple<FluidStack, IBlockState> tuple = ingredient.ingredientList().get(0);
        FluidStack required = tuple.getFirst();
        IBlockState state = tuple.getSecond();
        List<ItemStack> containers = fluidContainers(player.inventory.mainInventory);

        if (consumeInventory && !consumeInventoryFluid(required, containers)) {
            String posToString = MiscUtils.posToString(realPos);
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.missing", posToString));
            List<List<FluidStack>> missing = new ArrayList<>();
            missing.add(Collections.singletonList(required));
            sendReport(player, new ArrayList<>(), missing);
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.skipped", posToString));
            return;
        }

        if (!placeAndFireEvent(realPos, state)) {
            if (consumeInventory) {
                fillInventoryFluid(required, containers);
            }
            return;
        }
        world.playSoundEffect(realPos.getX() + 0.5, realPos.getY() + 0.5, realPos.getZ() + 0.5, "random.splash", 0.5F, 1.0F);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final MachineAssembly another)) {
            return false;
        }
        return ctrlPos.equals(another.ctrlPos);
    }

    @Override
    public int hashCode() {
        return ctrlPos != null ? ctrlPos.hashCode() : 0;
    }
}
