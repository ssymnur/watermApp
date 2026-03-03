package com.example.watermapp

import android.R.attr.order
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
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.style.TextAlign
import com.example.watermapp.ui.theme.WaterAppTheme
import com.example.watermapp.utils.toTL
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

//??????????????????????????????????????????????????????????????
//KDV VE İSKONTO TOPLAM ÜZERİNDEN Mİ ? DOLAR EOURO ÜZERİNDEN Mİ?
//cihaz adı ekledim
//KDV %20
//proje kısmı endüstriyel ürünler için(ekleme yapılacak mı sor)


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
data class OrderItemDetail(
    val model: String,
    val currency: String,
    val originalPrice: Double,
    val priceTL: Double,
    val quantity: Int,
    val totalTL: Double
)
data class Customer(
    val customerName: String = "",
    val companyName: String = "",
    val phone: String = "",
    val deviceName: String = "",
    val email: String = "",
    val staffName: String = "",
    val note: String = "",
    val address: String = ""
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

    val discountPercent: Double = 0.0,
    val kdvPercent: Double = 20.0,

    val baseTotalTL: Double = 0.0,
    val discountedTotalTL: Double = 0.0,
    val kdvAmountTL: Double = 0.0,
    val grandTotalTL: Double = 0.0,

    val usdRate: Double = 0.0,
    val eurRate: Double = 0.0,

    val timestamp: Long = System.currentTimeMillis(),

    val createdAt: Long = System.currentTimeMillis()
)

data class CartItem(
    val urun: BaseUrun,
    var quantity: MutableState<Int> = mutableStateOf(1)
)
object CartManager {
    var usdRate by mutableStateOf(32.0)
    var eurRate by mutableStateOf(35.0)

    var cart = mutableStateListOf<CartItem>()

    var appliedDiscountPercent by mutableStateOf(0.0)
    //KDV EKLE
    var kdvEnabled by mutableStateOf(false)
    var kdvRate = 0.20   // %20

    fun applyDiscount(percent: Double) {
        appliedDiscountPercent = percent
    }

    var discountPercent by mutableStateOf(0.0)

    fun rawTotalTL(): Double {

        return cart.sumOf { item ->

            val price = item.urun.fiyat ?: 0.0
            val qty = item.quantity.value

            val tlPrice = when (item.urun.paraBirimi) {

                "USD" -> price * usdRate
                "EUR" -> price * eurRate
                "TL" -> price
                else -> price
            }

            tlPrice * qty
        }
    }

    fun totalsByCurrency(): Map<String, Double> {

        return cart
            .groupBy { it.urun.paraBirimi }
            .mapValues { (_, items) ->
                items.sumOf { item ->
                    val price = item.urun.fiyat ?: 0.0
                    price * item.quantity.value
                }
            }
    }
    fun totalsWithDiscount(): Map<String, Double> {
        val base = totalsByCurrency()

        return base.mapValues { (_, total) ->

            var discounted = total * (1 - appliedDiscountPercent / 100)

            if (kdvEnabled) {
                discounted *= (1 + kdvRate)
            }

            discounted
        }
    }

    fun totalInTL(): Double {

        val baseTL = rawTotalTL()

        // ✅ Önce iskonto
        val afterDiscount = baseTL * (1 - appliedDiscountPercent / 100)

        // ✅ Sonra KDV
        val finalTotal =
            if (kdvEnabled)
                afterDiscount * (1 + kdvRate)
            else
                afterDiscount

        return finalTotal
    }

    fun add(urun: BaseUrun) {

        val existing = cart.find { it.urun.id == urun.id }

        if (existing != null) {
            existing.quantity.value++
        } else {
            cart.add(
                CartItem(
                    urun = urun,
                    quantity = mutableStateOf(1)
                )
            )
        }
    }

    object SessionManager {
        var isStaff by mutableStateOf(false)
    }

    fun remove(urun: BaseUrun) {

        val existing = cart.find { it.urun.id == urun.id }

        if (existing != null) {

            if (existing.quantity.value > 1) {
                existing.quantity.value--
            } else {
                cart.remove(existing)
            }
        }
    }

    fun getQuantity(urun: BaseUrun): Int {
        return cart.find { it.urun.id == urun.id }
            ?.quantity
            ?.value ?: 0
    }

    fun totalPrice(): Double {
        return cart.sumOf {
            (it.urun.fiyat ?: 0.0) * it.quantity.value
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

        // ÜRÜN DÜZENLEME
        composable(
            route = "edit/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val productId =
                backStackEntry.arguments?.getString("productId") ?: ""

            var urun by remember { mutableStateOf<BaseUrun?>(null) }

            LaunchedEffect(productId) {

                FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(productId)
                    .get()
                    .addOnSuccessListener { doc ->

                        if (!doc.exists()) return@addOnSuccessListener

                        val anaKategori = doc.getString("anaKategori")

                        urun = when (anaKategori) {

                            "Evsel" -> {
                                EvselUrun(
                                    id = doc.id,
                                    anaKategori = anaKategori ?: "",
                                    altKategori = doc.getString("altKategori") ?: "",
                                    urun_adi = doc.getString("urun_adi") ?: "",
                                    marka = doc.getString("marka"),

                                    urun_kodu = doc.get("urun_kodu")?.toString(),

                                    fiyat = when (val f = doc.get("fiyat")) {
                                        is Number -> f.toDouble()
                                        is String -> f.toDoubleOrNull()
                                        else -> null
                                    },

                                    paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                    ozellikler = doc.getString("ozellikler"),

                                    pompa = doc.getString("pompa"),
                                    asama = doc.getString("asama"),
                                    membran_gpd = (doc.get("membran_gpd") as? Number)?.toDouble(),
                                    tank_galon = (doc.get("tank_galon") as? Number)?.toDouble()
                                )
                            }

                            "Endüstriyel" -> {
                                EndustriyelUrun(
                                    id = doc.id,
                                    anaKategori = anaKategori ?: "",
                                    altKategori = doc.getString("altKategori") ?: "",
                                    urun_adi = doc.getString("urun_adi") ?: "",
                                    marka = doc.getString("marka"),

                                    urun_kodu = doc.get("urun_kodu")?.toString(),

                                    fiyat = when (val f = doc.get("fiyat")) {
                                        is Number -> f.toDouble()
                                        is String -> f.toDoubleOrNull()
                                        else -> null
                                    },

                                    paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                    ozellikler = doc.getString("ozellikler"),

                                    model = doc.getString("model"),
                                    tank_baglanti = doc.getString("tank_baglanti"),
                                    giris_cikis_baglanti = doc.getString("giris_cikis_baglanti"),
                                    debi_m3_saat = (doc.get("debi_m3_saat") as? Number)?.toDouble()
                                )
                            }

                            "Peyzaj" -> {
                                PeyzajUrun(
                                    id = doc.id,
                                    anaKategori = anaKategori ?: "",
                                    altKategori = doc.getString("altKategori") ?: "",
                                    urun_adi = doc.getString("urun_adi") ?: "",
                                    marka = doc.getString("marka"),

                                    urun_kodu = doc.get("urun_kodu")?.toString(),

                                    fiyat = when (val f = doc.get("fiyat")) {
                                        is Number -> f.toDouble()
                                        is String -> f.toDoubleOrNull()
                                        else -> null
                                    },

                                    paraBirimi = doc.getString("paraBirimi") ?: "USD",
                                    ozellikler = doc.getString("ozellikler"),

                                    ebat = doc.getString("ebat"),
                                    istasyon_sayisi = (doc.get("istasyon_sayisi") as? Number)?.toInt(),
                                    ambalaj_adedi = (doc.get("ambalaj_adedi") as? Number)?.toInt(),
                                    tip = doc.getString("tip"),
                                    koli_adedi = (doc.get("koli_adedi") as? Number)?.toInt(),
                                    kutu_adedi = (doc.get("kutu_adedi") as? Number)?.toInt(),
                                    nozul_rengi = doc.getString("nozul_rengi"),
                                    atis_mesafesi = doc.getString("atis_mesafesi"),
                                    basinc_degeri = doc.getString("basinc_degeri"),
                                    tanim = doc.getString("tanim")
                                )
                            }

                            else -> null
                        }
                    }
            }

            if (urun == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                EditProductScreen(
                    urun = urun!!,
                    productId = productId,
                    navController = navController
                )
            }
        }
        composable(
            route = "edit_project_product/{urunId}/{projeAdi}",
            arguments = listOf(
                navArgument("urunId") { type = NavType.StringType },
                navArgument("projeAdi") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val urunId = backStackEntry.arguments?.getString("urunId") ?: ""

            val projeAdi = Uri.decode(
                backStackEntry.arguments?.getString("projeAdi") ?: ""
            )

            EditProjectProductScreen(
                urunId = urunId,
                projeAdi = projeAdi,
                navController = navController
            )
        }
        composable(
            route = "detail/{ana}/{ust}",
            arguments = listOf(
                navArgument("ana") { type = NavType.StringType },
                navArgument("ust") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ana = backStackEntry.arguments?.getString("ana") ?: ""
            val ust = backStackEntry.arguments?.getString("ust")?.fromNavArg()

            DetailScreenCategory(
                anaKategori = ana,
                ustKategori = ust,
                navController = navController
            )
        }

        // PROJELER LİSTESİ
        composable(
            route = "proje/{projeAdi}",
            arguments = listOf(
                navArgument("projeAdi") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val projeAdi = Uri.decode(
                backStackEntry.arguments?.getString("projeAdi") ?: ""
            )

            ProjeDetayScreen(
                projeAdi = projeAdi,
                navController = navController
            )
        }


        // PROJEYE ÜRÜN EKLE
        composable(
            route = "add_project_product_form/{projeAdi}",
            arguments = listOf(
                navArgument("projeAdi") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val projeAdi = Uri.decode(
                backStackEntry.arguments?.getString("projeAdi") ?: ""
            )

            AddProjectProductFormScreen(
                navController = navController,
                projeAdi = projeAdi
            )
        }
        // YENİ ÜRÜN FORMU
        composable(
            route = "add_product_form/{ana}/{alt}",
            arguments = listOf(
                navArgument("ana") { type = NavType.StringType },
                navArgument("alt") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ana =
                backStackEntry.arguments?.getString("ana")?.fromNavArg() ?: ""

            val alt =
                backStackEntry.arguments?.getString("alt")?.fromNavArg() ?: ""

            AddProductFormScreen(navController, ana, alt)
        }

        // KATEGORİLER
        composable("categories") {
            MainLayout(navController) {
                CategoryScreen(navController)
            }
        }

        composable(
            route = "kategori/{ana}",
            arguments = listOf(
                navArgument("ana") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ana = backStackEntry.arguments?.getString("ana") ?: ""

            DetailScreenCategory(
                anaKategori = ana,
                ustKategori = null,
                navController = navController
            )
        }

        composable(
            route = "urunler/{ana}/{alt}",
            arguments = listOf(
                navArgument("ana") { type = NavType.StringType },
                navArgument("alt") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ana =
                backStackEntry.arguments?.getString("ana") ?: ""

            val alt =
                backStackEntry.arguments?.getString("alt") ?: ""

            DetailScreenProducts(ana, alt, navController)
        }
        composable(
            route = "projeler/{kategori}",
            arguments = listOf(
                navArgument("kategori") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val kategori =
                backStackEntry.arguments?.getString("kategori") ?: ""

            ProjelerScreen(
                navController = navController,
                kategori = kategori
            )
        }

        // SİPARİŞLER
        composable("orders") {
            OrdersScreen(navController)
        }

        composable(
            route = "editOrder/{orderId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val id =
                backStackEntry.arguments?.getString("orderId") ?: ""

            EditOrderScreen(id, navController)
        }

        // SEPET
        composable("cart") {
            CartScreen(navController)
        }

        // MÜŞTERİ FORMU
        composable("customerForm") {
            CustomerFormScreen(navController)
        }

        // LOGIN
        composable("login") {
            LoginScreen(navController)
        }

        // DİĞER SAYFALAR
        composable("endustriyel") {
            SimplePage("Endüstriyel Ürünler")
        }

        composable("kimyasal") {
            SimplePage("Kimyasal Ürünler")
        }

        composable("peyzaj") {
            SimplePage("Peyzaj Ürünleri")
        }
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
        Triple("Evsel", "kategori/Evsel", Icons.Default.Home),
        Triple("Endüstriyel", "kategori/Endüstriyel", Icons.Default.Build),
        Triple("Kimyasal", "kategori/Kimyasal", Icons.Default.Science),
        Triple("Peyzaj", "kategori/Peyzaj", Icons.Default.Park)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenCategory(
    anaKategori: String,
    ustKategori: String? = null,
    navController: NavHostController
) {

    var kategoriler by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(anaKategori, ustKategori) {

        // 🔹 1️⃣ PEYZAJ ANA KATEGORİ
        if (anaKategori == "Peyzaj" && ustKategori == null) {

            kategoriler = listOf("POELSAN", "RAINBIRD")

            onDispose { }

        }

        // 🔹 2️⃣ PEYZAJ → POELSAN ALT KATEGORİLERİ
        else if (anaKategori == "Peyzaj" && ustKategori == "POELSAN") {

            kategoriler = listOf(
                "CUP/BT PİLLİ SULAMA KONTROL ÜNİTESİ",
                "SPREYLER VE NOZULLAR",
                "ELEKTROFÜZYON VE SPİGOT EK PARÇALARI",
                "MAVİ SERİ",
                "YEŞİL SERİ",
                "PRİZ KOLYE",
                "PRİZ KOLYE GEÇMELİ",
                "LAYFLAT EK PARÇALARI",
                "DİŞLİ BAĞLANTI ELEMANLARI",
                "KÜRESEL VANALAR",
                "CAN SUYU VANA",
                "VANA KUTULARI",
                "PLASTİK FİLTRE",
                "MİNİ VANA (DN 8)",
                "MİNİ VANA (DN 15)",
                "HAT SONU TAHLİYE VANASI",
                "KİLİTLİ BAĞLANTI ELEMANLARI",
                "DAMLA SULAMA KURTAĞZI",
                "DAMLA SULAMA SOMUNLU",
                "YASSI DAMLA SULAMA SOMUNLU",
                "YASSI DAMLA SULAMA YÜZÜKLÜ",
                "MİNİ SPRİNKLER VE EK PARÇALARI",
                "HAT ÜSTÜ DAMLATICI"
            )

            onDispose { }

        }

        // 🔹 3️⃣ DİĞER KATEGORİLER (Firebase’den ÇEK)
        else {

            val listener = FirebaseFirestore.getInstance()
                .collection("products")
                .addSnapshotListener { snapshot, _ ->

                    kategoriler = snapshot?.documents
                        ?.mapNotNull { doc ->

                            val ana = doc.getString("anaKategori")
                                ?: doc.getString("kategori")

                            if (ana == anaKategori)
                                doc.getString("altKategori")
                            else null
                        }
                        ?.distinct()
                        ?.sorted()
                        ?: emptyList()
                }

            onDispose { listener.remove() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (ustKategori == null)
                            "$anaKategori Kategorileri"
                        else
                            ustKategori
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigate("cart") }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Sepet"
                        )
                    }
                }
            }
        },

        floatingActionButton = {
            if (anaKategori == "Endüstriyel") {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("projeler/Endüstriyel")
                    }
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Projeler"
                    )
                }
            }
        },

        floatingActionButtonPosition = FabPosition.End

    ) { padding ->

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

                            // 🔥 Eğer Peyzaj ana ekrandaysak → üst kategoriye git
                            if (anaKategori == "Peyzaj" && ustKategori == null) {

                                navController.navigate(
                                    "detail/$anaKategori/${kategori.toNavArg()}"
                                )

                            } else {

                                navController.navigate(
                                    "urunler/$anaKategori/${kategori.toNavArg()}"
                                )
                            }
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
@Composable
fun EvselUrunOzellikleri(urun: EvselUrun) {

    urun.pompa?.let { Text("Pompa: $it") }
    urun.asama?.let { Text("Aşama: $it") }
    urun.membran_gpd?.let { Text("Membran: $it GPD") }
    urun.tank_galon?.let { Text("Tank: $it Galon") }
}
@Composable
fun EndustriyelUrunOzellikleri(urun: EndustriyelUrun) {

    Column {

        urun.model?.let { Text("Model: $it") }
        urun.debi_m3_saat?.let { Text("Debi: $it m³/saat") }
        urun.tank_baglanti?.let { Text("Tank Bağlantı: $it") }
        urun.giris_cikis_baglanti?.let { Text("Giriş / Çıkış: $it") }

    }
}
@Composable
fun PeyzajUrunOzellikleri(urun: PeyzajUrun) {

    Column {

        urun.nozul_rengi?.let { Text("Nozul Rengi: $it") }
        urun.atis_mesafesi?.let { Text("Atış Mesafesi: $it") }
        urun.basinc_degeri?.let { Text("Basınç: $it") }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenProducts(
    anaKategori: String,
    kategoriAdi: String,
    navController: NavHostController
) {

    var urunler by remember {
        mutableStateOf<List<BaseUrun>>(emptyList())
    }

    // 🔥 FIREBASE CANLI LİSTE (SEALED CLASS UYUMLU)
    DisposableEffect(anaKategori, kategoriAdi) {

        val listener = FirebaseFirestore.getInstance()
            .collection("products")
            .whereEqualTo("anaKategori", anaKategori)
            .whereEqualTo("altKategori", kategoriAdi)
            .addSnapshotListener { snapshot, _ ->

                urunler = snapshot?.documents?.mapNotNull { doc ->

                    when (doc.getString("anaKategori")) {

                        "Evsel" ->
                            doc.toObject(EvselUrun::class.java)?.copy(id = doc.id)

                        "Endüstriyel" ->
                            doc.toObject(EndustriyelUrun::class.java)?.copy(id = doc.id)

                        "Peyzaj" ->
                            doc.toObject(PeyzajUrun::class.java)?.copy(id = doc.id)

                        else -> null
                    }

                } ?: emptyList()
            }

        onDispose { listener.remove() }
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
                    if (CartManager.SessionManager.isStaff) {
                        IconButton(onClick = {
                            navController.navigate(
                                "add_product_form/${anaKategori.toNavArg()}/${kategoriAdi.toNavArg()}"
                            )
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "")
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
                var expanded by remember { mutableStateOf(false) }

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

                        Column(modifier = Modifier.weight(1f)) {

                            Text(
                                urun.urun_adi,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text("Fiyat: ${urun.fiyat ?: "-"} ${urun.paraBirimi ?: ""}")

                            urun.marka
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Text("Marka: $it") }
                            urun.urun_kodu?.let { Text("Kod: $it") }

                            // 🔥 KATEGORİYE GÖRE TÜM ÖZELLİKLER

                            when (urun) {

                                is EvselUrun -> {
                                    urun.pompa?.let { Text("Pompa: $it") }
                                    urun.asama?.let { Text("Aşama: $it") }
                                    urun.membran_gpd?.let { Text("Membran: $it GPD") }
                                    urun.tank_galon?.let { Text("Tank: $it Galon") }
                                }

                                is EndustriyelUrun -> {
                                    urun.model?.let { Text("Model: $it") }
                                    urun.tank_baglanti?.let { Text("Tank Bağlantı: $it") }
                                    urun.giris_cikis_baglanti?.let { Text("Giriş/Çıkış: $it") }
                                    urun.debi_m3_saat?.let { Text("Debi: $it m3/saat") }
                                }

                                is PeyzajUrun -> {

                                    urun.ebat?.takeIf { it.isNotBlank() }?.let {
                                        Text("Ebat: $it")
                                    }

                                    urun.istasyon_sayisi?.let {
                                        Text("İstasyon Sayısı: $it")
                                    }

                                    urun.ambalaj_adedi?.let {
                                        Text("Ambalaj Adedi: $it")
                                    }

                                    urun.tip?.takeIf { it.isNotBlank() }?.let {
                                        Text("Tip: $it")
                                    }

                                    urun.koli_adedi?.let {
                                        Text("Koli Adedi: $it")
                                    }

                                    urun.kutu_adedi?.let {
                                        Text("Kutu Adedi: $it")
                                    }

                                    urun.nozul_rengi?.takeIf { it.isNotBlank() }?.let {
                                        Text("Nozul: $it")
                                    }

                                    urun.atis_mesafesi?.takeIf { it.isNotBlank() }?.let {
                                        Text("Atış: $it")
                                    }

                                    urun.basinc_degeri?.takeIf { it.isNotBlank() }?.let {
                                        Text("Basınç: $it")
                                    }

                                    urun.tanim?.takeIf { it.isNotBlank() }?.let {
                                        Text("Tanım: $it")
                                    }
                                }
                            }

                            TextButton(onClick = { expanded = !expanded }) {
                                Text(if (expanded) "Gizle" else "Açıklama")
                            }

                            if (expanded) {
                                Text(
                                    text = urun.ozellikler ?: "Bilgi yok",
                                    style = MaterialTheme.typography.bodySmall
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
                                textAlign = TextAlign.Center,
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
fun loadEndustriyelData(context: Context): Root {
    val json = context.assets.open("endustriyel_products.json")
        .bufferedReader()
        .use { it.readText() }

    return Gson().fromJson(json, Root::class.java)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavHostController) {

    val cart = CartManager.cart

    val totals by remember(
        CartManager.cart,
        CartManager.appliedDiscountPercent,
        CartManager.kdvEnabled,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf { CartManager.totalsWithDiscount() }
    }

    val baseTotals by remember(
        CartManager.cart,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf { CartManager.totalsByCurrency() }
    }

    val context = LocalContext.current
    val view = LocalView.current

    var usdText by remember { mutableStateOf(CartManager.usdRate.toString()) }
    var eurText by remember { mutableStateOf(CartManager.eurRate.toString()) }

    //swipe
    var itemToDelete by remember { mutableStateOf<CartItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            Text("Güncel Kur Gir", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = usdText,
                onValueChange = {
                    usdText = it
                    CartManager.usdRate = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("USD → TL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = eurText,
                onValueChange = {
                    eurText = it
                    CartManager.eurRate = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("EUR → TL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ✅ SEPET LİSTESİ
            LazyColumn(modifier = Modifier.weight(1f)) {

                items(cart) { item ->

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue ==
                            SwipeToDismissBoxValue.StartToEnd
                        ) {
                            itemToDelete = item
                            showDeleteDialog = true
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        backgroundContent = {

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White) // arka plan beyaz
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = Color(0xFFD32F2F) // kırmızı çöp kutusu
                                )
                            }
                        }
                    ) {
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
                                    text = item.urun.urun_adi,
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
            }
            if (showDeleteDialog && itemToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Ürünü Sil") },
                    text = { Text("Bu ürünü sepetten silmek istiyor musun?") },
                    confirmButton = {
                        TextButton(onClick = {
                            CartManager.cart.remove(itemToDelete!!)
                            showDeleteDialog = false
                        }) {
                            Text("Evet")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                        }) {
                            Text("Vazgeç")
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("% İskonto") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = CartManager.kdvEnabled,
                            onCheckedChange = { CartManager.kdvEnabled = it }
                        )
                        Text("KDV")
                    }

                    Button(
                        onClick = {
                            val percent = discountText.toDoubleOrNull() ?: 0.0
                            CartManager.applyDiscount(percent)
                        }
                    ) {
                        Text("Uygula")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ✅ TOPLAMLAR
            val totals = CartManager.totalsByCurrency()

            totals.forEach { (currency, baseTotal) ->

                // TL karşılığı
                val tlValue = when (currency) {
                    "USD" -> baseTotal * CartManager.usdRate
                    "EUR" -> baseTotal * CartManager.eurRate
                    "TL" -> baseTotal
                    else -> baseTotal
                }

                // TL'ye iskonto uygula
                val afterDiscount =
                    tlValue * (1 - CartManager.appliedDiscountPercent / 100)

                // TL'ye KDV uygula
                val finalTL =
                    if (CartManager.kdvEnabled)
                        afterDiscount * (1 + CartManager.kdvRate)
                    else
                        afterDiscount

                Column {

                    // 🔹 ÜSTTE GRİ TL (iskontolu + KDV'li)
                    Text(
                        text = "≈ %.2f ₺".format(finalTL),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // 🔹 ALTTA DÖVİZ (DEĞİŞMEZ)
                    Text(
                        text = when (currency) {
                            "USD" -> "%.2f $".format(baseTotal)
                            "EUR" -> "%.2f €".format(baseTotal)
                            "TL" -> "%.2f ₺".format(baseTotal)
                            else -> "$baseTotal"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

// ✅ GENEL TOPLAM TL
            val totalTL = CartManager.totalInTL()

            Text(
                text = "Genel Toplam (TL): %.2f ₺".format(totalTL),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32)
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
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjelerScreen(
    navController: NavHostController,
    kategori: String
) {

    val db = FirebaseFirestore.getInstance()

    var projeler by remember { mutableStateOf<List<String>>(emptyList()) }
    var yeniProjeAdi by remember { mutableStateOf("") }
    var silinecekProje by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {

        val listener = db.collection("projects")
            .addSnapshotListener { snapshot, _ ->
                projeler = snapshot?.documents?.mapNotNull { it.id } ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeler") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {

                items(
                    items = projeler,
                    key = { it }
                ) { projeAdi ->

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (
                            dismissState.currentValue ==
                            SwipeToDismissBoxValue.StartToEnd &&
                            CartManager.SessionManager.isStaff
                        ) {
                            silinecekProje = projeAdi
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd =
                            CartManager.SessionManager.isStaff,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White), // ✅ HER ZAMAN BEYAZ
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {

                                Spacer(modifier = Modifier.width(24.dp))

                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red // ✅ SADECE ÇÖP KIRMIZI
                                )
                            }
                        }
                    ) {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable {
                                    navController.navigate(
                                        "proje/${Uri.encode(projeAdi)}"
                                    )
                                }
                        ) {
                            Text(
                                text = projeAdi,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }

            if (CartManager.SessionManager.isStaff) {

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    OutlinedTextField(
                        value = yeniProjeAdi,
                        onValueChange = { yeniProjeAdi = it },
                        label = { Text("Yeni Proje Adı") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                            if (yeniProjeAdi.isNotBlank()) {

                                db.collection("projects")
                                    .document(yeniProjeAdi.trim())
                                    .set(
                                        mapOf(
                                            "createdAt" to
                                                    System.currentTimeMillis()
                                        )
                                    )

                                yeniProjeAdi = ""
                            }
                        }
                    ) {
                        Text("Proje Oluştur")
                    }
                }
            }
        }

        silinecekProje?.let { projeAdi ->

            AlertDialog(
                onDismissRequest = { silinecekProje = null },
                title = { Text("Projeyi Sil") },
                text = {
                    Text("Bu proje ve içindeki ürünler silinecek.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {

                            val projeRef =
                                db.collection("projects")
                                    .document(projeAdi)

                            projeRef.collection("urunler")
                                .get()
                                .addOnSuccessListener { snapshot ->

                                    val batch = db.batch()

                                    snapshot.documents.forEach {
                                        batch.delete(it.reference)
                                    }

                                    batch.delete(projeRef)

                                    batch.commit()
                                        .addOnSuccessListener {
                                            silinecekProje = null
                                        }
                                }
                        }
                    ) {
                        Text("Evet", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { silinecekProje = null }
                    ) {
                        Text("Vazgeç")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjeDetayScreen(
    projeAdi: String,
    navController: NavHostController
) {

    val db = FirebaseFirestore.getInstance()

    var urunler by remember { mutableStateOf<List<BaseUrun>>(emptyList()) }
    var silinecekUrun by remember { mutableStateOf<BaseUrun?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val isStaff = CartManager.SessionManager.isStaff

    // 🔹 Firestore Listener
    DisposableEffect(projeAdi) {
        val listener = db
            .collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                urunler = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(EndustriyelUrun::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                isLoading = false
            }

        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projeAdi) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (isStaff) {
                        IconButton(
                            onClick = {
                                navController.navigate(
                                    "add_project_product_form/${Uri.encode(projeAdi)}"
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, "Ürün Ekle")
                        }
                    }
                }
            )
        }
    ) { padding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            urunler.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bu projede henüz ürün yok")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(
                        items = urunler,
                        key = { it.id }
                    ) { urun ->

                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (
                                dismissState.currentValue ==
                                SwipeToDismissBoxValue.StartToEnd &&
                                isStaff
                            ) {
                                silinecekUrun = urun
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = isStaff,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(urun.urun_adi ?: "-", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    when (urun) {
                                        is EvselUrun -> {
                                            Text("Pompa: ${urun.pompa ?: "-"}")
                                            Text("Aşama: ${urun.asama ?: "-"}")
                                            Text("Membran: ${urun.membran_gpd ?: "-"} GPD")
                                            Text("Tank: ${urun.tank_galon ?: "-"} Galon")
                                        }
                                        is EndustriyelUrun -> {
                                            Text("Model: ${urun.model ?: "-"}")
                                            Text("Debi: ${urun.debi_m3_saat ?: "-"} m³/saat")
                                            Text("Tank Bağlantı: ${urun.tank_baglanti ?: "-"}")
                                            Text("Giriş/Çıkış: ${urun.giris_cikis_baglanti ?: "-"}")
                                        }
                                        is PeyzajUrun -> {
                                            Text("Nozul Rengi: ${urun.nozul_rengi ?: "-"}")
                                            Text("Atış Mesafesi: ${urun.atis_mesafesi ?: "-"}")
                                        }
                                    }

                                    Text("Marka: ${urun.marka ?: "-"}")

                                    Text(
                                        "Kod: ${
                                            when (urun) {
                                                is EvselUrun -> urun.urun_kodu?.toString() ?: "-"
                                                is EndustriyelUrun -> urun.urun_kodu?.toString() ?: "-"
                                                is PeyzajUrun -> urun.urun_kodu?.toString() ?: "-"
                                                else -> "-"
                                            }
                                        }"
                                    )

                                    Text("Fiyat: ${urun.fiyat ?: "-"} ${urun.paraBirimi}")
                                    Text("Özellikler: ${urun.ozellikler ?: "-"}")

                                    if (isStaff) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = {
                                                navController.navigate(
                                                    "edit_project_product/${urun.id}/${Uri.encode(projeAdi)}"
                                                )
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) { Text("Düzenle") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🔹 Silme Dialogu
        silinecekUrun?.let { urun ->
            AlertDialog(
                onDismissRequest = { silinecekUrun = null },
                title = { Text("Ürünü Sil") },
                text = { Text("Bu ürün silinecek. Emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            db.collection("projects")
                                .document(projeAdi)
                                .collection("urunler")
                                .document(urun.id)
                                .delete()
                            silinecekUrun = null
                        }
                    ) { Text("Evet", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { silinecekUrun = null }) { Text("Vazgeç") }
                }
            )
        }
    }
}

fun getFirestoreValueAsString(
    doc: DocumentSnapshot,
    field: String
): String {

    return when (val value = doc.get(field)) {

        is Double -> value.toString()
        is Long -> value.toString()
        is String -> value
        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectProductScreen(
    urunId: String,
    projeAdi: String,
    navController: NavHostController
) {

    val db = FirebaseFirestore.getInstance()

    var urunAdi by remember { mutableStateOf("") }
    var urunKodu by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }
    var marka by remember { mutableStateOf("") }
    var paraBirimi by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var tankBaglanti by remember { mutableStateOf("") }
    var girisCikis by remember { mutableStateOf("") }
    var debi by remember { mutableStateOf("") }
    var ozellikler by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    // 🔥 Firestore verisini çek
    LaunchedEffect(urunId) {

        db.collection("projects")
            .document(projeAdi)
            .collection("urunler")
            .document(urunId)
            .get()
            .addOnSuccessListener { doc ->

                urunAdi = doc.getString("urun_adi") ?: ""
                urunKodu = getFirestoreValueAsString(doc, "urun_kodu")

                fiyat = getFirestoreValueAsString(doc, "fiyat")

                marka = doc.getString("marka") ?: ""
                paraBirimi = doc.getString("paraBirimi") ?: ""
                debi =
                    getFirestoreValueAsString(doc, "debi_m3_saat")

                model = doc.getString("model") ?: ""
                tankBaglanti = doc.getString("tank_baglanti") ?: ""
                girisCikis = doc.getString("giris_cikis_baglanti") ?: ""
                ozellikler = doc.getString("ozellikler") ?: ""

                isLoading = false
            }
    }


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
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
            ) {

                FormField("Ürün Adı", urunAdi) { urunAdi = it }
                FormField("Ürün Kodu", urunKodu) { urunKodu = it }
                FormField("Fiyat", fiyat) { fiyat = it }
                FormField("Marka", marka) { marka = it }
                FormField("Para Birimi", paraBirimi) { paraBirimi = it }
                FormField("Model", model) { model = it }
                FormField("Tank Bağlantı", tankBaglanti) { tankBaglanti = it }
                FormField("Giriş/Çıkış", girisCikis) { girisCikis = it }
                FormField("Debi (m3/saat)", debi) { debi = it }
                FormField("Özellikler", ozellikler) { ozellikler = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {

                        val updatedData = mapOf(
                            "urun_adi" to urunAdi,
                            "urun_kodu" to urunKodu,
                            "fiyat" to fiyat.toDoubleOrNull(),
                            "marka" to marka,
                            "paraBirimi" to paraBirimi,
                            "model" to model,
                            "tank_baglanti" to tankBaglanti,
                            "giris_cikis_baglanti" to girisCikis,
                            "debi_m3_saat" to debi.toDoubleOrNull(),
                            "ozellikler" to ozellikler
                        )

                        db.collection("projects")
                            .document(projeAdi)
                            .collection("urunler")
                            .document(urunId)
                            .update(updatedData)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    urun: BaseUrun,
    productId: String,
    navController: NavHostController
) {

    // ---------------- ORTAK STATE ----------------

    var urunAdi by rememberSaveable { mutableStateOf(urun.urun_adi) }
    var marka by rememberSaveable { mutableStateOf(urun.marka ?: "") }
    var kod by rememberSaveable {
        mutableStateOf(
            when (urun) {
                is EvselUrun -> urun.urun_kodu?.toString() ?: ""
                is EndustriyelUrun -> urun.urun_kodu?.toString() ?: ""
                is PeyzajUrun -> urun.urun_kodu?.toString() ?: ""
                else -> ""
            }
        )
    }
    var fiyat by rememberSaveable { mutableStateOf(urun.fiyat?.toString() ?: "") }
    var paraBirimi by rememberSaveable { mutableStateOf(urun.paraBirimi) }

    val currencyOptions = listOf("USD", "TL", "EUR")
    var expandedCurrency by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ---------------- KATEGORİYE ÖZEL STATE ----------------

    var model by rememberSaveable { mutableStateOf((urun as? EndustriyelUrun)?.model ?: "") }
    var debi by rememberSaveable { mutableStateOf((urun as? EndustriyelUrun)?.debi_m3_saat?.toString() ?: "") }

    var pompa by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.pompa ?: "") }
    var asama by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.asama ?: "") }
    var membran by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.membran_gpd?.toString() ?: "") }
    var tank by rememberSaveable { mutableStateOf((urun as? EvselUrun)?.tank_galon?.toString() ?: "") }

    var nozul by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.nozul_rengi ?: "") }
    var atis by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.atis_mesafesi ?: "") }
    var basinc by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.basinc_degeri ?: "") }
    var ebat by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.ebat ?: "") }
    var istasyon by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.istasyon_sayisi?.toString() ?: "") }
    var ambalaj by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.ambalaj_adedi?.toString() ?: "") }
    var tip by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.tip ?: "") }
    var koli by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.koli_adedi?.toString() ?: "") }
    var kutu by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.kutu_adedi?.toString() ?: "") }
    var tanim by rememberSaveable { mutableStateOf((urun as? PeyzajUrun)?.tanim ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürünü Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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

            // -------- ORTAK ALANLAR --------

            item { OutlinedTextField(urunAdi, { urunAdi = it }, label = { Text("Ürün Adı") }) }
            item { OutlinedTextField(marka, { marka = it }, label = { Text("Marka") }) }
            item { OutlinedTextField(kod, { kod = it }, label = { Text("Kod") }) }

            // -------- KATEGORİYE GÖRE --------

            when (urun) {

                is EvselUrun -> {

                    item { OutlinedTextField(pompa, { pompa = it }, label = { Text("Pompa") }) }
                    item { OutlinedTextField(asama, { asama = it }, label = { Text("Aşama") }) }
                    item { OutlinedTextField(membran, { membran = it }, label = { Text("Membran (GPD)") }) }
                    item { OutlinedTextField(tank, { tank = it }, label = { Text("Tank (Galon)") }) }
                }

                is EndustriyelUrun -> {

                    item { OutlinedTextField(model, { model = it }, label = { Text("Model") }) }
                    item { OutlinedTextField(debi, { debi = it }, label = { Text("Debi (m³/saat)") }) }
                }

                is PeyzajUrun -> {

                    item { OutlinedTextField(nozul, { nozul = it }, label = { Text("Nozul Rengi") }) }
                    item { OutlinedTextField(atis, { atis = it }, label = { Text("Atış Mesafesi") }) }
                    item { OutlinedTextField(basinc, { basinc = it }, label = { Text("Basınç") }) }
                    item { OutlinedTextField(ebat, { ebat = it }, label = { Text("Ebat") }) }
                    item { OutlinedTextField(istasyon, { istasyon = it }, label = { Text("İstasyon Sayısı") }) }
                    item { OutlinedTextField(ambalaj, { ambalaj = it }, label = { Text("Ambalaj Adedi") }) }
                    item { OutlinedTextField(tip, { tip = it }, label = { Text("Tip") }) }
                    item { OutlinedTextField(koli, { koli = it }, label = { Text("Koli Adedi") }) }
                    item { OutlinedTextField(kutu, { kutu = it }, label = { Text("Kutu Adedi") }) }
                    item { OutlinedTextField(nozul, { nozul = it }, label = { Text("Nozul Rengi") }) }
                    item { OutlinedTextField(basinc, { basinc = it }, label = { Text("Basınç") }) }
                    item { OutlinedTextField(tanim, { tanim = it }, label = { Text("Tanım") }) }
                }
            }

            // -------- FİYAT --------

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    OutlinedTextField(
                        value = fiyat,
                        onValueChange = { fiyat = it },
                        label = { Text("Fiyat") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Box {
                        OutlinedButton(onClick = { expandedCurrency = true }) {
                            Text(paraBirimi)
                        }

                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencyOptions.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        paraBirimi = it
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------- KAYDET --------

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        val updated: BaseUrun = when (urun) {

                            is EvselUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                pompa = pompa,
                                asama = asama,
                                membran_gpd = membran.toDoubleOrNull(),
                                tank_galon = tank.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )

                            is EndustriyelUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                model = model,
                                debi_m3_saat = debi.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )

                            is PeyzajUrun -> urun.copy(
                                urun_adi = urunAdi,
                                marka = marka,
                                ebat = ebat,
                                istasyon_sayisi = istasyon.toIntOrNull(),
                                ambalaj_adedi = ambalaj.toIntOrNull(),
                                tip = tip,
                                koli_adedi = koli.toIntOrNull(),
                                kutu_adedi = kutu.toIntOrNull(),
                                nozul_rengi = nozul,
                                atis_mesafesi = atis,
                                basinc_degeri = basinc,
                                tanim = tanim,
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi
                            )
                        }

                        ProductRepository.updateProduct(productId, updated)

                        scope.launch {
                            snackbarHostState.showSnackbar("Kaydedildi")
                            delay(1000)
                            navController.popBackStack()
                        }
                    }
                ) { Text("Kaydet") }
            }

            // -------- SİL --------

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = { showDeleteDialog = true }
                ) { Text("Sil") }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Ürünü sil") },
                text = { Text("Silmek istediğinize emin misiniz?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Firebase.firestore
                                .collection("products")
                                .document(productId)
                                .delete()

                            navController.popBackStack()
                        }
                    ) { Text("Evet") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Hayır")
                    }
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
    var deviceName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var staffName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }



    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val previewOrder = remember {
        OrderCalculator.calculateOrder(
            customer = Customer(),
            items = CartManager.cart.map {
                OrderItem(
                    model = it.urun.urun_adi,
                    quantity = it.quantity.value,
                    price = it.urun.fiyat ?: 0.0,
                    currency = it.urun.paraBirimi ?: "USD"
                )
            },
            discountPercent = CartManager.appliedDiscountPercent,
            kdvPercent = 20.0,
            usdRate = CartManager.usdRate,
            eurRate = CartManager.eurRate
        )
    }

    val totalTL = previewOrder.grandTotalTL

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

                        val customer = Customer(
                            customerName = customerName,
                            companyName = companyName,
                            phone = phone,
                            deviceName = deviceName,
                            email = email,
                            staffName = staffName,
                            note = note,
                            address = address
                        )

                        val items = CartManager.cart.map {
                            OrderItem(
                                model = it.urun.urun_adi,
                                quantity = it.quantity.value,
                                price = it.urun.fiyat ?: 0.0,
                                currency = it.urun.paraBirimi ?: "USD"
                            )
                        }

                        val order = OrderCalculator.calculateOrder(
                            customer = customer,
                            items = items,
                            discountPercent = CartManager.appliedDiscountPercent,
                            kdvPercent = 20.0,
                            usdRate = CartManager.usdRate,
                            eurRate = CartManager.eurRate
                        )

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
                Text(
                    text = "Toplam Tutar: %.2f ₺".format(totalTL),
                    style = MaterialTheme.typography.headlineSmall
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
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Cihaz Adı") },
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

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

        }
    }
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<OrderDoc?>(null) }

    DisposableEffect(Unit) {

        val listener = Firebase.firestore
            .collection("orders")
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

    // DELETE DIALOG
    if (showDeleteDialog && orderToDelete != null) {

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Silme Onayı") },
            text = { Text("Siparişi silmek istiyor musun?") },
            confirmButton = {
                TextButton(onClick = {

                    Firebase.firestore
                        .collection("orders")
                        .document(orderToDelete!!.id)
                        .delete()

                    showDeleteDialog = false
                    orderToDelete = null
                }) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    orderToDelete = null
                }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Siparişler") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                label = { Text("Müşteri / Şirket Ara") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(filtered) { orderDoc ->

                    val order = orderDoc.order

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("Müşteri: ${order.customer.customerName}")
                            Text("Şirket: ${order.customer.companyName}")
                            Text("Telefon: ${order.customer.phone}")
                            Text("Personel: ${order.customer.staffName}")
                            Text("Tarih: ${formatDate(order.timestamp)}")

                            Spacer(Modifier.height(8.dp))

                            // ÜRÜN LİSTESİ
                            order.items.forEach { item ->

                                val tlPrice = when (item.currency) {
                                    "USD" -> item.price * order.usdRate
                                    "EUR" -> item.price * order.eurRate
                                    else -> item.price
                                }

                                val totalItemTL = tlPrice * item.quantity

                                Text("${item.model} x${item.quantity}")

                                Text(
                                    "Birim TL: %.2f ₺".format(tlPrice),
                                    color = Color.Gray
                                )

                                Text(
                                    "Satır Toplam: %.2f ₺".format(totalItemTL),
                                    color = Color.Blue
                                )

                                Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(12.dp))

                            // FİYAT ÖZETİ
                            Text(
                                "Ara Toplam: %.2f ₺".format(order.baseTotalTL),
                                color = Color.Gray
                            )

                            val discountAmount =
                                order.baseTotalTL - order.discountedTotalTL

                            Text(
                                text = "İskonto (%${order.discountPercent}) : -${"%.2f".format(discountAmount)} ₺",
                                color = Color.Gray
                            )

                            Text(
                                text = "KDV (%${order.kdvPercent}) : ${"%.2f".format(order.kdvAmountTL)} ₺",
                                color = Color.Blue
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Genel Toplam : %.2f ₺"
                                    .format(order.grandTotalTL),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(Modifier.height(12.dp))

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
            )
        },

        bottomBar = {
            BottomAppBar(
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = { navController.navigate("categories") }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Ana Sayfa")
                        }

                        Spacer(modifier = Modifier.width(40.dp))

                        IconButton(
                            onClick = { navController.navigate("projeler") }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Projeler")
                        }
                    }
                }
            )
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
    //FİYAT

    var urunAdi by rememberSaveable { mutableStateOf("") }
    var marka by rememberSaveable { mutableStateOf("") }
    var pompa by rememberSaveable { mutableStateOf("") }
    var asama by rememberSaveable { mutableStateOf("") }
    var membran by rememberSaveable { mutableStateOf("") }
    var tank by rememberSaveable { mutableStateOf("") }
    var kod by rememberSaveable { mutableStateOf("") }
    var fiyat by rememberSaveable { mutableStateOf("") }
    var ozellikler by rememberSaveable { mutableStateOf("") }

    // Endüstriyel alanlar
    var tankBaglanti by rememberSaveable { mutableStateOf("") }
    var girisCikis by rememberSaveable { mutableStateOf("") }
    var debi by rememberSaveable { mutableStateOf("") }
    var sayfa by rememberSaveable { mutableStateOf("") }
    var sayfaPdf by rememberSaveable { mutableStateOf("") }

    var paraBirimi by rememberSaveable { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    //PEYZAJ ALANLAR
    var ebat by rememberSaveable { mutableStateOf("") }
    var istasyonSayisi by rememberSaveable { mutableStateOf("") }
    var ambalajAdedi by rememberSaveable { mutableStateOf("") }
    var tip by rememberSaveable { mutableStateOf("") }
    var koliAdedi by rememberSaveable { mutableStateOf("") }
    var tanim by rememberSaveable { mutableStateOf("") }
    var kutuAdedi by rememberSaveable { mutableStateOf("") }
    var basincDegeri by rememberSaveable { mutableStateOf("") }

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

            fun field(value: String, onChange: (String) -> Unit, label: String) {
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ✅ Ortak alanlar: ürün adı, marka, ürün kodu, fiyat, özellikler
            field(urunAdi, { urunAdi = it }, "Ürün Adı")
            field(kod, { kod = it }, "Ürün Kodu")
            field(fiyat, { fiyat = it }, "Fiyat")
            field(ozellikler, { ozellikler = it }, "Özellikler")

            // ✅ Kategoriye özel alanlar
            when (anaKategori) {

                "Evsel" -> {
                    field(pompa, { pompa = it }, "Pompa")
                    field(asama, { asama = it }, "Aşama")
                    field(membran, { membran = it }, "Membran GPD")
                    field(tank, { tank = it }, "Tank Galon")
                    field(marka, { marka = it }, "Marka")
                }

                "Endüstriyel" -> {
                    field(sayfa, { sayfa = it }, "Model")
                    field(debi, { debi = it }, "Debi m³/saat")
                    field(tankBaglanti, { tankBaglanti = it }, "Tank Bağlantı")
                    field(girisCikis, { girisCikis = it }, "Giriş / Çıkış")
                    field(marka, { marka = it }, "Marka")
                }

                "Peyzaj" -> {
                    field(sayfa, { sayfa = it }, "Nozul Rengi")
                    field(sayfaPdf, { sayfaPdf = it }, "Atış Mesafesi")
                    field(ebat, { ebat = it }, "Ebat")
                    field(istasyonSayisi, { istasyonSayisi = it }, "İstasyon Sayısı")
                    field(ambalajAdedi, { ambalajAdedi = it }, "Ambalaj Adedi")
                    field(tip, { tip = it }, "Tip")
                    field(koliAdedi, { koliAdedi = it }, "Koli Adedi")
                    field(kutuAdedi, { kutuAdedi = it }, "Kutu Adedi")
                    field(basincDegeri, { basincDegeri = it }, "Basınç Değeri")
                    field(tanim, { tanim = it }, "Tanım")

                }
            }

            // ✅ Para birimi dropdown
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

            // ✅ Kaydet butonu
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val product = when (anaKategori) {

                            "Evsel" -> EvselUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = marka,
                                pompa = pompa,
                                asama = asama,
                                membran_gpd = membran.toDoubleOrNull(),
                                tank_galon = tank.toDoubleOrNull(),
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            "Endüstriyel" -> EndustriyelUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = marka,
                                model = sayfa,
                                debi_m3_saat = debi.toDoubleOrNull(),
                                tank_baglanti = tankBaglanti,
                                giris_cikis_baglanti = girisCikis,
                                urun_kodu = kod.ifBlank { null },
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            "Peyzaj" -> PeyzajUrun(
                                anaKategori = anaKategori,
                                altKategori = altKategori,
                                urun_adi = urunAdi,
                                marka = marka,
                                urun_kodu = kod.ifBlank { null },
                                ebat = ebat,
                                istasyon_sayisi = istasyonSayisi.toIntOrNull(),
                                ambalaj_adedi = ambalajAdedi.toIntOrNull(),
                                tip = tip,
                                koli_adedi = koliAdedi.toIntOrNull(),
                                kutu_adedi = kutuAdedi.toIntOrNull(),
                                nozul_rengi = sayfa,
                                atis_mesafesi = sayfaPdf,
                                basinc_degeri = basincDegeri,
                                tanim = tanim,
                                fiyat = fiyat.toDoubleOrNull(),
                                paraBirimi = paraBirimi,
                                ozellikler = ozellikler
                            )

                            else -> return@Button
                        }

                        Firebase.firestore
                            .collection("products")
                            .add(product)

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectProductFormScreen(
    navController: NavHostController,
    projeAdi: String
) {

    var urunAdi by rememberSaveable { mutableStateOf("") }
    var marka by rememberSaveable { mutableStateOf("") }
    var kod by rememberSaveable { mutableStateOf("") }
    var fiyat by rememberSaveable { mutableStateOf("") }
    var ozellikler by rememberSaveable { mutableStateOf("") }

    var tankBaglanti by rememberSaveable { mutableStateOf("") }
    var girisCikis by rememberSaveable { mutableStateOf("") }
    var debi by rememberSaveable { mutableStateOf("") }

    var paraBirimi by rememberSaveable { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projeye Ürün Ekle") },
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

            // Basit field fonksiyonu
            fun field(value: String, onChange: (String) -> Unit, label: String) {
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            field(urunAdi, { urunAdi = it }, "Ürün Adı")
            field(marka, { marka = it }, "Marka")
            field(kod, { kod = it }, "Ürün Kodu")
            field(tankBaglanti, { tankBaglanti = it }, "Tank Bağlantı")
            field(girisCikis, { girisCikis = it }, "Giriş / Çıkış")
            field(debi, { debi = it }, "Debi (m³/saat)")

            item {
                OutlinedTextField(
                    value = fiyat,
                    onValueChange = { fiyat = it },
                    label = { Text("Fiyat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Para birimi dropdown
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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

            field(ozellikler, { ozellikler = it }, "Özellikler")

            // Kaydet butonu
            item {

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = {

                        if (urunAdi.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Ürün adı boş olamaz")
                            }
                            return@Button
                        }

                        loading = true

                        // 🔹 BaseUrun yerine concrete class
                        val product: BaseUrun = EndustriyelUrun(
                            anaKategori = "Proje",
                            altKategori = projeAdi,
                            urun_adi = urunAdi.trim(),
                            marka = marka.trim(),
                            model = "",
                            tank_baglanti = tankBaglanti.trim(),
                            giris_cikis_baglanti = girisCikis.trim(),
                            debi_m3_saat = debi.toDoubleOrNull(),
                            urun_kodu = kod.ifBlank { null },
                            fiyat = fiyat.toDoubleOrNull(),
                            paraBirimi = paraBirimi,
                            ozellikler = ozellikler.trim()
                        )

                        Firebase.firestore
                            .collection("projects")
                            .document(projeAdi)
                            .collection("urunler")
                            .add(product)
                            .addOnSuccessListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Ürün projeye eklendi ✅")
                                    delay(800)
                                    navController.popBackStack()
                                }
                            }
                            .addOnFailureListener {
                                loading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Hata oluştu ❌")
                                }
                            }
                    }
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Kaydet")
                    }
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

    val currentOrder = order ?: return Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    var name by remember { mutableStateOf(currentOrder.customer.customerName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {

            Text("İskonto: %${currentOrder.discountPercent}")

            Spacer(Modifier.height(8.dp))

            Text(
                "Ara Toplam: %.2f ₺".format(
                    Locale.getDefault(),
                    currentOrder.baseTotalTL
                )
            )

            val discountAmount =
                currentOrder.baseTotalTL - currentOrder.discountedTotalTL

            Text(
                "İskonto (%${currentOrder.discountPercent}) : -%.2f ₺"
                    .format(Locale.getDefault(), discountAmount),
                color = Color(0xFF2E7D32)
            )

            Text(
                "KDV (%${currentOrder.kdvPercent}) : %.2f ₺"
                    .format(Locale.getDefault(), currentOrder.kdvAmountTL),
                color = Color.Blue
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "Genel Toplam: %.2f ₺"
                    .format(Locale.getDefault(), currentOrder.grandTotalTL),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32)
            )
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

                    Firebase.firestore
                        .collection("orders")
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