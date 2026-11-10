package com.verifyui.ashwinn796

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.testbed.OrderSummaryCard
import com.verifyui.ashwinn796.testbed.ProfileHeader
import com.verifyui.ashwinn796.testbed.SettingsRow
import com.verifyui.ashwinn796.testbed.sampleOrderItems
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerifyUiLoopTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var notify by remember { mutableStateOf(true) }
                    Column(
                        Modifier.padding(innerPadding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ProfileHeader("Ashwin Nair", "Android engineer · Bengaluru", "AN", onEdit = {})
                        OrderSummaryCard("Your order", sampleOrderItems, "₹160", onPay = {}, onRemoveItem = {})
                        SettingsRow("Order updates", "Get notified when your order ships", notify, { notify = it })
                    }
                }
            }
        }
    }
}
