package github.kasuminova.mmce.common.util.concurrent;

import github.kasuminova.mmce.compat.crt.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.modularmachinery.Action")
@FunctionalInterface
public interface Action {
    void doAction();
}
