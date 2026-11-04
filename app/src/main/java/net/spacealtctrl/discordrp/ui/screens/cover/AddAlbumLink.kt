package net.spacealtctrl.discordrp.ui.screens.cover

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.ui.kit.QuietButton

@Composable
fun AddAlbumLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    QuietButton(
        text = stringResource(R.string.album_add_action),
        icon = Icons.Rounded.LibraryAdd,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    )
}
