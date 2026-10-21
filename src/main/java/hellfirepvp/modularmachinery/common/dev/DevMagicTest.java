package hellfirepvp.modularmachinery.common.dev;

import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import kport.modularmagic.common.tile.TileAspectProvider;
import kport.modularmagic.common.tile.TileLifeEssenceProvider;
import kport.modularmagic.common.tile.TileManaProvider;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import vazkii.botania.api.mana.IManaBlock;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.wand.IWandBindable;

/**
 * 仅开发环境：DevSelfTest 的 magic 阶段（WITH_MAGIC=1：神秘时代 4、植物魔法、血魔法 1）。
 * <p>
 * 测试机 magic_test：控制器两侧依次为源质 / 魔力 / 生命精华的输入仓与输出仓。
 * <ul>
 *   <li>源质：装 64 火（ignis）的源质罐 → 源质管道 → 源质输入仓；源质输出仓 → 源质管道 → 空源质罐。</li>
 *   <li>魔力：创造魔力池旁的魔力发射器对准魔力输入仓发射；魔力输出仓旁的魔力发射器从中抽取魔力，射向另一个魔力池。</li>
 *   <li>生命精华：两个仓各放一个绑定到不同主人的魔术师血之宝珠；输入仓所连网络预置 50000 LP，输出仓所连网络从 0 开始。</li>
 *   <li>另有 testdata/scripts_magic/mmce_magic_test.zs 中用 addManaInput / addAspectInput / addLifeEssenceInput 定义的配方。</li>
 * </ul>
 */
final class DevMagicTest {

    private static final String MAGIC_TEST = "magic_test";
    private static final String OWNER_IN   = "mmce_selftest_in";
    private static final String OWNER_OUT  = "mmce_selftest_out";

    private final DevSelfTest t;
    private BlockPos ctrl;

    private DevMagicTest(DevSelfTest t) {
        this.t = t;
    }

    static void build(DevSelfTest t) {
        new DevMagicTest(t).steps();
    }

    private void info(String line) {
        CommandMMCETest.report(t.sender(), "INFO " + line);
    }

    private TileEntity at(int dx, int dy, int dz) {
        return DevSelfTest.tileAt(t.world(), ctrl.add(dx, dy, dz));
    }

    private void set(int dx, int dy, int dz, Block block, int meta) {
        BlockPos p = ctrl.add(dx, dy, dz);
        t.world().setBlock(p.getX(), p.getY(), p.getZ(), block, meta, 3);
    }

    private static Block block(String modId, String name) {
        Block b = GameRegistry.findBlock(modId, name);
        if (b == null) {
            throw new IllegalStateException(modId + ":" + name + " not found");
        }
        return b;
    }

    private int countOut(Item item, int meta) {
        int n = 0;
        if (at(0, 1, 0) instanceof TileItemBus bus) {
            IOInventory inv = bus.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null && s.getItem() == item && (meta < 0 || s.getItemDamage() == meta)) {
                    n += s.stackSize;
                }
            }
        }
        return n;
    }

    /** 源质罐 (dx, 0, 0) 中某种源质的量。TC4 的 TileJarFillable#containerContains 恒返回 0，只能读 getAspects()。 */
    private int jarAmount(int dx, Aspect aspect) {
        return ((IAspectContainer) at(dx, 0, 0)).getAspects().getAmount(aspect);
    }

    private int countOut(Block block) {
        return countOut(Item.getItemFromBlock(block), -1);
    }

    /**
     * 绑定一个魔力发射器（植物魔法法杖的绑定逻辑，API 接口 IWandBindable#bindTo）：转向目标方块。
     * 不用反射调用 TileSpreader 的方法——Class#getMethod 会解析它的全部公开方法签名，其中有仅客户端存在的类型，专用服务器上会抛 NoClassDefFoundError。
     */
    private void aimSpreader(BlockPos spreader, BlockPos target) {
        TileEntity te = DevSelfTest.tileAt(t.world(), spreader);
        boolean bound = ((IWandBindable) te).bindTo(FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) t.world()), new ItemStack(Items.stick),
            target.getX(), target.getY(), target.getZ(), 1);
        // ITileBound#getBinding 在 TileSpreader 里是仅客户端方法（服务器上被剥离），这里只记录 bindTo 的结果。
        info("spreader at " + spreader + " bound to " + target + ": " + bound);
    }

    private void steps() {
        t.step("magic: build magic_test with Thaumcraft / Botania / Blood Magic devices", 20, () -> {
            DynamicMachine machine = DevSelfTest.machine(MAGIC_TEST);
            if (machine == null) {
                throw new IllegalStateException("testdata machine magic_test not loaded (needs Thaumcraft, Botania and Blood Magic)");
            }
            World world = t.world();
            ChunkCoordinates spawn = world.getSpawnPoint();
            ctrl = DevSelfTest.groundAbove(world, spawn.posX + 110, spawn.posZ + 40);
            CommandMMCETest.buildAt(t.sender(), world, machine, ctrl);
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -5; dz <= -1; dz++) {
                    set(dx, -1, dz, Blocks.stone, 0);
                    for (int dy = 0; dy <= 2; dy++) {
                        set(dx, dy, dz, Blocks.air, 0);
                    }
                }
                if (Math.abs(dx) >= 4) {
                    set(dx, -1, 0, Blocks.stone, 0);
                    set(dx, 0, 0, Blocks.air, 0);
                }
                if (Math.abs(dx) >= 3) {
                    set(dx, 1, 0, Blocks.air, 0);
                }
            }

            // 源质：源质罐只在顶面与管道相连（TileJarFillable#isConnectable 只认 UP），管道从罐顶接到源质仓顶面
            Block jar = block("Thaumcraft", "blockJar");
            Block tube = block("Thaumcraft", "blockTube");
            set(-5, 0, 0, jar, 0);
            set(5, 0, 0, jar, 0);
            for (int dx : new int[]{-5, -4, -3, 3, 4, 5}) {
                set(dx, 1, 0, tube, 0);
            }
            int left = ((IAspectContainer) at(-5, 0, 0)).addToContainer(Aspect.FIRE, 64);
            info("source jar " + at(-5, 0, 0).getClass().getSimpleName() + " filled with 64 ignis (left " + left + "), tube " + at(-4, 1, 0).getClass().getSimpleName());

            // 魔力
            Block spreader = block("Botania", "spreader");
            Block pool = block("Botania", "pool");
            set(-2, 0, -3, spreader, 0);
            set(-2, 0, -4, pool, 1);   // 创造魔力池：发射器从相邻的魔力池抽取魔力
            set(2, 0, -1, spreader, 0);
            set(2, 0, -4, pool, 0);
            aimSpreader(ctrl.add(-2, 0, -3), ctrl.add(-2, 0, 0));
            aimSpreader(ctrl.add(2, 0, -1), ctrl.add(2, 0, -4));

            // 生命精华
            Item orb = GameRegistry.findItem("AWWayofTime", "magicianBloodOrb");
            ItemStack orbIn = new ItemStack(orb);
            SoulNetworkHandler.checkAndSetItemOwner(orbIn, OWNER_IN);
            ItemStack orbOut = new ItemStack(orb);
            SoulNetworkHandler.checkAndSetItemOwner(orbOut, OWNER_OUT);
            SoulNetworkHandler.setCurrentEssence(OWNER_IN, 50000);
            SoulNetworkHandler.setCurrentEssence(OWNER_OUT, 0);
            ((TileLifeEssenceProvider) at(-1, 0, 0)).getInventory().setStackInSlot(0, orbIn);
            ((TileLifeEssenceProvider) at(1, 0, 0)).getInventory().setStackInSlot(0, orbOut);
            info("blood orbs: in owner=" + ((TileLifeEssenceProvider) at(-1, 0, 0)).getOwner() + " cache cap=" + ((TileLifeEssenceProvider) at(-1, 0, 0)).getCacheCapacity()
                + ", out owner=" + ((TileLifeEssenceProvider) at(1, 0, 0)).getOwner() + "; network LP in=" + SoulNetworkHandler.getCurrentEssence(OWNER_IN));

            // 原料
            IOInventory bus = ((TileItemBus) at(0, 0, 1)).getInventory();
            ItemStack[] inputs = {new ItemStack(Blocks.cobblestone, 4), new ItemStack(Blocks.dirt, 4), new ItemStack(Blocks.sand, 4),
                new ItemStack(Blocks.gravel, 4), new ItemStack(Items.clay_ball, 4), new ItemStack(Items.flint, 4),
                new ItemStack(Items.wheat_seeds, 2), new ItemStack(Items.feather, 2), new ItemStack(Items.bone, 2)};
            for (int i = 0; i < inputs.length && i < bus.getSlots(); i++) {
                bus.setStackInSlot(i, inputs[i]);
            }
        });
        t.waitUntil("magic: essentia from the jar through the tube into the input hatch, 4 stone made (8 ignis each)", 2400,
            () -> countOut(Blocks.stone) >= 4);
        t.waitUntil("magic: 4 glass made with mana from the spreader's bursts (1000 each)", 2400, () -> countOut(Blocks.glass) >= 4);
        t.waitUntil("magic: 4 bricks made with LP from the soul network (500 each)", 2400, () -> countOut(Items.brick, -1) >= 4);
        t.waitUntil("magic: ZenScript recipes (addManaInput / addAspectInput / addLifeEssenceInput)", 2400,
            () -> countOut(Items.wheat, -1) >= 2 && countOut(Items.arrow, -1) >= 2 && countOut(Items.dye, 15) >= 2);
        t.waitUntil("magic: aqua from the output hatch reaches the empty jar through the tube (4 x 8)", 2400,
            () -> jarAmount(5, Aspect.WATER) >= 32);
        t.waitUntil("magic: spreader pulls mana from the mana output hatch and fills the pool", 2400,
            () -> ((IManaBlock) at(2, 0, -4)).getCurrentMana() > 0);
        t.waitUntil("magic: LP output hatch pushes 4 x 1000 LP into its soul network", 2400,
            () -> SoulNetworkHandler.getCurrentEssence(OWNER_OUT) >= 4000);
        t.step("magic: totals", 0, () -> {
            TileAspectProvider aspectIn = (TileAspectProvider) at(-3, 0, 0);
            TileManaProvider manaOut = (TileManaProvider) at(2, 0, 0);
            info("stone=" + countOut(Blocks.stone) + " glass=" + countOut(Blocks.glass) + " brick=" + countOut(Items.brick, -1)
                + " wheat=" + countOut(Items.wheat, -1) + " arrow=" + countOut(Items.arrow, -1) + " bonemeal=" + countOut(Items.dye, 15));
            info("source jar ignis=" + jarAmount(-5, Aspect.FIRE) + ", aspect input hatch "
                + (aspectIn.getAspect() == null ? "-" : aspectIn.getAspect().getTag()) + " x" + aspectIn.getAmount()
                + ", target jar aqua=" + jarAmount(5, Aspect.WATER));
            TileAspectProvider aspectOut = (TileAspectProvider) at(3, 0, 0);
            StringBuilder tubes = new StringBuilder();
            for (int dx : new int[]{3, 4, 5}) {
                thaumcraft.api.aspects.IEssentiaTransport tube = (thaumcraft.api.aspects.IEssentiaTransport) at(dx, 1, 0);
                Aspect st = tube.getSuctionType(net.minecraftforge.common.util.ForgeDirection.UNKNOWN);
                Aspect et = tube.getEssentiaType(net.minecraftforge.common.util.ForgeDirection.UNKNOWN);
                tubes.append(" tube(").append(dx).append(",1,0) suction=").append(tube.getSuctionAmount(net.minecraftforge.common.util.ForgeDirection.UNKNOWN))
                    .append('/').append(st == null ? "-" : st.getTag())
                    .append(" holds=").append(tube.getEssentiaAmount(net.minecraftforge.common.util.ForgeDirection.UNKNOWN)).append(et == null ? "" : " " + et.getTag());
            }
            info("aspect output hatch " + (aspectOut.getAspect() == null ? "-" : aspectOut.getAspect().getTag()) + " x" + aspectOut.getAmount()
                + ", target jar suction=" + ((thaumcraft.api.aspects.IEssentiaTransport) at(5, 0, 0)).getSuctionAmount(net.minecraftforge.common.util.ForgeDirection.UP) + ";" + tubes);
            info("mana input hatch=" + ((IManaReceiver) at(-2, 0, 0)).getCurrentMana() + ", mana output hatch=" + manaOut.getCurrentMana()
                + ", output-side spreader=" + ((IManaBlock) at(2, 0, -1)).getCurrentMana() + ", target pool=" + ((IManaBlock) at(2, 0, -4)).getCurrentMana());
            info("LP network in=" + SoulNetworkHandler.getCurrentEssence(OWNER_IN) + " (input cache " + ((TileLifeEssenceProvider) at(-1, 0, 0)).getLifeEssenceCache()
                + "), network out=" + SoulNetworkHandler.getCurrentEssence(OWNER_OUT) + " (output cache " + ((TileLifeEssenceProvider) at(1, 0, 0)).getLifeEssenceCache() + ")");
            int in = SoulNetworkHandler.getCurrentEssence(OWNER_IN) + ((TileLifeEssenceProvider) at(-1, 0, 0)).getLifeEssenceCache();
            t.expect("magic: LP consumed from the input network = 4 x 500 + 2 x 200 (network + cache = 47600)", in == 50000 - 2400);
            CommandMMCETest.statusAt(t.sender(), t.world(), DevSelfTest.machine(MAGIC_TEST), ctrl);
        });
    }
}
