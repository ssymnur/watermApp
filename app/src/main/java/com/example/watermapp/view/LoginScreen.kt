package com.example.watermapp.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watermapp.data.CartManager
import com.example.watermapp.data.LoginPrefs
import com.google.firebase.auth.FirebaseAuth

    @Composable
    fun LoginScreen(navController: NavHostController) {
        val context = LocalContext.current

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showLogin by remember { mutableStateOf(false) }
        var errorText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Giriş", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(20.dp))

            // ✅ MÜŞTERİ GİRİŞİ
            Button(onClick = {
                CartManager.SessionManager.isStaff = false   // ← önemli
                navController.navigate("categories")
            }) {
                Text("Müşteri Girişi")
            }

            Spacer(Modifier.height(10.dp))

            Button(onClick = { showLogin = true }) {
                Text("Personel Girişi")
            }

            if (showLogin) {

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Kullanıcı Adı") }
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Şifre") }
                )

                Spacer(Modifier.height(10.dp))

                Button(onClick = {

                    if (username == "waterm" && password == "waterm123") {

                        CartManager.SessionManager.isStaff = true

                        LoginPrefs.saveLogin(context, true)

                        FirebaseAuth.getInstance()
                            .signInAnonymously()

                        navController.navigate("categories") {
                            popUpTo("login") { inclusive = true }
                        }

                    } else {
                        errorText = "Hatalı giriş!"
                    }

                }) {
                    Text("Giriş Yap")
                }

                Text(errorText, color = MaterialTheme.colorScheme.error)
            }
        }
    }
