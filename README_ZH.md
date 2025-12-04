# ViewModelInCompose

一个为 Compose Multiplatform 提供增强 ViewModel 工具的 Kotlin 多平台库。

## 支持平台

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## 功能

- **LaunchedEffectOnce**：在页面生命周期内只执行一次副作用
- **EventEffect**：生命周期感知的一次性事件处理，用于 ViewModel 到 UI 的通信
- **SharedViewModel**：跨多个页面共享 ViewModel，自动管理生命周期

## 安装

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ocnyang:viewmodelincompose:<version>")
}
```

---

## LaunchedEffectOnce

在页面生命周期内只执行一次的 `LaunchedEffect`，不受重组和配置变更影响。

### 适用场景

- 一次性初始化（数据加载、埋点上报）
- 不应在重组或屏幕旋转时重复执行的副作用

### 与标准 LaunchedEffect 对比

| 行为 | LaunchedEffect | LaunchedEffectOnce |
|------|---------------|-------------------|
| 重组时 | 可能重新执行 | 不会重新执行 |
| 配置变更时 | 重新执行 | 不会重新执行 |
| 页面重建时 | 重新执行 | 重新执行 |

### 基本用法

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

### 多个独立的 Effect

```kotlin
@Composable
fun MyScreen() {
    // 使用 viewModelKey 区分不同的 effect
    LaunchedEffectOnce(viewModelKey = "loadData") {
        viewModel.loadData()
    }

    LaunchedEffectOnce(viewModelKey = "analytics") {
        analytics.trackPageView()
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
            // 执行一些操作...
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

    // UI 内容...
}
```

### API 参考

| API | 描述 |
|-----|------|
| `EventChannel<T>` | 用于 ViewModel 的基于 Channel 的事件发射器 |
| `EventChannel.send()` | 挂起函数，发送事件 |
| `EventChannel.trySend()` | 非挂起发送（缓冲区满时可能丢弃） |
| `EventEffect()` | 用于收集和处理事件的 Composable |

---

## SharedViewModel

跨多个页面共享 ViewModel，当所有共享页面都离开导航栈时自动清理。

### 适用场景

- 多个页面需要共享同一个 ViewModel 实例
- 在相关页面之间导航时数据应保持
- 离开整个流程时 ViewModel 应被清理

### 与路由作用域 ViewModel 对比

| 特性 | SharedViewModel | 路由作用域 ViewModel |
|------|-----------------|---------------------|
| 作用域定义 | 使用 `SharedScope` 自定义 | 绑定到导航路由生命周期 |
| 清理时机 | 当所有 `includedRoutes` 离开导航栈时 | 当路由被弹出时 |
| 访问方式 | 任意位置使用 `getSharedViewModelStore<Scope>()` | 必须从路由入口点传递 |
| 嵌套导航 | 支持跨嵌套导航图共享 | 仅限单个导航图 |

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
    // 使用 orderVm...
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
// Cart -> Checkout -> Payment（线性流程）
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

当用户离开这三个页面后，`OrderViewModel` 会自动清理。

#### 场景 2：父路由与嵌套导航

```kotlin
// 重要：使用父路由，而不是嵌套路由
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // 只传父路由！
)
```

在嵌套导航中，父路由保持在导航栈中，只有嵌套内容在变化。使用父路由可以让 Scope 在整个区域内保持活跃。

### API 参考

| API | 描述 |
|-----|------|
| `SharedScope` | 定义共享作用域的基类 |
| `ProvideSharedViewModelRegistry` | 向组合树提供 Registry |
| `RegisterSharedScope` | 注册 Scope 并监控路由栈以进行清理 |
| `getSharedViewModelStore<T>()` | 获取指定 Scope 的 ViewModelStoreOwner |
| `sharedViewModel<S, T>()` | 获取共享 ViewModel 的便捷函数 |

---

## 许可证

```
Apache License 2.0
```
