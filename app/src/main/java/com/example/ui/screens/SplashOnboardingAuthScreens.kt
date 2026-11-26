package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.data.DemoSeedData
import com.example.model.CityLocation
import com.example.model.UserRole
import com.example.ui.components.HydroGrowLogoMark
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.LeafGreenDeep
import com.example.ui.theme.LeafGreenPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.OnAmberContainer
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LeafGreenDeep, LeafGreenPrimary)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            HydroGrowLogoMark(size = 88.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "HydroGrow",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kelola Kebun, Kembangkan Usaha, Temukan Pasar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MintLight,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MintLight,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val description: String,
    val badge: String,
    val imageRes: Int
)

@Composable
fun OnboardingScreen(
    onContinueToAuth: () -> Unit,
    onEnterDemoMode: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Kelola Kebun Hidroponik Cerdas",
                description = "Pantau tanaman, jadwal perawatan, pH, EC/TDS, grafik pertumbuhan, dan prediksi panen dengan panduan AI.",
                badge = "SMART FARMING",
                imageRes = R.drawable.img_hero_hydroponic
            ),
            OnboardingPage(
                title = "Marketplace Lokal & Sekitar Saya",
                description = "Jual hasil panen segar langsung ke konsumen dan restoran terdekat tanpa rantai distribusi panjang.",
                badge = "PASAR LOKAL",
                imageRes = R.drawable.img_onboarding_market
            ),
            OnboardingPage(
                title = "Keuangan Usaha & Kemitraan B2B",
                description = "Catat modal, pengeluaran, pendapatan otomatis, ekspor laporan PDF, dan temukan pembeli grosir.",
                badge = "FINTECH AGRI",
                imageRes = R.drawable.img_edu_nft_system
            )
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val current = pages[pageIndex]

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HydroGrowLogoMark(size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("HydroGrow", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(
                    onClick = onContinueToAuth,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Lewati")
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = current.imageRes),
                    contentDescription = current.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(24.dp))
                StatusBadge(
                    text = current.badge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (idx == pageIndex) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == pageIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (pageIndex < pages.lastIndex) pageIndex++ else onContinueToAuth()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(if (pageIndex < pages.lastIndex) "Lanjut" else "Masuk / Daftar Akun")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
                OutlinedButton(
                    onClick = onEnterDemoMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_demo_button")
                ) {
                    Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Coba Mode Demo Sekarang")
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    onGoogleAuthSuccess: (selectedRole: UserRole, name: String, phone: String, address: String, city: CityLocation, photoUri: String) -> Unit,
    onEnterDemoMode: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(UserRole.PETANI) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf(DemoSeedData.indonesianCities.first()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showAccountHelp by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> photoUri = uri }
    )

    val selectableRoles = listOf(UserRole.PETANI, UserRole.PEMBELI, UserRole.SUPPLIER, UserRole.MITRA_PENGIRIMAN)

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HydroGrowLogoMark(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Masuk atau Daftar HydroGrow", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Pilih peran usaha Anda & masuk aman dengan Google",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Demo Mode Quick Card
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = OnAmberContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mode Demo Interaktif",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnAmberContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Uji seluruh fitur kebun, marketplace, peta lokal, keuangan, admin, dan asisten AI tanpa login atau uang nyata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnAmberContainer
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onEnterDemoMode(selectedRole) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("demo_mode_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreenDeep)
                    ) {
                        Text("Masuk Mode Demo (${selectedRole.label})")
                    }
                }
            }

            Text("1. Pilih Jenis Akun", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectableRoles.forEach { role ->
                    val isSelected = role == selectedRole
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedRole = role }
                            .testTag("role_option_${role.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedRole = role })
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(role.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    role.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Text("2. Data Profil Awal (Opsional sebelum Google Sign-In)", style = MaterialTheme.typography.titleMedium)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Foto profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pilih Foto Profil")
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap / Nama Kebun") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kota / Area Lokasi:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoSeedData.indonesianCities.take(4).forEach { city ->
                            FilterChip(
                                selected = selectedCity.name == city.name,
                                onClick = { selectedCity = city },
                                label = { Text(city.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            if (authError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            GoogleSignInButton(
                onAuthSuccess = {
                    authError = null
                    onGoogleAuthSuccess(
                        selectedRole,
                        fullName.trim(),
                        phone.trim(),
                        address.trim(),
                        selectedCity,
                        photoUri?.toString().orEmpty()
                    )
                },
                onAuthError = { msg -> authError = msg },
                label = "Masuk & Simpan dengan Google"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { showAccountHelp = true }) {
                    Text("Lupa Password / Bantuan Verifikasi Akun?")
                }
            }
        }
    }

    if (showAccountHelp) {
        AlertDialog(
            onDismissRequest = { showAccountHelp = false },
            title = { Text("Keamanan & Pemulihan Akun") },
            text = {
                Text(
                    "HydroGrow menggunakan Google Sign-In untuk keamanan tanpa menyimpan kata sandi di perangkat. " +
                        "Jika lupa kata sandi akun Google Anda, silakan pulihkan melalui halaman Pemulihan Akun Google. " +
                        "Verifikasi akun Penjual & Supplier diproses oleh Admin setelah profil lengkap disimpan."
                )
            },
            confirmButton = {
                Button(onClick = { showAccountHelp = false }) { Text("Mengerti") }
            }
        )
    }
}
