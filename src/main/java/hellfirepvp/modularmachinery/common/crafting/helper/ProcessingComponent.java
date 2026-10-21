/*******************************************************************************
 * HellFirePvP / Modular Machinery 2018
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting.helper;

import com.github.bsideup.jabel.Desugar;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;

import javax.annotation.Nullable;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: ProcessingComponent
 * Created by HellFirePvP
 * Date: 04.03.2019 / 22:44
 */
@Desugar
public record ProcessingComponent<T>(MachineComponent<T> component, T providedComponent, ComponentSelectorTag tag) {

    /**
     * Required by addon mod.
     */
    public MachineComponent<T> getComponent() {
        return component;
    }

    /**
     * Required by addon mod.
     */
    public T getProvidedComponent() {
        return providedComponent;
    }

    @Nullable
    public ComponentSelectorTag getTag() {
        return tag;
    }

}
