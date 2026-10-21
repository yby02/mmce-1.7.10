package hellfirepvp.modularmachinery.common.util;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import github.kasuminova.mmce.compat.crt.CraftTweakerMC;
import github.kasuminova.mmce.compat.crt.IBlockPos;
import net.minecraft.nbt.NBTTagCompound;
import github.kasuminova.mmce.compat.ResourceLocation;
import github.kasuminova.mmce.compat.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.SmartInterfaceData")
public class SmartInterfaceData {
    private final BlockPos         pos;
    private final ResourceLocation parent;
    private final String           type;
    private       float            value = 0;

    public SmartInterfaceData(BlockPos pos, ResourceLocation parent, String type) {
        this.pos = pos;
        this.parent = parent;
        this.type = type;
    }

    public SmartInterfaceData(BlockPos pos, ResourceLocation parent, String type, float value) {
        this.pos = pos;
        this.parent = parent;
        this.type = type;
        this.value = value;
    }

    public static SmartInterfaceData deserialize(NBTTagCompound compound) {
        return new SmartInterfaceData(
            BlockPos.fromLong(compound.getLong("pos")),
            new ResourceLocation(compound.getString("parent")),
            compound.getString("type"),
            compound.getFloat("value"));
    }

    public BlockPos getPos() {
        return pos;
    }

    /**
     * 返回此数据对应控制器所在的位置。
     */
    @ZenGetter("pos")
    public IBlockPos getIPos() {
        return CraftTweakerMC.getIBlockPos(pos);
    }

    public ResourceLocation getParent() {
        return parent;
    }

    /**
     * 获取此数据的机械注册名。
     */
    @ZenGetter("parentMachineName")
    public String getParentMachineName() {
        return parent.getNamespace();
    }

    /**
     * 获取此数据的类型。
     */
    @ZenGetter("interfaceType")
    public String getType() {
        return type;
    }

    /**
     * 获取数据值。
     */
    @ZenGetter("value")
    public float getValue() {
        return value;
    }

    /**
     * 设置数据值。
     */
    @ZenSetter("value")
    public void setValue(float value) {
        this.value = value;
    }

    public NBTTagCompound serialize() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setLong("pos", pos.toLong());
        compound.setString("parent", parent.toString());
        compound.setString("type", type);
        compound.setFloat("value", value);

        return compound;
    }
}
