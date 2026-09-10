package com.seymanur.watermapp.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun KimyasalForm(

    urunAdi: String,
    onUrunAdiChange: (String) -> Unit,

    fiyat: String,
    onFiyatChange: (String) -> Unit,

    ozellikler: String,
    onOzelliklerChange: (String) -> Unit,

    ambalajKg: String,
    onAmbalajKgChange: (String) -> Unit,

    kullanimAmaci: String,
    onKullanimAmaciChange: (String) -> Unit

) {

    OutlinedTextField(
        value = urunAdi,
        onValueChange = onUrunAdiChange,
        label = { Text("Ürün Adı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = fiyat,
        onValueChange = onFiyatChange,
        label = { Text("Fiyat") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ozellikler,
        onValueChange = onOzelliklerChange,
        label = { Text("Özellikler") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ambalajKg,
        onValueChange = onAmbalajKgChange,
        label = { Text("Ambalaj / KG") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = kullanimAmaci,
        onValueChange = onKullanimAmaciChange,
        label = { Text("Kullanım Amacı") },
        modifier = Modifier.fillMaxWidth()
    )

}