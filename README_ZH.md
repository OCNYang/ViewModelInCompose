# ViewModelInCompose

一个用于在 Compose Multiplatform 中跨多个页面共享 ViewModel 的 Kotlin 多平台库。

## 支持平台

- Android
- iOS
- Desktop (JVM)
- Web (JS/Wasm)

## SharedViewModel

SharedViewModel 允许多个页面共享同一个 ViewModel 实例，当所有共享页面都从导航栈中移除时，ViewModel 会自动清理。

### 特性

- **自动生命周期管理**：当所有使用共享 ViewModel 的页面都从导航栈移除时，ViewModel 自动清理
- **无需手动传递**：在任意 `@Composable` 函数中访问共享 ViewModel，无需通过参数层层传递
- **类型安全的 Scope 定义**：使用 `object` 定义 Scope，编译时类型检查
- **配置变更存活**：共享 ViewModel 在配置变更（如屏幕旋转）时保持存活

### 与路由作用域 ViewModel 的对比

| 特性 | SharedViewModel | 路由作用域 ViewModel |
|------|-----------------|---------------------|
| **作用域定义** | 使用 `SharedScope` 自定义作用域 | 绑定到导航路由的生命周期 |
| **清理时机** | 当所有 `includedRoutes` 离开导航栈时 | 当路由被弹出时 |
| **访问方式** | 在任意位置使用 `getSharedViewModelStore<Scope>()` | 必须从路由入口点传递 |
| **使用场景** | 跨多个相关页面共享数据 | 单个页面或父子层级结构 |
| **嵌套导航** | 支持跨嵌套导航图共享 | 仅限单个导航图 |

### 安装

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ocnyang:viewmodelincompose:<version>")
}
```

### 快速开始

#### 1. 定义 Scope

```kotlin
// 将 Scope 定义为 object
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

#### 2. 在根组件提供 Registry

```kotlin
@Composable
fun App() {
    ProvideSharedViewModelRegistry {
        val navController = rememberNavController()
        MyNavHost(navController)
    }
}
```

#### 3. 在 NavHost 中注册 Scope

```kotlin
@Composable
fun MyNavHost(navController: NavHostController) {
    // 收集当前路由栈
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { entry ->
            entry.destination.route?.let { getRouteClass(it) }
        }.toSet()
    }

    // 注册 Scope
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
    // 方式 1：获取 store 后使用 viewModel
    val store = getSharedViewModelStore<OrderFlowScope>()
    val orderVm: OrderViewModel = viewModel(viewModelStoreOwner = store) {
        OrderViewModel()
    }

    // 方式 2：使用便捷函数
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

当多个处于同一导航层级的页面需要共享数据时：

```kotlin
// Cart -> Checkout -> Payment（线性流程）
object OrderFlowScope : SharedScope(
    includedRoutes = setOf(Route.Cart::class, Route.Checkout::class, Route.Payment::class)
)
```

当用户完成支付并离开这三个页面后，`OrderViewModel` 会自动清理。

#### 场景 2：父路由与嵌套导航共享 ViewModel

当父路由包含嵌套导航，且需要与子页面共享 ViewModel 时：

```kotlin
// 主路由结构：
// - Route.Home
// - Route.Dashboard（包含嵌套导航的父路由）
//   - Route.Dashboard.Overview（嵌套页面）
//   - Route.Dashboard.Analytics（嵌套页面）
//   - Route.Dashboard.Settings（嵌套页面）

// 重要：includedRoutes 应该包含父路由，而不是嵌套路由
object DashboardScope : SharedScope(
    includedRoutes = setOf(Route.Dashboard::class)  // 只传父路由！
)
```

**为什么使用父路由而不是嵌套路由？**

在嵌套导航中，当你在嵌套目的地之间切换时：
- 父路由（`Route.Dashboard`）始终保持在导航栈中
- 只有嵌套内容在变化

如果你将嵌套路由如 `Route.Dashboard.Overview::class` 包含进去，当在嵌套目的地之间切换时，Scope 会被清理，因为这些特定路由会离开并重新进入导航栈。

通过使用父路由，只要用户还在 Dashboard 区域内的任何位置，Scope 就保持活跃。

```kotlin
@Composable
fun DashboardNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStack.collectAsState()
    val routesInStack = remember(backStack) {
        backStack.mapNotNull { it.destination.route?.let { getRouteClass(it) } }.toSet()
    }

    // 使用父路由监控注册
    RegisterSharedScope(routesInStack, DashboardScope)

    NavHost(navController, startDestination = "overview") {
        composable("overview") {
            // 访问共享 ViewModel
            val dashboardVm = sharedViewModel<DashboardScope, DashboardViewModel> {
                DashboardViewModel()
            }
            OverviewScreen(dashboardVm)
        }
        composable("analytics") {
            val dashboardVm = sharedViewModel<DashboardScope, DashboardViewModel> {
                DashboardViewModel()
            }
            AnalyticsScreen(dashboardVm)
        }
    }
}
```

### API 参考

| API | 描述 |
|-----|------|
| `SharedScope` | 定义共享作用域的基类 |
| `ProvideSharedViewModelRegistry` | 向组合树提供 Registry |
| `RegisterSharedScope` | 注册 Scope 并监控路由栈以进行清理 |
| `getSharedViewModelStore<T>()` | 获取指定 Scope 的 ViewModelStoreOwner |
| `sharedViewModel<S, T>()` | 获取共享 ViewModel 的便捷函数 |

### 架构

```
Activity/Fragment ViewModelStore
  └─ SharedViewModelRegistry（ViewModel，配置变更时存活）
      └─ Map<KClass<SharedScope>, SharedViewModelStore>
          ├─ OrderFlowScope::class → SharedViewModelStore
          │   └─ ViewModelStore
          │       └─ OrderViewModel
          ├─ DashboardScope::class → SharedViewModelStore
          │   └─ ViewModelStore
          │       └─ DashboardViewModel
          └─ ...
```

## 许可证

```
Apache License 2.0
```
