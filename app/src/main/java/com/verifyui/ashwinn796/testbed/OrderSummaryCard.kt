package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // PLANTED: no weight on the name, so a long name squeezes the price.
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(item.price, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    // PLANTED: 24dp touch target and no accessible label.
                    val tint = MaterialTheme.colorScheme.onSurfaceVariant
                    Box(Modifier.size(24.dp).clickable { onRemoveItem(item) }) {
                        Canvas(Modifier.size(24.dp)) {
                            val p = size.width * 0.3f
                            drawLine(tint, Offset(p, p), Offset(size.width - p, size.height - p), 4f)
                            drawLine(tint, Offset(size.width - p, p), Offset(p, size.height - p), 4f)
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                // PLANTED: fixed height fits 1x font only; clips at large font scale.
                Text(total, style = MaterialTheme.typography.titleMedium, modifier = Modifier.height(24.dp))
            }

            // PLANTED: 36dp tall touch target (minimum is 48dp).
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPay),
                contentAlignment = Alignment.Center,
            ) {
                Text("Pay $total", color = MaterialTheme.colorScheme.onPrimary)
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
                "Your order",
                listOf(
                    OrderItem("Paneer Butter Masala with Garlic Naan and Extra Gravy (Large)", "₹380"),
                    OrderItem("Filter Coffee", "₹40"),
                ),
                "₹420",
                onPay = {},
                onRemoveItem = {},
            )
        }
    }
}
