package com.example.watermapp.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EvselForm(

    pompa: String,
    onPompaChange: (String) -> Unit,

    asama: String,
    onAsamaChange: (String) -> Unit,

    membran: String,
    onMembranChange: (String) -> Unit,

    tank: String,
    onTankChange: (String) -> Unit,

    marka: String,
    onMarkaChange: (String) -> Unit

) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        OutlinedTextField(
            value = pompa,
            onValueChange = onPompaChange,
            label = { Text("Pompa") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = asama,
            onValueChange = onAsamaChange,
            label = { Text("Aşama") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = membran,
            onValueChange = onMembranChange,
            label = { Text("Membran GPD") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = tank,
            onValueChange = onTankChange,
            label = { Text("Tank Galon") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = marka,
            onValueChange = onMarkaChange,
            label = { Text("Marka") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}