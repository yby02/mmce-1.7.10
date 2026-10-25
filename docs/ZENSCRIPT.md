# ZenScript 使用教程（MineTweaker3 / CraftTweaker 3.3.1）

本文是 MMCE 1.7.10 移植版的 ZenScript 完整使用教程，覆盖机器、配方、事件、修改器、升级、智能接口与工厂。

文中所有类名、方法名、参数与实际源码一致，可对照：

- 事件：`src/main/java/github/kasuminova/mmce/common/event/`
- Zen 入口：`src/main/java/hellfirepvp/modularmachinery/common/integration/crafttweaker/`
- 注册机制：`src/main/java/github/kasuminova/mmce/compat/crt/ZenRegistration.java`

---

## 目录

1. [前置条件与环境](#1-前置条件与环境)
2. [脚本位置与加载时机](#2-脚本位置与加载时机)
3. [import 命名空间对照](#3-import-命名空间对照)
4. [事件系统](#4-事件系统)
5. [控制器 API（IMachineController）](#5-控制器-apiimachinecontroller)
6. [配方编写](#6-配方编写)
7. [机器编写（脚本内定义）](#7-机器编写脚本内定义)
8. [机器修改器](#8-机器修改器)
9. [自定义升级](#9-自定义升级)
10. [智能接口](#10-智能接口)
11. [工厂与配方线程](#11-工厂与配方线程)
12. [并发与 Sync](#12-并发与-sync)
13. [完整示例脚本](#13-完整示例脚本)
14. [排错](#14-排错)
15. [与 1.12.2 的差异](#15-与-1122-的差异)

---

## 1. 前置条件与环境

| 项 | 要求 |
|---|---|
| Minecraft | 1.7.10 + Forge 10.13.4.1614 或更高 |
| 本模组 | `modularmachinery-2.3.4-mc1.7.10.jar` 或更高 |
| 脚本引擎 | CraftTweaker 3.3.1（GTNH 版，MineTweaker3 血统） |
| 必需依赖 | GTNHLib ≥ 0.11.54 |

脚本文件放在：

```
.minecraft/config/minetweaker/*.zs
```

或整合包指定的脚本目录（`.minecraft/scripts/` 等，由 MineTweaker3 决定）。文件扩展名必须是 `.zs`。

编辑后可用命令重载，无需重启：

```
/mt reload
```

---

## 2. 脚本位置与加载时机

ZenScript 要求**所有 `import` 写在文件最开头**，任何语句不能出现在 import 之前。

加载流程（1.7.10 特有）：

```
服务器启动 / 执行 /mt reload
  ↓
onReloadEvent       → 清理 CrT 注册的配方、机器、事件、修改器
  ↓
执行所有 .zs 脚本    → 你的语句在这里运行
  ↓
onPostReload
  ├─ reloadMachine(脚本定义的机器)
  ├─ MachineModifier.loadAll()        → 应用修改器
  ├─ MMEvents.registryAll()           → 绑定事件处理器 ← 关键
  └─ loadRecipeRegistry()             → 加载 JSON 配方 + 适配器配方
```

**含义**：脚本里调用 `MMEvents.onXxx(...)` 时只是把动作排队，真正绑定发生在脚本全部执行完之后。所以「先定义机器再注册事件」或反过来**都能work**，顺序无关。

但机器名必须正确，否则日志里会出现：

```
Could not find machine `xxx`!
```

---

## 3. import 命名空间对照

`CraftTweaker 3.3.1` 是 MineTweaker3 血统，**没有 `crafttweaker.*` 包**。本模组为兼容 1.12.2 脚本，只补了少量同名 shim。

### 3.1 本模组提供的类型（全部可用）

```zenscript
// 事件
import mods.modularmachinery.MachineEvent;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.MachineStructureFormedEvent;
import mods.modularmachinery.MachineStructureUpdateEvent;
import mods.modularmachinery.SmartInterfaceUpdateEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;
import mods.modularmachinery.RecipeEvent;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeStartEvent;
import mods.modularmachinery.RecipeTickEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.RecipeFailureEvent;
import mods.modularmachinery.FactoryRecipeEvent;
import mods.modularmachinery.FactoryRecipeStartEvent;
import mods.modularmachinery.FactoryRecipeTickEvent;
import mods.modularmachinery.FactoryRecipeFinishEvent;
import mods.modularmachinery.FactoryRecipeFailureEvent;

// 入口
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipePrimer;
import mods.modularmachinery.MachineBuilder;
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.MachineController;
import mods.modularmachinery.MachineRegistry;
import mods.modularmachinery.Sync;

// 配方构件
import mods.modularmachinery.RecipeModifier;
import mods.modularmachinery.RecipeModifierBuilder;
import mods.modularmachinery.RecipeAdapterBuilder;
import mods.modularmachinery.RecipeFailureActions;
import mods.modularmachinery.IngredientArrayBuilder;
import mods.modularmachinery.IngredientArrayPrimer;
import mods.modularmachinery.AdvancedItemCheckerCT;
import mods.modularmachinery.AdvancedBlockChecker;
import mods.modularmachinery.AdvancedItemModifier;

// 结构
import mods.modularmachinery.BlockArray;
import mods.modularmachinery.BlockArrayBuilder;
import mods.modularmachinery.TaggedPositionBlockArray;
import mods.modularmachinery.MultiBlockModifierBuilder;
import mods.modularmachinery.MultiBlockModifierReplacement;
import mods.modularmachinery.StatedMachineComponentBuilder;
import mods.modularmachinery.IBlockStatePredicate;

// 升级
import mods.modularmachinery.MachineUpgrade;
import mods.modularmachinery.MachineUpgradeBuilder;
import mods.modularmachinery.MachineUpgradeHelper;
import mods.modularmachinery.DynamicMachineUpgradeBuilder;
import mods.modularmachinery.SimpleMachineUpgrade;
import mods.modularmachinery.SimpleDynamicMachineUpgrade;
import mods.modularmachinery.RegistryUpgrade;
import mods.modularmachinery.UpgradeEventHandler;
import mods.modularmachinery.UpgradeEventHandlerWrapper;
import mods.modularmachinery.IFunction;

// 智能接口 / 工厂
import mods.modularmachinery.SmartInterfaceType;
import mods.modularmachinery.SmartInterfaceData;
import mods.modularmachinery.FactoryRecipeThread;
import mods.modularmachinery.RecipeThread;
import mods.modularmachinery.ActiveMachineRecipe;
import mods.modularmachinery.IDynamicPatternInfo;

// 控制器
import mods.modularmachinery.IMachineController;
import mods.modularmachinery.Action;
```

### 3.2 世界/坐标类型的 shim（沿用 1.12.2 写法）

```zenscript
import crafttweaker.world.IWorld;
import crafttweaker.world.IBlockPos;
import crafttweaker.world.IFacing;
import crafttweaker.block.IBlockState;
import crafttweaker.block.IBlockStateMatcher;
import crafttweaker.util.IAxisAlignedBB;
```

### 3.3 MineTweaker3 原生类型

```zenscript
import minetweaker.item.IItemStack;
import minetweaker.item.IIngredient;
import minetweaker.item.IItemDefinition;
import minetweaker.item.IOreDictEntry;
import minetweaker.liquid.ILiquidStack;
import minetweaker.data.IData;
import minetweaker.entity.IEntityItem;
import minetweaker.api.event.IEventManager;
```

### 3.4 1.12.2 写法 → 1.7.10 对照

| 1.12.2 | 1.7.10 | 说明 |
|---|---|---|
| `crafttweaker.item.IItemStack` | `minetweaker.item.IItemStack` | 改命名空间 |
| `crafttweaker.item.IIngredient` | `minetweaker.item.IIngredient` | 改命名空间 |
| `crafttweaker.liquid.ILiquidStack` | `minetweaker.liquid.ILiquidStack` | 改命名空间 |
| `crafttweaker.data.IData` | `minetweaker.data.IData` | 改命名空间 |
| `crafttweaker.world.IWorld` | `crafttweaker.world.IWorld` | **不变**（shim） |
| `crafttweaker.world.IBlockPos` | `crafttweaker.world.IBlockPos` | **不变**（shim） |
| `crafttweaker.enchantments.IEnchantment` | — | **不存在** |
| `crafttweaker.util.Math` | — | **不存在** |
| `crafttweaker.event.EntityLivingDeathEvent` | — | **不存在** |
| `crafttweaker.event.ItemTossEvent` | — | **不存在** |
| `crafttweaker.event.EntityJoinWorldEvent` | — | **不存在** |

MineTweaker3 只提供玩家相关的这一组事件（`events.onXxx`）：

```
PlayerAttackEntityEvent     PlayerBonemealEvent          PlayerChangedDimensionEvent
PlayerCraftedEvent          PlayerDeathDropsEvent        PlayerFillBucketEvent
PlayerInteractEntityEvent   PlayerInteractEvent          PlayerLoggedInEvent
PlayerLoggedOutEvent        PlayerOpenContainerEvent     PlayerPickupEvent
PlayerPickupItemEvent       PlayerPickupXpEvent          PlayerRespawnEvent
PlayerSleepInBedEvent       PlayerSmeltedEvent           PlayerUseHoeEvent
PlayerUseItemStartEvent     PlayerUseItemTickEvent
```

需要「实体死亡 / 掉落 / 进入世界」的逻辑，请改用上表中语义最接近的事件，或在 MMCE 的配方/机器事件里实现。

---

## 4. 事件系统

### 4.1 总览

本模组共 **14 个**可在 ZenScript 中订阅的事件，分三类：

| 类别 | 事件 | 数量 |
|---|---|---|
| 机器级 | `MachineStructureFormedEvent`、`MachineStructureUpdateEvent`、`MachineTickEvent`、`ControllerGUIRenderEvent`、`SmartInterfaceUpdateEvent` | 5 |
| 普通配方级 | `RecipeCheckEvent`、`RecipeStartEvent`、`RecipeTickEvent`、`RecipeFinishEvent`、`RecipeFailureEvent` | 5 |
| 工厂配方级 | `FactoryRecipeStartEvent`、`FactoryRecipeTickEvent`、`FactoryRecipeFinishEvent`、`FactoryRecipeFailureEvent` | 4 |

所有事件都继承 `MachineEvent`，因此**都带** `controller` 属性与 `canceled` 可写属性。

### 4.2 事件类继承关系

```
MachineEvent                     ← controller, canceled
├── MachineTickEvent
├── MachineStructureFormedEvent
├── MachineStructureUpdateEvent
├── SmartInterfaceUpdateEvent    ← interfacePos, newData
├── ControllerGUIRenderEvent     ← extraInfo
└── RecipeEvent                  ← activeRecipe
    ├── RecipeCheckEvent         ← setFailed(String), parallelism
    ├── RecipeStartEvent
    ├── RecipeTickEvent          ← preventProgressing(String), setFailed(bool, String)
    ├── RecipeFinishEvent
    ├── RecipeFailureEvent       ← cause, destructRecipe
    └── FactoryRecipeEvent       ← factoryRecipeThread
        ├── FactoryRecipeStartEvent
        ├── FactoryRecipeTickEvent
        ├── FactoryRecipeFinishEvent
        └── FactoryRecipeFailureEvent
```

### 4.3 机器级事件（MMEvents 注册）

| 事件 | 注册方法 |
|---|---|
| `MachineStructureFormedEvent` | `MMEvents.onStructureFormed(name, fn)` |
| `MachineStructureUpdateEvent` | `MMEvents.onStructureUpdate(name, fn)` |
| `MachineTickEvent`（START） | `MMEvents.onMachinePreTick(name, fn)` |
| `MachineTickEvent`（END） | `MMEvents.onMachinePostTick(name, fn)` |
| `MachineTickEvent`（END，已废弃） | `MMEvents.onMachineTick(name, fn)` |
| `ControllerGUIRenderEvent` | `MMEvents.onControllerGUIRender(name, fn)` |
| `SmartInterfaceUpdateEvent` | `MMEvents.onSmartInterfaceUpdate(name, fn)` |

> `name` 是机器注册名，**不带 `modularmachinery:` 前缀**。JSON 里的 `registryname` 就是它。

#### 示例

```zenscript
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.MachineStructureFormedEvent;
import mods.modularmachinery.MachineStructureUpdateEvent;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;
import mods.modularmachinery.SmartInterfaceUpdateEvent;

// 结构成型
MMEvents.onStructureFormed("my_machine", function(event as MachineStructureFormedEvent) {
    print("结构成型: " ~ event.controller.formedMachineName);
});

// 结构变更（成型后外围方块被改也会触发）
MMEvents.onStructureUpdate("my_machine", function(event as MachineStructureUpdateEvent) {
    print("结构变更");
});

// 每 tick：START 阶段
MMEvents.onMachinePreTick("my_machine", function(event as MachineTickEvent) {
    if (!event.controller.isWorking) {
        return;
    }
    // 例如：逐 tick 消耗自定义数据
});

// 每 tick：END 阶段
MMEvents.onMachinePostTick("my_machine", function(event as MachineTickEvent) {
    // ...
});

// 控制器界面额外显示的文本
MMEvents.onControllerGUIRender("my_machine", function(event as ControllerGUIRenderEvent) {
    event.extraInfo = ["温度: " ~ event.controller.customData.temperature ~ " K"];
});

// 智能接口数值变化
MMEvents.onSmartInterfaceUpdate("my_machine", function(event as SmartInterfaceUpdateEvent) {
    print("接口 " ~ event.interfacePos.x ~ " 新值 " ~ event.newData.value);
});
```

### 4.4 普通配方级事件（RecipePrimer 注册）

这类事件绑定在**具体配方**上，只有该配方运行时才触发。

| 事件 | 注册方法 |
|---|---|
| `RecipeCheckEvent`（START） | `.addPreCheckHandler(fn)` |
| `RecipeCheckEvent`（END） | `.addPostCheckHandler(fn)` |
| `RecipeStartEvent` | `.addStartHandler(fn)`（**已废弃**） |
| `RecipeTickEvent`（START） | `.addPreTickHandler(fn)` |
| `RecipeTickEvent`（END） | `.addPostTickHandler(fn)` |
| `RecipeFinishEvent` | `.addFinishHandler(fn)` |
| `RecipeFailureEvent` | `.addFailureHandler(fn)` |

#### 示例

```zenscript
import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeTickEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.RecipeFailureEvent;

RecipeBuilder.newBuilder("my_recipe", "my_machine", 200)
  .addItemInput(<minecraft:iron_ingot> * 4)
  .addItemOutput(<minecraft:gold_ingot>)

  // 启动前检查：不满足则不启动
  .addPreCheckHandler(function(event as RecipeCheckEvent) {
      if (event.controller.customData.temperature < 800) {
          event.setFailed("温度不足 800K");
      }
  })

  // 每 tick 推进前：可暂停进度
  .addPreTickHandler(function(event as RecipeTickEvent) {
      if (event.controller.customData.temperature < 800) {
          event.preventProgressing("温度过低");
      }
  })

  .addFinishHandler(function(event as RecipeFinishEvent) {
      print("配方完成");
  })

  .addFailureHandler(function(event as RecipeFailureEvent) {
      print("配方失败: " ~ event.cause);
      event.destructRecipe = false;   // 失败但保留进度
  })

  .build();
```

#### 三个「失败/暂停」方法的区别

| 方法 | 效果 |
|---|---|
| `event.setFailed(String reason)` | 标记失败，**保留**当前配方进度，界面显示 reason |
| `event.setFailed(true, String reason)` | 标记失败并**清空**该线程的配方进度 |
| `event.preventProgressing(String reason)` | 进度**倒退 1 tick**（`activeRecipe.tick - 1`），状态显示 working + reason |

三者都会自动 `setCanceled(true)`。

### 4.5 工厂配方级事件

工厂多方块（`hasFactory` 机器）运行时使用另一套事件。

| 事件 | 注册方法 |
|---|---|
| `FactoryRecipeStartEvent` | `.addFactoryStartHandler(fn)` |
| `FactoryRecipeTickEvent`（START） | `.addFactoryPreTickHandler(fn)` |
| `FactoryRecipeTickEvent`（END） | `.addFactoryPostTickHandler(fn)` |
| `FactoryRecipeFinishEvent` | `.addFactoryFinishHandler(fn)` |
| `FactoryRecipeFailureEvent` | `.addFactoryFailureHandler(fn)` |

#### 示例

```zenscript
import mods.modularmachinery.FactoryRecipeTickEvent;

RecipeBuilder.newBuilder("my_factory_recipe", "my_factory", 100)
  .addItemInput(<minecraft:iron_ingot>)
  .addItemOutput(<minecraft:gold_ingot>)
  .addFactoryPostTickHandler(function(event as FactoryRecipeTickEvent) {
      // event.factoryRecipeThread 可以拿到具体线程
      if (event.factoryRecipeThread.threadName == "core_1") {
          // ...
      }
  })
  .build();
```

### 4.6 脚本定义机器时的事件注册

用 `MachineBuilder` 在脚本里定义机器时，可以直接挂事件：

```zenscript
import mods.modularmachinery.MachineBuilder;
import mods.modularmachinery.MachineTickEvent;

MachineBuilder.registerMachine("script_machine", "脚本机器");
MachineBuilder.getBuilder("script_machine")
  .addTickHandler(function(event as MachineTickEvent) {
      // 注意：START 与 END 都会进这里，需要自己判断
  })
  .build();
```

可用方法：

```
addStructureFormedHandler(fn)        // MachineStructureFormedEvent
addStructureUpdateHandler(fn)        // MachineStructureUpdateEvent
addTickHandler(fn)                   // MachineTickEvent（START + END）
addGUIRenderHandler(fn)              // ControllerGUIRenderEvent（仅客户端）
addSmartInterfaceUpdateHandler(fn)   // SmartInterfaceUpdateEvent
```

> `addTickHandler` 两个阶段都会触发，与 `MMEvents.onMachinePreTick/PostTick` 不同。要区分阶段只能靠副作用判断，一般建议改在外部用 `MMEvents` 注册。

### 4.7 事件成员速查

| 事件 | 可读 | 可写 |
|---|---|---|
| 所有事件 | `controller` | `canceled` |
| `ControllerGUIRenderEvent` | `extraInfo` | `extraInfo = [...]` |
| `SmartInterfaceUpdateEvent` | `interfacePos`、`newData` | — |
| `RecipeEvent` 及子类 | `activeRecipe` | — |
| `RecipeCheckEvent` | — | `setFailed(String)`、`parallelism = n` |
| `RecipeTickEvent` | — | `preventProgressing(String)`、`setFailed(bool, String)` |
| `RecipeFailureEvent` | `cause`、`destructRecipe` | `destructRecipe = bool` |
| `FactoryRecipeEvent` 及子类 | `factoryRecipeThread` | — |
| `FactoryRecipeFailureEvent` | `cause`、`destructRecipe` | `destructRecipe = bool` |

### 4.8 触发时机与线程

| 事件 | 侧 | 线程 |
|---|---|---|
| 全部机器/配方事件 | 服务端 | 服务端主线程（`ServerThread`） |
| `ControllerGUIRenderEvent` | **仅客户端** | 渲染线程 |

`MMEvents.onControllerGUIRender` 内部有 `isServer()` 判断，纯服务端环境不会注册。

### 4.9 事件系统已知坑

1. **不能订阅基类收全部事件**
   `DynamicMachine.getMachineEventHandlers(Class)` 是 `Map.get()` 精确类查找，`postCrTEvent` 用 `getClass()` 取具体类。注册 `MachineEvent` 收不到任何子类事件。

2. **`event.phase` 不要用**
   `MachineTickEvent.phase` 是 `public final Phase`，但 `Phase` 枚举**没有 `@ZenClass`**，ZenScript 里是未注册类型。判阶段请改用 `onMachinePreTick` / `onMachinePostTick`。

3. **配方事件会派发到两处**
   `RecipeEvent.postCrTEvent()` 先调 `super`（机器级 handler 表），再查配方自身的 handler 表。但 `MMEvents` 没有提供配方事件的注册入口，所以机器级永远收不到配方事件——配方事件只能挂在 `RecipePrimer` 上。

4. **`onMachineTick` 已废弃**
   内部转调 `onMachinePostTick` 并打一条 warning 日志。新脚本用 Pre/Post。

5. **`extraInfo` 是累加的**
   `setExtraInfo(String...)` 内部是 `addAll`，不是覆盖。每帧新建事件实例所以不会无限增长，但不要指望用它清空旧内容。

6. **`addStartHandler` 已废弃**
   需要拦截启动请用 `addPreCheckHandler`。

---

## 5. 控制器 API（IMachineController）

`event.controller` 返回 `IMachineController`。

### 5.1 属性

| 属性 | 类型 | 读写 |
|---|---|---|
| `world` | `IWorld` | 只读 |
| `blockState` | `IBlockState` | 只读 |
| `facing` | `IFacing` | 只读 |
| `pos` | `IBlockPos` | 只读 |
| `ticksExisted` | `int` | 只读 |
| `activeRecipe` | `ActiveMachineRecipe` | 只读 |
| `activeRecipeList` | `ActiveMachineRecipe[]` | 只读 |
| `recipeThreadList` | `RecipeThread[]` | 只读 |
| `extraThreadCount` | `int` | **读写** |
| `isWorking` | `boolean` | 只读 |
| `formedMachineName` | `String` | 只读 |
| `customData` | `IData` | **读写** |
| `smartInterfaceDataList` | `SmartInterfaceData[]` | 只读 |
| `foundModifiers` | `String[]` | 只读 |
| `ownerPlayer` | `IPlayer` | 只读 |
| `ownerName` | `String` | 只读 |
| `ownerUUID` | `String` | 只读 |

`statusMessage` 只有 setter：`event.controller.statusMessage = "文本";`

### 5.2 方法

```zenscript
// 状态
event.controller.overrideStatusInfo("自定义状态");       // 等价 statusMessage =
event.controller.rotateWithControllerFacing(pos);      // 相对朝向转换

// 配方修改器（运行时）
event.controller.addModifier("key", modifier);
event.controller.addPermanentModifier("key", modifier);
event.controller.removeModifier("key");
event.controller.removePermanentModifier("key");
event.controller.hasModifier("key");

// 智能接口
event.controller.getSmartInterfaceData("type");

// 升级
event.controller.hasMachineUpgrade("upgrade_name");
event.controller.getMachineUpgrade("upgrade_name");

// 结构查询
event.controller.getDynamicPattern("pattern_name");            // IDynamicPatternInfo
event.controller.getBlocksInPattern(<minecraft:stone>);        // int
event.controller.getBlocksInPattern("minecraft:stone");        // 按方块名
event.controller.getBlockPosInPattern(<minecraft:stone>);      // IBlockPos[]
```

### 5.3 自定义数据（customData）

`customData` 是 `IData`，可在事件里写、在其它事件里读，用于跨 tick 持久化状态。

```zenscript
MMEvents.onStructureFormed("my_machine", function(e as MachineStructureFormedEvent) {
    e.controller.customData = { temperature: 300, coolant: 0 } as IData;
});

MMEvents.onMachinePreTick("my_machine", function(e as MachineTickEvent) {
    val data = e.controller.customData;
    if (data == null) {
        return;
    }
    // 读
    val temp = data.temperature as int;
    // 写（必须整体赋回）
    e.controller.customData = {
        temperature: temp + 1,
        coolant: data.coolant
    } as IData;
});
```

### 5.4 从坐标获取控制器

```zenscript
import mods.modularmachinery.MachineController;
import crafttweaker.world.IWorld;
import crafttweaker.world.IBlockPos;

val ctrl = MachineController.getControllerAt(world, pos);
val ctrl2 = MachineController.getControllerAt(world, x, y, z);

if (ctrl != null) {
    print(ctrl.formedMachineName);
}
```

---

## 6. 配方编写

### 6.1 基本结构

```zenscript
RecipeBuilder.newBuilder(配方ID, 机器注册名, 加工时间tick [, 排序优先级 [, 每tick失败则取消]])
```

三个重载：

```zenscript
newBuilder(String recipeRegistryName, String machineName, int processingTickTime)
newBuilder(String recipeRegistryName, String machineName, int processingTickTime, int sortingPriority)
newBuilder(String recipeRegistryName, String machineName, int processingTickTime, int sortingPriority, boolean cancelIfPerTickFails)
```

必须以 `.build()` 结尾。

### 6.2 输入输出

```zenscript
.addItemInput(<minecraft:iron_ingot>)              // 物品
.addItemInput(<ore:ingotIron> * 2)                 // 矿物词典
.addItemInputs(a, b, c)                            // 多输入
.addItemOutput(<minecraft:gold_ingot>)
.addItemOutputs(a, b)
.addRandomItemOutput(ingredientArrayPrimer)        // 随机产出

.addFluidInput(<liquid:water> * 1000)              // 流体（mB）
.addFluidInputs(...)
.addFluidOutput(<liquid:steam> * 500)
.addFluidOutputs(...)
.addFluidPerTickInput(<liquid:water> * 10)         // 每 tick 流体
.addFluidPerTickOutput(...)

.addEnergyPerTickInput(20)                         // 每 tick 能量（RF/t）
.addEnergyPerTickOutput(5)

.addFuelItemInput(1600)                            // 燃料（总燃烧时间）

.addCatalystInput(<item>, ["提示"], [modifier...])  // 催化剂
```

### 6.3 气体（需要 Mekanism）

```zenscript
.addGasInput("oxygen", 1000)        // 按气体名 + 数量
.addGasOutput("hydrogen", 500)
.addGasPerTickInput("oxygen", 10)
.addGasPerTickOutput("hydrogen", 5)

.addGasInput(<gas:oxygen> * 1000)   // 按气体栈
.addGasInputs(...)
.addGasOutput(...)
.addGasOutputs(...)
.addGasPerTickInput(...)
.addGasPerTickInputs(...)
.addGasPerTickOutput(...)
.addGasPerTickOutputs(...)
```

### 6.4 其它开关

```zenscript
.setParallelized(true)              // 是否可并行
.setMaxThreads(4)                   // 最大线程数
.setThreadName("thread_1")          // 线程名
.setChance(0.5)                     // 产出概率
.setTag("selector_tag")             // 标签选择器
.setLoadJEI(true)                   // 是否在 NEI 显示
.setTriggerTime(100)                // 触发时间
.setTriggerRepeatable(true)         // 可重复触发
.setIgnoreOutputCheck(true)         // 忽略输出空间检查
.setPreViewNBT(nbt)                 // NEI 预览 NBT
.setMinMaxAmount(1, 4)              // 输入数量范围
.consumeDurability(10)              // 消耗耐久
.addRecipeTooltip("第一行", "第二行") // NEI 提示
.addSmartInterfaceDataInput("type", 0.0, 100.0)  // 智能接口输入范围
.addSmartInterfaceDataInput("type", 50.0)        // 固定值
.setParallelizeUnaffected(true)     // 并行不影响
.setNBTChecker(checker)             // NBT 检查器
.addItemModifier(modifier)          // 物品修改器
.addIngredientArrayInput(primer)    // 多物品数组输入
```

### 6.5 输入 NBT

```zenscript
RecipeBuilder.newBuilder("nbt_recipe", "my_machine", 100)
  .addItemInput(<minecraft:diamond>.withTag({ display: { Name: "定制" } }))
  .addItemOutput(<minecraft:emerald>)
  .build();
```

### 6.6 随机产出

```zenscript
import mods.modularmachinery.IngredientArrayBuilder;

val pool = IngredientArrayBuilder.newBuilder()
  .addChancedIngredient(<minecraft:diamond>, 0.1)     // 10%
  .addChancedIngredient(<minecraft:gold_ingot>, 0.5)  // 50%
  .addIngredient(<minecraft:cobblestone>);            // 保底

RecipeBuilder.newBuilder("random_recipe", "my_machine", 100)
  .addItemInput(<minecraft:stone>)
  .addRandomItemOutput(pool)
  .build();
```

---

## 7. 机器编写（脚本内定义）

### 7.1 注册与获取 Builder

```zenscript
MachineBuilder.registerMachine("machine_id", "显示名称");
MachineBuilder.registerMachine("machine_id", "显示名称", hasFactory, factoryOnly);
MachineBuilder.registerMachine("machine_id", "显示名称", requiresBlueprint, failureAction, color);

val builder = MachineBuilder.getBuilder("machine_id");
```

### 7.2 结构定义

```zenscript
import mods.modularmachinery.MachineBuilder;

MachineBuilder.registerMachine("script_machine", "脚本机器");
val builder = MachineBuilder.getBuilder("script_machine");

builder.getBlockArrayBuilder()
    .addBlock(-1, 0, 0, "modularmachinery:blockinputbus")   // 按方块名
    .addBlock(1, 0, 0, "modularmachinery:blockoutputbus")
    .addBlock(0, -1, 0, "modularmachinery:blockcasing@0")   // @meta 指定变种
    .addBlock(0, 1, 0, <minecraft:stone>)                   // 按物品
    .addBlock(0, 0, 1, "minecraft:stone", "minecraft:cobblestone");  // 多种替选

builder.setColor(9416783);
builder.build();   // 必须调用，否则机器不注册
```

> 注意：`MachineBuilder` **没有** `setBlockArrayBuilder`，结构只能通过 `getBlockArrayBuilder()` 取得后直接 `addBlock`。改完结构务必调用 `builder.build()`，否则机器不会注册。

`addBlock` 的四种参数形态：

```zenscript
addBlock(int x, int y, int z, IBlockState... states)
addBlock(int x, int y, int z, IItemStack... stacks)
addBlock(int x, int y, int z, String... blockNames)
addBlock(int[] xList, int[] yList, int[] zList, ...)   // 批量
```

### 7.3 其它机器属性

```zenscript
.setParallelizable(true)
.setMaxParallelism(8)
.setInternalParallelism(4)
.setMaxThreads(4)
.setRequiresBlueprint(true)
.setFailureAction(RecipeFailureActions.getFailureAction("explode"))
.setColor(0x00FF00)
.setHasFactory(true)
.setFactoryOnly(false)
.setNBT(nbt)
.setPreviewNBT(nbt)
.setBlockChecker(checker)
.setControllerBoundingBox(aabb)
.addSmartInterfaceType(type)
.addCoreThread(thread)
.addSingleBlockModifier(x, y, z, blockState, "描述", modifiers...)
.addMultiBlockModifier(multiBlockModifier)
```

### 7.4 自定义机器组件

```zenscript
import mods.modularmachinery.StatedMachineComponentBuilder;

StatedMachineComponentBuilder.newBuilder("my_component")
  .setColoured(true)
  .build();
```

### 7.5 修改已有机器

```zenscript
import mods.modularmachinery.MachineBuilder;

// 直接改 JSON 定义的机器
val b = MachineBuilder.getBuilder("alloy_furnace");
b.setMaxParallelism(4);
```

---

## 8. 机器修改器

`MachineModifier` 用于修改**已存在**的机器（含 JSON 定义的机器）。

```zenscript
import mods.modularmachinery.MachineModifier;
import mods.modularmachinery.SmartInterfaceType;
import mods.modularmachinery.FactoryRecipeThread;

// 加智能接口
MachineModifier.addSmartInterfaceType("alloy_furnace",
    SmartInterfaceType.create("temperature", 300.0)
      .setHeaderInfo("温度")
      .setValueInfo("K")
      .setPriority(0));

// 并行 / 线程
MachineModifier.setMaxParallelism("alloy_furnace", 4);
MachineModifier.setInternalParallelism("alloy_furnace", 2);
MachineModifier.setMaxThreads("alloy_furnace", 8);

// 工厂核心线程
MachineModifier.addCoreThread("my_factory", FactoryRecipeThread.createCoreThread("core_1"));

// 前缀 / 模型
MachineModifier.setMachinePrefix("alloy_furnace", "prefix_name");
```

> `setMachineGeoModel` 是 1.12.2 GeckoLib 模型入口。1.7.10 没有 GeckoLib，调用不会生效。

---

## 9. 自定义升级

### 9.1 创建升级

```zenscript
import mods.modularmachinery.MachineUpgradeBuilder;
import mods.modularmachinery.RecipeModifierBuilder;
import mods.modularmachinery.MachineUpgradeHelper;

MachineUpgradeBuilder.newBuilder("my_upgrade", "我的升级", 1.0, 4)
  .addDescriptions("第一行说明", "第二行说明")
  .addCompatibleMachines("my_machine")
  .addIncompatibleMachines("other_machine")
  .addModifier(true, "speed", RecipeModifierBuilder
      .create("modularmachinery:speed", "input", 2.0, 1, false).build())
  .buildAndRegister();

// 绑定到物品
MachineUpgradeHelper.addFixedUpgrade(<minecraft:diamond>, "my_upgrade");
```

### 9.2 升级事件

升级也能挂事件，handler 类型是 `UpgradeEventHandler`：

```zenscript
import mods.modularmachinery.UpgradeEventHandlerWrapper;
import mods.modularmachinery.MachineEvent;

MachineUpgradeBuilder.newBuilder("guarded_upgrade", "守护升级", 1.0, 1)
  .addMachinePreTickHandler(function(wrapper as UpgradeEventHandlerWrapper) {
      val event = wrapper.event;
      val upgrade = wrapper.upgrade;
      // ...
  })
  .buildAndRegister();
```

`UpgradeEventHandlerWrapper` 的两个只读属性：`event`（`MachineEvent`）、`upgrade`（`MachineUpgrade`）。

可用事件挂载点：

```
addRecipeCheckHandler          addPreRecipeCheckHandler      addPostRecipeCheckHandler
addRecipeStartHandler          addRecipePreTickHandler       addRecipePostTickHandler
addRecipeTickHandler           addRecipeFailureHandler       addRecipeFinishHandler
addFactoryRecipeStartHandler   addFactoryRecipePreTickHandler
addFactoryRecipePostTickHandler addFactoryRecipeFailureHandler
addFactoryRecipeFinishHandler  addMachinePreTickHandler      addMachinePostTickHandler
addStructureFormedHandler      addStructureUpdateHandler
addControllerGUIRenderHandler  addSmartInterfaceUpdateHandler
```

### 9.3 动态升级

`DynamicMachineUpgradeBuilder` 用法与 `MachineUpgradeBuilder` 相同，区别是支持自定义描述处理器：

```zenscript
import mods.modularmachinery.DynamicMachineUpgradeBuilder;
import mods.modularmachinery.SimpleDynamicMachineUpgrade;

DynamicMachineUpgradeBuilder.newBuilder("dynamic_upgrade", "动态升级", 1.0, 8)
  .setDescriptionHandler(function(upgrade as SimpleDynamicMachineUpgrade) {
      // 用 itemData / customData 拼出动态说明（没有 level 属性，等级请自己存进 customData）
      val data = upgrade.customData;
      if (data == null) {
          return ["未初始化"];
      }
      return ["当前数值: " ~ (data.value as int)];
  })
  .buildAndRegister();
```

`SimpleDynamicMachineUpgrade` 可用成员：

| 属性 / 方法 | 类型 | 说明 |
|---|---|---|
| `itemData` | `IData` | 读写，物品 NBT 里的数据 |
| `customData` | `IData` | 读写，自定义数据 |
| `parentStack` | `IItemStack` | 只读，所属物品 |
| `decrementItemDurability(int)` | void | 扣物品耐久 |

### 9.4 查询升级

```zenscript
val up = MachineUpgradeHelper.getUpgrade("my_upgrade");
val simple = MachineUpgradeHelper.castToSimpleMachineUpgrade(up);

// 控制器上查询
if (event.controller.hasMachineUpgrade("my_upgrade")) {
    val list = event.controller.getMachineUpgrade("my_upgrade");
}
```

---

## 10. 智能接口

智能接口是机器对外暴露的数值通道，可被其它模组/脚本读写。

### 10.1 声明

```zenscript
import mods.modularmachinery.SmartInterfaceType;

val tempType = SmartInterfaceType.create("temperature", 300.0)   // 类型名, 默认值
  .setHeaderInfo("温度")        // 界面标题
  .setValueInfo("K")           // 单位
  .setFooterInfo("范围 0-2000") // 底部说明
  .setPriority(0)              // 排序优先级
  .setNotEqualMessage("温度不匹配");
```

### 10.2 挂到机器

```zenscript
// 脚本定义的机器
MachineBuilder.getBuilder("my_machine").addSmartInterfaceType(tempType);

// 已有的机器
MachineModifier.addSmartInterfaceType("alloy_furnace", tempType);
```

### 10.3 读取

```zenscript
import mods.modularmachinery.SmartInterfaceUpdateEvent;

MMEvents.onSmartInterfaceUpdate("my_machine", function(e as SmartInterfaceUpdateEvent) {
    // 注意属性名是 interfaceType，不是 type
    print("类型 " ~ e.newData.interfaceType ~ " 值 " ~ e.newData.value);
    e.newData.value = 500.0;   // value 可写，改这里会影响机器
});

// 主动读
val data = event.controller.getSmartInterfaceData("temperature");
if (data != null) {
    print(data.value);
}
```

`SmartInterfaceData` 成员：

| 属性 | 类型 | 读写 |
|---|---|---|
| `pos` | `IBlockPos` | 只读 |
| `parentMachineName` | `String` | 只读 |
| `interfaceType` | `String` | 只读 |
| `value` | `float` | **读写** |

### 10.4 配方需求

```zenscript
RecipeBuilder.newBuilder("temp_recipe", "my_machine", 100)
  .addItemInput(<minecraft:iron_ingot>)
  .addSmartInterfaceDataInput("temperature", 500.0, 1000.0)  // 要求 500~1000
  .build();
```

---

## 11. 工厂与配方线程

### 11.1 工厂机器

```zenscript
MachineBuilder.registerMachine("my_factory", "我的工厂", true, true);  // hasFactory, factoryOnly
```

### 11.2 核心线程

```zenscript
import mods.modularmachinery.FactoryRecipeThread;

val core = FactoryRecipeThread.createCoreThread("core_1")
  .addRecipe("recipe_a")
  .addRecipe("recipe_b");

MachineBuilder.getBuilder("my_factory").addCoreThread(core);
// 或对已有机器
MachineModifier.addCoreThread("my_factory", core);
```

线程属性（只读）：`isWorking`、`isIdle`、`isCoreThread`、`threadName`。

### 11.3 并行

```zenscript
.setParallelizable(true)   // 允许并行
.setMaxParallelism(8)      // 上限
.setInternalParallelism(4) // 内部并行
.setMaxThreads(4)          // 线程数
```

---

## 12. 并发与 Sync

跨线程操作切回服务端主线程：

```zenscript
import mods.modularmachinery.Sync;
import mods.modularmachinery.Action;

Sync.addSyncTask(function() {
    // 在主线程执行
} as Action);
```

| 方法 | 说明 |
|---|---|
| `Sync.addSyncTask(Action)` | 排队到主线程执行（Zen 可调用） |
| `Sync.doSyncAction(Action)` | 内部使用 |
| `Sync.executeSyncIfPresent(mutex, runnable)` | 内部使用 |

---

## 13. 完整示例脚本

```zenscript
// ============================================================
// 完整示例：带温度机制的机器 + 配方 + 事件
// 放在 .minecraft/config/minetweaker/example.zs
// ============================================================

// ---- import 必须在最上方 ----
import mods.modularmachinery.RecipeBuilder;
import mods.modularmachinery.MMEvents;
import mods.modularmachinery.MachineStructureFormedEvent;
import mods.modularmachinery.MachineTickEvent;
import mods.modularmachinery.ControllerGUIRenderEvent;
import mods.modularmachinery.RecipeCheckEvent;
import mods.modularmachinery.RecipeTickEvent;
import mods.modularmachinery.RecipeFinishEvent;
import mods.modularmachinery.RecipeFailureEvent;
import minetweaker.data.IData;

// ---- 配方 ----
RecipeBuilder.newBuilder("example_smelting", "alloy_furnace", 100)
  .addItemInput(<minecraft:iron_ore> * 2)
  .addItemOutput(<minecraft:iron_ingot>)

  .addPreCheckHandler(function(e as RecipeCheckEvent) {
      val data = e.controller.customData;
      if (data != null && (data.temperature as int) < 800) {
          e.setFailed("温度不足 800K");
      }
  })

  .addPreTickHandler(function(e as RecipeTickEvent) {
      val data = e.controller.customData;
      if (data != null && (data.temperature as int) < 800) {
          e.preventProgressing("温度过低");
      }
  })

  .addFinishHandler(function(e as RecipeFinishEvent) {
      print("[example] 配方完成");
  })

  .addFailureHandler(function(e as RecipeFailureEvent) {
      print("[example] 配方失败: " ~ e.cause);
      e.destructRecipe = false;
  })

  .build();

// ---- 事件 ----
MMEvents.onStructureFormed("alloy_furnace", function(e as MachineStructureFormedEvent) {
    e.controller.customData = { temperature: 300 } as IData;
    print("[example] 结构成型，初始温度 300K");
});

MMEvents.onMachinePreTick("alloy_furnace", function(e as MachineTickEvent) {
    val data = e.controller.customData;
    if (data == null) {
        return;
    }
    var temp = data.temperature as int;

    // 工作时升温，闲置时降温
    if (e.controller.isWorking) {
        temp = temp + 2;
    } else {
        temp = temp - 1;
    }
    if (temp < 300) { temp = 300; }
    if (temp > 2000) { temp = 2000; }

    e.controller.customData = { temperature: temp } as IData;
});

MMEvents.onControllerGUIRender("alloy_furnace", function(e as ControllerGUIRenderEvent) {
    val data = e.controller.customData;
    if (data != null) {
        e.extraInfo = ["温度: " ~ (data.temperature as int) ~ " K"];
    }
});

print("[example] 脚本加载完成");
```

---

## 14. 排错

### 14.1 日志位置

```
.minecraft/logs/minetweaker.log     ← 只包含脚本相关输出（推荐先看这个）
.minecraft/logs/latest.log          ← 完整日志
.minecraft/crash-reports/           ← 崩溃报告
```

脚本语法错误会在这里指出文件、行号和原因。

### 14.2 常见错误

| 现象 | 原因 | 处理 |
|---|---|---|
| `Could not find machine 'xxx'` | 机器名错/未加载 | 用注册名，不带 `modularmachinery:` 前缀；确认机器确实注册成功 |
| `import` 报「unresolved」 | 命名空间写错 | 按第 3 节对照表改用 `minetweaker.*` |
| 事件完全不触发 | 机器名不对 / 事件绑错类 | 检查日志是否有 `Registered N ZenScript classes`；确认机器名 |
| 配方不出现 | 缺 `.build()` | 每个配方链必须以 `.build()` 结尾 |
| `event.phase` 报错 | `Phase` 不是 Zen 类型 | 改用 `onMachinePreTick` / `onMachinePostTick` |
| `customData` 读不到 | 结构未成型/未初始化 | 在 `onStructureFormed` 里初始化 |
| 改了脚本没生效 | 未重载 | `/mt reload` |

### 14.3 确认模组已注册 Zen 类

服务器启动日志里应出现：

```
[ModularMachinery] Registered N ZenScript classes to MineTweaker3.
```

以及脚本加载完成：

```
[ModularMachinery] Scripts loaded, N recipes registered.
```

若 Number 为 0，说明 CraftTweaker 未安装或加载顺序有问题。

### 14.4 确认事件已绑定

绑定发生在脚本加载完之后。若日志里出现 `Could not find machine`，说明这条事件注册被丢弃了。

---

## 15. 与 1.12.2 的差异

### 15.1 完全一致的部分

- `MMEvents` 的全部注册方法（名称、参数、语义）
- 全部 14 个事件类及其成员
- `RecipePrimer` / `MachineBuilder` / `MachineModifier` 的方法集合
- 配方级事件的 13 个注册方法

### 15.2 不支持的部分

| 项 | 说明 |
|---|---|
| `ControllerModelGetEvent` | 1.12.2 GeckoLib 模型事件，1.7.10 无 GeckoLib |
| `ControllerModelAnimationEvent` | 同上 |
| `MachineModifier.setMachineGeoModel` | 存在但无实际作用（需 GeckoLib） |
| `crafttweaker.*` 大部分类型 | 改名为 `minetweaker.*`，见第 3.4 节 |
| `crafttweaker.util.Math` | 不存在 |
| `crafttweaker.event.*`（实体类） | 不存在，改用 MineTweaker3 的 `PlayerXxxEvent` |

### 15.3 写法差异

1.12.2：

```zenscript
events.onPlayerLoggedIn(function(event) { ... });
```

1.7.10 使用 MineTweaker3 的 `events`：

```zenscript
import minetweaker.api.event.PlayerLoggedInEvent;
events.onPlayerLoggedIn(function(event as PlayerLoggedInEvent) {
    print(event.player.name);
});
```

事件参数建议**始终写明类型**（`function(e as XxxEvent)`），1.7.10 的 ZenScript 对隐式类型推断支持较弱。

---

## 附：Zen 类全表

本模组注册的全部 Zen 类（`mods.modularmachinery.*`）：

```
Action                        ActiveMachineRecipe          AdvancedBlockChecker
AdvancedItemCheckerCT         AdvancedItemModifier         BlockArray
BlockArrayBuilder             ControllerGUIRenderEvent     DynamicMachineUpgradeBuilder
FactoryRecipeEvent            FactoryRecipeFailureEvent    FactoryRecipeFinishEvent
FactoryRecipeStartEvent       FactoryRecipeThread          FactoryRecipeTickEvent
IBlockStatePredicate          IDynamicPatternInfo          IFunction
IMachineController            IngredientArrayBuilder       IngredientArrayPrimer
MachineBuilder                MachineController            MachineEvent
MachineModifier               MachineRecipeThread          MachineRegistry
MachineStructureFormedEvent   MachineStructureUpdateEvent  MachineTickEvent
MachineUpgrade                MachineUpgradeBuilder        MachineUpgradeHelper
MMEvents                      MultiblockModifierBuilder    MultiBlockModifierReplacement
RecipeAdapterBuilder          RecipeBuilder                RecipeCheckEvent
RecipeEvent                   RecipeFailureActions         RecipeFailureEvent
RecipeFinishEvent             RecipeModifier               RecipeModifierBuilder
RecipePrimer                  RecipeStartEvent             RecipeThread
RecipeTickEvent               RegistryUpgrade              SimpleDynamicMachineUpgrade
SimpleMachineUpgrade          SmartInterfaceData           SmartInterfaceType
SmartInterfaceUpdateEvent     StatedMachineComponentBuilder Sync
TaggedPositionBlockArray      UpgradeEventHandler          UpgradeEventHandlerWrapper
```

另有 shim：`crafttweaker.world.IWorld`、`crafttweaker.world.IBlockPos`、`crafttweaker.world.IFacing`、`crafttweaker.block.IBlockState`、`crafttweaker.block.IBlockStateMatcher`、`crafttweaker.util.IAxisAlignedBB`。
