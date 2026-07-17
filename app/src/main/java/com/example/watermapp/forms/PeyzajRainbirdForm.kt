package com.example.watermapp.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PeyzajRainbirdForm(

    urunAdi: String,
    onUrunAdiChange: (String) -> Unit,

    kod: String,
    onKodChange: (String) -> Unit,

    fiyat: String,
    onFiyatChange: (String) -> Unit,

    ozellikler: String,
    onOzelliklerChange: (String) -> Unit,

    model: String,
    onModelChange: (String) -> Unit,

    aciklama: String,
    onAciklamaChange: (String) -> Unit,

    agirlikKg: String,
    onAgirlikKgChange: (String) -> Unit,

    bayiiAdedi: String,
    onBayiiAdediChange: (String) -> Unit,

    koliAdedi: String,
    onKoliAdediChange: (String) -> Unit,

    paletAdedi: String,
    onPaletAdediChange: (String) -> Unit

) {

    OutlinedTextField(
        value = urunAdi,
        onValueChange = onUrunAdiChange,
        label = { Text("Ürün Adı") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = kod,
        onValueChange = onKodChange,
        label = { Text("Kod") },
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
        value = model,
        onValueChange = onModelChange,
        label = { Text("Model") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = aciklama,
        onValueChange = onAciklamaChange,
        label = { Text("Açıklama") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = agirlikKg,
        onValueChange = onAgirlikKgChange,
        label = { Text("Ağırlık / KG") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = bayiiAdedi,
        onValueChange = onBayiiAdediChange,
        label = { Text("Bayii Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = koliAdedi,
        onValueChange = onKoliAdediChange,
        label = { Text("Koli Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = paletAdedi,
        onValueChange = onPaletAdediChange,
        label = { Text("Palet Adedi") },
        modifier = Modifier.fillMaxWidth()
    )

}