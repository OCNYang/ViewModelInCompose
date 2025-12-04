# ViewModelInCompose

[![](https://jitpack.io/v/OCNYang/ViewModelInCompose.svg)](https://jitpack.io/#OCNYang/ViewModelInCompose)

[English](README.md)

一个为 Compose Multiplatform 提供增强 ViewModel 工具的 Kotlin 多平台库。

## 支持平台

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## 功能

- **LaunchedEffectOnce**：在页面生命周期内只执行一次副作用
- **EventEffect**：生命周期感知的一次性事件处理
- **StateBus**：跨页面状态共享，自动管理生命周期
- **SharedViewModel**：跨多个页面共享 ViewModel，自动清理

## 安装

在 `settings.gradle.kts` 中添加 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        // ...
        maven("https://jitpack.io")
    }
}
```

在模块的 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation("com.github.OCNYang:ViewModelInCompose:<version>")
}
```

> 将 `<version>` 替换为上方徽章显示的最新版本，或查看 [releases](https://github.com/OCNYang/ViewModelInCompose/releases)。

---

## LaunchedEffectOnce

在页面生命周期内只执行一次的 `LaunchedEffect`，不受重组和配置变更影响。当跳转到下一页面并返回时不会重新执行，只会在退出当前页面后重新打开时执行。

### 适用场景

- 一次性初始化（数据加载、埋点上报）
- 不应在重组或屏幕旋转时重复执行的副作用

### 与标准 LaunchedEffect 对比

| 行为       | LaunchedEffect | LaunchedEffectOnce |
|----------|-------------|-------------------|
| 重组时      | 可能重新执行 | 不会重新执行 |
| 从下一页面返回时 | 重新执行 | 不会重新执行 |
| 配置变更时    | 重新执行 | 不会重新执行 |
| 页面重建时    | 重新执行 | 重新执行 |

### 用法

```kotlin
@Composable
fun MyScreen() {
    // 页面生命周期内只执行一次
    LaunchedEffectOnce {
        viewModel.loadData()
        analytics.trackPageView()
    }
}
```

### 使用 Key

```kotlin
@Composable
fun UserScreen(userId: String) {
    // 仅当 userId 变化时重新执行
    LaunchedEffectOnce(userId) {
        viewModel.loadUser(userId)
    }
}
```

---

## EventEffect

生命周期感知的事件收集，用于处理来自 ViewModel 的一次性 UI 事件。

### 适用场景

- 导航事件
- 显示 Toast 或 Snackbar
- ViewModel 触发的一次性 UI 操作

### 特性

- 生命周期感知：仅在 UI 可见时收集事件
- 事件保证送达（配置变更时不会丢失）
- 每个事件只消费一次

### 用法

#### 1. 在 ViewModel 中定义事件

```kotlin
class MyViewModel : ViewModel() {
    private val _events = EventChannel<UiEvent>()
    val events: Flow<UiEvent> = _events.flow

    fun onSubmit() {
        viewModelScope.launch {
            _events.send(UiEvent.ShowToast("成功！"))
            _events.send(UiEvent.NavigateBack)
        }
    }
}

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data object NavigateBack : UiEvent
}
```

#### 2. 在 Composable 中收集事件

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    EventEffect(viewModel.events) { event ->
        when (event) {
            is UiEvent.ShowToast -> showToast(event.message)
            is UiEvent.NavigateBack -> navigator.navigateBack()
        }
    }
}
```

---

## StateBus

跨页面状态共享解决方案，自动管理生命周期。

### 适用场景

- 页面间传递数据（如从页面 B 返回选择结果到页面 A）
- 多个页面共享临时状态
- 需要在没有观察者时自动清理

### 特性

- **自动监听者追踪**：自动追踪观察者数量
- **自动清理**：当没有观察者时自动清理状态
- **线程安全**：使用同步锁和原子计数器
- **配置变更存活**：屏幕旋转时状态保持
- **进程死亡恢复**：Android 平台通过 SavedStateHandle 支持状态恢复
- **Kotlin 多平台**：支持所有平台

### 快速开始

#### 1. 在根组件提供 StateBus

```kotlin
@Composable
fun App() {
    ProvideStateBus {
        MyNavHost()
    }
}
```

#### 2. 设置状态（发送方页面）

```kotlin
@Composable
fun ScreenB() {
    val stateBus = LocalStateBus.current

    Button(onClick = {
        stateBus.setState<Person>(selectedPerson)
        navigator.navigateBack()
    }) {
        Text("确认选择")
    }
}
```

#### 3. 观察状态（接收方页面）

```kotlin
@Composable
fun ScreenA() {
    val stateBus = LocalStateBus.current
    val person = stateBus.observeState<Person?>()

    Text("已选择: ${person?.name ?: "无"}")
}
```

### 使用自定义 Key

当使用可能冲突的泛型类型时（如 `Result<String>` vs `Result<Int>`），指定自定义 key：

```kotlin
// 使用显式 key 避免类型擦除冲突
val userResult = stateBus.observeState<Result<User>?>(stateKey = "userResult")
val orderResult = stateBus.observeState<Result<Order>?>(stateKey = "orderResult")
```

### API 参考

| API | 描述 |
|-----|------|
| `ProvideStateBus` | 向组合树提供 StateBus |
| `LocalStateBus.current` | 获取当前 StateBus 实例 |
| `observeState<T>()` | 观察状态，自动追踪监听者 |
| `setState<T>()` | 设置状态值 |
| `removeState<T>()` | 手动移除状态 |
| `hasState()` | 检查状态是否存在 |

### 架构

```
Activity/Fragment ViewModelStore
  └─ StateBus (ViewModel)
      └─ stateDataMap (线程安全的状态缓存)
      └─ persistence (平台特定，Android 使用 SavedStateHandle)

NavBackStackEntry (每个页面)
  └─ StateBusListenerViewModel
      └─ 在生命周期中注册/取消注册监听者
```

### 平台说明

| 平台 | 进程死亡恢复 |
|------|------------|
| Android | ✅ 支持 (SavedStateHandle) |
| iOS | ❌ 不支持 |
| Desktop | ❌ 不支持 |
| Web | ❌ 不支持 |

---

## SharedViewModel

跨多个页面共享 ViewModel，当所有共享页面都离开导航栈时自动清理。

### 适用场景

- 多个页面需要共享同一个 ViewModel 实例
- 在相关页面之间导航时数据应保持
- 离开整个流程时 ViewModel 应被清理

### 与 StateBus 对比

| 特性 | SharedViewModel | StateBus |
|------|-----------------|----------|
| **用途** | 共享 ViewModel 实例 | 共享状态值 |
| **作用域** | 由 `SharedScope` 定义 | StateBus 内全局 |
| **清理时机** | 当所有 `includedRoutes` 离开栈时 | 当没有观察者时 |
| **使用场景** | 复杂业务逻辑共享 | 简单数据传递 |

### 快速开始

#### 1. 定义 Scope

```kotlin
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### 2. 在根组件提供 Registry

```kotlin
@Composable
fun App() {
    ProvideSharedViewModelRegistry {
        MyNavHost()
    }
}
```

#### 3. 在 NavHost 中注册 Scope

```kotlin
@Composable
fun MyNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { entry ->
            entry.destination.route?.let { getRouteClass(it) }
        }.toSet()
    }

    RegisterSharedScope(routesInStack, OrderFlowScope)

    NavHost(navController, startDestination = Route.Cart) {
        composable<Route.Cart> { CartScreen() }
        composable<Route.Checkout> { CheckoutScreen() }
        composable<Route.Payment> { PaymentScreen() }
    }
}
```

#### 4. 在页面中使用

```kotlin
@Composable
fun CartScreen() {
    val orderVm = sharedViewModel<OrderFlowScope, OrderViewModel> {
        OrderViewModel()
    }
}

@Composable
fun CheckoutScreen() {
    // 与 CartScreen 共享同一个 ViewModel 实例
    val orderVm = sharedViewModel<OrderFlowScope, OrderViewModel> {
        OrderViewModel()
    }
}
```

### 使用场景

#### 场景 1：同级页面共享 ViewModel

```kotlin
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### 场景 2：父路由与嵌套导航

```kotlin
// 重要：使用父路由，而不是嵌套路由
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // 只传父路由！
)
```

---

## 许可证

```
Apache License 2.0
```
