package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NearbySellersMapCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.util.HydroPhysics
import com.example.viewmodel.HydroGrowViewModel

private enum class MarketTab(val label: String) {
    BELANJA("Belanja"),
    SEKITAR_SAYA("Sekitar Saya"),
    PESANAN("Pesanan"),
    TOKO_SAYA("Toko Saya")
}

private enum class MarketSort(val label: String) {
    TERDEKAT("Terdekat"),
    TERMURAH("Termurah"),
    RATING("Rating Tertinggi")
}

@Composable
fun MarketplaceScreen(
    viewModel: HydroGrowViewModel,
    userRole: UserRole,
    isDemoMode: Boolean,
    selectedLocation: CityLocation,
    products: List<MarketplaceProduct>,
    sellerProducts: List<MarketplaceProduct>,
    cartItems: List<CartItem>,
    buyerOrders: List<MarketplaceOrder>,
    sellerOrders: List<MarketplaceOrder>,
    reviews: List<ProductReview>,
    sellerChats: Map<String, List<ChatMessage>>,
    commissionPct: Double,
    defaultAddress: String
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(MarketTab.BELANJA) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MainCategory?>(null) }
    var maxPriceFilter by remember { mutableStateOf<Long?>(null) }
    var radiusKm by remember { mutableDoubleStateOf(25.0) }
    var sortBy by remember { mutableStateOf(MarketSort.TERDEKAT) }
    var selectedSellerId by remember { mutableStateOf<String?>(null) }

    var detailProduct by remember { mutableStateOf<MarketplaceProduct?>(null) }
    var showCartDialog by remember { mutableStateOf(false) }
    var chatSellerPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<MarketplaceProduct?>(null) }

    val productsWithDistance = remember(products, selectedLocation) {
        products.map { prod ->
            val dist = HydroPhysics.calculateDistanceKm(
                selectedLocation.latitude, selectedLocation.longitude,
                prod.sellerLat, prod.sellerLng
            )
            prod to dist
        }
    }

    val filteredProducts = remember(productsWithDistance, searchQuery, selectedCategory, maxPriceFilter, sortBy, selectedSellerId) {
        productsWithDistance
            .filter { (p, _) ->
                (selectedCategory == null || p.mainCategory == selectedCategory!!.name) &&
                    (selectedSellerId == null || p.sellerId == selectedSellerId) &&
                    (maxPriceFilter == null || p.price <= maxPriceFilter!!) &&
                    (searchQuery.isBlank() ||
                        p.name.contains(searchQuery, ignoreCase = true) ||
                        p.subCategory.contains(searchQuery, ignoreCase = true) ||
                        p.sellerArea.contains(searchQuery, ignoreCase = true))
            }
            .let { list ->
                when (sortBy) {
                    MarketSort.TERDEKAT -> list.sortedWith(compareByDescending<Pair<MarketplaceProduct, Double>> { it.first.promoted }.thenBy { it.second })
                    MarketSort.TERMURAH -> list.sortedBy { it.first.price }
                    MarketSort.RATING -> list.sortedByDescending { it.first.rating }
                }
            }
    }

    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        floatingActionButton = {
            if (activeTab == MarketTab.TOKO_SAYA) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductForm = true
                    },
                    icon = { Icon(Icons.Filled.AddBusiness, contentDescription = null) },
                    text = { Text("Tambah Produk") },
                    modifier = Modifier.testTag("seller_add_product_fab")
                )
            } else {
                BadgedBox(
                    badge = {
                        if (cartCount > 0L) Badge { Text(cartCount.toString()) }
                    }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showCartDialog = true },
                        icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                        text = { Text("Keranjang") },
                        modifier = Modifier.testTag("marketplace_cart_fab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            ScrollableTabRow(
                selectedTabIndex = MarketTab.entries.indexOf(activeTab),
                edgePadding = 16.dp
            ) {
                MarketTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("market_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (activeTab) {
                MarketTab.BELANJA -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari selada, AB Mix, rockwool, jasa...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("market_search_input")
                            )
                        }

                        // Category Filter
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("Semua Kategori") }
                                    )
                                }
                                items(MainCategory.entries) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat.label) }
                                    )
                                }
                            }
                        }

                        // Sort & Price Filter
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(MarketSort.entries) { sortOption ->
                                    FilterChip(
                                        selected = sortBy == sortOption,
                                        onClick = { sortBy = sortOption },
                                        label = { Text(sortOption.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = maxPriceFilter == 50000L,
                                        onClick = { maxPriceFilter = if (maxPriceFilter == 50000L) null else 50000L },
                                        label = { Text("≤ Rp50.000", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                if (selectedSellerId != null) {
                                    item {
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedSellerId = null },
                                            label = { Text("Reset Filter Toko") },
                                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Produk Tidak Ditemukan",
                                    subtitle = "Ubah kata kunci pencarian atau reset filter kategori & harga."
                                )
                            }
                        } else {
                            items(filteredProducts, key = { it.first.id }) { (product, distKm) ->
                                ProductCard(
                                    product = product,
                                    distanceKm = distKm,
                                    onClick = { detailProduct = product },
                                    onAddToCart = { viewModel.addToCart(product, 1L) }
                                )
                            }
                        }
                    }
                }

                MarketTab.SEKITAR_SAYA -> {
                    val nearbyFiltered = remember(productsWithDistance, radiusKm, selectedSellerId) {
                        productsWithDistance
                            .filter { (_, dist) -> dist <= radiusKm }
                            .filter { (p, _) -> selectedSellerId == null || p.sellerId == selectedSellerId }
                            .sortedBy { it.second }
                    }
                    val sellersGrouped = remember(nearbyFiltered) {
                        nearbyFiltered.groupBy { it.first.sellerId }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Filter Radius Jarak dari ${selectedLocation.name}", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(1.0, 5.0, 10.0, 25.0, 50.0)) { r ->
                                    FilterChip(
                                        selected = radiusKm == r,
                                        onClick = { radiusKm = r },
                                        label = { Text("${r.toInt()} km") }
                                    )
                                }
                            }
                        }

                        item {
                            NearbySellersMapCanvas(
                                userLocation = selectedLocation,
                                radiusKm = radiusKm,
                                products = products,
                                selectedSellerId = selectedSellerId,
                                onSelectSeller = { sid ->
                                    selectedSellerId = if (selectedSellerId == sid) null else sid
                                }
                            )
                        }

                        if (sellersGrouped.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Penjual Dalam Radius ${radiusKm.toInt()} km",
                                    subtitle = "Perbesar radius pencarian (misalnya 25 km atau 50 km) untuk menemukan kebun & supplier terdekat."
                                )
                            }
                        } else {
                            items(sellersGrouped.entries.toList(), key = { it.key }) { (sellerId, sellerEntries) ->
                                val sample = sellerEntries.first().first
                                val dist = sellerEntries.first().second
                                ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sample.sellerName, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${sample.sellerArea} • Jarak ${Formatters.formatDistanceKm(dist)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            StatusBadge("${sellerEntries.size} Produk", LeafGreenContainer, OnLeafGreenContainer)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        sellerEntries.take(3).forEach { (prod, _) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { detailProduct = prod }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("• ${prod.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                Text("${Formatters.rupiah(prod.price)}/${prod.unit}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedSellerId = sellerId
                                                    activeTab = MarketTab.BELANJA
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Lihat Toko")
                                            }
                                            Button(
                                                onClick = {
                                                    val uri = Uri.parse("geo:${sample.sellerLat},${sample.sellerLng}?q=${sample.sellerLat},${sample.sellerLng}(${Uri.encode(sample.sellerName)})")
                                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                                    runCatching { context.startActivity(intent) }.onFailure {
                                                        viewModel.showMessage("Aplikasi peta tidak tersedia; jarak ke ${sample.sellerName}: ${Formatters.formatDistanceKm(dist)}")
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Navigasi")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MarketTab.PESANAN -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (buyerOrders.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Pesanan",
                                    subtitle = "Pesanan hasil panen, perlengkapan, dan jasa hidroponik Anda akan tampil di sini."
                                )
                            }
                        } else {
                            items(buyerOrders, key = { it.id }) { ord ->
                                val statusEnum = OrderStatus.entries.firstOrNull { it.name == ord.orderStatus } ?: OrderStatus.MENUNGGU_KONFIRMASI
                                val payEnum = PaymentStatus.entries.firstOrNull { it.name == ord.paymentStatus } ?: PaymentStatus.MENUNGGU
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Order #${ord.id.take(10)}", style = MaterialTheme.typography.titleSmall)
                                            StatusBadge(statusEnum.label, LeafGreenContainer, OnLeafGreenContainer)
                                        }
                                        Text(ord.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                                        Text("Penjual: ${ord.sellerName} • ${ord.deliveryMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "Total: ${Formatters.rupiah(ord.totalAmount)} • ${ord.paymentMethod} (${payEnum.label})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (ord.orderStatus == OrderStatus.DIKIRIM.name || ord.orderStatus == OrderStatus.DIPROSES.name) {
                                            Button(
                                                onClick = { viewModel.advanceOrderStatus(ord, OrderStatus.SELESAI) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Konfirmasi Pesanan Diterima (Selesai)")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MarketTab.TOKO_SAYA -> {
                    val totalSellerRevenue = sellerOrders.filter { it.orderStatus == OrderStatus.SELESAI.name }.sumOf { it.subtotal }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Dashboard Penjual (${userRole.label})", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Pendapatan Penjualan Selesai: ${Formatters.rupiah(totalSellerRevenue)} • Produk Aktif: ${sellerProducts.count { it.active }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        item { Text("Pesanan Masuk (${sellerOrders.size})", style = MaterialTheme.typography.titleMedium) }
                        if (sellerOrders.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Belum Ada Pesanan Masuk",
                                    subtitle = "Tambahkan produk panen atau perlengkapan agar pembeli lokal dapat memesan."
                                )
                            }
                        } else {
                            items(sellerOrders, key = { "seller_${it.id}" }) { ord ->
                                val statusEnum = OrderStatus.entries.firstOrNull { it.name == ord.orderStatus } ?: OrderStatus.MENUNGGU_KONFIRMASI
                                Card(shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${ord.buyerName} • #${ord.id.take(8)}", style = MaterialTheme.typography.titleSmall)
                                            StatusBadge(statusEnum.label, AmberContainer, OnAmberContainer)
                                        }
                                        Text(ord.itemsSummary, style = MaterialTheme.typography.bodySmall)
                                        Text("Subtotal: ${Formatters.rupiah(ord.subtotal)} (Komisi ilustratif: ${Formatters.rupiah(ord.commissionAmount)})", style = MaterialTheme.typography.labelMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (ord.orderStatus == OrderStatus.MENUNGGU_KONFIRMASI.name) {
                                                Button(onClick = { viewModel.advanceOrderStatus(ord, OrderStatus.DIPROSES) }) {
                                                    Text("Proses Pesanan")
                                                }
                                            } else if (ord.orderStatus == OrderStatus.DIPROSES.name) {
                                                Button(onClick = { viewModel.advanceOrderStatus(ord, OrderStatus.DIKIRIM) }) {
                                                    Text("Kirim Pesanan")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Text("Katalog Produk Saya (${sellerProducts.size})", style = MaterialTheme.typography.titleMedium) }
                        items(sellerProducts, key = { "myprod_${it.id}" }) { prod ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${Formatters.rupiah(prod.price)}/${prod.unit} • Stok ${prod.stock}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = prod.active,
                                        onCheckedChange = { viewModel.toggleProductActive(prod.id, it) }
                                    )
                                    IconButton(onClick = {
                                        editingProduct = prod
                                        showProductForm = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit produk")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailProduct?.let { prod ->
        val dist = HydroPhysics.calculateDistanceKm(
            selectedLocation.latitude, selectedLocation.longitude,
            prod.sellerLat, prod.sellerLng
        )
        ProductDetailDialog(
            product = prod,
            distanceKm = dist,
            reviews = reviews.filter { it.productId == prod.id },
            onAddToCart = { qty ->
                viewModel.addToCart(prod, qty)
                detailProduct = null
            },
            onAddReview = { r, comment -> viewModel.addProductReview(prod.id, r, comment) },
            onOpenChat = {
                chatSellerPair = prod.sellerId to prod.sellerName
                detailProduct = null
            },
            onDismiss = { detailProduct = null }
        )
    }

    if (showCartDialog) {
        CartAndCheckoutDialog(
            cartItems = cartItems,
            isDemoMode = isDemoMode,
            commissionPct = commissionPct,
            defaultAddress = defaultAddress.ifBlank { "Jl. Kebun Hidroponik, ${selectedLocation.name}" },
            onUpdateQty = { item, newQty -> viewModel.updateCartQuantity(item, newQty) },
            onCheckout = { deliveryMethod, addr, paymentMethod ->
                viewModel.checkoutCart(cartItems, deliveryMethod, addr, paymentMethod)
                showCartDialog = false
            },
            onDismiss = { showCartDialog = false }
        )
    }

    chatSellerPair?.let { (sellerId, sellerName) ->
        SellerChatDialog(
            sellerName = sellerName,
            messages = sellerChats[sellerId].orEmpty(),
            onSendMessage = { msg -> viewModel.sendSellerChat(sellerId, sellerName, msg) },
            onDismiss = { chatSellerPair = null }
        )
    }

    if (showProductForm) {
        ProductFormDialog(
            initial = editingProduct,
            onSave = { id, name, desc, mainCat, subCat, price, unit, stock, freshness, delivery, promoted, photoUrl ->
                viewModel.saveProduct(id, name, desc, mainCat, subCat, price, unit, stock, freshness, delivery, promoted, photoUrl)
                showProductForm = false
            },
            onDismiss = { showProductForm = false }
        )
    }
}

@Composable
private fun ProductCard(
    product: MarketplaceProduct,
    distanceKm: Double,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = product.photoUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                val fallbackImg = when (product.mainCategory) {
                    MainCategory.HASIL_PANEN.name -> R.drawable.img_onboarding_market
                    MainCategory.JASA.name -> R.drawable.img_edu_nft_system
                    else -> R.drawable.img_hero_hydroponic
                }
                Image(
                    painter = painterResource(id = fallbackImg),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (product.promoted) StatusBadge("PROMOSI", AmberContainer, OnAmberContainer)
                    if (product.isDemo) StatusBadge("DEMO", SurfaceVariantLight, TextPrimaryLight)
                }
                Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${product.sellerName} • ${Formatters.formatDistanceKm(distanceKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${Formatters.rupiah(product.price)}/${product.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "★ ${product.rating} • Stok ${product.stock}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onAddToCart,
                        enabled = product.stock > 0L
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = "Tambah ke keranjang")
                    }
                }
            }
        }
    }
}
