package com.example.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings Tile allowing 1-tap activation of the Echo Floating Voice Assistant Orb.
 */
class EchoQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Echo Assistant"
            tile.subtitle = "Tap to talk"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Directly summon the Floating Orb Voice Assistant Overlay
        EchoFloatingBubbleService.startVoiceInteraction(this)
    }
}
