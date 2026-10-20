package com.neurotube.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neurotube.app.R
import com.neurotube.app.account.YouTubeAccount
import com.neurotube.app.ui.components.shared.FlowActionButton
import com.neurotube.app.ui.components.shared.FlowMorphingPortrait

private val CardPadding = 16.dp
private val CardSpacing = 12.dp
private val GlyphSize = 40.dp
private val GlyphIconSize = 24.dp
private val PortraitSize = 40.dp

/**
 * The optional YouTube sign-in, offered beside the import tiles on the last setup step.
 *
 * A card rather than another import tile because it is not an import: nothing is copied off the
 * device, the session stays in NeuroTube's own encrypted store. The same control is the first row
 * of Settings, for anyone who skips setup.
 */
@Composable
internal fun OnboardingAccountCard(
    account: YouTubeAccount?,
    working: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
                if (account != null) {
                    FlowMorphingPortrait(
                        imageUrl = account.thumbnailUrl.orEmpty(),
                        fallback = account.name,
                        diameter = PortraitSize,
                        accent = MaterialTheme.colorScheme.secondaryContainer,
                        onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(GlyphSize)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(GlyphIconSize),
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text =
                            if (account != null) {
                                stringResource(R.string.youtube_account_signed_in_as, account.name)
                            } else {
                                stringResource(R.string.youtube_account_onboarding_title)
                            },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        text =
                            account?.email
                                ?: stringResource(
                                    if (working) R.string.youtube_login_working else R.string.youtube_account_onboarding_subtitle,
                                ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowActionButton(
                text = stringResource(if (account != null) R.string.youtube_account_sign_out else R.string.youtube_account_sign_in),
                onClick = if (account != null) onSignOut else onSignIn,
                enabled = !working,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.youtube_account_risk_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
