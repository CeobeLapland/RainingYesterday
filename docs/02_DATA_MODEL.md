# 02 · 数据模型

数据分三类，先分清再谈 schema（另有第四种来源——真实信号，见 2.12）：

| 类别 | 形态 | 生命周期 | 载体 |
| --- | --- | --- | --- |
| 世界内容 | 静态 | 随包发布 / 可热更新 | JSON 文件 |
| 玩家存档 | 动态 | 随游戏推进变化 | Room |
| 运行时状态 | 内存 | 从内容 + 存档 + 时钟推导 | 领域层对象 |
| 真实信号 | 外部 / 实时 | 实时获取 / 可本地模拟 | 外部 API + 本地兜底 + 内存缓存 |

## 1. 通用条件 Condition

触发引擎的统一输入。一个条件是一次对世界/玩家状态的谓词判断。

```jsonc
// 单一条件
{ "kind": "time_range", "value": { "from": "21:50", "to": "23:00" } }

// 组合条件
{ "all": [ {cond}, {cond} ] }   // AND
{ "any": [ {cond}, {cond} ] }   // OR
```

### kind 一览

| kind | value 结构 | 说明 |
| --- | --- | --- |
| `time_range` | `{ from, to }` | 当日时间区间（"HH:mm"） |
| `weather` | `"rain"` | 天气 |
| `season` | `"autumn"` | 季节 |
| `near` | `{ place, radius_m }` | 距某地点半径内 |
| `exploration` | `{ min }` | 探索度 ≥ min（0~1） |
| `relationship` | `{ npc, min }` | 熟悉度 ≥ min |
| `has_item` | `{ item, min }` | 持有物数量 ≥ min |
| `day_of_week` | `[1,3,5]` | 星期（1=周一） |
| `date` | `{ month, day }` | 具体日期（节日） |

## 2. 世界内容 JSON

### 2.1 地点 place

```jsonc
{
  "id": "old_hall",               // 唯一 ID，全站蛇形命名
  "name": "旧礼堂",
  "type": "building",             // building|nature|public|hidden
  "lat": 31.2000,
  "lng": 121.4000,
  "radius_m": 40,                 // 进入该半径算"发现"
  "built_year": 1987,
  "former_name": "老礼堂",
  "history": [                    // 数组索引 = 解锁层级
    "这座礼堂建于 1987 年。",
    "2019 年经历过最后一次翻修。"
  ],
  "resources": ["rain_mushroom"], // 可采集资源 item id
  "linked_npcs": ["guard_li"],
  "linked_rumors": ["gym_night_sound"],
  "tags": ["深夜", "怀旧"],
  "ar": {
    "asset": "old_hall_past",
    "anchor_mode": "device_relative",
    "billboard": true
  }
}
```

### 2.2 人物 npc

```jsonc
{
  "id": "guard_li",
  "name": "李叔",
  "role": "老门卫",
  "home": "guard_room",
  "schedule": [
    { "from": "07:30", "to": "12:00", "place": "teaching_a", "chance": 0.9 },
    { "from": "12:00", "to": "13:00", "place": "canteen",     "chance": 1.0 }
  ],
  "dialogue": [
    { "min_familiarity": 0, "lines": ["晚上好。", "又见面了。"] },
    { "min_familiarity": 3, "lines": ["你知道这片操场以前是什么吗？"] }
  ],
  "gifts": { "liked": ["tea"], "disliked": ["junk"] },
  "api": { "enabled": false, "base_url": "", "model": "" },
  "ar": { "asset": "npc_guard_li", "anchor_mode": "geo_anchor", "lat": 31.2000, "lng": 121.4000 }
}
```

`api` 是玩家可配置项：接入外部 LLM 时填写，否则用 `dialogue` 预设兜底。两条轨共用同一数据结构。

### 2.3 任务 task（normal 与 anomaly 共用）

```jsonc
{
  "id": "anomaly_old_hall_night",
  "name": "旧礼堂的夜声",
  "type": "anomaly",                 // normal | anomaly
  "source": "rumor:gym_night_sound", // 来源：npc:xxx | rumor:xxx | world
  "unlock": [
    { "kind": "time_range", "value": { "from": "21:50", "to": "23:00" } },
    { "kind": "exploration", "value": { "min": 0.6 } }
  ],
  "objectives": [
    { "kind": "visit", "place": "old_hall" },
    { "kind": "collect", "item": "sound_recorder", "count": 1 }
  ],
  "allow_empty": true,
  "rewards": {
    "exploration": 10,
    "commemorative": 5,
    "reputation": 3,
    "unlock_history": "old_hall",
    "unlock_rumor": "statue_no_name"
  }
}
```

约束：`type == "anomaly"` 的任务必须带 `unlock`（时空 + 探索度门槛），不可被玩家无限接取；`allow_empty` 允许蹲守落空。

### 2.4 物品 item

```jsonc
{
  "id": "rain_mushroom",
  "name": "雨夜菌",
  "category": "material",      // material|knowledge|memory|culture|trace|story
  "rarity": "rare",            // common|uncommon|rare|epic|mystic
  "spawn_places": ["old_hall", "lake_side"],
  "spawn_condition": [
    { "kind": "weather", "value": "rain" },
    { "kind": "time_range", "value": { "from": "18:00", "to": "23:00" } }
  ],
  "used_in": ["mushroom_brew"],
  "ar": {}
}
```

### 2.5 配方 recipe

```jsonc
{
  "id": "mushroom_brew",
  "name": "菌酿",
  "machine": "brew_keg",              // 需要的加工台 id
  "inputs": { "rain_mushroom": 2 },
  "output": { "item": "mushroom_brew", "count": 1 },
  "duration_hours": 8,
  "condition": []                      // 可选：雨天加速等
}
```

### 2.6 随机事件 event

```jsonc
{
  "id": "cat_follow",
  "name": "跟着你的猫",
  "tier": "rare",                    // common|uncommon|rare|epic|mystic
  "trigger": [
    { "kind": "near", "value": { "place": "library", "radius_m": 100 } }
  ],
  "effects": [
    { "kind": "unlock_place", "place": "hidden_alley" },
    { "kind": "gain_item", "item": "cat_hair", "count": 1 }
  ],
  "text": "一只猫跟了你一段路。"
}
```

### 2.7 传闻 rumor

```jsonc
{
  "id": "gym_night_sound",
  "name": "体育馆夜声",
  "text": "有人说，晚上十点后旧体育馆附近会听见奇怪声音。",
  "condition": [
    { "kind": "time_range", "value": { "from": "21:50", "to": "23:00" } },
    { "kind": "near", "value": { "place": "gym", "radius_m": 120 } }
  ],
  "resolve": "anomaly:gym_night_sound",
  "may_fail": true,
  "evidence": "gym_night_sound_photo"
}
```

### 2.8 主题 theme（地图包）

一个主题 = 一组内容文件的清单，用于换主题图 / 玩家导入。

```jsonc
{
  "id": "campus_default",
  "name": "默认校园",
  "version": 1,
  "files": {
    "places": "places.json",
    "npcs": "npcs.json",
    "creatures": "creatures.json",
    "tasks": "tasks.json",
    "items": "items.json",
    "recipes": "recipes.json",
    "events": "events.json",
    "rumors": "rumors.json",
    "rituals": "rituals.json",
    "festivals": "festivals.json",
    "tools": "tools.json"
  }
}
```

### 2.9 生灵 creature

GDD 7.11 宠物/友善生灵的世界内容侧（校园里可发现/领养的生灵清单）。玩家领养后的养成状态存 Room `PetState`（见 §3），不复制内容字段。

```jsonc
{
  "id": "campus_cat",
  "name": "橘猫",
  "species": "cat",                     // cat | bird | squirrel | ...
  "home_places": ["library", "canteen"],
  "rarity": "uncommon",                 // 见枚举 Rarity
  "found_condition": [                  // 出现条件，复用 Condition
    { "kind": "time_range", "value": { "from": "17:00", "to": "19:00" } }
  ],
  "gifts": { "liked": ["dried_fish"] },
  "ar": { "asset": "creature_cat", "anchor_mode": "device_relative", "billboard": true }
}
```

### 2.10 地点微仪式 ritual

GDD 7.30 地点微仪式的数据落地：玩家主动触发的地点级轻事件，产出记忆节点/足迹/图鉴，不做数值膨胀。

```jsonc
{
  "id": "sit_by_lake",
  "name": "在湖边坐一会儿",
  "place": "lake_side",
  "action": "sit",                       // sit | photo | watch | pick | ...
  "cooldown_days": 0,                    // 0 = 一次性
  "effects": [
    { "kind": "create_memory", "text": "在湖边坐了一会儿。" },
    { "kind": "gain_item", "item": "ginkgo_leaf", "count": 1 }
  ]
}
```

### 2.11 AR 通用字段（P2 占位）

`ar` 是可选对象，出现在 `place` / `npc` / `item` / `creature` 上，缺省即不进 AR 渲染。MVP 只解析、不渲染。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `asset` | string | 虚拟物资源 id（2D 贴片或低模 3D 占位） |
| `anchor_mode` | string | `geo_anchor`（锚定真实经纬度，需 `lat`/`lng`）或 `device_relative`（相对设备摆放，会漂移） |
| `lat` / `lng` | number \| null | `anchor_mode=geo_anchor` 时必填 |
| `scale` | number | 缩放，默认 1.0 |
| `billboard` | bool | true=面向相机的 2D 贴片；false=低模 3D |

P2 实现时把 `anchor_mode` 落成 Kotlin 枚举（`ArAnchorMode`，见 §5），在此之前解析层用字符串接收即可，不硬编码魔法值到领域层。

### 2.12 真实信号（外部源，非 JSON 内容）

真实天气 / 季节日照 / 校园动态是"外部信号"，不进世界内容 JSON，也不进 Room 存档，由领域层通过 `SignalSource` 接口读取（真实实现或本地模拟实现）。此处只约定数据形状：

```text
WeatherSignal    { code: sunny|cloudy|rain|snow }        // 对齐枚举 Weather
DaylightSignal   { sunrise: "HH:mm", sunset: "HH:mm" }
CampusSignal     { events: [ { id, text, place? } ] }    // 校园动态，可人工维护兜底
```

外部获取失败由数据层降级到本地模拟，绝不中断游戏（见 `03` §6）。

### 2.13 地点微仪式 · festival（新增，GDD 7.22 节日）

校园真实节点（开学、樱花季、银杏季、毕业季），作为周期性事件注入限定资源 / NPC 聚集 / 限定内容，由人工维护。

```jsonc
{
  "id": "sakura_season",
  "name": "樱花季",
  "start": { "month": 3, "day": 20 },
  "end": { "month": 4, "day": 10 },
  "conditions": [],                  // 额外的周期/年份限定（可选，多数留空）
  "resource_bonuses": ["ginkgo_leaf"], // 期间可采集/出现的限定资源
  "npc_gathering": ["librarian_lin"],  // 周年聚集的 NPC
  "limited_content": ["rare_paint"],   // 限定内容（物品/任务 filtered unknowable here, data置顶）
  "text": "校园樱花开了，树下游人如织。"
}
```

### 2.14 工具升级线 · tool（新增，GDD 7.10）

工具是探索的"效率成长线"，用探索材料升级，反哺探索效率。工具本身是内容（哪几把、升级档位、材料需求、效果），玩家当前的工具等级存 Room（见 §3 备注）。

```jsonc
{
  "id": "map_detector",
  "name": "地图探测器",
  "tiers": [
    { "level": 1, "name": "初代探测器", "effect": { "fog_radius_m": 15 } },
    { "level": 2, "name": "改良探测器", "effect": { "fog_radius_m": 25 }, "cost": { "copper": 3 } },
    { "level": 3, "name": "高精度探测器", "effect": { "fog_radius_m": 40 }, "cost": { "iron": 2, "copper": 5 } }
  ]
}
```

`effect` 采用可扩展的键值对结构（`fog_radius_m`、`collect_capacity`、`fish_eff` 等），由领域引擎按需读取，不做死 schema；`cost` 为 required 物品（缩略），完整版可为 `{ itemId: count }`。

> 工具升级状态存 Room `ToolState { toolId, level }`（见 §3），不复制内容字段。

### 2.15 新增内容类型占位说明（festival / tool）

> 编辑提示：`tool` 与 `festival` 为 v2 新增内容类型。若主题 JSON 缺这两个文件，ContentRepository 应优雅跳过（`files` 不强制全部存在），保证旧主题包兼容。

## 3. 玩家存档 Room 实体

| 实体 | 关键字段 | 说明 |
| --- | --- | --- |
| `PlayerProfile` | exploration, commemorative, reputation | 探索度 + 三币 |
| `FogCell` | hexId, explored | 六边形迷雾 |
| `DiaryEntry` | lat, lng, timestamp, text, photoPath | 记忆节点 / 日记 |
| `InventoryItem` | itemId, count | 背包 |
| `Relationship` | npcId, familiarity, level | 熟悉度 |
| `CompletionRecord` | refType, refId, doneAt | 任务/配方完成标记 |
| `EncyclopediaEntry` | refType, refId, unlockedAt | 图鉴条目 |
| `PlantSlot` | itemId, plantedAt, progress | 种植台 |
| `FishPool` | fishId, count | 鱼池 |
| `PetState` | creatureId, affinity | 宠物 / 友善生灵 |
| `BuildSlot` | x, y, machineId | 我的空间布局 |
| `PlayerSettings` | showOthers, locationMode, npcApi... | 单机/多人及定位等 |
| `ToolState` | toolId, level | 工具升级线当前等级（2.14） |

所有 `refId` 指向世界内容 JSON 的 `id`，存档本身不复制内容字段。

## 4. 运行时状态（领域层对象）

```text
WorldState   { time, weather, season, date, daylight }        // daylight 来自真实信号
DerivedState { visibleEvents, activeRumors, npcPositions, spawnables, availableRituals, arVisible }  // 触发引擎算出的"当前世界"
GameState    { WorldState + PlayerProfile + 图鉴 + 关系 + 存档聚合 }
UiState      { 每屏一个，Project 自 GameState 的只读快照 }
```

`DerivedState` 由触发引擎基于 WorldState + PlayerProfile + 真实信号动态计算，每次世界状态变化重算一次。`availableRituals` 是当前地点可执行的微仪式；`arVisible` 是按地点与条件筛出的"该处应出现的虚拟物"（P2 才渲染，MVP 只算不展示）。

## 5. 枚举

| 枚举 | 取值 |
| --- | --- |
| PlaceType | building, nature, public, hidden |
| ItemCategory | material, knowledge, memory, culture, trace, story |
| Rarity | common, uncommon, rare, epic, mystic |
| TaskType | normal, anomaly |
| EventTier | common, uncommon, rare, epic, mystic |
| Weather | sunny, cloudy, rain, snow |
| Season | spring, summer, autumn, winter |
| ArAnchorMode | geo_anchor, device_relative |