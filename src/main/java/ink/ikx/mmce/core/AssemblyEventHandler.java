package ink.ikx.mmce.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.mmce.compat.BlockPos;
import github.kasuminova.mmce.compat.EnumFacing;
import github.kasuminova.mmce.compat.ItemStacks;
import github.kasuminova.mmce.compat.RegistryNames;
import github.kasuminova.mmce.compat.Worlds;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.lib.ItemsMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import ink.ikx.mmce.common.assembly.MachineAssembly;
import ink.ikx.mmce.common.assembly.MachineAssemblyManager;
import ink.ikx.mmce.common.utils.StructureIngredient;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 右键控制器：手持蓝图时把蓝图放入控制器；手持组装物品（默认木棍）时开始自动组装。
 * <p>
 * 1.7.10 移植：1.12.2 由客户端发 PktAutoAssemblyRequest 携带“当前世界投影的动态结构尺寸”；1.7.10 的
 * PlayerInteractEvent 在服务端同样触发，这里直接在服务端处理并取消事件（客户端不取消，否则请求不会发到服务端）。
 * 动态结构尺寸改为由客户端在开始 / 清除世界投影时用 {@link hellfirepvp.modularmachinery.common.network.PktAssemblyPatternSize}
 * 同步过来：投影期间对同一台机器组装时使用投影的尺寸，否则使用各自的最小尺寸。
 * 需在 Forge 与 FML 两条总线上注册（交互事件 / 玩家 tick 与登出事件）。
 */
public class AssemblyEventHandler {

    public static final AssemblyEventHandler INSTANCE = new AssemblyEventHandler();

    /** 防止一次点击被重复处理，以及过快的连续点击。 */
    private static final Cache<EntityPlayer, Boolean> RECENT_REQUEST = CacheBuilder.newBuilder()
                                                                                   .expireAfterWrite(1, TimeUnit.SECONDS)
                                                                                   .weakKeys()
                                                                                   .build();

    /** 玩家当前世界投影的机器与动态结构尺寸（键为玩家 UUID）。 */
    private static final Map<UUID, ProjectionPatternSize> PROJECTION_PATTERN_SIZES = new ConcurrentHashMap<>();

    /** 记录玩家当前投影的机器与动态结构尺寸；machine 为空表示投影已清除。 */
    public static void setProjectionPatternSize(EntityPlayer player, String machine, int size) {
        if (machine == null || machine.isEmpty()) {
            PROJECTION_PATTERN_SIZES.remove(player.getUniqueID());
        } else {
            PROJECTION_PATTERN_SIZES.put(player.getUniqueID(), new ProjectionPatternSize(machine, size));
        }
    }

    /** 玩家正在投影该机器时返回投影的动态结构尺寸，否则返回 0（即各动态结构的最小尺寸）。 */
    public static int getProjectionPatternSize(EntityPlayer player, DynamicMachine machine) {
        ProjectionPatternSize projection = PROJECTION_PATTERN_SIZES.get(player.getUniqueID());
        if (projection == null || machine == null || !projection.machine.equals(machine.getRegistryName().toString())) {
            return 0;
        }
        return projection.size;
    }

    private static Item assemblyItem() {
        Item item = RegistryNames.findItem(AssemblyConfig.itemName);
        return item == null ? Items.stick : item;
    }

    private static void assemblyBefore(DynamicMachine machine, EntityPlayer player, World world, BlockPos pos, int dynamicPatternSize) {
        if (machine == null) {
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.no_machine"));
            return;
        }
        if (MachineAssemblyManager.checkMachineExist(pos)) {
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.already_assembly"));
            return;
        }
        EnumFacing controllerFacing = BlockController.getFacing(Worlds.getBlockState(world, pos));
        BlockArray machinePattern = new BlockArray(BlockArrayCache.getBlockArrayCache(machine.getPattern(), controllerFacing));
        Map<String, DynamicPattern> dynamicPatterns = machine.getDynamicPatterns();
        for (final DynamicPattern pattern : dynamicPatterns.values()) {
            dynamicPatternSize = Math.max(dynamicPatternSize, pattern.getMinSize());
        }
        for (final DynamicPattern pattern : dynamicPatterns.values()) {
            pattern.addPatternToBlockArray(
                machinePattern,
                Math.min(Math.max(pattern.getMinSize(), dynamicPatternSize), pattern.getMaxSize()),
                pattern.getFaces().iterator().next(),
                controllerFacing);
        }

        MachineAssembly assembly = new MachineAssembly(world, pos, player, StructureIngredient.of(world, pos, machinePattern));
        if (player.capabilities.isCreativeMode) {
            assembly.assemblyCreative();
            return;
        }
        if (MachineAssembly.checkAllItems(player, assembly.getIngredient().copy())) {
            assembly.buildIngredients(false);
            MachineAssemblyManager.addMachineAssembly(assembly);
            return;
        }
        if (!AssemblyConfig.needAllBlocks) {
            player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.partial_assembly"));
            assembly.buildIngredients(false);
            MachineAssemblyManager.addMachineAssembly(assembly);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        World world = event.world;
        if (world.isRemote || player.isSneaking()) {
            return;
        }
        BlockPos pos = new BlockPos(event.x, event.y, event.z);
        if (!(Worlds.getTileEntity(world, pos) instanceof TileMultiblockMachineController ctrl)) {
            return;
        }
        ItemStack stack = player.getCurrentEquippedItem();
        if (ItemStacks.isEmpty(stack)) {
            return;
        }

        // 蓝图：放入控制器的蓝图槽
        if (stack.getItem() == ItemsMM.blueprint) {
            ItemStack slotted = ctrl.getInventory().getStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT);
            if (ItemStacks.isEmpty(slotted)) {
                ItemStack copy = ItemStacks.copyWithSize(stack, 1);
                if (!player.capabilities.isCreativeMode) {
                    stack.stackSize--;
                    if (stack.stackSize <= 0) {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    }
                }
                ctrl.getInventory().setStackInSlot(TileMultiblockMachineController.BLUEPRINT_SLOT, copy);
                player.inventoryContainer.detectAndSendChanges();
            }
            event.setCanceled(true);
            return;
        }

        // 自动组装
        if (stack.getItem() == assemblyItem() && stack.getItemDamage() == AssemblyConfig.itemMeta) {
            event.setCanceled(true);
            if (RECENT_REQUEST.getIfPresent(player) != null) {
                player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.too_quickly"));
                return;
            }
            RECENT_REQUEST.put(player, Boolean.TRUE);
            processAutoAssembly(player, world, pos, ctrl);
        }
    }

    private void processAutoAssembly(EntityPlayer player, World world, BlockPos pos, TileMultiblockMachineController ctrl) {
        DynamicMachine machine = ctrl.getBlueprintMachine();
        if (machine == null) {
            Block block = world.getBlock(pos.getX(), pos.getY(), pos.getZ());
            if (block instanceof BlockController controller) {
                machine = controller.getParentMachine();
            }
        }
        assemblyBefore(machine, player, world, pos, getProjectionPatternSize(player, machine));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = player.worldObj;
        if (event.phase == TickEvent.Phase.START || world.isRemote || world.getTotalWorldTime() % AssemblyConfig.tickBlock != 0) {
            return;
        }
        Collection<MachineAssembly> assemblies = MachineAssemblyManager.getMachineAssemblyListFromPlayer(player);
        if (assemblies == null || assemblies.isEmpty()) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            for (final MachineAssembly assembly : assemblies) {
                if (assembly.isControllerInvalid()) {
                    MachineAssemblyManager.removeMachineAssembly(assembly.getCtrlPos());
                    player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.cancelled"));
                    return;
                }
                assembly.assembly(true);
                if (assembly.isCompleted()) {
                    MachineAssemblyManager.removeMachineAssembly(assembly.getCtrlPos());
                    player.addChatMessage(new ChatComponentTranslation("message.assembly.tip.success"));
                }
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MachineAssemblyManager.removeMachineAssembly(event.player);
        PROJECTION_PATTERN_SIZES.remove(event.player.getUniqueID());
    }

    private static final class ProjectionPatternSize {
        final String machine;
        final int    size;

        ProjectionPatternSize(String machine, int size) {
            this.machine = machine;
            this.size = size;
        }
    }
}
