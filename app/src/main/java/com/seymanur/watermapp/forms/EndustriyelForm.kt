package com.seymanur.watermapp.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EndustriyelForm(

    model: String,
    onModelChange: (String) -> Unit,

    debi: String,
    onDebiChange: (String) -> Unit,

    tankBaglanti: String,
    onTankBaglantiChange: (String) -> Unit,

    girisCikis: String,
    onGirisCikisChange: (String) -> Unit,

    marka: String,
    onMarkaChange: (String) -> Unit

) {

    OutlinedTextField(
        value = model,
        onValueChange = onModelChange,
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = debi,
        onValueChange = onDebiChange,
        label = { Text("Debi (m³/saat)") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = tankBaglanti,
        onValueChange = onTankBaglantiChange,
        label = { Text("Tank Bağlantı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = girisCikis,
        onValueChange = onGirisCikisChange,
        label = { Text("Giriş / Çıkış") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = marka,
        onValueChange = onMarkaChange,
        label = { Text("Marka") },
        modifier = Modifier.fillMaxWidth()
    )

}