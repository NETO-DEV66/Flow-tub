package com.neurotube.app.ui.screens.settings.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neurotube.app.R
import com.neurotube.app.ui.components.shared.FlowAlertDialog

private val ContentMaxHeight = 440.dp

/** States plainly that the app is independent and unaffiliated with the services it reads. */
@Composable
internal fun DisclaimerDialog(onDismiss: () -> Unit) {
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_disclaimer_title)) },
        text = {
            Box(Modifier.fillMaxWidth()) {
                SelectionContainer(Modifier.heightIn(max = ContentMaxHeight).verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.about_disclaimer_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
