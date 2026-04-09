package com.example.watermapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import android.net.Uri
import androidx.compose.material.icons.filled.Person
import com.example.watermapp.data.CartManager
import com.example.watermapp.data.LoginPrefs
import com.example.watermapp.ui.theme.WaterAppTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.example.watermapp.model.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)

        //SeedUploader.uploadFromJson(this, "Evsel", ::loadEvselData)
        //SeedUploader.uploadFromJson(this, "Endüstriyel", ::loadEndustriyelData)
        //SeedUploader.uploadFromJson(this, "Peyzaj", ::loadPeyzajData)
        //SeedUploader.uploadFromJson(this, "Kimyasal", ::loadKimyasalData)

        setContent {
            val context = LocalContext.current

            CartManager.SessionManager.isStaff =
                LoginPrefs.isLoggedIn(context)

            val startDestination =
                if (LoginPrefs.isLoggedIn(context)) "categories"
                else "login"

            WaterAppTheme {
                AppNavigator(startDestination)
            }
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}


fun saveOrderToFirebase(order: Order) {
    val db = FirebaseFirestore.getInstance()

    db.collection("orders")
        .add(order)
        .addOnSuccessListener { docRef ->
            println("Sipariş başarıyla kaydedildi: ${docRef.id}")
        }
        .addOnFailureListener { e ->
            println("Sipariş kaydedilemedi: $e")
        }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    navController: NavHostController,
    content: @Composable () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WaterApp") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(onClick = {
                        navController.navigate("categories") {
                            popUpTo("categories") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Home, "Ana Sayfa")
                    }

                    IconButton(onClick = {
                        navController.navigate("cart")
                    }) {
                        Icon(Icons.Default.ShoppingCart, "Sepet")
                    }

                    // ✅ sadece personel görür
                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                            navController.navigate("orders")
                        }) {
                            Icon(Icons.Default.Person, "Müşteriler")
                        }
                    }
                }
            }
        }

    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}


fun String.toNavArg(): String = Uri.encode(this)
fun String.fromNavArg(): String = Uri.decode(this)

fun formatDate(time: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(time))
}
