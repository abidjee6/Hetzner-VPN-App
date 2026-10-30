package com.hetzner.vpnclient

import android.content.Intent
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.InetAddresses
import com.wireguard.config.InetNetwork
import com.wireguard.config.Peer
import com.wireguard.crypto.KeyPair
import com.wireguard.crypto.Key
import java.net.InetAddress
import kotlin.concurrent.thread

class HetznerVpnService : VpnService() {

    private var backend: GoBackend? = null
    private val tunnel = SimpleTunnel("HetznerTunnel")

    override fun onCreate() {
        super.onCreate()
        backend = GoBackend(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startWireGuard()
            "STOP" -> stopWireGuard()
        }
        return START_STICKY
    }

    private fun startWireGuard() {
        thread {
            try {
                val clientPrivateKey = Key.fromBase64("4I79Sfrbw7npBnxC5wOT4bab6Gmsadac3sfDTC976Wg=")
                val serverPublicKey = Key.fromBase64("E+2IfFhA7YA5pAjyIFxgKIirkT1SzQu2yMSmlipg5Fk=")

                val iface = Interface.Builder()
                    .setKeyPair(KeyPair(clientPrivateKey))
                    .addAddress(InetNetwork.parse("10.8.0.2/32"))
                    .addDnsServer(InetAddress.getByName("1.1.1.1"))
                    .addDnsServer(InetAddress.getByName("8.8.8.8"))
                    .build()

                val peer = Peer.Builder()
                    .setPublicKey(serverPublicKey)
                    .setEndpoint(InetAddresses.parse("2.31.89.14:51820"))
                    .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
                    .setPersistentKeepalive(25)
                    .build()

                val config = Config.Builder()
                    .setInterface(iface)
                    .addPeer(peer)
                    .build()

                backend?.setState(tunnel, Tunnel.State.UP, config)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopWireGuard() {
        thread {
            try {
                backend?.setState(tunnel, Tunnel.State.DOWN, null)
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWireGuard()
    }

    class SimpleTunnel(private val tunnelName: String) : Tunnel {
        override fun getName(): String = tunnelName
        override fun onStateChange(newState: Tunnel.State) {}
    }
}
