package com.example.watermapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

class CategoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryName = intent.getStringExtra("categoryName")
        setContent {
            MaterialTheme {
                Surface {
                    Text(text = "Kategori: $categoryName", modifier = androidx.compose.ui.Modifier.padding(16.dp))
                }
            }
        }
    }
}
