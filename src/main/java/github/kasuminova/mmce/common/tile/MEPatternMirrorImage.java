package github.kasuminova.mmce.common.tile;

import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.compat.BlockPos;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * ME 机械样板镜像：用 AE2 内存卡绑定一台 ME 机械样板供应器后，作为该供应器的“分身”放进另一台机器的结构中，
 * 两台机器共用同一个供应器的缓存。自身不接入 ME 网络。
 * 1.7.10 移植：逻辑同上游；绑定坐标以 x / y / z 三个整数保存（上游为 BlockPos.toLong）。
 */
public class MEPatternMirrorImage extends TileColorableMachineComponent implements SelectiveUpdateTileEntity, MachineComponentTile, MachineCombinationComponent {

    public BlockPos providerPos;

    @Nullable
    private MEPatternProvider provider() {
        if (worldObj == null || worldObj.isRemote || providerPos == null) {
            return null;
        }
        if (!worldObj.blockExists(providerPos.getX(), providerPos.getY(), providerPos.getZ())) {
            return null;
        }
        TileEntity te = worldObj.getTileEntity(providerPos.getX(), providerPos.getY(), providerPos.getZ());
        return te instanceof MEPatternProvider mep ? mep : null;
    }

    @Nullable
    @Override
    public MachineComponent<?> provideComponent() {
        MEPatternProvider mep = provider();
        return mep == null ? null : mep.provideComponent();
    }

    @Nonnull
    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        MEPatternProvider mep = provider();
        return mep == null ? Collections.emptyList() : mep.provideComponents();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (compound.hasKey("providerX")) {
            this.providerPos = new BlockPos(compound.getInteger("providerX"), compound.getInteger("providerY"), compound.getInteger("providerZ"));
        } else {
            this.providerPos = null;
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (providerPos != null) {
            compound.setInteger("providerX", providerPos.getX());
            compound.setInteger("providerY", providerPos.getY());
            compound.setInteger("providerZ", providerPos.getZ());
        }
    }
}
