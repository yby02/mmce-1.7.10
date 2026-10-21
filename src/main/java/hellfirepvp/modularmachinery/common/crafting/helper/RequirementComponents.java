package hellfirepvp.modularmachinery.common.crafting.helper;

import com.github.bsideup.jabel.Desugar;

import java.util.List;

@Desugar
public record RequirementComponents(ComponentRequirement<?, ?> requirement, List<ProcessingComponent<?>> components) {
}
