package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme

@Composable
fun PromoBanner(code: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = code, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
fun PromoBannerPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface { PromoBanner("WELCOME20", "20% off your first ride") }
    }
}

@Preview(showBackground = true)
@Composable
fun PromoBannerLongTextPreview() {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            PromoBanner("WELCOME20", "Get 20% off up to 150 rupees on your first three intercity bookings this month")
        }
    }
}
