package com.hetzner.vpnclient

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.io.FileInputStream
import java.io.FileOutputStream

class HetznerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var vpnThread: Thread? = null

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

            // سرور کی کنفیگریشن کے مطابق 10.8.0.2 سیٹ کریں
            val builder = Builder()
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1280)
                .setSession("HetznerVPN")

            vpnInterface = builder.establish()

            isRunning = true
            startDataTransfer()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startDataTransfer() {
        vpnThread = Thread {
            var tunnelChannel: DatagramChannel? = null
            try {
                tunnelChannel = DatagramChannel.open()
                protect(tunnelChannel.socket()) // لوپ پروٹیکشن تاکہ وی پی این کا ڈیٹا خود اندر نہ پھنسے
                tunnelChannel.connect(InetSocketAddress("2.31.89.14", 51820))
                tunnelChannel.configureBlocking(false)

                val vpnInput = FileInputStream(vpnInterface?.fileDescriptor).channel
                val vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor).channel

                val packet = ByteBuffer.allocate(32767)

                while (isRunning) {
                    var hasActivity = false

                    // فون سے ڈیٹا لے کر Hetzner سرور کی طرف بھیجنا
                    packet.clear()
                    val lengthFromDevice = vpnInput.read(packet)
                    if (lengthFromDevice > 0) {
                        packet.flip()
                        tunnelChannel.write(packet)
                        hasActivity = true
                    }

                    // سرور سے آنے والا ڈیٹا فون کی طرف بھیجنا
                    packet.clear()
                    val lengthFromServer = tunnelChannel.read(packet)
                    if (lengthFromServer > 0) {
                        packet.flip()
                        vpnOutput.write(packet)
                        hasActivity = true
                    }

                    if (!hasActivity) {
                        Thread.sleep(10)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    tunnelChannel?.close()
                } catch (ignored: Exception) {}
            }
        }
        vpnThread?.start()
    }

    private fun stopTunnel() {
        try {
            isRunning = false
            vpnThread?.interrupt()
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
