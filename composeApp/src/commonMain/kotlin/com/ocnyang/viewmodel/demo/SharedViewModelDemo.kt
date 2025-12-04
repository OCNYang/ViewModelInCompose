package com.ocnyang.viewmodel.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.ocnyang.viewmodelincompose.sharedviewmodel.ProvideSharedViewModelRegistry
import com.ocnyang.viewmodelincompose.sharedviewmodel.RegisterSharedScope
import com.ocnyang.viewmodelincompose.sharedviewmodel.SharedScope
import com.ocnyang.viewmodelincompose.sharedviewmodel.sharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * SharedViewModel Demo
 *
 * Demonstrates sharing a ViewModel across multiple screens in an order flow.
 * The shared ViewModel maintains state while navigating between Cart, Checkout, and Payment screens.
 */

// Define the screens for the order flow
private enum class OrderScreen {
    Cart,
    Checkout,
    Payment,
    Complete
}

// Define the shared scope - includes all screens that share the ViewModel
private object OrderFlowScope : SharedScope(
    includedRoutes = setOf(
        OrderScreen.Cart::class,
        OrderScreen.Checkout::class,
        OrderScreen.Payment::class
        // Note: Complete is NOT included, so ViewModel will be cleared when navigating to Complete
    )
)

/**
 * Shared ViewModel for the order flow
 */
class OrderFlowViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _shippingAddress = MutableStateFlow("")
    val shippingAddress: StateFlow<String> = _shippingAddress.asStateFlow()

    private val _paymentMethod = MutableStateFlow("")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private var instanceId = instanceCounter++

    init {
        // Add some default items
        _cartItems.value = listOf(
            CartItem("Kotlin Book", 29.99, 1),
            CartItem("Compose Guide", 39.99, 2)
        )
    }

    fun getInstanceInfo(): String = "ViewModel #$instanceId"

    fun addItem(item: CartItem) {
        _cartItems.value = _cartItems.value + item
    }

    fun removeItem(name: String) {
        _cartItems.value = _cartItems.value.filterNot { it.name == name }
    }

    fun updateQuantity(name: String, quantity: Int) {
        _cartItems.value = _cartItems.value.map {
            if (it.name == name) it.copy(quantity = quantity) else it
        }
    }

    fun setShippingAddress(address: String) {
        _shippingAddress.value = address
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun getTotalPrice(): Double {
        return _cartItems.value.sumOf { it.price * it.quantity }
    }

    fun clearOrder() {
        _cartItems.value = emptyList()
        _shippingAddress.value = ""
        _paymentMethod.value = ""
    }

    companion object {
        private var instanceCounter = 1
    }
}

data class CartItem(
    val name: String,
    val price: Double,
    val quantity: Int
)

/**
 * Main entry point for SharedViewModel Demo
 */
@Composable
fun SharedViewModelDemo(
    onBack: () -> Unit
) {
    // Track current screen
    var currentScreen by remember { mutableStateOf(OrderScreen.Cart) }

    // Calculate routes in stack (simulate navigation stack)
    val routesInStack: Set<KClass<*>> = remember(currentScreen) {
        when (currentScreen) {
            OrderScreen.Cart -> setOf(OrderScreen.Cart::class)
            OrderScreen.Checkout -> setOf(OrderScreen.Cart::class, OrderScreen.Checkout::class)
            OrderScreen.Payment -> setOf(OrderScreen.Cart::class, OrderScreen.Checkout::class, OrderScreen.Payment::class)
            OrderScreen.Complete -> setOf(OrderScreen.Complete::class) // Cart, Checkout, Payment removed
        }
    }

    // Provide SharedViewModelRegistry at the root
    ProvideSharedViewModelRegistry {
        // Register the scope - this monitors routesInStack and clears when no routes match
        RegisterSharedScope(routesInStack, OrderFlowScope)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "SharedViewModel Demo",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Navigation indicator
            NavigationIndicator(currentScreen = currentScreen)

            HorizontalDivider()

            // Screen content
            when (currentScreen) {
                OrderScreen.Cart -> CartScreen(
                    onProceed = { currentScreen = OrderScreen.Checkout },
                    onReset = { currentScreen = OrderScreen.Cart }
                )
                OrderScreen.Checkout -> CheckoutScreen(
                    onBack = { currentScreen = OrderScreen.Cart },
                    onProceed = { currentScreen = OrderScreen.Payment }
                )
                OrderScreen.Payment -> PaymentScreen(
                    onBack = { currentScreen = OrderScreen.Checkout },
                    onComplete = { currentScreen = OrderScreen.Complete }
                )
                OrderScreen.Complete -> CompleteScreen(
                    onStartNew = { currentScreen = OrderScreen.Cart }
                )
            }
        }
    }
}

/**
 * Navigation progress indicator
 */
@Composable
private fun NavigationIndicator(currentScreen: OrderScreen) {
    val steps = listOf("Cart", "Checkout", "Payment", "Complete")
    val currentIndex = currentScreen.ordinal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index <= currentIndex
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Cart Screen - First step in order flow
 */
@Composable
private fun CartScreen(
    onProceed: () -> Unit,
    onReset: () -> Unit
) {
    // Get shared ViewModel - same instance across Cart, Checkout, Payment
    val viewModel = sharedViewModel<OrderFlowScope, OrderFlowViewModel> {
        OrderFlowViewModel()
    }

    val cartItems by viewModel.cartItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ViewModel instance info
        ViewModelInfoCard(viewModel.getInstanceInfo())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Shopping Cart",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Cart items
        cartItems.forEach { item ->
            CartItemCard(
                item = item,
                onRemove = { viewModel.removeItem(item.name) },
                onQuantityChange = { viewModel.updateQuantity(item.name, it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add item button
        OutlinedButton(
            onClick = {
                viewModel.addItem(
                    CartItem("New Item ${cartItems.size + 1}", 19.99, 1)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Item")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format("%.2f", viewModel.getTotalPrice())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth(),
            enabled = cartItems.isNotEmpty()
        ) {
            Text("Proceed to Checkout")
        }
    }
}

/**
 * Cart item display card
 */
@Composable
private fun CartItemCard(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) }
                ) {
                    Text("-")
                }
                Text(
                    text = "${item.quantity}",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedButton(
                    onClick = { onQuantityChange(item.quantity + 1) }
                ) {
                    Text("+")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onRemove) {
                    Text("×")
                }
            }
        }
    }
}

/**
 * Checkout Screen - Second step in order flow
 */
@Composable
private fun CheckoutScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    // Same ViewModel instance as Cart screen
    val viewModel = sharedViewModel<OrderFlowScope, OrderFlowViewModel> {
        OrderFlowViewModel()
    }

    val shippingAddress by viewModel.shippingAddress.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ViewModel instance info - should show same instance as Cart
        ViewModelInfoCard(viewModel.getInstanceInfo())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Checkout",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Order summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Order Summary",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.name} x${item.quantity}")
                        Text("$${String.format("%.2f", item.price * item.quantity)}")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format("%.2f", viewModel.getTotalPrice())}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shipping address selection
        Text(
            text = "Shipping Address",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        val addresses = listOf(
            "123 Main St, City A",
            "456 Oak Ave, City B",
            "789 Pine Rd, City C"
        )

        addresses.forEach { address ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shippingAddress == address)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { viewModel.setShippingAddress(address) }
            ) {
                Text(
                    text = address,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onProceed,
                modifier = Modifier.weight(1f),
                enabled = shippingAddress.isNotEmpty()
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Payment Screen - Third step in order flow
 */
@Composable
private fun PaymentScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    // Same ViewModel instance as Cart and Checkout
    val viewModel = sharedViewModel<OrderFlowScope, OrderFlowViewModel> {
        OrderFlowViewModel()
    }

    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val shippingAddress by viewModel.shippingAddress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ViewModel instance info - should show same instance
        ViewModelInfoCard(viewModel.getInstanceInfo())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Payment",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Shipping to:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = shippingAddress,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$${String.format("%.2f", viewModel.getTotalPrice())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment method selection
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        val methods = listOf(
            "Credit Card",
            "PayPal",
            "Bank Transfer"
        )

        methods.forEach { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (paymentMethod == method)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = { viewModel.setPaymentMethod(method) }
            ) {
                Text(
                    text = method,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = paymentMethod.isNotEmpty()
            ) {
                Text("Place Order")
            }
        }
    }
}

/**
 * Order Complete Screen - Final step
 *
 * Note: This screen is NOT in OrderFlowScope.includedRoutes,
 * so when navigating here, the SharedViewModel will be automatically cleared.
 */
@Composable
private fun CompleteScreen(
    onStartNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Order Placed!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Thank you for your order.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SharedViewModel Behavior:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The OrderFlowViewModel was automatically cleared when you navigated to this screen, " +
                        "because 'Complete' is not in OrderFlowScope.includedRoutes.\n\n" +
                        "When you start a new order, a NEW ViewModel instance will be created.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartNew) {
            Text("Start New Order")
        }
    }
}

/**
 * Card showing ViewModel instance info
 */
@Composable
private fun ViewModelInfoCard(instanceInfo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shared ViewModel Instance:",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = instanceInfo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
