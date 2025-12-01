# StateBus - 状态总线

一个生产级别的 Compose 状态管理工具，用于页面间状态传递和共享。

## 特性

- ✅ **自动追踪监听者** - 自动记录有多少个 Composable 在监听状态
- ✅ **自动资源清理** - 当监听者数量为 0 时，自动移除状态
- ✅ **线程安全** - 使用 `ConcurrentHashMap` 和 `AtomicInteger`，支持多线程访问
- ✅ **配置更改恢复** - 支持屏幕旋转和进程死亡后的状态恢复
- ✅ **零学习成本** - API 简单直观，易于上手

## 快速开始

### 方式一：使用 CompositionLocal（推荐）

```kotlin
@Composable
fun MyApp() {
    // 在根级别提供 StateBus
    ProvideStateBus {
        MaterialTheme {
            NavHost(...)
        }
    }
}

// 在任意子组件中使用
@Composable
fun HomeScreen() {
    val stateBus = LocalStateBus.current
    val person = stateBus.observeState<Person?>()

    // 使用 person
}
```

### 方式二：手动传递（适用于局部使用）

```kotlin
@Composable
fun MyApp() {
    // 创建 StateBus（会在配置更改时保留状态）
    val stateBus = rememberStateBus()

    // 手动传递给需要的组件
    MyNavHost(stateBus)
}
```

### 2. 监听状态

```kotlin
@Composable
fun HomeScreen(stateBus: StateBus) {
    // 自动注册为监听者
    val person = stateBus.observeState<Person?>()

    Column {
        if (person != null) {
            Text("Name: ${person.name}")
            Text("Age: ${person.age}")
        } else {
            Text("No data")
        }

        Button(onClick = { /* 导航到编辑页面 */ }) {
            Text("Edit Person")
        }
    }
}

// 当 HomeScreen 离开时，自动取消注册
// 如果这是最后一个监听者，状态会被自动清理
```

### 3. 设置状态

```kotlin
@Composable
fun EditPersonScreen(stateBus: StateBus, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    Column {
        TextField(value = name, onValueChange = { name = it })
        TextField(value = age, onValueChange = { age = it })

        Button(
            onClick = {
                // 设置状态（所有监听者会收到更新）
                val person = Person(name = name, age = age.toIntOrNull() ?: 0)
                stateBus.setState<Person>(person)

                // 返回上一页
                onBack()
            }
        ) {
            Text("Save")
        }
    }
}
```

## 核心概念

### 监听者追踪

StateBus 会自动追踪每个状态的监听者数量：

```kotlin
// 页面 A 进入
@Composable
fun PageA() {
    val person = stateBus.observeState<Person?>()
    // 监听者计数: 1
}

// 页面 B 进入（页面 A 在后台）
@Composable
fun PageB() {
    val person = stateBus.observeState<Person?>()
    // 监听者计数: 2
}

// 页面 B 离开
// 监听者计数: 1

// 页面 A 离开
// 监听者计数: 0 → 自动清理状态 🗑️
```

### 自动清理机制

```kotlin
// 工作流程
1. Composable 进入 → observeState() → 监听者计数 +1
2. Composable 离开 → DisposableEffect.onDispose → 监听者计数 -1
3. 计数变为 0 → 自动删除状态数据
```

### 线程安全

```kotlin
// 可以从任何线程设置状态
viewModelScope.launch(Dispatchers.IO) {
    val data = fetchDataFromNetwork()
    stateBus.setState<Data>(data)  // ✅ 线程安全
}

// Composable 在主线程监听
@Composable
fun MyScreen() {
    val data = stateBus.observeState<Data?>()  // ✅ 线程安全
}
```

## API 文档

### 创建 StateBus

```kotlin
@Composable
fun rememberStateBus(): StateBus
```

创建一个会记住配置更改的 StateBus。

### 监听状态

```kotlin
@Composable
inline fun <reified T> observeState(stateKey: String = T::class.toString()): T?
```

观察状态，自动追踪监听者。当 Composable 进入时自动注册，离开时自动取消注册。

**参数**：
- `stateKey` - 状态的唯一标识，默认使用类型名称

**返回**：
- 状态值，如果不存在则返回 `null`

**注意事项**：
- ⚠️ 不要在 `LazyColumn`/`LazyRow` 的 `item` 中直接调用
- ✅ 应该在外层调用，然后传递给 `item`

### 设置状态

```kotlin
inline fun <reified T> setState(stateKey: String = T::class.toString(), state: T)
```

设置状态，线程安全。所有监听此状态的 Composable 会自动重组。

**参数**：
- `stateKey` - 状态的唯一标识
- `state` - 状态值

### 移除状态

```kotlin
inline fun <reified T> removeState(stateKey: String = T::class.toString()): Boolean
```

手动移除状态（不管是否有监听者）。

**返回**：
- 是否成功移除

**注意**：一般情况下不需要手动调用，系统会在没有监听者时自动清理。

### 查询方法

```kotlin
// 获取监听者数量
fun getListenerCount(stateKey: String): Int

// 获取所有监听者数量
fun getAllListenerCounts(): Map<String, Int>

// 获取所有状态的 key
fun getAllKeys(): Set<String>

// 检查状态是否存在
fun hasState(stateKey: String): Boolean
```

## 使用场景

### 场景 1: 页面间传递编辑结果 ✅

```kotlin
@Composable
fun MyApp() {
    val stateBus = rememberStateBus()
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(stateBus) {
                navController.navigate("edit")
            }
        }

        composable("edit") {
            EditScreen(stateBus) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
fun HomeScreen(stateBus: StateBus, onEdit: () -> Unit) {
    val person = stateBus.observeState<Person?>()

    Column {
        Text("Person: ${person?.name ?: "No data"}")
        Button(onClick = onEdit) { Text("Edit") }
    }
}

@Composable
fun EditScreen(stateBus: StateBus, onBack: () -> Unit) {
    Button(
        onClick = {
            stateBus.setState<Person>(Person("John", 30))
            onBack()
        }
    ) {
        Text("Save")
    }
}
```

### 场景 2: 多个组件共享状态 ✅

```kotlin
@Composable
fun Dashboard(stateBus: StateBus) {
    // 3 个组件同时监听
    Column {
        // 顶部工具栏
        TopAppBar {
            val user = stateBus.observeState<User?>()
            Text("Welcome, ${user?.name}")
        }

        // 主内容
        MainContent {
            val user = stateBus.observeState<User?>()
            ProfileCard(user)
        }

        // 底部导航
        BottomNav {
            val user = stateBus.observeState<User?>()
            if (user?.hasNotifications == true) {
                Badge()
            }
        }
    }

    // 监听者数量: 3
    // 当 Dashboard 离开时 → 3 → 2 → 1 → 0 → 自动清理 ✅
}
```

### 场景 3: 配置更改恢复 ✅

```kotlin
@Composable
fun MyScreen(stateBus: StateBus) {
    val data = stateBus.observeState<UserData?>()

    // 用户旋转屏幕
    // ✅ stateBus 通过 rememberSaveable 保留
    // ✅ data 会自动恢复
    // ✅ 监听者会自动重新注册
}
```

## 最佳实践

### ✅ 推荐做法

```kotlin
// 1. 在需要时才创建和监听
@Composable
fun MyScreen(stateBus: StateBus) {
    if (userLoggedIn) {
        val settings = stateBus.observeState<Settings?>()
        // 使用 settings
    }
}

// 2. 在外层监听，传递给子组件
@Composable
fun ParentScreen(stateBus: StateBus) {
    val data = stateBus.observeState<Data?>()  // ✅ 在外层

    LazyColumn {
        items(list) { item ->
            ChildComponent(data, item)  // ✅ 传递数据
        }
    }
}

// 3. 使用类型安全的 API
data class UserProfile(val name: String, val age: Int)

val profile = stateBus.observeState<UserProfile?>()  // ✅ 类型安全
```

### ❌ 避免做法

```kotlin
// 1. 不要在 LazyColumn item 中直接监听
LazyColumn {
    items(users) { user ->
        val data = stateBus.observeState<Data?>()  // ❌ 错误！
        UserCard(data, user)
    }
}

// 2. 不要在非 Composable 函数中调用 observeState
fun processData(stateBus: StateBus) {
    val data = stateBus.observeState<Data?>()  // ❌ 编译错误！
}

// 3. 不要频繁改变 stateKey
@Composable
fun MyScreen(stateBus: StateBus) {
    var key by remember { mutableStateOf("key1") }
    val data = stateBus.observeState<Data>(stateKey = key)
    // ❌ 每次 key 改变都会重新注册
}
```

## 性能考虑

### 内存开销

| 操作 | 内存开销 |
|------|---------|
| 存储一个状态 | 1 个 `MutableState` + 1 个 `AtomicInteger` (约 ~50 bytes) |
| 每个监听者 | 仅计数器 +1 (4 bytes) |

**结论**: 内存开销极小，可以忽略不计。

### CPU 开销

| 操作 | 时间复杂度 | 说明 |
|------|-----------|------|
| `observeState()` | O(1) | Map 查找 |
| `setState()` | O(1) | Map 查找 + 赋值 |
| 注册监听者 | O(1) | 原子递增 |
| 取消注册 | O(1) | 原子递减 + Map 删除 |

**结论**: 所有操作都是 O(1)，性能开销可以忽略。

### 线程安全开销

使用 `ConcurrentHashMap` 和 `AtomicInteger` 带来的开销：
- 读操作：几乎零开销（无锁读取）
- 写操作：< 1% 开销（CAS 操作）

**结论**: 线程安全带来的性能开销微乎其微。

## 与其他方案对比

### StateBus vs ViewModel

| 特性 | StateBus | ViewModel |
|------|---------|-----------|
| 适用场景 | 轻量级状态传递 | 复杂业务逻辑 |
| 学习成本 | 低 | 中 |
| 自动清理 | ✅ | ❌ 需手动 |
| 配置更改 | ✅ | ✅ |
| 线程安全 | ✅ | 取决于实现 |

**推荐**:
- 简单的页面间结果传递 → StateBus
- 复杂的业务逻辑和数据处理 → ViewModel

### StateBus vs EventBus

| 特性 | StateBus | EventBus |
|------|----------|----------|
| 消费模式 | 状态（多次读取） | 事件（单次消费） |
| 订阅者数量 | 多个 | 多个 |
| 自动清理 | ✅ | ❌ |
| 适用场景 | 持久化状态 | 一次性通知 |

**推荐**:
- 需要持久化和多次读取 → StateBus
- 一次性事件通知 → EventBus

### StateBus vs CompositionLocal

| 特性 | StateBus | CompositionLocal |
|------|----------|------------------|
| 作用域 | 灵活（可全局可局部） | 组合树作用域 |
| 跨页面传递 | ✅ | ❌ |
| 自动清理 | ✅ | ✅ |
| 学习成本 | 低 | 中 |

**推荐**:
- 跨页面状态传递 → StateBus
- 主题、配置等组合树内共享 → CompositionLocal

## 故障排查

### Q: 为什么我的状态被意外清理了？

**A**: 检查是否所有监听者都离开了。

```kotlin
// 检查监听者数量
val count = stateBus.getListenerCount(Person::class.toString())
Log.d("MyApp", "Listener count: $count")
```

如果计数为 0，说明没有 Composable 在监听，状态会被自动清理。

### Q: 为什么我的状态没有被清理？

**A**: 可能还有监听者存在。常见原因：

1. 某个 Composable 仍然在显示
2. 后台页面仍在监听
3. 检查所有监听者

```kotlin
val counts = stateBus.getAllListenerCounts()
Log.d("MyApp", "All listeners: $counts")
```

### Q: 可以在多线程中使用吗？

**A**: 完全可以。StateBus 是线程安全的。

```kotlin
// ✅ 从后台线程设置状态
viewModelScope.launch(Dispatchers.IO) {
    val data = fetchData()
    stateBus.setState<Data>(data)
}

// ✅ 在主线程监听
@Composable
fun MyScreen() {
    val data = stateBus.observeState<Data?>()
}
```

### Q: 进程死亡后会发生什么？

**A**: StateBus 会通过 `rememberSaveable` 自动恢复状态数据。

监听者计数策略：
- 恢复时监听者计数总是从 0 开始
- 当 Composable 重新创建时，会自动重新注册监听者
- 这样可以确保计数的准确性，避免累积错误

### Q: Navigation 3 的 NavDisplay 会触发 DisposableEffect.onDispose 吗？

**A**: 会的！经过实际测试验证：

**Navigation 3 的行为**：
- 前进导航时（PageA → PageB）：PageA 的 Composable 会被销毁，触发 `DisposableEffect.onDispose`
- 返回导航时（PageB → PageA）：PageA 会被重新创建，DisposableEffect 重新执行

**对 StateBus 的影响**：
```kotlin
PageA: observeState() → count = 1
导航到 PageB:
  - PageB observeState() → count = 2
  - PageA 销毁 → unregister() → count = 1  ✅ 正确
返回 PageA:
  - PageA 重新创建 → observeState() → count = 2
  - PageB 销毁 → unregister() → count = 1  ✅ 正确
```

**结论**：StateBus 的自动清理机制在 Navigation 3 中完全安全，计数始终准确。

**性能影响**：
- Navigation 3 的 NavDisplay 会在导航时销毁后台页面
- 这意味着页面状态需要使用 `rememberSaveable` 保存
- 会有额外的创建/销毁开销

**与 Navigation Compose 2.x 的区别**：
| 导航库 | 后退栈中的页面 | DisposableEffect 行为 |
|--------|---------------|---------------------|
| Navigation 2.x | 保留 Composable | 保留在栈中时不触发 onDispose |
| Navigation 3 NavDisplay | 销毁 Composable | 立即触发 onDispose |

两种导航库下 StateBus 都是安全的 ✅

### Q: 在 LazyColumn 中使用有什么注意事项？

**A**: 不要在 item 中直接调用 `observeState()`。

```kotlin
// ❌ 错误
LazyColumn {
    items(list) { item ->
        val data = stateBus.observeState<Data?>()  // 每个 item 都注册！
    }
}

// ✅ 正确
LazyColumn {
    val data = stateBus.observeState<Data?>()  // 在外层注册一次

    items(list) { item ->
        ItemContent(data, item)  // 传递数据
    }
}
```

## 示例代码

完整的示例代码请参考：
- [基础用法示例](examples/BasicExample.kt)
- [多监听者示例](examples/MultiListenerExample.kt)

## 版本历史

### v1.0 (2024-11-26)
- ✅ 初始版本
- ✅ 自动监听者追踪
- ✅ 自动资源清理
- ✅ 线程安全
- ✅ 配置更改恢复

## 许可证

Apache License 2.0
