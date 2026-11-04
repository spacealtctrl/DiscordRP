package net.spacealtctrl.discordrp.ui.screens.cover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.ui.kit.QuietButton
import net.spacealtctrl.discordrp.ui.theme.Look

@Composable
fun AddCoverLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cover_missing_note),
            style = MaterialTheme.typography.bodySmall,
            color = Look.palette.inkGhost,
        )
        QuietButton(
            text = stringResource(R.string.cover_add_action),
            icon = Icons.Rounded.AddPhotoAlternate,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Look.gaps.snug),
        )
    }
}
