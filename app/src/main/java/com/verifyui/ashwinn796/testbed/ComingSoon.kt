package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifyui.ashwinn796.ui.theme.color_green_400
import com.verifyui.ashwinn796.ui.theme.color_white

/**
 * Created by Ashwin Nirmale on 17/11/26.
 */
@Composable
fun ComingSoonPill(modifier: Modifier,
                   text: String = "Coming Soon",
                   backgroundColor: Color = color_green_400) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color_white,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}