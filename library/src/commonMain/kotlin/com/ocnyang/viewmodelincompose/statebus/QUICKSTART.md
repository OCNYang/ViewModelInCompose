# StateBus 快速开始指南

## 5 分钟上手 StateBus

### 1. 创建 StateBus (1 分钟)

```kotlin
@Composable
fun MyApp() {
    val stateBus = rememberStateBus()

    // 你的导航和 UI
    MyNavHost(stateBus)
}
```

### 2. 监听状态 (2 分钟)

```kotlin
// 定义数据模型
data class User(val name: String, val age: Int)

// 在页面 A 监听
@Composable
fun HomeScreen(stateBus: StateBus) {
    val user = stateBus.observeState<User?>()

    Text("User: ${user?.name ?: "No data"}")

    Button(onClick = { /* 导航到编辑页 */ }) {
        Text("Edit")
    }
}
```

### 3. 设置状态 (2 分钟)

```kotlin
// 在页面 B 设置
@Composable
fun EditScreen(stateBus: StateBus, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }

    Column {
        TextField(value = name, onValueChange = { name = it })

        Button(
            onClick = {
                // 设置状态
                stateBus.setState<User>(User(name, 25))

                // 返回
                onBack()
            }
        ) {
            Text("Save")
        }
    }
}
```

### 完成！🎉

现在你已经可以在页面间传递状态了！

## 关键特性

### ✅ 自动清理

```kotlin
// 当所有监听者离开时，状态会自动清理
// 无需手动调用 removeState()
```

### ✅ 线程安全

```kotlin
// 可以从任何线程设置状态
viewModelScope.launch(Dispatchers.IO) {
    val data = fetchData()
    stateBus.setState<Data>(data)  // ✅ 线程安全
}
```

### ✅ 配置更改恢复

```kotlin
// 屏幕旋转后，状态会自动恢复
// 无需额外处理
```

## 常见问题

### Q: 什么时候状态会被清理？

**A**: 当最后一个监听者离开时自动清理。

### Q: 可以多个页面同时监听吗？

**A**: 可以！StateBus 支持多个监听者。

### Q: 线程安全吗？

**A**: 完全线程安全，可以从任何线程使用。

## 下一步

- 查看 [README.md](README.md) 了解完整文档
- 查看 [examples/](examples/) 目录学习更多示例

## 示例代码

### 完整的导航示例

```kotlin
@Composable
fun MyApp() {
    val stateBus = rememberStateBus()
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                stateBus = stateBus,
                onNavigate = { navController.navigate("edit") }
            )
        }

        composable("edit") {
            EditScreen(
                stateBus = stateBus,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(stateBus: StateBus, onNavigate: () -> Unit) {
    val user = stateBus.observeState<User?>()

    Column {
        Text("User: ${user?.name ?: "No data"}")
        Button(onClick = onNavigate) { Text("Edit") }
    }
}

@Composable
fun EditScreen(stateBus: StateBus, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }

    Column {
        TextField(value = name, onValueChange = { name = it })

        Button(
            onClick = {
                stateBus.setState<User>(User(name, 25))
                onBack()
            }
        ) {
            Text("Save")
        }
    }
}
```

就这么简单！开始使用 StateBus 吧！🚀
