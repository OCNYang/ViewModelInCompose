> 如此说来，完全没有一种完美的在多Compose页面共享大对象的方案吗？ 
  ⎿  ⧉ Selected 1 lines from module/module_main/src/main/java/com/app/module_main/page/NavGraph.kt in Android Studio

⏺ 你说得对！让我重新审视这个问题。确实，现有的方案都有各自的局限性。让我提出一个更完美的解决方案。

  现有方案的问题总结

  | 方案                        | 问题                                 |
  |---------------------------|------------------------------------|
  | Activity-scoped ViewModel | ❌ 生命周期太长，等同于全局变量，无法自动清理            |
  | NavGraph-scoped ViewModel | ❌ 当切换到其他 tab 时仍保留数据，不符合"退出页面清理"的需求 |
  | Navigation Arguments      | ❌ 只能传递可序列化的小对象，有 1MB Bundle 限制     |
  | SavedStateHandle          | ❌ 数据隔离，无法跨页面共享                     |
  | CompositionLocal          | ❌ 只能传递引用，无法控制生命周期                  |

  ---
  真正完美的方案：自定义 Scoped ViewModel Store

  我发现你的需求非常明确：
  在两个 Compose 页面（Home 和 Order）之间共享数据，当退出这两个页面时，数据自动销毁

  这需要创建一个 自定义作用域的 ViewModel，生命周期精确绑定到这两个页面。

  方案：使用 ViewModelStoreOwner + 生命周期管理

  // 1. 创建一个自定义的 ViewModelStoreOwner
  class ScopedViewModelStoreOwner : ViewModelStoreOwner {
      private val _viewModelStore = ViewModelStore()

      override val viewModelStore: ViewModelStore
          get() = _viewModelStore

      fun clear() {
          _viewModelStore.clear()
      }
  }

  // 2. 在 CompositionLocal 中提供
  val LocalScopedViewModelStoreOwner = staticCompositionLocalOf<ScopedViewModelStoreOwner?> {
      null
  }

  // 3. 在导航图中创建和管理这个 scope
  @Composable
  fun MainTabNavHost(navContainer: NavHostController, topNav2Next: (Any) -> Unit, topNavBack: () -> Boolean) {
      // 创建一个只存在于 Home 和 Order 页面之间的 ViewModelStore
      val scopedStoreOwner = remember { ScopedViewModelStoreOwner() }

      // 监听导航状态，当离开 Home 和 Order 页面时清理
      val currentDestination = navContainer.currentBackStackEntryAsState().value?.destination
      DisposableEffect(currentDestination) {
          val isInScope = currentDestination?.hasRoute<Route.MainTab.Home>() == true ||
                         currentDestination?.hasRoute<Route.MainTab.Order>() == true

          onDispose {
              // 当离开 Home/Order 的作用域时，清理 ViewModel
              if (!isInScope) {
                  scopedStoreOwner.clear()
              }
          }
      }

      CompositionLocalProvider(LocalScopedViewModelStoreOwner provides scopedStoreOwner) {
          NavHost(
              navController = navContainer,
              startDestination = Route.MainTab.Home,
          ) {
              composable<Route.MainTab.Home> {
                  HomeScreen(
                      topNav2Next = topNav2Next,
                      nav2Next = { route -> navContainer.navigate(route) },
                  )
              }
              composable<Route.MainTab.Order> {
                  val route = it.toRoute<Route.MainTab.Order>()
                  OrderScreen(
                      topNav2Next = topNav2Next,
                      nav2Next = { route -> navContainer.navigate(route) },
                      topNavBack = topNavBack,
                      initialOrderId = route.initialOrderId,
                  )
              }
              composable<Route.MainTab.Product> {
                  ProductScreen(...)  // 这里不提供 scopedStoreOwner
              }
              // 其他页面...
          }
      }
  }

  // 4. 在 Home 和 Order 页面中使用共享的 ViewModel 
  @Composable
  fun HomeScreen(...) {
      val scopedStoreOwner = LocalScopedViewModelStoreOwner.current
          ?: error("ScopedViewModelStoreOwner not provided")

      // 使用自定义 scope 的 ViewModel
      val sharedViewModel: SharedOrderViewModel = viewModel(
          viewModelStoreOwner = scopedStoreOwner
      )

      // 使用 sharedViewModel...
  }

  @Composable
  fun OrderScreen(...) {
      val scopedStoreOwner = LocalScopedViewModelStoreOwner.current
          ?: error("ScopedViewModelStoreOwner not provided")

      // 获取相同的 ViewModel 实例
      val sharedViewModel: SharedOrderViewModel = viewModel(
          viewModelStoreOwner = scopedStoreOwner
      )

      // 使用 sharedViewModel...
  }

  这个方案的优势：

  ✅ 精确的生命周期控制：只在 Home 和 Order 之间共享
  ✅ 自动清理：切换到 Product/Customer/Report 时自动清理
  ✅ 无大小限制：可以存储任意大对象
  ✅ 响应式更新：支持 StateFlow/LiveData
  ✅ 符合架构：遵循 MVVM 和 Compose 最佳实践

  ---
  更优雅的实现：使用 rememberSaveable 的自定义 Saver

  但上面的方案还有点复杂。让我提供一个更简洁的方案：

  终极方案：使用 Navigation Entry 的生命周期

  @Composable
  fun MainTabNavHost(navContainer: NavHostController, topNav2Next: (Any) -> Unit, topNavBack: () -> Boolean) {
      // 关键：为 Home 和 Order 创建一个"虚拟的父 entry"
      // 当这两个页面都不在栈中时，这个父 entry 会被清理
      val sharedScope = rememberSharedScope(
          navController = navContainer,
          includedRoutes = listOf(
              Route.MainTab.Home::class,
              Route.MainTab.Order::class
          )
      )

      NavHost(
          navController = navContainer,
          startDestination = Route.MainTab.Home,
      ) {
          composable<Route.MainTab.Home> {
              HomeScreen(
                  sharedViewModel = sharedScope.getViewModel(),
                  ...
              )
          }
          composable<Route.MainTab.Order> {
              OrderScreen(
                  sharedViewModel = sharedScope.getViewModel(),
                  ...
              )
          }
          // 其他页面不提供 sharedScope
      }
  }

  // 自定义的 SharedScope 实现
  class SharedScope(private val viewModelStore: ViewModelStore) {
      inline fun <reified T : ViewModel> getViewModel(): T {
          return ViewModelProvider(
              object : ViewModelStoreOwner {
                  override val viewModelStore = this@SharedScope.viewModelStore
              }
          )[T::class.java]
      }

      fun clear() {
          viewModelStore.clear()
      }
  }

  @Composable
  fun rememberSharedScope(
      navController: NavHostController,
      includedRoutes: List<KClass<out Route>>
  ): SharedScope {
      val scope = remember { SharedScope(ViewModelStore()) }
      val currentDestination = navController.currentBackStackEntryAsState().value?.destination

      LaunchedEffect(currentDestination) {
          // 检查当前页面是否在 scope 内
          val isInScope = includedRoutes.any { routeClass ->
              currentDestination?.hasRoute(routeClass) == true
          }

          // 如果不在 scope 内，清理 ViewModel
          if (!isInScope && currentDestination != null) {
              scope.clear()
          }
      }

      return scope
  }

  ---
  实际使用示例

  // Home 页面
  @Composable
  fun HomeScreen(
      sharedViewModel: SharedOrderViewModel,  // 通过参数传入
      ...
  ) {
      val scope = rememberCoroutineScope()

      Button(onClick = {
          viewModel.onCreateOrder(onSuccess = { orderId ->
              // 1. 存储到共享 ViewModel
              sharedViewModel.setOrderId(orderId)

              // 2. 切换到 Order tab
              scope.launch {
                  EventBus.post(SwitchMainTabEvent(tabIndex = 1))
              }
          })
      }) {
          Text("Create Order")
      }
  }

  // Order 页面
  @Composable
  fun OrderScreen(
      sharedViewModel: SharedOrderViewModel,  // 相同的实例
      ...
  ) {
      val orderId by sharedViewModel.orderId.collectAsState()

      LaunchedEffect(orderId) {
          if (orderId != null) {
              // 显示订单详情
              navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, orderId)
          }
      }
  }

  // 共享的 ViewModel
  class SharedOrderViewModel : ViewModel() {
      private val _orderId = MutableStateFlow<Long?>(null)
      val orderId: StateFlow<Long?> = _orderId.asStateFlow()

      fun setOrderId(id: Long) {
          _orderId.value = id
      }

      override fun onCleared() {
          super.onCleared()
          LogX.d("SharedOrderViewModel cleared - data destroyed")
      }
  }

  // Product 页面（不使用 sharedViewModel）
  @Composable
  fun ProductScreen(...) {
      // 这里无法访问 SharedOrderViewModel
      // 当用户切换到这里时，SharedOrderViewModel 会被清理
  }

  ---
  最终方案对比

  | 方案                  | 生命周期控制  | 自动清理 | 大对象支持 | 实现复杂度 |
  |---------------------|---------|------|-------|-------|
  | Activity-scoped     | ❌       | ❌    | ✅     | 简单    |
  | NavGraph-scoped     | ⚠️ 不够精确 | ⚠️   | ✅     | 简单    |
  | 自定义 Scoped Store    | ✅ 精确控制  | ✅    | ✅     | 中等    |
  | rememberSharedScope | ✅ 精确控制  | ✅    | ✅     | 中等    |

  ---
  我的推荐

  根据你的需求，我推荐 rememberSharedScope 方案，因为：

  1. ✅ 精确的生命周期：只在 Home 和 Order 之间共享，离开这两个页面立即清理
  2. ✅ 支持大对象：无限制，可以存储任何数据
  3. ✅ 自动内存管理：通过 ViewModel 的 onCleared 自动清理资源
  4. ✅ 可测试：ViewModel 可以独立测试
  5. ✅ 符合 Compose 最佳实践：使用 remember + DisposableEffect 管理生命周期

  这才是真正完美的方案！