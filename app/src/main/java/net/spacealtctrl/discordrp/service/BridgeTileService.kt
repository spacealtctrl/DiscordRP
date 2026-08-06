package net.spacealtctrl.discordrp.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.settings.Stash

class BridgeTileService : TileService() {
    override fun onClick() {
        super.onClick()
        when (qsTile?.state) {
            Tile.STATE_ACTIVE -> {
                stopService(Intent(this, BridgeService::class.java))
                Toast.makeText(this, getString(R.string.tile_stopped_toast), Toast.LENGTH_SHORT)
                    .show()
            }
            Tile.STATE_INACTIVE -> {
                startForegroundService(Intent(this, BridgeService::class.java))
                Toast.makeText(this, getString(R.string.tile_started_toast), Toast.LENGTH_SHORT)
                    .show()
            }
            else -> Unit
        }
        repaint()
    }

    override fun onStartListening() {
        super.onStartListening()
        repaint()
    }

    private fun repaint() {
        val tile = qsTile ?: return
        if (!Stash.of(this).signedIn) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
            return
        }

        val running = BridgeService.alive.value
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(
            this,
            if (running) R.drawable.ic_tile_halt else R.drawable.ic_tile_go,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (running) R.string.tile_on_subtitle else R.string.tile_off_subtitle
            )
        }
        tile.updateTile()
    }
}
