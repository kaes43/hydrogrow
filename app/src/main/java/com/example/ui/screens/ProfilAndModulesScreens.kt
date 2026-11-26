package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DemoSeedData
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.viewmodel.HydroGrowViewModel

@Composable
fun ProfilScreen(
    viewModel: HydroGrowViewModel,
    userProfile: UserProfile?,
    userRole: UserRole,
    isDemoMode: Boolean,
    unreadNotificationCount: Int,
    selectedLocation: CityLocation,
    onOpenEducation: () -> Unit,
    onOpenWholesale: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSwitchToRealAuth: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditProfile by remember { mutableStateOf(false) }
    val displayName = userProfile?.fullName?.ifBlank { "Pengguna HydroGrow" } ?: "Pengguna HydroGrow"
    val email = userProfile?.email.orEmpty()
    val canAccessAdmin = userRole == UserRole.ADMIN || email.equals("mnurkhasani43@gmail.com", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
    ) {
        item {
            Text("Profil & Ekosistem", style = MaterialTheme.typography.headlineSmall)
        }

        // User Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photoUrl = userProfile?.photoUrl.orEmpty()
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Foto profil $displayName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_avatar_farmer),
                                contentDescription = "Avatar pengguna",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (email.isNotBlank()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(
                                    text = userRole.label,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (userProfile?.verifiedSeller == true || userProfile?.verifiedSupplier == true) {
                                    StatusBadge(
                                        text = "Terverifikasi",
                                        containerColor = AmberContainer,
                                        contentColor = OnAmberContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Lokasi: ${userProfile?.city ?: selectedLocation.name} • ${userProfile?.phone?.ifBlank { "Belum diisi" } ?: "Belum diisi"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!userProfile?.address.isNullOrBlank()) {
                        Text(
                            text = "Alamat: ${userProfile?.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showEditProfile = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_edit_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pengaturan & Edit Profil")
                    }
                }
            }
        }

        // Demo Mode Role Switcher
        if (isDemoMode) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Science, contentDescription = null, tint = OnAmberContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Mode Demo — Uji Peran Pengguna",
                                style = MaterialTheme.typography.titleSmall,
                                color = OnAmberContainer
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(UserRole.entries) { role ->
                                FilterChip(
                                    selected = role == userRole,
                                    onClick = { viewModel.switchDemoRole(role) },
                                    label = { Text(role.label.substringBefore(" /"), style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("demo_switch_${role.name.lowercase()}")
                                )
                            }
                        }
                        Button(
                            onClick = onSwitchToRealAuth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_to_google_auth_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreenDeep)
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Masuk dengan Google (Simpan ke Cloud)")
                        }
                    }
                }
            }
        }

        // Core Modules Section
        item {
            Text("Layanan & Modul Terintegrasi", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileModuleRow(
                    title = "Asisten AI Hidroponik",
                    subtitle = "Analisis pH/EC, hama, estimasi modal & jadwal tanam",
                    icon = Icons.Filled.AutoAwesome,
                    accentColor = LeafGreenPrimary,
                    tag = "profile_ai_button",
                    onClick = onOpenAiAssistant
                )
                ProfileModuleRow(
                    title = "Edukasi, Forum & Konsultasi Ahli",
                    subtitle = "7+ panduan lengkap, video, diskusi komunitas & booking ahli",
                    icon = Icons.Filled.MenuBook,
                    accentColor = MintSecondary,
                    tag = "profile_education_button",
                    onClick = onOpenEducation
                )
                ProfileModuleRow(
                    title = "Cari Pembeli Grosir & Kemitraan B2B",
                    subtitle = "Hubungkan panen ke restoran, hotel, & katering tanpa tengkulak",
                    icon = Icons.Filled.Handshake,
                    accentColor = OrangeHighlight,
                    tag = "profile_wholesale_button",
                    onClick = onOpenWholesale
                )
                ProfileModuleRow(
                    title = "Pusat Notifikasi (${unreadNotificationCount} belum dibaca)",
                    subtitle = "Jadwal perawatan, perkiraan panen, pesanan & promo",
                    icon = Icons.Filled.Notifications,
                    accentColor = LeafGreenDeep,
                    tag = "profile_notifications_button",
                    onClick = onOpenNotifications
                )
                if (canAccessAdmin) {
                    ProfileModuleRow(
                        title = "Dashboard Admin & Monetisasi",
                        subtitle = "Atur komisi platform, pantau GMV terpisah & verifikasi penjual",
                        icon = Icons.Filled.AdminPanelSettings,
                        accentColor = Color(0xFF7C3AED),
                        tag = "profile_admin_button",
                        onClick = onOpenAdmin
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isDemoMode) "Keluar dari Mode Demo" else "Keluar Akun Google")
            }
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            currentProfile = userProfile,
            currentRole = userRole,
            currentLocation = selectedLocation,
            isDemoMode = isDemoMode,
            onSave = { name, phone, role, address, city, photoUrl ->
                viewModel.saveProfile(name, phone, role, address, city, photoUrl)
                showEditProfile = false
            },
            onDismiss = { showEditProfile = false }
        )
    }
}

@Composable
private fun ProfileModuleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentProfile: UserProfile?,
    currentRole: UserRole,
    currentLocation: CityLocation,
    isDemoMode: Boolean,
    onSave: (String, String, UserRole, String, CityLocation, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentProfile?.fullName.orEmpty()) }
    var phone by remember { mutableStateOf(currentProfile?.phone.orEmpty()) }
    var address by remember { mutableStateOf(currentProfile?.address.orEmpty()) }
    var role by remember { mutableStateOf(currentRole) }
    var city by remember {
        mutableStateOf(
            DemoSeedData.indonesianCities.find { it.name == currentProfile?.city } ?: currentLocation
        )
    }
    var photoUri by remember { mutableStateOf<Uri?>(currentProfile?.photoUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri }
    )
    val availableRoles = if (isDemoMode) UserRole.entries else listOf(
        UserRole.PETANI, UserRole.PEMBELI, UserRole.SUPPLIER, UserRole.MITRA_PENGIRIMAN
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profil Pengguna") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoUri != null) "Ganti Foto Profil" else "Unggah Foto")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap / Nama Usaha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Peran Akun:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableRoles) { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.label.substringBefore(" /"), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Kota / Wilayah:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DemoSeedData.indonesianCities) { c ->
                        FilterChip(
                            selected = city.name == c.name,
                            onClick = { city = c },
                            label = { Text(c.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, phone, role, address, city, photoUri?.toString().orEmpty()) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private enum class EduSubTab(val label: String) {
    ARTIKEL("Artikel & Video"),
    FORUM("Forum Diskusi"),
    KONSULTASI("Konsultasi Ahli")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdukasiScreen(
    viewModel: HydroGrowViewModel,
    forumPosts: List<ForumPost>,
    bookedConsultations: List<String>,
    consultationCommissionPct: Double,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var activeSubTab by remember { mutableStateOf(EduSubTab.ARTIKEL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("Semua") }
    var expandedArticleId by remember { mutableStateOf<String?>(null) }
    var showNewPostDialog by remember { mutableStateOf(false) }

    val categories = remember { listOf("Semua") + DemoSeedData.educationArticles.map { it.category }.distinct() }
    val filteredArticles = remember(searchQuery, selectedTopic) {
        DemoSeedData.educationArticles.filter { art ->
            (selectedTopic == "Semua" || art.category == selectedTopic) &&
                (searchQuery.isBlank() || art.title.contains(searchQuery, ignoreCase = true) ||
                    art.summary.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edukasi, Forum & Konsultasi") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("edu_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeSubTab == EduSubTab.FORUM) {
                ExtendedFloatingActionButton(
                    onClick = { showNewPostDialog = true },
                    icon = { Icon(Icons.Filled.AddComment, contentDescription = null) },
                    text = { Text("Buat Diskusi") },
                    modifier = Modifier.testTag("edu_new_post_button")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EduSubTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = activeSubTab == tab,
                            onClick = { activeSubTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(idx, EduSubTab.entries.size)
                        ) {
                            Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            when (activeSubTab) {
                EduSubTab.ARTIKEL -> {
                    item {
                        Image(
                            painter = painterResource(id = R.drawable.img_edu_nft_system),
                            contentDescription = "Ilustrasi Sistem Hidroponik NFT",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari topik NFT, Wick, AB Mix, hama, panen...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedTopic == cat,
                                    onClick = { selectedTopic = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    items(filteredArticles, key = { it.id }) { art ->
                        val expanded = expandedArticleId == art.id
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedArticleId = if (expanded) null else art.id }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(
                                        text = art.category,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text("${art.readMinutes} mnt baca", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(art.title, style = MaterialTheme.typography.titleMedium)
                                Text(art.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = OrangeHighlight)
                                        Spacer(Modifier.width(8.dp))
                                        Text(art.videoTitle, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        Text(art.videoDuration, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (expanded) {
                                    HorizontalDivider()
                                    Text(art.content, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = if (expanded) "Tutup Materi" else "Baca Selengkapnya",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                EduSubTab.FORUM -> {
                    if (forumPosts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Belum Ada Diskusi",
                                subtitle = "Mulai pertanyaan atau bagikan pengalaman kebun hidroponik Anda.",
                                actionLabel = "Buat Diskusi",
                                onAction = { showNewPostDialog = true }
                            )
                        }
                    } else {
                        items(forumPosts, key = { it.id }) { post ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusBadge(
                                            text = post.topic,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text("${post.replyCount} balasan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(post.title, style = MaterialTheme.typography.titleSmall)
                                    Text(post.content, style = MaterialTheme.typography.bodySmall)
                                    Text("Oleh: ${post.authorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                EduSubTab.KONSULTASI -> {
                    item {
                        Surface(color = AmberContainer, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = "Konsultasi ahli dikenakan biaya layanan platform ${consultationCommissionPct.toInt()}% (ilustrasi mode demo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnAmberContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    items(DemoSeedData.expertConsultants, key = { it.id }) { expert ->
                        val isBooked = bookedConsultations.contains(expert.id)
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(expert.name, style = MaterialTheme.typography.titleMedium)
                                        Text(expert.specialty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    StatusBadge(
                                        text = "★ ${expert.rating}",
                                        containerColor = AmberContainer,
                                        contentColor = OnAmberContainer
                                    )
                                }
                                Text(
                                    "Pengalaman ${expert.experienceYears} tahun • Area ${expert.area}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${Formatters.rupiah(expert.feePerSession)} / sesi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = { viewModel.bookConsultation(expert) },
                                        enabled = !isBooked
                                    ) {
                                        Text(if (isBooked) "Terjadwal" else "Jadwalkan Konsultasi")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewPostDialog) {
        var title by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("Nutrisi & pH") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPostDialog = false },
            title = { Text("Buat Topik Forum Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Diskusi") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topik (mis. Nutrisi, Hama)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Pertanyaan / Pengalaman") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createForumPost(title, content, topic)
                    showNewPostDialog = false
                }) { Text("Posting") }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WholesaleScreen(
    viewModel: HydroGrowViewModel,
    requests: List<WholesaleRequest>,
    selectedLocation: CityLocation,
    wholesaleCommissionPct: Double,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showCreateDialog by remember { mutableStateOf(false) }
    var offerTarget by remember { mutableStateOf<WholesaleRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kemitraan & Pembeli Grosir") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wholesale_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.AddBusiness, contentDescription = null) },
                text = { Text("Buat Permintaan") },
                modifier = Modifier.testTag("wholesale_create_button")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cari Pembeli Grosir (B2B)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Hubungkan hasil panen kebun langsung dengan restoran, hotel, katering, dan supermarket lokal tanpa tengkulak. Komisi kemitraan: ${wholesaleCommissionPct.toInt()}%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (requests.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Belum Ada Permintaan Grosir",
                        subtitle = "Buat permintaan pengadaan sayuran hidroponik untuk mitra restoran & katering."
                    )
                }
            } else {
                items(requests, key = { it.id }) { req ->
                    val statusEnum = WholesaleStatus.entries.find { it.name == req.status } ?: WholesaleStatus.TERBUKA
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.buyerCompany, style = MaterialTheme.typography.titleMedium)
                                    Text("${req.buyerType} • ${req.area}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    text = statusEnum.label,
                                    containerColor = if (statusEnum == WholesaleStatus.TERBUKA) MaterialTheme.colorScheme.primaryContainer else AmberContainer,
                                    contentColor = if (statusEnum == WholesaleStatus.TERBUKA) MaterialTheme.colorScheme.onPrimaryContainer else OnAmberContainer
                                )
                            }
                            Text(
                                "Komoditas: ${req.cropNeeded} (${req.quantityKg} kg)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target Harga: ${Formatters.rupiah(req.targetPricePerKg)}/kg", style = MaterialTheme.typography.bodySmall)
                                Text("Jadwal: ${req.deliverySchedule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (req.farmerOfferPrice > 0L) {
                                Text(
                                    "Penawaran diajukan: ${Formatters.rupiah(req.farmerOfferPrice)}/kg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OrangeHighlight
                                )
                            }
                            Button(
                                onClick = { offerTarget = req },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ajukan Penawaran Harga")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var company by remember { mutableStateOf("") }
        var buyerType by remember { mutableStateOf("Restoran") }
        var crop by remember { mutableStateOf("Selada Hijau") }
        var qty by remember { mutableStateOf("25") }
        var targetPrice by remember { mutableStateOf("20000") }
        var schedule by remember { mutableStateOf("Setiap Senin & Kamis") }
        var area by remember { mutableStateOf(selectedLocation.name) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Permintaan Pengadaan Grosir") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Nama Restoran / Hotel / Usaha") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Restoran", "Hotel", "Katering").forEach { t ->
                            FilterChip(selected = buyerType == t, onClick = { buyerType = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    OutlinedTextField(value = crop, onValueChange = { crop = it }, label = { Text("Sayuran Dibutuhkan") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Volume (kg)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = targetPrice, onValueChange = { targetPrice = it }, label = { Text("Target Rp/kg") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Jadwal Pengiriman") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area Pengiriman") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createWholesaleRequest(
                        company = company,
                        buyerType = buyerType,
                        cropNeeded = crop,
                        quantityKg = qty.toLongOrNull() ?: 10L,
                        targetPrice = targetPrice.toLongOrNull() ?: 18000L,
                        schedule = schedule,
                        area = area
                    )
                    showCreateDialog = false
                }) { Text("Publikasikan") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Batal") }
            }
        )
    }

    offerTarget?.let { target ->
        var offerText by remember { mutableStateOf(target.targetPricePerKg.toString()) }
        AlertDialog(
            onDismissRequest = { offerTarget = null },
            title = { Text("Ajukan Penawaran ke ${target.buyerCompany}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kebutuhan: ${target.cropNeeded} (${target.quantityKg} kg)", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = offerText,
                        onValueChange = { offerText = it },
                        label = { Text("Harga Penawaran Anda (Rp/kg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = offerText.toLongOrNull() ?: target.targetPricePerKg
                    viewModel.submitWholesaleOffer(target.id, price)
                    offerTarget = null
                }) { Text("Kirim Penawaran") }
            },
            dismissButton = {
                TextButton(onClick = { offerTarget = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: HydroGrowViewModel,
    notifications: List<AppNotification>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Notifikasi") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyStateCard(
                    title = "Belum Ada Notifikasi",
                    subtitle = "Pengingat perawatan kebun, panen, dan pesanan akan tampil di sini."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.read) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.markNotificationRead(notif.id) }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (notif.read) Icons.Filled.NotificationsNone else Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!notif.read) {
                                StatusBadge("BARU", AmberContainer, OnAmberContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}
