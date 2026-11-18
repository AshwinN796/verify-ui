package com.verifyui.ashwinn796.testbed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.verifyui.ashwinn796.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verifyui.ashwinn796.ui.theme.VerifyUiLoopTheme
import com.verifyui.ashwinn796.ui.theme.color_blue_100
import com.verifyui.ashwinn796.ui.theme.color_blue_400
import com.verifyui.ashwinn796.ui.theme.color_blue_600
import com.verifyui.ashwinn796.ui.theme.color_green_400
import com.verifyui.ashwinn796.ui.theme.color_white
import com.verifyui.ashwinn796.ui.theme.color_white_700
import com.verifyui.ashwinn796.ui.theme.color_yellow_600
import com.verifyui.ashwinn796.ui.theme.color_yellow_700

/**
 * Created by Ashwin Nirmale on 17/11/26.
 */
@Composable
fun UserRoleCard(
    title: String,
    description: String,
    resource: String?,
    isSelected: Boolean,
    isComingSoon: Boolean,
    showSelectIcon: Boolean = true,
    termsChecked: Boolean = false,
    onToggleTerms: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onSelectItem: () -> Unit) {

    val background = if (isSelected) color_yellow_700 else color_white
    val borderColor = if (isSelected) color_yellow_600 else color_white_700

    val contentAlpha = if (isComingSoon) 0.7f else 1f

    Box (
        modifier = Modifier.fillMaxWidth()
            .padding( vertical = 15.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .shadow(elevation = 0.dp, shape = RoundedCornerShape(12.dp))
                .selectable(
                    selected = isSelected,
                    enabled = !isComingSoon,
                    role = Role.RadioButton,
                    onClick = onSelectItem,
                )
                .background(background),
            colors = CardDefaults.cardColors(containerColor = background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {

            Column(modifier = Modifier.fillMaxWidth()) {

                // Top section: image | divider | text, with select indicator at top-end
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_role_placeholder),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = contentAlpha,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }

                        VerticalDivider(
                            color = color_white_700,
                            thickness = 1.dp,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(start = 6.dp, end = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(
                                    text = title,
                                    color = color_blue_100,
                                    modifier = Modifier.alpha(contentAlpha),
                                )

                                if (isComingSoon) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ComingSoonPill(modifier = Modifier)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = description,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = color_blue_100,
                                modifier = Modifier.alpha(contentAlpha),
                            )
                        }
                    }

                    if (showSelectIcon) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(color_green_400, CircleShape),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = color_white,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .border(1.dp, color_white_700, CircleShape),
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    thickness = 1.dp,
                    color = color_white_700
                )

                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Row(
                        modifier = Modifier
                            .toggleable(
                                value = termsChecked,
                                enabled = !isComingSoon,
                                role = Role.Checkbox,
                                onValueChange = { onToggleTerms() },
                            )
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        Checkbox(
                            checked = termsChecked,
                            enabled = !isComingSoon,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = color_blue_600,
                                uncheckedColor = color_blue_100,
                            )
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(text = "Agree to", color = color_blue_100)

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = "terms and condition",
                        color = color_blue_400,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(role = Role.Button, onClick = onOpenTerms)
                            .minimumInteractiveComponentSize(),
                    )
                }

            }

        }
    }

}

private const val ROLE_TITLE = "Driver & Owner"
private const val ROLE_DESCRIPTION = "I own my vehicle and also want to drive and accept booking"

@Composable
private fun RolePreview(
    title: String = ROLE_TITLE,
    description: String = ROLE_DESCRIPTION,
    resource: String? = "",
    isSelected: Boolean = false,
    isComingSoon: Boolean = false,
    showSelectIcon: Boolean = true,
    termsChecked: Boolean = false,
) {
    VerifyUiLoopTheme(dynamicColor = false) {
        Surface {
            UserRoleCard(
                title = title,
                description = description,
                resource = resource,
                isSelected = isSelected,
                isComingSoon = isComingSoon,
                showSelectIcon = showSelectIcon,
                termsChecked = termsChecked,
            ) { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserRoleCardPreview() = RolePreview()

@Preview(showBackground = true)
@Composable
private fun UserRoleCardNoSelectIconPreview() = RolePreview(showSelectIcon = false)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardNoResourcePreview() = RolePreview(resource = null)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardLongTextPreview() = RolePreview(
    title = "Fleet Owner & Driver Partner",
    description = "I own several vehicles, also drive one of them myself, and want to accept " +
        "bookings, assign trips to my drivers and track every payout in one place",
)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardSelectedPreview() = RolePreview(isSelected = true)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardComingSoonPreview() = RolePreview(isComingSoon = true)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardTermsCheckedPreview() = RolePreview(termsChecked = true)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardAllOnPreview() =
    RolePreview(isSelected = true, isComingSoon = true, termsChecked = true)

@Preview(showBackground = true)
@Composable
private fun UserRoleCardDisabledCheckedPreview() =
    RolePreview(isComingSoon = true, termsChecked = true)
