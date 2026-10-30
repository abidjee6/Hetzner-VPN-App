package com.hetzner.vpnclient

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val VPN_REQUEST_CODE = 101
    private lateinit var etUser: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)

        val prefs = getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        etUser.setText(prefs.getString("user", "vpnuser"))
        etPass.setText(prefs.getString("pass", "DATWADUoFh5w7uHr"))

        btnConnect.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Username aur Password enter karein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("user", user).putString("pass", pass).apply()

            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
            } else {
                startVpn()
            }
        }
    }

    private fun startVpn() {
        val intent = Intent(this, HetznerVpnService::class.java).apply {
            action = "START"
        }
        startService(intent)
        tvStatus.text = "Status: Connected (2.31.89.14)"
        btnConnect.text = "Disconnect"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }
}
