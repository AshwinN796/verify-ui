package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme

data class OrderItem(val name: String, val price: String)

@Composable
fun OrderSummaryCard(
    title: String,
    items: List<OrderItem>,
    total: String,
    onPay: () -> Unit,
    onRemoveItem: (OrderItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)

            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(items) { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.price, style = MaterialTheme.typography.bodyLarge)
                    IconButton(
                        onClick = { onRemoveItem(item) },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Remove ${item.name}" },
                    ) {
                        val tint = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(Modifier.size(24.dp)) {
                            val p = size.width * 0.3f
                            drawLine(tint, Offset(p, p), Offset(size.width - p, size.height - p), 4f)
                            drawLine(tint, Offset(size.width - p, p), Offset(p, size.height - p), 4f)
                        }
                    }
                }
                }
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(total, style = MaterialTheme.typography.titleMedium)
            }

            Button(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
                Text("Pay $total")
            }
        }
    }
}

val sampleOrderItems = listOf(
    OrderItem("Masala Dosa", "₹120"),
    OrderItem("Filter Coffee", "₹40"),
)

@Preview(showBackground = true)
@Composable
fun OrderSummaryCardPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            OrderSummaryCard("Your order", sampleOrderItems, "₹160", onPay = {}, onRemoveItem = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrderSummaryCardLongTextPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            OrderSummaryCard(
                "Your order from Saravana Bhavan, Indiranagar",
                listOf(
                    OrderItem("Paneer Butter Masala with Garlic Naan and Extra Gravy (Large)", "₹380"),
                    OrderItem("Filter Coffee", "₹40"),
                ),
                "₹1,42,420.00",
                onPay = {},
                onRemoveItem = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrderSummaryCardEmptyPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            OrderSummaryCard("Your order", emptyList(), "₹0", onPay = {}, onRemoveItem = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrderSummaryCardManyItemsPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            OrderSummaryCard(
                "Your order",
                List(22) { OrderItem("Item ${it + 1} - Masala Dosa", "₹${120 + it}") },
                "₹3,000",
                onPay = {},
                onRemoveItem = {},
            )
        }
    }
}
