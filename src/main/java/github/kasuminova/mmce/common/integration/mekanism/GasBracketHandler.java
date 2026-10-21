package github.kasuminova.mmce.common.integration.mekanism;

import mekanism.api.gas.GasRegistry;
import minetweaker.IBracketHandler;
import minetweaker.runtime.GlobalRegistry;
import stanhebben.zenscript.compiler.IEnvironmentGlobal;
import stanhebben.zenscript.expression.ExpressionCallStatic;
import stanhebben.zenscript.expression.ExpressionString;
import stanhebben.zenscript.expression.partial.IPartialExpression;
import stanhebben.zenscript.parser.Token;
import stanhebben.zenscript.symbols.IZenSymbol;
import stanhebben.zenscript.type.natives.IJavaMethod;
import stanhebben.zenscript.type.natives.JavaMethod;
import stanhebben.zenscript.util.ZenPosition;

import java.util.List;

/**
 * {@code <gas:名称>} 尖括号（写法与 MineTweaker3 自带的 {@code <liquid:名称>} 相同）。
 * 气体名未在 Mekanism 中注册时不解析，MineTweaker 报 “Could not resolve &lt;gas : ...&gt;”，与未知物品 / 流体一致。
 */
public class GasBracketHandler implements IBracketHandler {

    /** 尖括号编译后调用的方法：返回数量为 1 的气体。 */
    public static ZenGasStack getGas(String name) {
        return GasRegistry.getGas(name) == null ? null : new ZenGasStack(name, 1);
    }

    @Override
    public IZenSymbol resolve(IEnvironmentGlobal environment, List<Token> tokens) {
        if (tokens.size() <= 2 || !"gas".equals(tokens.get(0).getValue()) || !":".equals(tokens.get(1).getValue())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < tokens.size(); i++) {
            sb.append(tokens.get(i).getValue());
        }
        String name = sb.toString();
        if (GasRegistry.getGas(name) == null) {
            return null;
        }
        return new IZenSymbol() {
            @Override
            public IPartialExpression instance(ZenPosition position) {
                IJavaMethod method = JavaMethod.get(GlobalRegistry.getTypeRegistry(), GasBracketHandler.class, "getGas", String.class);
                return new ExpressionCallStatic(position, environment, method, new ExpressionString(position, name));
            }
        };
    }
}
