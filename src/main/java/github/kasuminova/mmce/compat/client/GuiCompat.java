package github.kasuminova.mmce.compat.client;

import net.minecraft.client.gui.GuiTextField;

/**
 * 1.7.10 移植适配：1.12.2 GUI 控件中返回值语义不同的方法。仅客户端使用。
 */
public final class GuiCompat {

    private GuiCompat() {
    }

    /**
     * 对应 1.12.2 的 GuiTextField#mouseClicked（返回是否点中并聚焦了输入框）；1.7.10 的同名方法返回 void。
     */
    public static boolean textFieldClicked(GuiTextField field, int mouseX, int mouseY, int mouseButton) {
        field.mouseClicked(mouseX, mouseY, mouseButton);
        boolean inside = mouseX >= field.xPosition && mouseX < field.xPosition + field.width
            && mouseY >= field.yPosition && mouseY < field.yPosition + field.height;
        return field.getVisible() && inside && field.isFocused() && mouseButton == 0;
    }
}
