package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme

@Composable
fun ProfileHeader(
    name: String,
    subtitle: String,
    initials: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // PLANTED: absolute arrangement and padding, so the row does not mirror in RTL.
    Row(
        modifier
            .fillMaxWidth()
            .absolutePadding(left = 16.dp, right = 8.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Absolute.Left,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // PLANTED: hard-coded near-black, unreadable on a dark surface.
            Text(name, color = Color(0xFF1C1B1F), style = MaterialTheme.typography.titleMedium)
            // PLANTED: light grey on white, contrast far below 4.5:1.
            Text(subtitle, color = Color(0xFFC4C4C4), style = MaterialTheme.typography.bodyMedium)
        }
        // Control: a correct Material button. Must NOT be flagged.
        TextButton(onClick = onEdit) { Text("Edit") }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileHeaderPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            ProfileHeader("Ashwin Nair", "Android engineer · Bengaluru", "AN", onEdit = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileHeaderLongTextPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            ProfileHeader(
                "Ashwin Venkataraman Subramaniam Nair",
                "Senior Android engineer, Payments Platform · Bengaluru, Karnataka",
                "AN",
                onEdit = {},
            )
        }
    }
}
