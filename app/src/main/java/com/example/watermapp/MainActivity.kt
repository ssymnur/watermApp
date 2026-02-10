package com.example.watermapp

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Print
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("seed_done", false)) {
            SeedUploader.uploadFromJson(this)
            prefs.edit().putBoolean("seed_done", true).apply()
        }

        setContent {

            val context = LocalContext.current

            val startDestination =
                if (LoginPrefs.isLoggedIn(context)) "categories"
                else "login"

            AppNavigator(startDestination)
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
    val price: Double = 0.0
)

data class Order(
    val customer: Customer = Customer(),
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CartItem(
    val urun: Urun,
    var quantity: MutableState<Int> = mutableStateOf(1)
)
object CartManager {

    var cart = mutableStateListOf<CartItem>()

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
        composable("add_product/{mainCategory}") { backStackEntry ->
            val mainCategory = backStackEntry.arguments?.getString("mainCategory") ?: ""
            AddProductCategoryScreen(navController, mainCategory)
        }

        composable("add_product_form/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            AddProductFormScreen(navController, category)
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

        composable("categories") {
            CategoryScreen(navController)
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


@Composable
fun CategoryScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Kategoriler", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        CategoryButton("Evsel", "evsel", navController)
        CategoryButton("Endüstriyel", "endustriyel", navController)
        CategoryButton("Kimyasal", "kimyasal", navController)
        CategoryButton("Peyzaj", "peyzaj", navController)
    }
}

@Composable
fun EvselScreen(navController: NavHostController) {

    var kategoriler by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        ProductRepository.getProducts { products ->
            kategoriler = products
                .map { it.kategori }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
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
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
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
            .whereEqualTo("kategori", kategoriAdi)
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
                            Text("Fiyat: ${urun.fiyat_usd ?: "-"} USD")
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
    val total = CartManager.totalPrice()
    val context = LocalContext.current
    val view = LocalView.current

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

                    // ✅ PRINT BUTONU
                    IconButton(onClick = {
                        printView(context, view)
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Yazdır")
                    }

                    // ✅ MÜŞTERİ KAYDET BUTONU
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

            Text(
                text = "Toplam: $total USD",
                style = MaterialTheme.typography.headlineSmall
            )
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
}@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(title = { Text("Ürünü Düzenle") })
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
                            kategori = urun.kategori,
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
                                price = it.urun.fiyat_usd ?: 0.0
                            )
                        }

                        // ✅ sipariş oluştur
                        val order = Order(
                            customer = customer,
                            items = items,
                            total = CartManager.totalPrice()
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

//müşteri liste ekranı

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavHostController) {

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }

    DisposableEffect(Unit) {

        val listener = Firebase.firestore.collection("orders")
            .addSnapshotListener { snapshot, _ ->
                orders = snapshot?.documents?.mapNotNull {
                    it.toObject(Order::class.java)
                } ?: emptyList()
            }

        onDispose {
            listener.remove()
        }
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(orders) { order ->

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

                        Text("Toplam: ${order.total} USD")
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
                    IconButton(onClick = {
                        navController.navigate("add_product/evsel")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Ürün ekle")
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    navController.navigate("categories")
                }) {
                    Icon(Icons.Default.Home, contentDescription = "Ana sayfa")
                }
            }
        }

    ) { padding ->
        content(padding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductCategoryScreen(navController: NavController, mainCategory: String) {

    val categories = listOf(
        "Direkt akış cihazlar",
        "Ekipmanlar",
        "Tezgah altı açık kasa cihazlar",
        "Tezgah altı kabinli cihazlar"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Ekle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    navController.navigate("categories")
                }) {
                    Icon(Icons.Default.Home, null)
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            categories.forEach { cat ->
                Button(
                    onClick = {
                        navController.navigate("add_product_form/$cat")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(cat)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductFormScreen(navController: NavController, category: String) {

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Ekle - $category") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    navController.navigate("categories")
                }) {
                    Icon(Icons.Default.Home, null)
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(name, { name = it }, label = { Text("Ürün adı") })
            OutlinedTextField(price, { price = it }, label = { Text("Fiyat") })
            OutlinedTextField(desc, { desc = it }, label = { Text("Açıklama") })

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {

                    val product = hashMapOf(
                        "urun_model" to name,
                        "fiyat_usd" to price.toDoubleOrNull(),
                        "ozellikler" to desc,
                        "kategori" to category
                    )

                    db.collection("products")
                        .add(product)
                        .addOnSuccessListener {
                            navController.popBackStack()
                        }
                },
                modifier = Modifier.fillMaxWidth()
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

