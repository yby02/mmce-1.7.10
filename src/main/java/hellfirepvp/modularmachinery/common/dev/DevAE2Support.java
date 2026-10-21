package hellfirepvp.modularmachinery.common.dev;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IBlocks;
import appeng.api.definitions.IItems;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import com.glodblock.github.loader.ItemAndBlockHolder;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import github.kasuminova.mmce.compat.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import java.util.concurrent.Future;

/**
 * 仅开发环境：AE2 自测（DevSelfTest 的 ae2 阶段）中直接调用 AE2 / AE2FC 的部分。只在装有 AE2 时由 DevSelfTest 调用。
 * 测试网络为无控制器的临时网络（最多 8 个占用频道的设备）：创造能源元件、ME 驱动器（64k 物品元件 + AE2FC 64k 流体元件）、
 * 1k 合成存储器（单方块合成 CPU），与机器结构中的 5 个 ME 部件相邻连通。
 */
final class DevAE2Support {

    private DevAE2Support() {
    }

    private static void place(World world, BlockPos pos, ItemStack blockStack) {
        world.setBlock(pos.getX(), pos.getY(), pos.getZ(), Block.getBlockFromItem(blockStack.getItem()), blockStack.getItemDamage(), 3);
    }

    static void placeInfrastructure(World world, BlockPos energy, BlockPos drive, BlockPos cpu) {
        IBlocks blocks = AEApi.instance().definitions().blocks();
        IItems items = AEApi.instance().definitions().items();
        place(world, energy, blocks.energyCellCreative().maybeStack(1).get());
        place(world, drive, blocks.drive().maybeStack(1).get());
        TileEntity te = world.getTileEntity(drive.getX(), drive.getY(), drive.getZ());
        IInventory inv = (IInventory) te;
        inv.setInventorySlotContents(0, items.cell64k().maybeStack(1).get());
        inv.setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL64K));
        place(world, cpu, blocks.craftingStorage1k().maybeStack(1).get());
    }

    static boolean isActive(TileEntity te) {
        return te instanceof MEMachineComponent c && c.getProxy().isActive() && c.getProxy().isPowered();
    }

    private static MEMachineComponent node(TileEntity te) {
        return (MEMachineComponent) te;
    }

    static long itemAmount(TileEntity anyPart, ItemStack stack) throws GridAccessException {
        IMEMonitor<IAEItemStack> inv = node(anyPart).getProxy().getStorage().getItemInventory();
        IAEItemStack found = inv.getStorageList().findPrecise(AEApi.instance().storage().createItemStack(stack));
        return found == null ? 0 : found.getStackSize();
    }

    static long fluidAmount(TileEntity anyPart, FluidStack stack) throws GridAccessException {
        IMEMonitor<IAEFluidStack> inv = node(anyPart).getProxy().getStorage().getFluidInventory();
        IAEFluidStack found = inv.getStorageList().findPrecise(AEApi.instance().storage().createFluidStack(stack));
        return found == null ? 0 : found.getStackSize();
    }

    /** 放进 ME 网络，返回放不下的数量。 */
    static long injectItem(TileEntity anyPart, ItemStack stack, long amount) throws GridAccessException {
        IAEItemStack ae = AEApi.instance().storage().createItemStack(stack).setStackSize(amount);
        IAEItemStack left = node(anyPart).getProxy().getStorage().getItemInventory().injectItems(ae, Actionable.MODULATE, new MachineSource(node(anyPart)));
        return left == null ? 0 : left.getStackSize();
    }

    static long injectFluid(TileEntity anyPart, FluidStack stack) throws GridAccessException {
        IAEFluidStack ae = AEApi.instance().storage().createFluidStack(stack);
        IAEFluidStack left = node(anyPart).getProxy().getStorage().getFluidInventory().injectItems(ae, Actionable.MODULATE, new MachineSource(node(anyPart)));
        return left == null ? 0 : left.getStackSize();
    }

    static ItemStack capacityCard() {
        return AEApi.instance().definitions().materials().cardCapacity().maybeStack(1).get();
    }

    static ItemStack memoryCard() {
        return AEApi.instance().definitions().items().memoryCard().maybeStack(1).get();
    }

    /** AE2 rv3 的“终极样板”（输入 / 输出可以是物品或流体，NBT 格式同 UltimatePatternHelper）：2 金锭 + 1000 mB 熔岩 → 1 钻石。 */
    static ItemStack diamondPattern(ItemStack gold, ItemStack diamond, FluidStack lava) {
        ItemStack pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList();
        in.appendTag(Platform.writeStackNBT(AEApi.instance().storage().createItemStack(gold), new NBTTagCompound(), true));
        in.appendTag(Platform.writeStackNBT(AEApi.instance().storage().createFluidStack(lava), new NBTTagCompound(), true));
        NBTTagList out = new NBTTagList();
        out.appendTag(Platform.writeStackNBT(AEApi.instance().storage().createItemStack(diamond), new NBTTagCompound(), true));
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("substitute", false);
        tag.setBoolean("beSubstitute", false);
        tag.setString("author", "mmce-selftest");
        pattern.setTagCompound(tag);
        return pattern;
    }

    /** 样板解析出的输入中是否含流体（确认终极样板确实带着流体原料）。 */
    static String describeInputs(ICraftingPatternDetails details) {
        StringBuilder sb = new StringBuilder();
        for (IAEStack<?> s : details.getAEInputs()) {
            if (s == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (s instanceof IAEFluidStack f) {
                sb.append("fluid ").append(f.getFluidStack().getFluid().getName()).append(" x").append(f.getStackSize());
            } else if (s instanceof IAEItemStack i) {
                sb.append("item ").append(i.getItemStack().getUnlocalizedName()).append(" x").append(i.getStackSize());
            }
        }
        return sb.toString();
    }

    static boolean hasFluidInput(ICraftingPatternDetails details) {
        for (IAEStack<?> s : details.getAEInputs()) {
            if (s instanceof IAEFluidStack) {
                return true;
            }
        }
        return false;
    }

    static boolean craftable(TileEntity anyPart, ItemStack output) throws GridAccessException {
        ICraftingGrid cg = node(anyPart).getProxy().getCrafting();
        return !cg.getCraftingFor(AEApi.instance().storage().createItemStack(output), null, 0, anyPart.getWorldObj()).isEmpty();
    }

    static int cpuCount(TileEntity anyPart) throws GridAccessException {
        return node(anyPart).getProxy().getCrafting().getCpus().size();
    }

    @SuppressWarnings("rawtypes")
    static Future<ICraftingJob> beginJob(TileEntity anyPart, ItemStack output) throws GridAccessException {
        IGrid grid = node(anyPart).getProxy().getGrid();
        return node(anyPart).getProxy().getCrafting().beginCraftingJob(anyPart.getWorldObj(), grid, new MachineSource(node(anyPart)),
            AEApi.instance().storage().createItemStack(output), null);
    }

    /** 提交合成任务（不指定请求者与 CPU，产物进入网络存储，同玩家在终端中下单）。返回说明文字，失败时以 FAIL 开头。 */
    @SuppressWarnings("rawtypes")
    static String submitJob(TileEntity anyPart, Future<ICraftingJob> future) throws Exception {
        ICraftingJob job = future.get();
        if (job.isSimulation()) {
            return "FAIL job is a simulation (missing ingredients), bytes=" + job.getByteTotal();
        }
        ICraftingGrid cg = node(anyPart).getProxy().getCrafting();
        cg.submitJob(job, null, null, false, new MachineSource(node(anyPart)));
        for (ICraftingCPU cpu : cg.getCpus()) {
            if (cpu.isBusy()) {
                return "submitted, cpu " + cpu.getName() + " busy, bytes=" + job.getByteTotal();
            }
        }
        return "FAIL no cpu busy after submit";
    }

    static boolean anyCpuBusy(TileEntity anyPart) throws GridAccessException {
        for (ICraftingCPU cpu : node(anyPart).getProxy().getCrafting().getCpus()) {
            if (cpu.isBusy()) {
                return true;
            }
        }
        return false;
    }

    /** 合成 CPU 推送样板时使用的表（MEInventoryCrafting），供直接调用样板供应器的 pushPattern。 */
    static MEInventoryCrafting table(IAEStack<?>... stacks) {
        MEInventoryCrafting table = new MEInventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < stacks.length; i++) {
            table.setInventorySlotContents(i, stacks[i]);
        }
        return table;
    }

    /** 直接调用样板供应器的 pushPattern（样板取 patternSlot 格），表中依次放入给定的 AE 物品 / 流体。 */
    static boolean push(MEPatternProvider provider, int patternSlot, Object... stacks) {
        IAEStack<?>[] arr = new IAEStack<?>[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            arr[i] = (IAEStack<?>) stacks[i];
        }
        return provider.pushPattern(provider.getPatternDetails(patternSlot), table(arr));
    }

    static IAEItemStack aeItem(ItemStack stack) {
        return AEApi.instance().storage().createItemStack(stack);
    }

    static IAEFluidStack aeFluid(FluidStack stack) {
        return AEApi.instance().storage().createFluidStack(stack);
    }

    /** AE2FC 的流体滴物品（旧式流体样板中代表流体的物品）。 */
    static ItemStack fluidDrop(FluidStack fluid) {
        return com.glodblock.github.common.item.ItemFluidDrop.newStack(fluid);
    }
}
