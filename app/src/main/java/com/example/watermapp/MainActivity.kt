package com.example.watermapp

import android.R.attr.category
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.gson.Gson
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.watermapp.ui.theme.WaterAppTheme
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)


            //SeedUploader.uploadFromJson(this) //KALDIR firestorden veri çekiyor


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



//sharedpreferences
object LoginPrefs {

    fun saveLogin(context: Context, isStaff: Boolean) {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_staff", isStaff).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_staff", false)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences("login", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}


//sepetteki üürnler ve müşteri kayıt formunu firebase kaydet
data class Customer(
    val customerName: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val staffName: String = ""
)

data class OrderItem(
    val model: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val currency: String = "USD"
)

data class Order(
    val customer: Customer = Customer(),
    val items: List<OrderItem> = emptyList(),
    val baseTotals: Map<String, Double> = emptyMap(),      // iskonto öncesi
    val discountedTotals: Map<String, Double> = emptyMap(), // iskonto sonrası
    val discountPercent: Double = 0.0,                      // % iskonto
    val timestamp: Long = System.currentTimeMillis()
)

data class CartItem(
    val urun: Urun,
    var quantity: MutableState<Int> = mutableStateOf(1)
)
object CartManager {
    var usdRate by mutableStateOf(32.0)
    var eurRate by mutableStateOf(35.0)

    var cart = mutableStateListOf<CartItem>()

    var appliedDiscountPercent by mutableStateOf(0.0)

    fun applyDiscount(percent: Double) {
        appliedDiscountPercent = percent
    }

    var discountPercent by mutableStateOf(0.0)

    fun totalsByCurrency(): Map<String, Double> {
        return cart
            .groupBy { it.urun.paraBirimi }
            .mapValues { entry ->
                entry.value.sumOf {
                    (it.urun.fiyat_usd ?: 0.0) * it.quantity.value
                }
            }
    }

    fun totalsWithDiscount(): Map<String, Double> {
        val base = totalsByCurrency()

        return base.mapValues { (_, total) ->
            total * (1 - appliedDiscountPercent / 100)
        }
    }

    fun totalInTL(): Double {

        val rawTotal = cart.sumOf { item ->

            val price = item.urun.fiyat_usd ?: 0.0
            val qty = item.quantity.value

            when (item.urun.paraBirimi) {
                "USD" -> price * usdRate * qty
                "EUR" -> price * eurRate * qty
                "TL" -> price * qty
                else -> 0.0
            }
        }

        // iskonto uygula
        return rawTotal * (1 - appliedDiscountPercent / 100)
    }

    fun add(urun: Urun) {
        val existing = cart.find { it.urun.urun_model == urun.urun_model }

        if (existing != null) {
            existing.quantity.value++
        } else {
            cart.add(CartItem(urun))
        }
    }

    object SessionManager {
        var isStaff by mutableStateOf(false)
    }

    fun remove(urun: Urun) {
        val existing = cart.find { it.urun.urun_model == urun.urun_model }

        if (existing != null) {
            existing.quantity.value--

            if (existing.quantity.value <= 0) {
                cart.remove(existing)
            }
        }
    }

    fun getQuantity(urun: Urun): Int {
        return cart.find { it.urun.urun_model == urun.urun_model }
            ?.quantity?.value ?: 0
    }

    fun totalPrice(): Double {
        return cart.sumOf {
            (it.urun.fiyat_usd ?: 0.0) * it.quantity.value
        }
    }
}

@Composable
fun AppNavigator(startDestination: String) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(
            "edit/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->

            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            var urun by remember { mutableStateOf<Urun?>(null) }

            LaunchedEffect(productId) {
                ProductRepository.getProducts { list ->
                    urun = list.find { it.id == productId }
                }
            }

            if (urun == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                EditProductScreen(urun!!, productId, navController)
            }


        }

        composable(
            "add_product_form/{ana}/{alt}",
            arguments = listOf(
                navArgument("ana") { type = NavType.StringType },
                navArgument("alt") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ana = backStackEntry.arguments?.getString("ana")?.fromNavArg() ?: ""
            val alt = backStackEntry.arguments?.getString("alt")?.fromNavArg() ?: ""

            AddProductFormScreen(navController, ana, alt)
        }

        composable("categories") {
            MainLayout(navController) {
                CategoryScreen(navController)
            }
        }

        composable("editOrder/{orderId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("orderId") ?: ""
            EditOrderScreen(id, navController)
        }

        composable("orders") {
            OrdersScreen(navController)
        }

        composable("cart") {
            CartScreen(navController)
        }

        composable("customerForm") {
            CustomerFormScreen(navController)
        }

        composable(
            "evselDetail/{kategoriAdi}",
            arguments = listOf(navArgument("kategoriAdi") { type = NavType.StringType })
        ) { backStackEntry ->

            val kategoriAdi =
                backStackEntry.arguments?.getString("kategoriAdi")?.fromNavArg() ?: ""

            EvselDetailScreen(kategoriAdi, navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("evsel") {
            EvselScreen(navController)
        }


        //composable("evsel") { SimplePage("Evsel Ürünler") }
        composable("endustriyel") { SimplePage("Endüstriyel Ürünler") }
        composable("kimyasal") { SimplePage("Kimyasal Ürünler") }
        composable("peyzaj") { SimplePage("Peyzaj Ürünleri") }
    }
}
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
                    navController.navigate("categories")
                }
                else {
                    errorText = "Hatalı giriş!"
                }
            }) {
                Text("Giriş Yap")
            }

            Text(errorText, color = MaterialTheme.colorScheme.error)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(navController: NavHostController) {

    val categories = listOf(
        Triple("Evsel", "evsel", Icons.Default.Home),
        Triple("Endüstriyel", "endustriyel", Icons.Default.Build),
        Triple("Kimyasal", "kimyasal", Icons.Default.Science),
        Triple("Peyzaj", "peyzaj", Icons.Default.Park)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "Kategoriler",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(categories) { (title, route, icon) ->

                Card(
                    onClick = { navController.navigate(route) },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF8C00)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            icon,
                            contentDescription = title,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EvselScreen(navController: NavHostController) {

    var kategoriler by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {

        val listener = FirebaseFirestore.getInstance()
            .collection("products")
            .addSnapshotListener { snapshot, _ ->

                kategoriler = snapshot?.documents
                    ?.mapNotNull { doc ->

                        val ana = doc.getString("anaKategori")
                            ?: doc.getString("kategori") // eski alan

                        if (ana == "Evsel")
                            doc.getString("altKategori")
                        else null
                    }
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    AppScaffold("Evsel Ürün Kategorileri", navController) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(kategoriler) { kategori ->

                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate(
                                "evselDetail/${kategori.toNavArg()}"
                            )
                        }
                ) {
                    Text(
                        text = kategori,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvselDetailScreen(
    kategoriAdi: String,
    navController: NavHostController
) {
    var urunler by remember { mutableStateOf<List<Urun>>(emptyList()) }

    // 🔥 FIREBASE CANLI LİSTE
    DisposableEffect(kategoriAdi) {

        val listener = FirebaseFirestore.getInstance()
            .collection("products")
            .whereEqualTo("anaKategori", "Evsel")
            .whereEqualTo("altKategori", kategoriAdi)
            .addSnapshotListener { snapshot, _ ->
                urunler = snapshot?.documents?.map { doc ->
                    doc.toObject(Urun::class.java)!!.copy(id = doc.id)
                } ?: listOf()
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text(kategoriAdi) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {

                    // ✅ SADECE PERSONEL GÖRSÜN
                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                            navController.navigate(
                                "add_product_form/${"Evsel".toNavArg()}/${kategoriAdi.toNavArg()}"
                            )
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Ürün ekle")
                        }
                    }
                }
            )
        },

                bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { navController.navigate("cart") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ShoppingCart, "Sepet")
                }
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(urunler) { urun ->

                val qty = CartManager.getQuantity(urun)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        var expanded by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.weight(1f)) {

                            Text(urun.urun_model, style = MaterialTheme.typography.titleMedium)
                            Text("Fiyat: ${urun.fiyat_usd ?: "-"} ${urun.paraBirimi}")
                            urun.marka?.let { Text("Marka: $it") }
                            urun.pompa?.let { Text("Pompa: $it") }
                            urun.asama?.let { Text("Aşama: $it") }
                            urun.membran_gpd?.let { Text("Membran: $it GPD") }
                            urun.tank_galon?.let { Text("Tank: $it Galon") }


                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "Gizle" else "Açıklama")
                            }

                            if (expanded) {
                                Text(
                                    text = urun.ozellikler ?: "Bilgi yok",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = Int.MAX_VALUE
                                )
                            }

                            if (CartManager.SessionManager.isStaff) {
                                TextButton(onClick = {
                                    navController.navigate("edit/${urun.id}")
                                }) {
                                    Text("Düzenle")
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            IconButton(onClick = { CartManager.remove(urun) }) {
                                Text("-", style = MaterialTheme.typography.headlineMedium)
                            }

                            Text(
                                text = qty.toString(),
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(onClick = { CartManager.add(urun) }) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}


fun loadEvselData(context: Context): Root {
    val jsonString = context.assets
        .open("evsel_products.json") // dosya adın bu olmalı!
        .bufferedReader()
        .use { it.readText() }

    return Gson().fromJson(jsonString, Root::class.java)
}

@Composable
fun CategoryButton(
    title: String,
    route: String,
    navController: NavHostController
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = { navController.navigate(route) }
    ) {
        Text(title)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavHostController) {

    val cart = CartManager.cart
    val totals = CartManager.totalsWithDiscount()
    val baseTotals = CartManager.totalsByCurrency()

    val context = LocalContext.current
    val view = LocalView.current

    var usdText by remember { mutableStateOf(CartManager.usdRate.toString()) }
    var eurText by remember { mutableStateOf(CartManager.eurRate.toString()) }

    var discountText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sepet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {

                    IconButton(onClick = {
                        printView(context, view)
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Yazdır")
                    }

                    IconButton(onClick = {
                        navController.navigate("customerForm")
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Müşteri Kaydet")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    value = usdText,
                    onValueChange = {
                        usdText = it
                        CartManager.usdRate = it.toDoubleOrNull() ?: 0.0
                    },
                    label = { Text("USD Kuru") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = eurText,
                    onValueChange = {
                        eurText = it
                        CartManager.eurRate = it.toDoubleOrNull() ?: 0.0
                    },
                    label = { Text("EUR Kuru") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Genel Toplam: %.2f ₺".format(CartManager.totalInTL()),
                style = MaterialTheme.typography.headlineMedium
            )

            // ✅ SEPET LİSTESİ
            LazyColumn(modifier = Modifier.weight(1f)) {

                items(cart) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = item.urun.urun_model,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "Adet: ${item.quantity.value}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ✅ İSKONTO ALANI
            Row(verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("% İskonto") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(10.dp))

                Button(onClick = {
                    val percent = discountText.toDoubleOrNull() ?: 0.0
                    CartManager.applyDiscount(percent)
                }) {
                    Text("Uygula")
                }
            }

            Spacer(Modifier.height(12.dp))

            // ✅ TOPLAMLAR
            baseTotals.forEach { (currency, baseTotal) ->

                val discounted = totals[currency] ?: baseTotal

                Column {

                    // 👉 indirimsiz (silik)
                    if (CartManager.appliedDiscountPercent > 0){
                        Text(
                            text = when (currency) {
                                "USD" -> "USD: %.2f $".format(baseTotal)
                                "EUR" -> "EUR: %.2f €".format(baseTotal)
                                "TL" -> "TL: %.2f ₺".format(baseTotal)
                                else -> "$currency: $baseTotal"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    // 👉 indirimli (ana fiyat)
                    Text(
                        text = when (currency) {
                            "USD" -> "Toplam: %.2f $".format(discounted)
                            "EUR" -> "Toplam: %.2f €".format(discounted)
                            "TL" -> "Toplam: %.2f ₺".format(discounted)
                            else -> "$currency: $discounted"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

@Composable
fun SimplePage(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    urun: Urun,
    productId: String,
    navController: NavHostController
) {

    var model by rememberSaveable { mutableStateOf(urun.urun_model) }
    var marka by rememberSaveable { mutableStateOf(urun.marka ?: "") }
    var pompa by rememberSaveable { mutableStateOf(urun.pompa ?: "") }
    var asama by rememberSaveable { mutableStateOf(urun.asama ?: "") }
    var membran by rememberSaveable { mutableStateOf(urun.membran_gpd?.toString() ?: "") }
    var tank by rememberSaveable { mutableStateOf(urun.tank_galon?.toString() ?: "") }
    var kod by rememberSaveable { mutableStateOf(urun.urun_kodu?.toString() ?: "") }
    var fiyat by rememberSaveable { mutableStateOf(urun.fiyat_usd?.toString() ?: "") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürünü Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item { OutlinedTextField(model, { model = it }, label = { Text("Model") }) }
            item { OutlinedTextField(marka, { marka = it }, label = { Text("Marka") }) }
            item { OutlinedTextField(pompa, { pompa = it }, label = { Text("Pompa") }) }
            item { OutlinedTextField(asama, { asama = it }, label = { Text("Aşama") }) }
            item { OutlinedTextField(membran, { membran = it }, label = { Text("Membran") }) }
            item { OutlinedTextField(tank, { tank = it }, label = { Text("Tank") }) }
            item { OutlinedTextField(kod, { kod = it }, label = { Text("Kod") }) }
            item { OutlinedTextField(fiyat, { fiyat = it }, label = { Text("Fiyat USD") }) }

            // ✅ KAYDET
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        val updated = urun.copy(
                            urun_model = model,
                            marka = marka,
                            pompa = pompa,
                            asama = asama,
                            membran_gpd = membran.toDoubleOrNull(),
                            tank_galon = tank.toDoubleOrNull(),
                            urun_kodu = kod.toIntOrNull(),
                            fiyat_usd = fiyat.toDoubleOrNull()
                        )

                        ProductRepository.updateProduct(productId, updated)

                        scope.launch {
                            snackbarHostState.showSnackbar("Değişiklikler kaydedildi")
                            delay(1200)
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            }

            // ✅ SİL
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = { showDeleteDialog = true }
                ) {
                    Text("Sil")
                }
            }
        }

        // ✅ DELETE DIALOG
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Ürünü sil") },
                text = { Text("Silmek istediğinize emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            FirebaseFirestore.getInstance()
                                .collection("products")
                                .document(productId)
                                .delete()

                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    ) { Text("Evet") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) { Text("Hayır") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(navController: NavHostController) {

    var customerName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var staffName by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Müşteri Kaydı") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {

                        // ✅ müşteri oluştur
                        val customer = Customer(
                            customerName = customerName,
                            companyName = companyName,
                            phone = phone,
                            email = email,
                            staffName = staffName
                        )

                        // ✅ sepet ürünlerini al
                        val items = CartManager.cart.map {
                            OrderItem(
                                model = it.urun.urun_model,
                                quantity = it.quantity.value,
                                price = it.urun.fiyat_usd ?: 0.0,
                                currency = it.urun.paraBirimi
                            )
                        }

                        // ✅ sipariş oluştur
                        val baseTotals = CartManager.totalsByCurrency()
                        val discountedTotals = CartManager.totalsWithDiscount()

                        val order = Order(
                            customer = customer,
                            items = items,
                            baseTotals = baseTotals,
                            discountedTotals = discountedTotals,
                            discountPercent = CartManager.appliedDiscountPercent
                        )

                        // ✅ firebase kaydet
                        saveOrderToFirebase(order)

                        scope.launch {
                            snackbarHostState.showSnackbar("Sipariş kaydedildi ✅")
                            delay(1200)

                            CartManager.cart.clear()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            }
        },

        snackbarHost = { SnackbarHost(snackbarHostState) }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Müşteri Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Şirket Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Mail Adresi") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = staffName,
                    onValueChange = { staffName = it },
                    label = { Text("Personel Adı") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

fun saveOrderToFirebase(order: Order) {
    val db = Firebase.firestore

    db.collection("orders")
        .add(order)
        .addOnSuccessListener {
            println("Sipariş kaydedildi")
        }
        .addOnFailureListener {
            println("Hata: ${it.message}")
        }
}
data class OrderDoc(
    val id: String,
    val order: Order
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavHostController) {

    if (!CartManager.SessionManager.isStaff) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    var search by remember { mutableStateOf("") }
    var orders by remember { mutableStateOf<List<OrderDoc>>(emptyList()) }

    // 👉 silme için dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<OrderDoc?>(null) }

    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("orders")
            .addSnapshotListener { snapshot, _ ->
                orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.let {
                        OrderDoc(doc.id, it)
                    }
                } ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    val filtered = orders.filter {
        it.order.customer.customerName.contains(search, true) ||
                it.order.customer.companyName.contains(search, true)
    }

    // ✅ SİLME ONAY DİYALOĞU
    if (showDeleteDialog && orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Silme Onayı") },
            text = { Text("Bu siparişi silmek istediğine emin misin?") },
            confirmButton = {
                TextButton(onClick = {
                    Firebase.firestore
                        .collection("orders")
                        .document(orderToDelete!!.id)
                        .delete()

                    showDeleteDialog = false
                    orderToDelete = null
                }) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    orderToDelete = null
                }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Müşteri Kayıtları") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Müşteri / Şirket ara") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn {
                items(filtered) { orderDoc ->

                    val order = orderDoc.order

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {

                            Text("Müşteri: ${order.customer.customerName}")
                            Text("Şirket: ${order.customer.companyName}")
                            Text("Telefon: ${order.customer.phone}")
                            Text("Personel: ${order.customer.staffName}")
                            Text("Tarih: ${formatDate(order.timestamp)}")

                            Spacer(Modifier.height(6.dp))

                            order.items.forEach {
                                Text("- ${it.model} x${it.quantity}")
                            }

                            Text("İskonto: %${order.discountPercent}")

                            order.baseTotals.forEach { (currency, base) ->

                                val discounted = order.discountedTotals[currency] ?: base
                                val diff = base - discounted

                                Column {

                                    // normal fiyat
                                    Text(
                                        "$currency Normal: %.2f".format(base),
                                        color = Color.Gray
                                    )

                                    // indirimli fiyat
                                    Text(
                                        "$currency İndirimli: %.2f".format(discounted),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    // iskonto miktarı
                                    if (diff > 0) {
                                        Text(
                                            "$currency İskonto: %.2f".format(diff),
                                            color = Color(0xFF2E7D32) // yeşil
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // ✅ SİL BUTONU
                            Button(
                                onClick = {
                                    orderToDelete = orderDoc
                                    showDeleteDialog = true
                                }
                            ) {
                                Text("Sil")
                            }
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },

                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },

                actions = {
                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Ürün ekle")
                        }
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                        navController.navigate("categories")
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Ana sayfa")
                    }
                }
            }
        }
    ) { padding ->
        content(padding)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductFormScreen(
    navController: NavHostController,
    anaKategori: String,
    altKategori: String
) {

    var model by rememberSaveable { mutableStateOf("") }
    var marka by rememberSaveable { mutableStateOf("") }
    var pompa by rememberSaveable { mutableStateOf("") }
    var asama by rememberSaveable { mutableStateOf("") }
    var membran by rememberSaveable { mutableStateOf("") }
    var tank by rememberSaveable { mutableStateOf("") }
    var kod by rememberSaveable { mutableStateOf("") }
    var fiyat by rememberSaveable { mutableStateOf("") }
    var ozellikler by rememberSaveable { mutableStateOf("") }

    var paraBirimi by rememberSaveable { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Ekle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Text("Kategori: $anaKategori / $altKategori")
            }

            item { OutlinedTextField(model, { model = it }, label = { Text("Model") }) }
            item { OutlinedTextField(marka, { marka = it }, label = { Text("Marka") }) }
            item { OutlinedTextField(pompa, { pompa = it }, label = { Text("Pompa") }) }
            item { OutlinedTextField(asama, { asama = it }, label = { Text("Aşama") }) }
            item { OutlinedTextField(membran, { membran = it }, label = { Text("Membran") }) }
            item { OutlinedTextField(tank, { tank = it }, label = { Text("Tank") }) }
            item { OutlinedTextField(kod, { kod = it }, label = { Text("Kod") }) }

            // ✅ Fiyat
            item {
                OutlinedTextField(
                    fiyat,
                    { fiyat = it },
                    label = { Text("Fiyat") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ✅ PARA BİRİMİ DROPDOWN
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = paraBirimi,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Para Birimi") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("USD", "EUR", "TL").forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    paraBirimi = currency
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item { OutlinedTextField(ozellikler, { ozellikler = it }, label = { Text("Özellikler") }) }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        val urun = Urun(
                            anaKategori = anaKategori,
                            altKategori = altKategori,
                            urun_model = model,
                            marka = marka,
                            pompa = pompa,
                            asama = asama,
                            membran_gpd = membran.toDoubleOrNull(),
                            tank_galon = tank.toDoubleOrNull(),
                            urun_kodu = kod.toIntOrNull(),
                            fiyat_usd = fiyat.toDoubleOrNull(),
                            paraBirimi = paraBirimi,
                            ozellikler = ozellikler
                        )

                        Firebase.firestore
                            .collection("products")
                            .add(urun)

                        scope.launch {
                            snackbar.showSnackbar("Ürün eklendi ✅")
                            delay(1000)
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            }
        }
    }
}
@Composable
fun EditOrderScreen(orderId: String, navController: NavHostController) {

    var order by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(orderId) {
        Firebase.firestore.collection("orders")
            .document(orderId)
            .get()
            .addOnSuccessListener {
                order = it.toObject(Order::class.java)
            }
    }

    val currentOrder = order

    if (currentOrder == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var name by remember { mutableStateOf(currentOrder.customer.customerName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            Text(
                "İskonto: %${currentOrder.discountPercent}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(currentOrder.baseTotals.toList()) { (currency, base) ->

            val discounted = currentOrder.discountedTotals[currency] ?: base
            val diff = base - discounted

            Column {
                Text(
                    "$currency Normal: %.2f".format(base),
                    color = Color.Gray
                )
                Text("$currency İndirimli: %.2f".format(discounted))
                Text("$currency İskonto: %.2f".format(diff))
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Müşteri Adı") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    val updated = currentOrder.copy(
                        customer = currentOrder.customer.copy(
                            customerName = name
                        )
                    )

                    Firebase.firestore.collection("orders")
                        .document(orderId)
                        .set(updated)

                    navController.popBackStack()
                }
            ) {
                Text("Kaydet")
            }
        }
    }
}

fun String.toNavArg(): String = Uri.encode(this)
fun String.fromNavArg(): String = Uri.decode(this)

fun formatDate(time: Long): String {
    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(time))
}

fun String.normalizeCategory(): String {
    return this
        .trim()
        .lowercase()
        .replace("ı", "i")
        .replace("ş", "s")
        .replace("ç", "c")
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ö", "o")
        .replaceFirstChar { it.uppercase() }
}