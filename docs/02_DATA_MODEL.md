# 02 · 数据模型

数据分三类，先分清再谈 schema：

| 类别 | 形态 | 生命周期 | 载体 |
| --- | --- | --- | --- |
| 世界内容 | 静态 | 随包发布 / 可热更新 | JSON 文件 |
| 玩家存档 | 动态 | 随游戏推进变化 | Room |
| 运行时状态 | 内存 | 从内容 + 存档 + 时钟推导 | 领域层对象 |

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
  "tags": ["深夜", "怀旧"]
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
  "api": { "enabled": false, "base_url": "", "model": "" }
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
  "used_in": ["mushroom_brew"]
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
  "may_fail": true
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
    "tasks": "tasks.json",
    "items": "items.json",
    "recipes": "recipes.json",
    "events": "events.json",
    "rumors": "rumors.json",
    "festivals": "festivals.json"
  }
}
```

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

所有 `refId` 指向世界内容 JSON 的 `id`，存档本身不复制内容字段。

## 4. 运行时状态（领域层对象）

```text
WorldState   { time, weather, season, date }
DerivedState { visibleEvents, activeRumors, npcPositions, spawnables }  // 触发引擎算出的"当前世界"
GameState    { WorldState + PlayerProfile + 图鉴 + 关系 + 存档聚合 }
UiState      { 每屏一个，Project 自 GameState 的只读快照 }
```

`DerivedState` 由触发引擎基于 WorldState + PlayerProfile 动态计算，每次世界状态变化重算一次。

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