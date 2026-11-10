package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme

@Composable
fun SettingsRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            SettingsRow("Order updates", "Get notified when your order ships", checked = true, onCheckedChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowLongTextPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            SettingsRow(
                "Notify me about order updates and delivery changes",
                "Get notified when your order is confirmed, packed, shipped, out for delivery and delivered",
                checked = true,
                onCheckedChange = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowUncheckedPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            SettingsRow("Order updates", "Get notified when your order ships", checked = false, onCheckedChange = {})
        }
    }
}