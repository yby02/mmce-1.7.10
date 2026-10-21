package github.kasuminova.mmce.common.util;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasTankInfoProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * 1.7.10 移植：方向参数改为 ForgeDirection；补上 Mekanism 1.7.10 IGasHandler 中已弃用但仍需实现的两参数 receiveGas / drawGas。
 */
@SuppressWarnings("unused")
public class MultiGasTank implements IExtendedGasHandler {
    private final GasStack[]    contents;
    private final GasTankInfo[] props;
    private       int           capacity;
    private       IntConsumer   onSlotChanged = null;

    public MultiGasTank(int capacity, int tankCount) {
        this.capacity = capacity;
        this.contents = new GasStack[tankCount];
        this.props = new GasTankInfo[tankCount];

        for (int i = 0; i < this.props.length; i++) {
            props[i] = new GasTankInfoImpl(i);
        }
    }

    public MultiGasTank(MultiGasTank from) {
        this(from.capacity, from.contents.length);

        for (int i = 0; i < contents.length; i++) {
            GasStack stack = from.contents[i];
            if (stack != null) {
                contents[i] = stack.copy();
            }
        }
    }

    public MultiGasTank(IGasTankInfoProvider from) {
        this(0, from.getTankInfo().length);

        GasTankInfo[] properties = from.getTankInfo();
        for (int i = 0; i < contents.length; i++) {
            GasTankInfo prop = properties[i];
            this.capacity = Math.max(capacity, prop.getMaxGas());

            GasStack stack = prop.getGas();
            if (stack != null) {
                contents[i] = stack.copy();
            }
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public MultiGasTank setCapacity(final int capacity) {
        this.capacity = capacity;
        return this;
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return props;
    }

    @Override
    public synchronized int receiveGas(ForgeDirection side, GasStack gasStack, boolean doFill) {
        if (gasStack == null || gasStack.amount <= 0) {
            return 0;
        }

        final GasStack insert = gasStack.copy();

        int totalFillAmount = 0;
        for (int i = 0; i < contents.length; i++) {
            int filled = fill(i, insert, doFill);
            totalFillAmount += filled;

            if (insert.amount <= filled) {
                break;
            }

            insert.amount -= filled;
        }
        return totalFillAmount;
    }

    public int fill(final int slot, final GasStack insert, final boolean doFill) {
        if (insert == null || insert.amount <= 0) {
            return 0;
        }

        GasStack content = contents[slot];

        if (content == null) {
            if (capacity > insert.amount) {
                if (doFill) {
                    contents[slot] = insert;
                    onSlotChanged(slot);
                }
                return insert.amount;
            }
            if (doFill) {
                GasStack copied = insert.copy();
                copied.amount = capacity;
                contents[slot] = copied;
                onSlotChanged(slot);
            }
            return capacity;
        }

        if (!content.isGasEqual(insert) || content.amount >= capacity) {
            return 0;
        }

        int maxCanFill = capacity - content.amount;

        if (maxCanFill > insert.amount) {
            if (doFill) {
                content.amount += insert.amount;
                onSlotChanged(slot);
            }
            return insert.amount;
        }
        if (doFill) {
            content.amount = capacity;
            onSlotChanged(slot);
        }
        return maxCanFill;
    }

    @Nullable
    @Override
    public synchronized GasStack drawGas(final GasStack resource, final boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }

        GasStack res = resource.copy();

        int totalDrainAmount = 0;
        for (int i = 0; i < contents.length; i++) {
            GasStack content = contents[i];
            if (content == null || !content.isGasEqual(res)) {
                continue;
            }

            GasStack drainedStack = drain(i, res.amount, doDrain);
            if (drainedStack == null) {
                continue;
            }

            int drained = drainedStack.amount;
            totalDrainAmount += drained;

            if (drained >= res.amount) {
                break;
            }
            res.amount -= drained;
        }

        GasStack drained = res.copy();
        drained.amount = totalDrainAmount;
        return drained;
    }

    @Nullable
    public GasStack drain(final int slot, final int maxDrain, final boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }

        GasStack content = contents[slot];
        if (content == null) {
            return null;
        }

        if (content.amount < maxDrain) {
            if (doDrain) {
                contents[slot] = null;
                onSlotChanged(slot);
                return content;
            }
            return content.copy();
        }

        if (doDrain) {
            content.amount -= maxDrain;
            onSlotChanged(slot);
        }

        GasStack copied = content.copy();
        copied.amount = maxDrain;
        return copied;
    }

    @Nullable
    @Override
    public synchronized GasStack drawGas(final ForgeDirection side, final int maxDrain, final boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }

        GasStack toDrain = Arrays.stream(contents)
                                 .filter(Objects::nonNull)
                                 .findFirst()
                                 .map(GasStack::copy)
                                 .orElse(null);

        if (toDrain == null) {
            return null;
        }

        toDrain.amount = maxDrain;

        return drawGas(toDrain, doDrain);
    }

    @Override
    @Deprecated
    public int receiveGas(final ForgeDirection side, final GasStack stack) {
        return receiveGas(side, stack, true);
    }

    @Override
    @Deprecated
    public GasStack drawGas(final ForgeDirection side, final int amount) {
        return drawGas(side, amount, true);
    }

    public GasStack getGasInSlot(final int slot) {
        return slot >= 0 && slot < contents.length ? contents[slot] : null;
    }

    public void setGasInSlot(final int slot, final GasStack stack) {
        if (slot >= 0 && slot < contents.length) {
            contents[slot] = stack;
            onSlotChanged(slot);
        }
    }

    @Override
    public boolean canReceiveGas(final ForgeDirection side, final Gas type) {
        return receiveGas(null, new GasStack(type, 1), false) > 0;
    }

    @Override
    public boolean canDrawGas(final ForgeDirection side, final Gas type) {
        return drawGas(new GasStack(type, 1), false) != null;
    }

    public int getSlots() {
        return contents.length;
    }

    public void setOnSlotChanged(final IntConsumer onSlotChanged) {
        this.onSlotChanged = onSlotChanged;
    }

    private void onSlotChanged(final int slot) {
        if (onSlotChanged != null) {
            onSlotChanged.accept(slot);
        }
    }

    public void readFromNBT(final NBTTagCompound compound, final String name) {
        NBTTagCompound tag = compound.getCompoundTag(name);
        Arrays.fill(contents, null);

        if (tag.hasNoTags()) {
            return;
        }

        for (int i = 0; i < contents.length; i++) {
            NBTTagCompound t = tag.getCompoundTag("#" + i);
            if (t.hasNoTags()) {
                contents[i] = null;
                continue;
            }

            contents[i] = GasStack.readFromNBT(t);
        }
    }

    public void writeToNBT(final NBTTagCompound compound, final String name) {
        NBTTagCompound tag = new NBTTagCompound();

        for (int i = 0; i < contents.length; i++) {
            GasStack stack = contents[i];
            if (stack == null) {
                continue;
            }

            NBTTagCompound t = new NBTTagCompound();
            stack.write(t);

            tag.setTag("#" + i, t);
        }

        compound.setTag(name, tag);
    }

    public class GasTankInfoImpl implements GasTankInfo {

        private final int index;

        public GasTankInfoImpl(final int index) {
            this.index = index;
        }

        @Nullable
        @Override
        public GasStack getGas() {
            return contents[index];
        }

        @Override
        public int getStored() {
            GasStack content = contents[index];
            return content != null ? content.amount : 0;
        }

        @Override
        public int getMaxGas() {
            return capacity;
        }
    }
}
