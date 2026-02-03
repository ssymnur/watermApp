package com.example.watermapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.watermapp.ui.theme.WatermAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.FirebaseApp //
import com.google.firebase.firestore.FirebaseFirestore //
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentSnapshot



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            WatermAppTheme {
                AppNavigator()
            }
        }
    }
}

val db = FirebaseFirestore.getInstance()


@Composable
fun StartScreen(navController: NavHostController) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") } // ✅ eksik olan buydu

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Hoşgeldiniz", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))

        // 👤 Müşteri girişi
        Button(onClick = { navController.navigate("main/customer") }) {
            Text("Müşteri")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoggedIn) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Kullanıcı Adı") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Şifre") },
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                if (username == "waterm" && password == "waterm123") {
                    error = ""
                    isLoggedIn = true
                    navController.navigate("main/staff")
                } else {
                    error = "Kullanıcı adı veya şifre hatalı!"
                }
            }) {
                Text("Personel Girişi")
            }

            // ❌ Hata mesajı
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {
            Button(onClick = { navController.navigate("main/staff") }) {
                Text("Personel Paneline Git")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, role: String) {

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val isStaff = role == "staff"

    val categories = remember {
        mutableStateListOf(
            Category("Valfler ve Yedek Parçaları"),
            Category("Tanklar"),
            Category("Kabinetler"),
            Category("Seko"),
            Category("UV"),
            Category("Filtreler"),
            Category("Pompa"),
            Category("Motor"),
            Category("Vanalar"),
            Category("Borular"),
            Category("Fittings"),
            Category("Diğer")
        )
    }

    var totalPrice by remember { mutableStateOf(0.0) }

    fun updateTotal() {
        totalPrice = categories.sumOf { cat ->
            cat.products.sumOf { it.price * it.quantity.value }
        }
    }

    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var showCartDialog by remember { mutableStateOf(false) }

    val totalItems by derivedStateOf {
        categories.sumOf { cat -> cat.products.sumOf { it.quantity.value } }
    }

    // ✅ FIREBASE REALTIME LISTENER
    LaunchedEffect(Unit) {
        db.collection("products")
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) return@addSnapshotListener

                categories.forEach { it.products.clear() }

                snapshot.documents.forEach { doc ->

                    val product = Product(
                        doc.getString("name") ?: "",
                        doc.getString("code") ?: "",
                        doc.getString("model") ?: "",
                        doc.getDouble("price") ?: 0.0
                    )

                    val cat = categories.find {
                        it.name == doc.getString("category")
                    }

                    cat?.products?.add(product)
                }

                updateTotal()
            }
    }



    // ---------------- UI ----------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WaterM Ürün Kategorileri", color = Color.White) },
                actions = {
                    if (isStaff) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Text("+", color = Color.White, fontSize = 28.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        },

        bottomBar = {
            Surface(color = Color(0xFFFF8F00)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Toplam Fiyat: $totalPrice TL", color = Color.White)

                    if (isStaff) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showCartDialog = true }
                        ) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                            Text(" $totalItems", color = Color.White)
                        }
                    }
                }
            }
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3F2FD))
                .padding(innerPadding)
        ) {
            LazyColumn {
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        isExpanded = expandedCategory == category.name,
                        onExpandToggle = {
                            expandedCategory =
                                if (expandedCategory == category.name) null else category.name
                        },
                        onProductChange = { updateTotal() },
                        onEdit = { editingProduct = it },
                        isStaff = isStaff
                    )
                }
            }
        }
    }

    // ---------------- ÜRÜN EKLE ----------------

    if (showAddDialog) {

        var selectedCategory by remember { mutableStateOf(categories.first().name) }
        var name by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Ürün Ekle") },
            text = {
                Column {
                    DropdownMenuBox(
                        categories = categories.map { it.name },
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )

                    OutlinedTextField(name, { name = it }, label = { Text("Ürün Adı") })
                    OutlinedTextField(model, { model = it }, label = { Text("Model") })
                    OutlinedTextField(code, { code = it }, label = { Text("Kod") })
                    OutlinedTextField(price, { price = it }, label = { Text("Fiyat") })
                }
            },
            confirmButton = {
                TextButton(onClick = {

                    val productData = hashMapOf(
                        "name" to name,
                        "model" to model,
                        "code" to code,
                        "price" to (price.toDoubleOrNull() ?: 0.0),
                        "category" to selectedCategory
                    )

                    db.collection("products").add(productData)

                    showAddDialog = false
                }) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }


    // ---------------- ÜRÜN GÜNCELLE DIALOG ----------------

    editingProduct?.let { product ->

        var name by remember { mutableStateOf(product.name) }
        var model by remember { mutableStateOf(product.model) }
        var code by remember { mutableStateOf(product.code) }
        var price by remember { mutableStateOf(product.price.toString()) }

        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text("Ürün Güncelle") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Ürün Adı") })
                    OutlinedTextField(model, { model = it }, label = { Text("Model") })
                    OutlinedTextField(code, { code = it }, label = { Text("Kod") })
                    OutlinedTextField(price, { price = it }, label = { Text("Fiyat") })
                }
            },
            confirmButton = {
                TextButton(onClick = {

                    product.name = name
                    product.model = model
                    product.code = code
                    product.price = price.toDoubleOrNull() ?: product.price

                    updateTotal()
                    editingProduct = null

                }) {
                    Text("Kaydet")
                }
            },

                    dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text("İptal")
                }
            }
        )
    }


    // ---------------- SEPET ----------------

    if (showCartDialog) {

        var discount by remember { mutableStateOf(0f) }

        val itemsInCart = categories
            .flatMap { it.products }
            .filter { it.quantity.value > 0 }

        val discountedTotal =
            (itemsInCart.sumOf { it.price * it.quantity.value }) * (1 - discount / 100)

        AlertDialog(
            onDismissRequest = { showCartDialog = false },
            title = { Text("Sepetiniz") },
            text = {
                Column {
                    if (itemsInCart.isEmpty()) {
                        Text("Sepetiniz boş")
                    } else {
                        itemsInCart.forEach { prod ->
                            Text("${prod.name} x${prod.quantity.value}")
                        }

                        OutlinedTextField(
                            value = if (discount == 0f) "" else discount.toString(),
                            onValueChange = { discount = it.toFloatOrNull() ?: 0f },
                            label = { Text("İskonto (%)") }
                        )

                        Text("İskontolu Toplam: $discountedTotal TL")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCartDialog = false
                    navController.navigate(
                        "customer/${discountedTotal.toFloat()}/${discount}"
                    )
                }) { Text("Kaydet") }
            }
        )
    }
}


@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "start") {
        composable("start") { StartScreen(navController) }
        composable("main/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "customer"
            MainScreen(navController, role)
        }
        composable(
            "customer/{total}/{discount}",
            arguments = listOf(
                navArgument("total") { type = NavType.FloatType },
                navArgument("discount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val total = backStackEntry.arguments?.getFloat("total") ?: 0f
            val discount = backStackEntry.arguments?.getFloat("discount") ?: 0f
            CustomerScreen(total.toDouble(), discount.toDouble())
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(total: Double, discount: Double) {
    var customerName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val personel = "Ali Veli"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Müşteri Kaydı", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Müşteri Adı Soyadı") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Şirket Adı") })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Toplam Tutar: $total TL")
        Text("Uygulanan İskonto: $discount %")
        Text("İlgilenen Personel: $personel")
        Text("Tarih: $date")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItem(
    category: Category,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onProductChange: () -> Unit,
    onEdit: (Product) -> Unit,
    isStaff: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF64B5F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandToggle() },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(if (isExpanded) "▶" else "▼")
            }
            if (isExpanded && category.products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                category.products.forEach { product ->
                    ProductRow(
                        product = product,
                        onQuantityChange = onProductChange,
                        onEdit = onEdit,
                        isStaff = isStaff
                    )
                }

            }
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    onQuantityChange: () -> Unit,
    onEdit: (Product) -> Unit,
    isStaff: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        // Ürün bilgisi (üst satır)
        val unitPrice = product.price
        val totalProductPrice = unitPrice * product.quantity.value

        Text(
            if (product.quantity.value == 0)
                "${product.name} "
            else
                "${product.name} (${unitPrice} TL)  →  $totalProductPrice TL",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            "Model: ${product.model} • Kod: ${product.code}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Alt kontrol satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = {
                    if (product.quantity.value > 0) {
                        product.quantity.value--
                        onQuantityChange()
                    }
                }) { Text("-") }

                Text(
                    "${product.quantity.value}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                IconButton(onClick = {
                    product.quantity.value++
                    onQuantityChange()
                }) { Text("+") }

                if (isStaff) {
                    TextButton(onClick = { onEdit(product) }) {
                        Text("Düzenle")
                    }
                }
            }

            Text(
                "${product.price} TL",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Divider()
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

