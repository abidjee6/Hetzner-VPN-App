package com.hetzner.vpnclient

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class HetznerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START") {
            createTunnel()
        } else if (intent?.action == "STOP") {
            stopTunnel()
        }
        return START_STICKY
    }

    private fun createTunnel() {
        try {
            vpnInterface?.close()

            val builder = Builder()
                .addAddress("192.168.42.50", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1300)
                .setSession("HetznerVPN")

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopTunnel() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTunnel()
    }
}
