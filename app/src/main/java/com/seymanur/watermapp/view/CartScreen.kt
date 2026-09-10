package com.seymanur.watermapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.seymanur.watermapp.data.CartManager
import com.seymanur.watermapp.model.Customer
import com.seymanur.watermapp.model.Order
import com.seymanur.watermapp.model.OrderItem
import com.seymanur.watermapp.printOrder
import com.seymanur.watermapp.utils.MoneyFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavHostController) {

    var showSummary by remember { mutableStateOf(false) }

    val expandedProjects = remember { mutableStateMapOf<String, Boolean>() }

    var projectToDelete by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var itemToDelete by remember {
        mutableStateOf<CartManager.CartItem?>(null)
    }

    val context = LocalContext.current

    val cart = CartManager.cart

    // ---------------------------------------------------------
    // PROJE ÜRÜNLERİ
    // ---------------------------------------------------------

    val projectItems = cart.filter {
        !it.projectName.isNullOrBlank() &&
                !it.urun?.urun_adi.isNullOrBlank()
    }

    // ---------------------------------------------------------
    // NORMAL ÜRÜNLER
    // ---------------------------------------------------------

    val otherItems = cart.filter {
        it.projectName == null
    }

    // ---------------------------------------------------------
    // PROJE GRUPLAMA
    // ---------------------------------------------------------

    val groupedProjects = projectItems.groupBy {
        it.projectInstanceId
    }

    // ---------------------------------------------------------
    // TOPLAMLAR
    // ---------------------------------------------------------

    val totals by remember(
        CartManager.cart,
        CartManager.appliedDiscountPercent,
        CartManager.kdvEnabled,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf {
            CartManager.totalsWithDiscount()
        }
    }

    val baseTotals by remember(
        CartManager.cart,
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        derivedStateOf {
            CartManager.totalsByCurrency()
        }
    }

    var usdText by remember { mutableStateOf("") }
    var eurText by remember { mutableStateOf("") }

    LaunchedEffect(
        CartManager.usdRate,
        CartManager.eurRate
    ) {
        usdText = CartManager.usdRate.toString()
        eurText = CartManager.eurRate.toString()
    }

    var discountText by remember {
        mutableStateOf("")
    }


    // =========================================================
    // CART -> ORDER
    // =========================================================

    fun createOrderFromCart(): Order {

        // -----------------------------------------------------
        // Sepetteki ürünleri OrderItem listesine çevir
        // -----------------------------------------------------

        val orderItems = CartManager.cart.mapNotNull { cartItem ->

            val product = cartItem.urun

            if (product == null) {
                null
            } else {

                OrderItem(
                    model = product.urun_adi ?: "",
                    quantity = cartItem.quantity.value,
                    price = product.fiyat ?: 0.0,
                    currency = product.paraBirimi ?: "TL"
                )
            }
        }


        // -----------------------------------------------------
        // Müşteri bilgileri
        //
        // Eğer müşteri formu daha önce doldurulmadıysa boş
        // değerler kullanılır.
        // -----------------------------------------------------

        val customer = Customer(
            customerName = "",
            companyName = "",
            phone = "",
            deviceName = "",
            email = "",
            staffName = "",
            note = "",
            address = ""
        )


        // -----------------------------------------------------
        // İskonto öncesi TL toplam
        // -----------------------------------------------------

        var baseTotalTL = 0.0

        CartManager.cart.forEach { cartItem ->

            val product = cartItem.urun ?: return@forEach

            val price = product.fiyat ?: 0.0

            val currency = product.paraBirimi ?: "TL"

            val quantity = cartItem.quantity.value

            val priceTL = when (currency) {

                "USD" ->
                    price * CartManager.usdRate

                "EUR" ->
                    price * CartManager.eurRate

                else ->
                    price
            }

            baseTotalTL += priceTL * quantity
        }


        // -----------------------------------------------------
        // İSKONTO
        // -----------------------------------------------------

        val discountPercent =
            CartManager.appliedDiscountPercent

        val discountedTotalTL =
            baseTotalTL *
                    (1 - discountPercent / 100.0)


        // -----------------------------------------------------
        // KDV
        // -----------------------------------------------------

        val kdvPercent =
            if (CartManager.kdvEnabled)
                CartManager.kdvRate * 100
            else
                0.0

        val kdvAmountTL =
            if (CartManager.kdvEnabled)
                discountedTotalTL * CartManager.kdvRate
            else
                0.0


        // -----------------------------------------------------
        // GENEL TOPLAM
        // -----------------------------------------------------

        val grandTotalTL =
            discountedTotalTL + kdvAmountTL


        // -----------------------------------------------------
        // ORDER OLUŞTUR
        // -----------------------------------------------------

        return Order(
            customer = customer,

            items = orderItems,

            discountPercent = discountPercent,

            kdvPercent = kdvPercent,

            baseTotalTL = baseTotalTL,

            discountedTotalTL = discountedTotalTL,

            kdvAmountTL = kdvAmountTL,

            grandTotalTL = grandTotalTL,

            totalTL = grandTotalTL,

            usdRate = CartManager.usdRate,

            eurRate = CartManager.eurRate,

            timestamp = System.currentTimeMillis(),

            createdAt = System.currentTimeMillis()
        )
    }


    // =========================================================
    // EKRAN
    // =========================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Sepet")
                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },

                actions = {

                    // =================================================
                    // YAZDIR BUTONU
                    // =================================================

                    IconButton(
                        onClick = {

                            // 🔥 SEPETTEN GÜNCEL ORDER OLUŞTUR
                            val order = createOrderFromCart()

                            // 🔥 ORDER'I YAZDIR
                            printOrder(
                                context,
                                order
                            )
                        }
                    ) {

                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Yazdır"
                        )
                    }


                    // =================================================
                    // MÜŞTERİ FORMU
                    // =================================================

                    if (CartManager.SessionManager.isStaff) {

                        IconButton(
                            onClick = {

                                navController.navigate(
                                    "customerForm"
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        ) {

                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Müşteri Formu"
                            )
                        }
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


            // =====================================================
            // KURLAR
            // =====================================================

            Text(
                "Güncel Kur Gir",
                style = MaterialTheme.typography.titleMedium
            )


            OutlinedTextField(

                value = usdText,

                onValueChange = {

                    usdText = it

                    CartManager.usdRate =
                        it.toDoubleOrNull() ?: 0.0
                },

                label = {
                    Text("USD → TL")
                },

                modifier = Modifier.fillMaxWidth()
            )


            OutlinedTextField(

                value = eurText,

                onValueChange = {

                    eurText = it

                    CartManager.eurRate =
                        it.toDoubleOrNull() ?: 0.0
                },

                label = {
                    Text("EUR → TL")
                },

                modifier = Modifier.fillMaxWidth()
            )


            Spacer(
                Modifier.height(4.dp)
            )


            // =====================================================
            // SEPET
            // =====================================================

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {


                // =================================================
                // PROJELER
                // =================================================

                items(
                    groupedProjects.toList()
                ) { (instanceId, items) ->

                    val projectName =
                        items.first().projectName
                            ?: "Proje"


                    val dismissState =
                        rememberSwipeToDismissBoxState()


                    LaunchedEffect(
                        dismissState.currentValue
                    ) {

                        if (
                            dismissState.currentValue ==
                            SwipeToDismissBoxValue.StartToEnd
                        ) {

                            projectToDelete =
                                instanceId

                            showDeleteDialog =
                                true

                            itemToDelete =
                                null

                            dismissState.reset()
                        }
                    }


                    SwipeToDismissBox(

                        state = dismissState,

                        enableDismissFromStartToEnd = true,

                        enableDismissFromEndToStart = false,

                        backgroundContent = {

                            Row(

                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Start
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.width(24.dp)
                                )

                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            }
                        }

                    ) {


                        Card(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {

                            Column {


                                Row(

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(

                                        text =
                                            "📦 $projectName",

                                        style =
                                            MaterialTheme.typography.titleLarge,

                                        modifier =
                                            Modifier.weight(1f)
                                    )


                                    IconButton(

                                        onClick = {

                                            expandedProjects[
                                                instanceId ?: ""
                                            ] =
                                                !(
                                                        expandedProjects[
                                                            instanceId ?: ""
                                                        ] ?: false
                                                        )
                                        }
                                    ) {

                                        Icon(

                                            imageVector =

                                                if (
                                                    expandedProjects[
                                                        instanceId ?: ""
                                                    ] == true
                                                )

                                                    Icons.Default.KeyboardArrowUp

                                                else

                                                    Icons.Default.KeyboardArrowDown,

                                            contentDescription =
                                                "Expand"
                                        )
                                    }
                                }


                                // =================================
                                // PROJE ÜRÜNLERİ
                                // =================================

                                if (
                                    expandedProjects[
                                        instanceId ?: ""
                                    ] == true
                                ) {

                                    items.forEach { item ->

                                        if (
                                            item.urun
                                                ?.urun_adi
                                                .isNullOrBlank()
                                        ) {
                                            return@forEach
                                        }


                                        Row(

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 16.dp,
                                                        bottom = 8.dp
                                                    ),

                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {


                                            Column(
                                                modifier =
                                                    Modifier.weight(1f)
                                            ) {

                                                Text(

                                                    text =
                                                        item.urun
                                                            ?.urun_adi
                                                            ?: "",

                                                    style =
                                                        MaterialTheme.typography.titleMedium
                                                )


                                                val price =
                                                    item.urun
                                                        ?.fiyat
                                                        ?: 0.0

                                                val currency =
                                                    item.urun
                                                        ?.paraBirimi
                                                        ?: "TL"


                                                Text(

                                                    text = when (currency) {

                                                        "USD" ->
                                                            "Birim Fiyat: ${
                                                                MoneyFormatter.usd(
                                                                    price
                                                                )
                                                            }"

                                                        "EUR" ->
                                                            "Birim Fiyat: ${
                                                                MoneyFormatter.eur(
                                                                    price
                                                                )
                                                            }"

                                                        else ->
                                                            "Birim Fiyat: ${
                                                                MoneyFormatter.tl(
                                                                    price
                                                                )
                                                            }"
                                                    },

                                                    style =
                                                        MaterialTheme.typography.bodySmall,

                                                    color =
                                                        Color.Gray
                                                )


                                                val tlPrice = when (currency) {

                                                    "USD" ->
                                                        price *
                                                                CartManager.usdRate

                                                    "EUR" ->
                                                        price *
                                                                CartManager.eurRate

                                                    else ->
                                                        price
                                                }


                                                Text(

                                                    text =
                                                        "Toplam: ${
                                                            MoneyFormatter.tl(
                                                                tlPrice *
                                                                        item.quantity.value
                                                            )
                                                        }",

                                                    style =
                                                        MaterialTheme.typography.bodyMedium,

                                                    color =
                                                        Color(0xFF2E7D32)
                                                )
                                            }


                                            // =================================
                                            // ADET
                                            // =================================

                                            Column(

                                                horizontalAlignment =
                                                    Alignment.CenterHorizontally
                                            ) {

                                                IconButton(

                                                    onClick = {

                                                        if (
                                                            item.quantity.value > 1
                                                        ) {

                                                            item.quantity.value--

                                                        } else {

                                                            CartManager.cart.remove(
                                                                item
                                                            )
                                                        }
                                                    }
                                                ) {

                                                    Icon(
                                                        Icons.Default.KeyboardArrowDown,
                                                        null
                                                    )
                                                }


                                                Text(
                                                    text =
                                                        "${item.quantity.value} Adet",
                                                    style =
                                                        MaterialTheme.typography.bodyMedium
                                                )


                                                IconButton(

                                                    onClick = {

                                                        item.quantity.value++
                                                    }
                                                ) {

                                                    Icon(
                                                        Icons.Default.KeyboardArrowUp,
                                                        null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // =================================================
                // NORMAL ÜRÜNLER
                // =================================================

                items(otherItems) { item ->

                    val dismissState =
                        rememberSwipeToDismissBoxState()


                    LaunchedEffect(
                        dismissState.currentValue
                    ) {

                        if (
                            dismissState.currentValue ==
                            SwipeToDismissBoxValue.StartToEnd
                        ) {

                            itemToDelete =
                                item

                            showDeleteDialog =
                                true

                            projectToDelete =
                                null

                            dismissState.reset()
                        }
                    }


                    SwipeToDismissBox(

                        state = dismissState,

                        enableDismissFromStartToEnd = true,

                        enableDismissFromEndToStart = false,

                        backgroundContent = {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.White),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Start
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.width(24.dp)
                                )

                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            }
                        }

                    ) {


                        Card(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                        ) {

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {


                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(

                                        text =
                                            item.urun
                                                ?.urun_adi
                                                ?: "",

                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )


                                    val price =
                                        item.urun
                                            ?.fiyat
                                            ?: 0.0

                                    val currency =
                                        item.urun
                                            ?.paraBirimi
                                            ?: "TL"


                                    Text(

                                        text = when (currency) {

                                            "USD" ->
                                                "Birim Fiyat: ${
                                                    MoneyFormatter.usd(
                                                        price
                                                    )
                                                }"

                                            "EUR" ->
                                                "Birim Fiyat: ${
                                                    MoneyFormatter.eur(
                                                        price
                                                    )
                                                }"

                                            else ->
                                                "Birim Fiyat: ${
                                                    MoneyFormatter.tl(
                                                        price
                                                    )
                                                }"
                                        },

                                        style =
                                            MaterialTheme.typography.bodySmall,

                                        color =
                                            Color.Gray
                                    )


                                    val tlPrice = when (currency) {

                                        "USD" ->
                                            price *
                                                    CartManager.usdRate

                                        "EUR" ->
                                            price *
                                                    CartManager.eurRate

                                        else ->
                                            price
                                    }


                                    Text(

                                        text =
                                            "Toplam: ${
                                                MoneyFormatter.tl(
                                                    tlPrice *
                                                            item.quantity.value
                                                )
                                            }",

                                        style =
                                            MaterialTheme.typography.bodyMedium,

                                        color =
                                            Color(0xFF2E7D32)
                                    )
                                }


                                // =================================
                                // ADET
                                // =================================

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    IconButton(

                                        onClick = {

                                            if (
                                                item.quantity.value > 1
                                            ) {

                                                item.quantity.value--

                                            } else {

                                                CartManager.cart.remove(
                                                    item
                                                )
                                            }
                                        }
                                    ) {

                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription =
                                                "Azalt"
                                        )
                                    }


                                    Text(

                                        text =
                                            item.quantity.value.toString(),

                                        modifier =
                                            Modifier.padding(
                                                horizontal = 8.dp
                                            )
                                    )


                                    IconButton(

                                        onClick = {

                                            item.quantity.value++
                                        }
                                    ) {

                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription =
                                                "Arttır"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // =====================================================
            // SİLME DİALOG
            // =====================================================

            if (showDeleteDialog) {

                AlertDialog(

                    onDismissRequest = {

                        showDeleteDialog =
                            false

                        itemToDelete =
                            null

                        projectToDelete =
                            null
                    },


                    title = {

                        Text(

                            if (
                                itemToDelete != null
                            )
                                "Ürünü Sil"
                            else
                                "Projeyi Sil"
                        )
                    },


                    text = {

                        Text(

                            if (
                                itemToDelete != null
                            )

                                "Bu ürünü sepetten silmek istiyor musunuz?"

                            else

                                "Bu projeyi ve içindeki tüm ürünleri silmek istiyor musunuz?"
                        )
                    },


                    confirmButton = {

                        TextButton(

                            onClick = {

                                if (
                                    itemToDelete != null
                                ) {

                                    CartManager.cart.remove(
                                        itemToDelete!!
                                    )
                                }


                                if (
                                    projectToDelete != null
                                ) {

                                    CartManager.cart.removeAll {

                                        it.projectInstanceId ==
                                                projectToDelete
                                    }
                                }


                                itemToDelete =
                                    null

                                projectToDelete =
                                    null

                                showDeleteDialog =
                                    false
                            }
                        ) {

                            Text("Evet")
                        }
                    },


                    dismissButton = {

                        TextButton(

                            onClick = {

                                itemToDelete =
                                    null

                                projectToDelete =
                                    null

                                showDeleteDialog =
                                    false
                            }
                        ) {

                            Text("Vazgeç")
                        }
                    }
                )
            }


            // =====================================================
            // DETAYLAR BUTONU
            // =====================================================

            Spacer(
                Modifier.height(8.dp)
            )


            Button(

                onClick = {
                    showSummary =
                        !showSummary
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(

                    if (showSummary)
                        "Detayları Gizle ▲"
                    else
                        "Detayları Göster ▼"
                )
            }


            // =====================================================
            // ÖZET
            // =====================================================

            if (showSummary) {

                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    elevation =
                        CardDefaults.cardElevation(
                            4.dp
                        )
                ) {

                    Column {


                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),

                            verticalAlignment =
                                Alignment.CenterVertically,

                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {


                            OutlinedTextField(

                                value =
                                    discountText,

                                onValueChange = {
                                    discountText =
                                        it
                                },

                                label = {
                                    Text("% İskonto")
                                },

                                modifier =
                                    Modifier.weight(1f),

                                singleLine = true
                            )


                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Checkbox(

                                    checked =
                                        CartManager.kdvEnabled,

                                    onCheckedChange = {

                                        CartManager.kdvEnabled =
                                            it
                                    }
                                )

                                Text("KDV")
                            }


                            Button(

                                onClick = {

                                    val percent =
                                        discountText
                                            .toDoubleOrNull()
                                            ?: 0.0

                                    CartManager.applyDiscount(
                                        percent
                                    )
                                }
                            ) {

                                Text("Uygula")
                            }
                        }


                        // =========================================
                        // DÖVİZ TOPLAMLARI
                        // =========================================

                        val totals =
                            CartManager.totalsByCurrency()


                        totals.forEach {
                                (currency, baseTotal) ->


                            val discounted =
                                baseTotal *
                                        (
                                                1 -
                                                        CartManager.appliedDiscountPercent /
                                                        100
                                                )


                            val finalCurrency =

                                if (
                                    CartManager.kdvEnabled
                                )

                                    discounted *
                                            (
                                                    1 +
                                                            CartManager.kdvRate
                                                    )

                                else

                                    discounted


                            val finalTL = when (currency) {

                                "USD" ->
                                    finalCurrency *
                                            CartManager.usdRate

                                "EUR" ->
                                    finalCurrency *
                                            CartManager.eurRate

                                "TL" ->
                                    finalCurrency

                                else ->
                                    finalCurrency
                            }


                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )
                            ) {


                                Text(

                                    text =
                                        "≈ ${
                                            MoneyFormatter.tl(
                                                finalTL
                                            )
                                        }",

                                    style =
                                        MaterialTheme.typography.bodySmall,

                                    color =
                                        Color.Gray
                                )


                                Text(

                                    text = when (currency) {

                                        "USD" ->
                                            MoneyFormatter.usd(
                                                finalCurrency
                                            )

                                        "EUR" ->
                                            MoneyFormatter.eur(
                                                finalCurrency
                                            )

                                        "TL" ->
                                            MoneyFormatter.tl(
                                                finalCurrency
                                            )

                                        else ->
                                            finalCurrency.toString()
                                    },

                                    style =
                                        MaterialTheme.typography.headlineSmall
                                )
                            }
                        }


                        Spacer(
                            Modifier.height(8.dp)
                        )


                        val totalTL =
                            CartManager.totalInTL()


                        Text(

                            text =
                                "Genel Toplam: ${
                                    MoneyFormatter.tl(
                                        totalTL
                                    )
                                }",

                            modifier =
                                Modifier.padding(12.dp),

                            style =
                                MaterialTheme.typography.titleMedium,

                            color =
                                Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}