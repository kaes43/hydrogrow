package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun ProductDetailDialog(
    product: MarketplaceProduct,
    distanceKm: Double,
    reviews: List<ProductReview>,
    onAddToCart: (Long) -> Unit,
    onAddReview: (Int, String) -> Unit,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember { mutableLongStateOf(1L) }
    var ratingInput by remember { mutableIntStateOf(5) }
    var commentInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (product.promoted) StatusBadge("PROMOSI / SPONSOR", AmberContainer, OnAmberContainer)
                    if (product.isDemo) StatusBadge("DEMO", SurfaceVariantLight, TextPrimaryLight)
                    StatusBadge(product.freshnessStatus, LeafGreenContainer, OnLeafGreenContainer)
                }

                if (product.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.photoUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Text(
                    "${Formatters.rupiah(product.price)} / ${product.unit}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(product.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Penjual: ${product.sellerName}\nLokasi: ${product.sellerArea} (${Formatters.formatDistanceKm(distanceKm)})\nStok tersedia: ${product.stock} ${product.unit}\nPengiriman: ${product.deliveryMethods}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quantity picker
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Jumlah:", style = MaterialTheme.typography.titleSmall)
                    FilledTonalIconButton(onClick = { if (qty > 1L) qty-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Kurangi")
                    }
                    Text("$qty ${product.unit}", style = MaterialTheme.typography.titleMedium)
                    FilledTonalIconButton(onClick = { if (qty < product.stock) qty++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah")
                    }
                }

                OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chat & Hubungi Penjual")
                }

                HorizontalDivider()
                Text("Rating & Ulasan (${reviews.size})", style = MaterialTheme.typography.titleSmall)
                reviews.forEach { rev ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rev.reviewerName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("★ ${rev.rating}/5", style = MaterialTheme.typography.labelSmall, color = OrangeHighlight)
                            }
                            Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating $star",
                            tint = if (star <= ratingInput) AmberAccent else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { ratingInput = star }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("Tulis ulasan...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (commentInput.isNotBlank()) {
                            onAddReview(ratingInput, commentInput)
                            commentInput = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim ulasan")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddToCart(qty) },
                enabled = product.stock > 0L
            ) {
                Text(if (product.stock > 0L) "Tambah ke Keranjang" else "Stok Habis")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun CartAndCheckoutDialog(
    cartItems: List<CartItem>,
    isDemoMode: Boolean,
    commissionPct: Double,
    defaultAddress: String,
    onUpdateQty: (CartItem, Long) -> Unit,
    onCheckout: (deliveryMethod: String, address: String, paymentMethod: String) -> Unit,
    onDismiss: () -> Unit
) {
    var deliveryMethod by remember { mutableStateOf("Kurir Lokal") }
    var address by remember { mutableStateOf(defaultAddress) }
    var paymentMethod by remember { mutableStateOf("QRIS") }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val shippingFee = if (cartItems.isEmpty() || deliveryMethod == "Ambil Langsung") 0L else 12000L
    val serviceFee = if (cartItems.isEmpty()) 0L else 2000L
    val commissionEstimate = ((subtotal * commissionPct) / 100.0).toLong()
    val total = subtotal + shippingFee + serviceFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keranjang & Checkout") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cartItems.isEmpty()) {
                    EmptyStateCard(
                        title = "Keranjang Kosong",
                        subtitle = "Pilih hasil panen segar, nutrisi, atau jasa hidroponik terlebih dahulu."
                    )
                } else {
                    cartItems.forEach { item ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.titleSmall)
                                    Text("${Formatters.rupiah(item.unitPrice)}/${item.unit} • ${item.sellerName}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onUpdateQty(item, item.quantity - 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Kurangi")
                                }
                                Text("${item.quantity}", style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { onUpdateQty(item, item.quantity + 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = "Tambah")
                                }
                            }
                        }
                    }

                    Text("Metode Pengiriman:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Kurir Lokal", "Ambil Langsung").forEach { m ->
                            FilterChip(selected = deliveryMethod == m, onClick = { deliveryMethod = m }, label = { Text(m) })
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Pengiriman / Catatan Lokasi") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Metode Pembayaran (Tanpa menyimpan PIN/CVV/Kartu):", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("QRIS", "Virtual Account", "Transfer Bank", "E-Wallet")) { pm ->
                            FilterChip(selected = paymentMethod == pm, onClick = { paymentMethod = pm }, label = { Text(pm, style = MaterialTheme.typography.labelSmall) })
                        }
                    }

                    Surface(
                        color = if (isDemoMode) AmberContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isDemoMode) {
                                "Simulasi Pembayaran DEMO: Transaksi tidak menggunakan uang sungguhan."
                            } else {
                                "Pembayaran melalui penyedia pembayaran berizin. Status terkonfirmasi setelah verifikasi penyedia."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDemoMode) OnAmberContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.rupiah(subtotal), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ongkos Kirim", style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.rupiah(shippingFee), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Biaya Layanan", style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.rupiah(serviceFee), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Komisi Platform Ilustratif (${commissionPct}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formatters.rupiah(commissionEstimate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium)
                        Text(Formatters.rupiah(total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            if (cartItems.isNotEmpty()) {
                Button(onClick = { onCheckout(deliveryMethod, address, paymentMethod) }) {
                    Text(if (isDemoMode) "Buat Pesanan Demo" else "Buat Pesanan")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun SellerChatDialog(
    sellerName: String,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat: $sellerName") },
        text = {
            Column(modifier = Modifier.heightIn(min = 200.dp, max = 340.dp)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                "Mulai percakapan dengan penjual untuk menanyakan kesegaran panen, stok grosir, atau jadwal kirim.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (msg.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 220.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Ketik pesan...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = {
                        if (input.isNotBlank()) {
                            onSendMessage(input)
                            input = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun ProductFormDialog(
    initial: MarketplaceProduct?,
    onSave: (
        id: String,
        name: String,
        desc: String,
        mainCat: MainCategory,
        subCat: String,
        price: Long,
        unit: String,
        stock: Long,
        freshness: String,
        delivery: String,
        promoted: Boolean,
        photoUrl: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var mainCat by remember {
        mutableStateOf(
            MainCategory.entries.firstOrNull { it.name == initial?.mainCategory } ?: MainCategory.HASIL_PANEN
        )
    }
    var subCat by remember { mutableStateOf(initial?.subCategory ?: "Selada") }
    var priceStr by remember { mutableStateOf((initial?.price ?: 20000L).toString()) }
    var unit by remember { mutableStateOf(initial?.unit ?: "kg") }
    var stockStr by remember { mutableStateOf((initial?.stock ?: 20L).toString()) }
    var freshness by remember { mutableStateOf(initial?.freshnessStatus ?: "Panen Hari Ini") }
    var delivery by remember { mutableStateOf(initial?.deliveryMethods ?: "Kurir Lokal, Ambil Langsung") }
    var promoted by remember { mutableStateOf(initial?.promoted ?: false) }
    var photoUrl by remember { mutableStateOf(initial?.photoUrl ?: "") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUrl = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk / Jasa" else "Edit Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MainCategory.entries) { c ->
                        FilterChip(selected = mainCat == c, onClick = { mainCat = c }, label = { Text(c.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Produk / Jasa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subCat, onValueChange = { subCat = it }, label = { Text("Sub-kategori (Selada, AB Mix, dll)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Harga (Rp)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Satuan") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Stok") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = freshness, onValueChange = { freshness = it }, label = { Text("Status Kesegaran") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = delivery, onValueChange = { delivery = it }, label = { Text("Metode Pengiriman") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Produk") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = promoted, onCheckedChange = { promoted = it })
                    Text("Tandai sebagai Produk Promosi / Sponsor", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUrl.isBlank()) "Unggah Foto Produk" else "Foto Dipilih ✓")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initial?.id ?: "",
                    name,
                    desc,
                    mainCat,
                    subCat.ifBlank { "Umum" },
                    priceStr.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                    unit.ifBlank { "kg" },
                    stockStr.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                    freshness,
                    delivery,
                    promoted,
                    photoUrl
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
