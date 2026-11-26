package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.DemoHydroGrowRepository
import com.example.data.FirestoreHydroGrowRepository
import com.example.model.*
import com.example.ui.components.HydroGrowLogoMark
import com.example.ui.screens.*
import com.example.ui.theme.HydroGrowTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.HydroGrowViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppLaunchStage {
    SPLASH,
    ONBOARDING,
    AUTH,
    DEMO,
    AUTHENTICATED
}

private enum class AppSubRoute {
    NONE,
    NOTIFICATIONS,
    AI_ASSISTANT,
    WHOLESALE,
    EDUCATION,
    ADMIN
}

private data class PendingRegistration(
    val role: UserRole,
    val name: String,
    val phone: String,
    val address: String,
    val city: CityLocation,
    val photoUri: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            HydroGrowTheme {
                HydroGrowRootApp()
            }
        }
    }
}

@Composable
private fun HydroGrowRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var stage by remember { mutableStateOf(AppLaunchStage.SPLASH) }
    var splashDone by remember { mutableStateOf(false) }
    var initialAutoSignInChecked by remember { mutableStateOf(false) }
    var initialDemoRole by remember { mutableStateOf(UserRole.PETANI) }
    var demoSessionNonce by remember { mutableIntStateOf(0) }
    var pendingRegistration by remember { mutableStateOf<PendingRegistration?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                firebaseUser = Firebase.auth.currentUser
                initialAutoSignInChecked = true
            },
            onUnauthenticated = {
                initialAutoSignInChecked = true
            },
            scope = scope
        )
    }

    LaunchedEffect(splashDone, initialAutoSignInChecked, firebaseUser) {
        if (splashDone && initialAutoSignInChecked) {
            if (firebaseUser != null && stage != AppLaunchStage.DEMO) {
                stage = AppLaunchStage.AUTHENTICATED
            } else if (stage == AppLaunchStage.SPLASH) {
                stage = AppLaunchStage.ONBOARDING
            }
        }
    }

    when (stage) {
        AppLaunchStage.SPLASH -> {
            SplashScreen(onFinished = { splashDone = true })
        }

        AppLaunchStage.ONBOARDING -> {
            OnboardingScreen(
                onContinueToAuth = { stage = AppLaunchStage.AUTH },
                onEnterDemoMode = {
                    initialDemoRole = UserRole.PETANI
                    demoSessionNonce++
                    stage = AppLaunchStage.DEMO
                }
            )
        }

        AppLaunchStage.AUTH -> {
            AuthScreen(
                onGoogleAuthSuccess = { role, name, phone, address, city, photoUri ->
                    pendingRegistration = PendingRegistration(role, name, phone, address, city, photoUri)
                    firebaseUser = Firebase.auth.currentUser
                    stage = AppLaunchStage.AUTHENTICATED
                },
                onEnterDemoMode = { role ->
                    initialDemoRole = role
                    demoSessionNonce++
                    stage = AppLaunchStage.DEMO
                }
            )
        }

        AppLaunchStage.DEMO -> {
            val demoKey = "demo_$demoSessionNonce"
            key(demoKey) {
                val vm: HydroGrowViewModel = viewModel(
                    key = demoKey,
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repo = DemoHydroGrowRepository().apply {
                                switchDemoRole(initialDemoRole)
                            }
                            return HydroGrowViewModel(
                                repository = repo,
                                currentUserId = "demo_user_${initialDemoRole.name.lowercase()}",
                                defaultDisplayName = "Pengguna Demo",
                                defaultEmail = "demo@hydrogrow.id"
                            ) as T
                        }
                    }
                )
                HydroGrowMainScaffold(
                    viewModel = vm,
                    onSwitchToRealAuth = { stage = AppLaunchStage.AUTH },
                    onSignOut = { stage = AppLaunchStage.AUTH }
                )
            }
        }

        AppLaunchStage.AUTHENTICATED -> {
            val activeUser = firebaseUser
            if (activeUser == null) {
                LaunchedEffect(Unit) {
                    stage = AppLaunchStage.AUTH
                }
                SplashScreen(onFinished = {})
            } else {
                key(activeUser.uid) {
                    val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
                    val vm: HydroGrowViewModel = viewModel(
                        key = activeUser.uid,
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                val db = FirebaseFirestore.getInstance(databaseId)
                                return HydroGrowViewModel(
                                    repository = FirestoreHydroGrowRepository(db),
                                    currentUserId = activeUser.uid,
                                    defaultDisplayName = activeUser.displayName ?: "Pengguna HydroGrow",
                                    defaultEmail = activeUser.email.orEmpty()
                                ) as T
                            }
                        }
                    )

                    val profileState by vm.userProfileState.collectAsStateWithLifecycle()
                    LaunchedEffect(profileState, pendingRegistration) {
                        val success = profileState as? UiState.Success ?: return@LaunchedEffect
                        val existing = success.data
                        val pending = pendingRegistration
                        if (pending != null) {
                            val resolvedName = pending.name.ifBlank {
                                existing?.fullName?.ifBlank { activeUser.displayName ?: "Pengguna HydroGrow" }
                                    ?: (activeUser.displayName ?: "Pengguna HydroGrow")
                            }
                            val resolvedPhoto = pending.photoUri.ifBlank {
                                existing?.photoUrl?.ifBlank { activeUser.photoUrl?.toString().orEmpty() }
                                    ?: activeUser.photoUrl?.toString().orEmpty()
                            }
                            vm.saveProfile(
                                fullName = resolvedName,
                                phone = pending.phone.ifBlank { existing?.phone.orEmpty() },
                                role = pending.role,
                                address = pending.address.ifBlank { existing?.address.orEmpty() },
                                city = pending.city,
                                photoUrl = resolvedPhoto
                            )
                            pendingRegistration = null
                        } else if (existing == null) {
                            val isBootstrappedAdmin = activeUser.email.equals("mnurkhasani43@gmail.com", ignoreCase = true)
                            vm.saveProfile(
                                fullName = activeUser.displayName ?: "Pengguna HydroGrow",
                                phone = "",
                                role = if (isBootstrappedAdmin) UserRole.ADMIN else UserRole.PETANI,
                                address = "",
                                city = vm.selectedLocation.value,
                                photoUrl = activeUser.photoUrl?.toString().orEmpty()
                            )
                        }
                    }

                    HydroGrowMainScaffold(
                        viewModel = vm,
                        onSwitchToRealAuth = {},
                        onSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = {
                                    firebaseUser = null
                                    stage = AppLaunchStage.AUTH
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HydroGrowMainScaffold(
    viewModel: HydroGrowViewModel,
    onSwitchToRealAuth: () -> Unit,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val plantsState by viewModel.plantsState.collectAsStateWithLifecycle()
    val schedulesState by viewModel.schedulesState.collectAsStateWithLifecycle()
    val monitoringLogsState by viewModel.monitoringLogsState.collectAsStateWithLifecycle()
    val harvestsState by viewModel.harvestsState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val revenuesState by viewModel.revenuesState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val sellerProductsState by viewModel.sellerProductsState.collectAsStateWithLifecycle()
    val buyerOrdersState by viewModel.buyerOrdersState.collectAsStateWithLifecycle()
    val sellerOrdersState by viewModel.sellerOrdersState.collectAsStateWithLifecycle()
    val wholesaleState by viewModel.wholesaleState.collectAsStateWithLifecycle()
    val forumPostsState by viewModel.forumPostsState.collectAsStateWithLifecycle()
    val adminSetting by viewModel.adminSettingsState.collectAsStateWithLifecycle()

    val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()
    val searchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val sellerChats by viewModel.sellerChats.collectAsStateWithLifecycle()
    val consultationsBooked by viewModel.consultationsBooked.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiStreamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val userRole = remember(userProfile) {
        UserRole.entries.find { it.name == userProfile?.role } ?: UserRole.PETANI
    }
    val userName = userProfile?.fullName?.ifBlank { "Pengguna HydroGrow" } ?: "Pengguna HydroGrow"
    val plants = (plantsState as? UiState.Success)?.data.orEmpty()
    val schedules = (schedulesState as? UiState.Success)?.data.orEmpty()
    val monitoringLogs = (monitoringLogsState as? UiState.Success)?.data.orEmpty()
    val harvests = (harvestsState as? UiState.Success)?.data.orEmpty()
    val expenses = (expensesState as? UiState.Success)?.data.orEmpty()
    val revenues = (revenuesState as? UiState.Success)?.data.orEmpty()
    val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val products = (productsState as? UiState.Success)?.data.orEmpty()
    val sellerProducts = (sellerProductsState as? UiState.Success)?.data.orEmpty()
    val buyerOrders = (buyerOrdersState as? UiState.Success)?.data.orEmpty()
    val sellerOrders = (sellerOrdersState as? UiState.Success)?.data.orEmpty()
    val wholesaleRequests = (wholesaleState as? UiState.Success)?.data.orEmpty()
    val forumPosts = (forumPostsState as? UiState.Success)?.data.orEmpty()

    var currentTab by remember { mutableStateOf(AppTab.BERANDA) }
    var subRoute by remember { mutableStateOf(AppSubRoute.NONE) }
    var initialAiPrompt by remember { mutableStateOf<String?>(null) }
    var showDemoRoleSwitcher by remember { mutableStateOf(false) }

    val navItems = remember {
        listOf(
            AppTab.BERANDA to Icons.Filled.Home,
            AppTab.KEBUN to Icons.Filled.Eco,
            AppTab.MARKETPLACE to Icons.Filled.Storefront,
            AppTab.KEUANGAN to Icons.Filled.AccountBalanceWallet,
            AppTab.PROFIL to Icons.Filled.Person
        )
    }

    when (subRoute) {
        AppSubRoute.NOTIFICATIONS -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
                NotificationsScreen(
                    viewModel = viewModel,
                    notifications = notifications,
                    onBack = { subRoute = AppSubRoute.NONE }
                )
            }
            return
        }

        AppSubRoute.AI_ASSISTANT -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
                AiAssistantScreen(
                    viewModel = viewModel,
                    plants = plants,
                    monitoringLogs = monitoringLogs,
                    messages = aiMessages,
                    streamingText = aiStreamingText,
                    aiError = aiError,
                    initialContextText = initialAiPrompt,
                    onBack = {
                        initialAiPrompt = null
                        subRoute = AppSubRoute.NONE
                    }
                )
            }
            return
        }

        AppSubRoute.WHOLESALE -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
                WholesaleScreen(
                    viewModel = viewModel,
                    requests = wholesaleRequests,
                    selectedLocation = selectedLocation,
                    wholesaleCommissionPct = adminSetting.wholesaleCommissionPct,
                    onBack = { subRoute = AppSubRoute.NONE }
                )
            }
            return
        }

        AppSubRoute.EDUCATION -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
                EdukasiScreen(
                    viewModel = viewModel,
                    forumPosts = forumPosts,
                    bookedConsultations = consultationsBooked,
                    consultationCommissionPct = adminSetting.consultationCommissionPct,
                    onBack = { subRoute = AppSubRoute.NONE }
                )
            }
            return
        }

        AppSubRoute.ADMIN -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
                AdminDashboardScreen(
                    viewModel = viewModel,
                    isDemoMode = viewModel.isDemoMode,
                    adminSetting = adminSetting,
                    orders = sellerOrders.ifEmpty { buyerOrders },
                    products = products,
                    onBack = { subRoute = AppSubRoute.NONE }
                )
            }
            return
        }

        AppSubRoute.NONE -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        navItems.forEach { (tab, icon) ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                HydroGrowLogoMark(size = 40.dp)
                            }
                        }
                    ) {
                        navItems.forEach { (tab, icon) ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        when (currentTab) {
                            AppTab.BERANDA -> DashboardScreen(
                                viewModel = viewModel,
                                userRole = userRole,
                                userName = userName,
                                isDemoMode = viewModel.isDemoMode,
                                plants = plants,
                                schedules = schedules,
                                monitoringLogs = monitoringLogs,
                                products = products,
                                buyerOrders = buyerOrders,
                                sellerOrders = sellerOrders,
                                expenses = expenses,
                                revenues = revenues,
                                notifications = notifications,
                                selectedLocation = selectedLocation,
                                searchQuery = searchQuery,
                                onSearchQueryChange = viewModel::updateGlobalSearch,
                                onNavigateTab = { currentTab = it },
                                onOpenNotifications = { subRoute = AppSubRoute.NOTIFICATIONS },
                                onOpenAiAssistant = { subRoute = AppSubRoute.AI_ASSISTANT },
                                onOpenWholesale = { subRoute = AppSubRoute.WHOLESALE },
                                onOpenEducation = { subRoute = AppSubRoute.EDUCATION },
                                onOpenAdmin = { subRoute = AppSubRoute.ADMIN },
                                onSwitchRoleClick = { showDemoRoleSwitcher = true }
                            )

                            AppTab.KEBUN -> KebunScreen(
                                viewModel = viewModel,
                                plants = plants,
                                schedules = schedules,
                                monitoringLogs = monitoringLogs,
                                harvests = harvests,
                                onAskAiForPlant = { plant, latestLog ->
                                    initialAiPrompt = if (latestLog != null) {
                                        "Tolong analisis kondisi ${plant.name} (${plant.cropType}): pH ${latestLog.ph}, EC ${latestLog.ec}, suhu air ${latestLog.waterTemp}°C, tinggi ${latestLog.heightCm} cm."
                                    } else {
                                        "Berikan panduan perawatan harian untuk ${plant.name} (${plant.cropType}) sistem ${plant.systemType}."
                                    }
                                    subRoute = AppSubRoute.AI_ASSISTANT
                                }
                            )

                            AppTab.MARKETPLACE -> MarketplaceScreen(
                                viewModel = viewModel,
                                userRole = userRole,
                                isDemoMode = viewModel.isDemoMode,
                                selectedLocation = selectedLocation,
                                products = products,
                                sellerProducts = sellerProducts,
                                cartItems = cartItems,
                                buyerOrders = buyerOrders,
                                sellerOrders = sellerOrders,
                                reviews = reviews,
                                sellerChats = sellerChats,
                                commissionPct = adminSetting.marketplaceCommissionPct,
                                defaultAddress = userProfile?.address?.ifBlank { selectedLocation.areaLabel }
                                    ?: selectedLocation.areaLabel
                            )

                            AppTab.KEUANGAN -> KeuanganScreen(
                                viewModel = viewModel,
                                expenses = expenses,
                                revenues = revenues
                            )

                            AppTab.PROFIL -> ProfilScreen(
                                viewModel = viewModel,
                                userProfile = userProfile,
                                userRole = userRole,
                                isDemoMode = viewModel.isDemoMode,
                                unreadNotificationCount = notifications.count { !it.read },
                                selectedLocation = selectedLocation,
                                onOpenEducation = { subRoute = AppSubRoute.EDUCATION },
                                onOpenWholesale = { subRoute = AppSubRoute.WHOLESALE },
                                onOpenAiAssistant = { subRoute = AppSubRoute.AI_ASSISTANT },
                                onOpenNotifications = { subRoute = AppSubRoute.NOTIFICATIONS },
                                onOpenAdmin = { subRoute = AppSubRoute.ADMIN },
                                onSwitchToRealAuth = onSwitchToRealAuth,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDemoRoleSwitcher && viewModel.isDemoMode) {
        AlertDialog(
            onDismissRequest = { showDemoRoleSwitcher = false },
            title = { Text("Pilih Peran Mode Demo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        Surface(
                            onClick = {
                                viewModel.switchDemoRole(role)
                                showDemoRoleSwitcher = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (role == userRole) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(role.label, style = MaterialTheme.typography.titleSmall)
                                Text(role.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDemoRoleSwitcher = false }) { Text("Tutup") }
            }
        )
    }
}
