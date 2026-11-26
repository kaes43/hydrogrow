package com.example.data

import com.example.model.*

object DemoSeedData {
    private val now = System.currentTimeMillis()
    private const val DAY_MS = 86_400_000L

    val indonesianCities = listOf(
        CityLocation("Bandung", "Dago & Lembang, Bandung", -6.9175, 107.6191),
        CityLocation("Lembang", "Lembang, Kab. Bandung Barat", -6.8117, 107.6175),
        CityLocation("Cimahi", "Cimahi Utara", -6.8722, 107.5425),
        CityLocation("Jakarta Selatan", "Kemang, Jakarta Selatan", -6.2615, 106.8106),
        CityLocation("Bogor", "Bogor Tengah", -6.5971, 106.8060),
        CityLocation("Surabaya", "Gubeng, Surabaya", -7.2658, 112.7521),
        CityLocation("Yogyakarta", "Sleman, Yogyakarta", -7.7956, 110.3695),
        CityLocation("Malang", "Lowokwaru, Malang", -7.9553, 112.6130)
    )

    fun demoProfile(role: UserRole = UserRole.PETANI): UserProfile {
        val (name, city, lat, lng) = when (role) {
            UserRole.PETANI -> listOf("Budi Santoso (DEMO)", "Bandung", -6.9175, 107.6191)
            UserRole.PEMBELI -> listOf("Sari Wulandari (DEMO)", "Bandung", -6.9025, 107.6188)
            UserRole.SUPPLIER -> listOf("Toko Hidro Nusantara (DEMO)", "Lembang", -6.8117, 107.6175)
            UserRole.MITRA_PENGIRIMAN -> listOf("Rizky Kurir Segar (DEMO)", "Bandung", -6.9147, 107.6098)
            UserRole.ADMIN -> listOf("Admin HydroGrow (DEMO)", "Bandung", -6.9175, 107.6191)
        }
        return UserProfile(
            id = "demo_user_${role.name.lowercase()}",
            userId = "demo_user_${role.name.lowercase()}",
            fullName = name as String,
            email = "demo.${role.name.lowercase()}@hydrogrow.id",
            phone = "0812-3456-7890",
            role = role.name,
            address = "Jl. Kebun Hidroponik No. 12, ${city as String}",
            city = city,
            latitude = lat as Double,
            longitude = lng as Double,
            verifiedSeller = true,
            verifiedSupplier = true
        )
    }

    val initialPlants = listOf(
        HydroPlant(
            id = "demo_plant_1",
            userId = "demo_user_petani",
            name = "Selada Butterhead Rak A",
            cropType = "Selada Hijau",
            systemType = "NFT",
            holeCount = 120L,
            plantedDateMillis = now - 28 * DAY_MS,
            transplantDateMillis = now - 20 * DAY_MS,
            harvestAgeDays = 35L,
            status = PlantStatus.VEGETATIF.name,
            phMin = 5.5,
            phMax = 6.5,
            ecMin = 0.8,
            ecMax = 1.4,
            notes = "DEMO: Pertumbuhan daun seragam, pompa jalan 24 jam"
        ),
        HydroPlant(
            id = "demo_plant_2",
            userId = "demo_user_petani",
            name = "Pakcoy Nauli Rak B",
            cropType = "Pakcoy",
            systemType = "DFT",
            holeCount = 80L,
            plantedDateMillis = now - 30 * DAY_MS,
            transplantDateMillis = now - 22 * DAY_MS,
            harvestAgeDays = 32L,
            status = PlantStatus.SIAP_PANEN.name,
            phMin = 6.0,
            phMax = 7.0,
            ecMin = 1.5,
            ecMax = 2.0,
            notes = "DEMO: Siap panen untuk pesanan restoran akhir pekan"
        ),
        HydroPlant(
            id = "demo_plant_3",
            userId = "demo_user_petani",
            name = "Kangkung Bangkok Bak C",
            cropType = "Kangkung",
            systemType = "Rakit Apung",
            holeCount = 150L,
            plantedDateMillis = now - 14 * DAY_MS,
            transplantDateMillis = now - 9 * DAY_MS,
            harvestAgeDays = 24L,
            status = PlantStatus.VEGETATIF.name,
            phMin = 5.5,
            phMax = 6.5,
            ecMin = 1.2,
            ecMax = 2.0,
            notes = "DEMO: Semai padat di rockwool"
        ),
        HydroPlant(
            id = "demo_plant_4",
            userId = "demo_user_petani",
            name = "Tomat Ceri Dutch Bucket",
            cropType = "Tomat Ceri",
            systemType = "Dutch Bucket",
            holeCount = 24L,
            plantedDateMillis = now - 40 * DAY_MS,
            transplantDateMillis = now - 25 * DAY_MS,
            harvestAgeDays = 75L,
            status = PlantStatus.VEGETATIF.name,
            phMin = 5.5,
            phMax = 6.5,
            ecMin = 2.0,
            ecMax = 3.5,
            notes = "DEMO: Mulai muncul bunga tahap awal"
        )
    )

    val initialSchedules = listOf(
        MaintenanceSchedule(
            id = "demo_sched_1",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Butterhead Rak A",
            taskType = "Cek pH",
            title = "Cek pH & suhu tandon NFT Rak A",
            dueDateMillis = now,
            completed = false,
            notes = "DEMO: Target pH 5.5 - 6.5"
        ),
        MaintenanceSchedule(
            id = "demo_sched_2",
            userId = "demo_user_petani",
            plantId = "demo_plant_2",
            plantName = "Pakcoy Nauli Rak B",
            taskType = "Cek EC/TDS",
            title = "Ukur EC & tambah air baku Rak B",
            dueDateMillis = now,
            completed = false,
            notes = "DEMO: Jaga EC 1.5 - 2.0 mS/cm"
        ),
        MaintenanceSchedule(
            id = "demo_sched_3",
            userId = "demo_user_petani",
            plantId = "demo_plant_3",
            plantName = "Kangkung Bangkok Bak C",
            taskType = "Pemberian Nutrisi",
            title = "Tambah larutan AB Mix daun 150 ml",
            dueDateMillis = now + DAY_MS,
            completed = false,
            notes = "DEMO: Aduk merata setelah pengisian"
        ),
        MaintenanceSchedule(
            id = "demo_sched_4",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Butterhead Rak A",
            taskType = "Pemeriksaan Akar",
            title = "Periksa kebersihan akar & saringan pompa",
            dueDateMillis = now - DAY_MS,
            completed = true,
            notes = "DEMO: Akar putih sehat tidak berlendir"
        )
    )

    val initialMonitoringLogs = listOf(
        PlantMonitoringLog(
            id = "demo_log_1",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Butterhead Rak A",
            ph = 6.0,
            ec = 1.0,
            tds = 500L,
            waterTemp = 23.0,
            ambientTemp = 26.5,
            heightCm = 6.2,
            rootCondition = "Putih bersih",
            plantCondition = "Tumbuh stabil",
            healthStatus = HealthStatus.NORMAL.name,
            notes = "DEMO: Minggu ke-2",
            recordedAtMillis = now - 14 * DAY_MS
        ),
        PlantMonitoringLog(
            id = "demo_log_2",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Butterhead Rak A",
            ph = 6.2,
            ec = 1.2,
            tds = 600L,
            waterTemp = 24.0,
            ambientTemp = 27.5,
            heightCm = 11.4,
            rootCondition = "Putih lebat",
            plantCondition = "Daun melebar sehat",
            healthStatus = HealthStatus.NORMAL.name,
            notes = "DEMO: Minggu ke-3",
            recordedAtMillis = now - 7 * DAY_MS
        ),
        PlantMonitoringLog(
            id = "demo_log_3",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Butterhead Rak A",
            ph = 6.1,
            ec = 1.3,
            tds = 650L,
            waterTemp = 23.5,
            ambientTemp = 26.8,
            heightCm = 16.8,
            rootCondition = "Putih bersih",
            plantCondition = "Siap menuju panen",
            healthStatus = HealthStatus.NORMAL.name,
            notes = "DEMO: Minggu ke-4",
            recordedAtMillis = now - 1 * DAY_MS
        ),
        PlantMonitoringLog(
            id = "demo_log_4",
            userId = "demo_user_petani",
            plantId = "demo_plant_2",
            plantName = "Pakcoy Nauli Rak B",
            ph = 7.2,
            ec = 1.8,
            tds = 900L,
            waterTemp = 26.5,
            ambientTemp = 29.0,
            heightCm = 18.5,
            rootCondition = "Putih sedikit kecokelatan di ujung",
            plantCondition = "Daun agak hangat siang hari",
            healthStatus = HealthStatus.PERLU_DIPERIKSA.name,
            notes = "DEMO: pH sedikit naik, turunkan dengan pH down",
            recordedAtMillis = now - 2 * DAY_MS
        )
    )

    val initialHarvests = listOf(
        HarvestRecord(
            id = "demo_harv_1",
            userId = "demo_user_petani",
            plantId = "demo_plant_1",
            plantName = "Selada Hijau Siklus #1",
            cropType = "Selada Hijau",
            cycleLabel = "Periode Oktober 2026",
            weightKg = 18.5,
            holeCount = 120L,
            durationDays = 34L,
            notes = "DEMO: Terserap habis oleh katering & marketplace lokal",
            harvestedAtMillis = now - 35 * DAY_MS
        ),
        HarvestRecord(
            id = "demo_harv_2",
            userId = "demo_user_petani",
            plantId = "demo_plant_2",
            plantName = "Pakcoy Siklus #2",
            cropType = "Pakcoy",
            cycleLabel = "Periode Oktober 2026",
            weightKg = 14.2,
            holeCount = 80L,
            durationDays = 31L,
            notes = "DEMO: Bobot rata-rata 177g per lubang",
            harvestedAtMillis = now - 20 * DAY_MS
        )
    )

    val initialProducts = listOf(
        MarketplaceProduct(
            id = "demo_prod_1",
            sellerId = "demo_seller_bandung_1",
            sellerName = "Kebun Hijau Dago (DEMO)",
            sellerArea = "Dago, Bandung",
            sellerLat = -6.8898,
            sellerLng = 107.6131,
            mainCategory = MainCategory.HASIL_PANEN.name,
            subCategory = "Selada",
            name = "Selada Hijau Hidroponik Segar",
            description = "[DEMO] Selada hijau renyah bebas pestisida, dipetik pagi hari langsung dari instalasi NFT.",
            price = 20000L,
            unit = "kg",
            stock = 35L,
            freshnessStatus = "Panen Hari Ini",
            deliveryMethods = "Kurir Lokal, Ambil di Kebun",
            rating = 4.9,
            reviewCount = 42L,
            promoted = true,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_2",
            sellerId = "demo_seller_bandung_1",
            sellerName = "Kebun Hijau Dago (DEMO)",
            sellerArea = "Dago, Bandung",
            sellerLat = -6.8898,
            sellerLng = 107.6131,
            mainCategory = MainCategory.HASIL_PANEN.name,
            subCategory = "Pakcoy",
            name = "Pakcoy Hidroponik Organik",
            description = "[DEMO] Pakcoy batang tebal dan daun segar, cocok untuk tumisan dan sup sehat.",
            price = 18000L,
            unit = "kg",
            stock = 28L,
            freshnessStatus = "Panen Pagi",
            deliveryMethods = "Kurir Lokal, Ambil di Kebun",
            rating = 4.8,
            reviewCount = 31L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_3",
            sellerId = "demo_seller_lembang_2",
            sellerName = "Lembang Fresh Hydro (DEMO)",
            sellerArea = "Lembang, Bandung Barat",
            sellerLat = -6.8117,
            sellerLng = 107.6175,
            mainCategory = MainCategory.HASIL_PANEN.name,
            subCategory = "Kangkung",
            name = "Kangkung Hidroponik Akar Bersih",
            description = "[DEMO] Kangkung tanpa tanah dan tanpa lumpur, akar bersih siap olah.",
            price = 15000L,
            unit = "kg",
            stock = 40L,
            freshnessStatus = "Panen Hari Ini",
            deliveryMethods = "Kurir Lokal",
            rating = 4.9,
            reviewCount = 27L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_4",
            sellerId = "demo_seller_cimahi_3",
            sellerName = "Kale & Greens Cimahi (DEMO)",
            sellerArea = "Cimahi",
            sellerLat = -6.8722,
            sellerLng = 107.5425,
            mainCategory = MainCategory.HASIL_PANEN.name,
            subCategory = "Kale",
            name = "Kale Nero Superfood Hidroponik",
            description = "[DEMO] Daun kale segar untuk jus hijau dan salad, ditanam dengan nutrisi terukur.",
            price = 32000L,
            unit = "kg",
            stock = 14L,
            freshnessStatus = "Segar Terjaga",
            deliveryMethods = "Kurir Lokal, Same Day",
            rating = 4.7,
            reviewCount = 19L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_5",
            sellerId = "demo_supplier_1",
            sellerName = "Tani Hidro Nusantara (DEMO)",
            sellerArea = "Bandung Tengah",
            sellerLat = -6.9218,
            sellerLng = 107.6070,
            mainCategory = MainCategory.PERLENGKAPAN.name,
            subCategory = "Nutrisi AB Mix",
            name = "Nutrisi AB Mix Daun Pekatan 5 Liter",
            description = "[DEMO] Formulasi lengkap unsur hara makro dan mikro untuk sayuran daun hidroponik.",
            price = 80000L,
            unit = "paket",
            stock = 60L,
            freshnessStatus = "Stok Baru",
            deliveryMethods = "Kurir Lokal, Ekspedisi",
            rating = 4.9,
            reviewCount = 86L,
            promoted = true,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_6",
            sellerId = "demo_supplier_1",
            sellerName = "Tani Hidro Nusantara (DEMO)",
            sellerArea = "Bandung Tengah",
            sellerLat = -6.9218,
            sellerLng = 107.6070,
            mainCategory = MainCategory.PERLENGKAPAN.name,
            subCategory = "Rockwool",
            name = "Rockwool Cultilene 1 Slab (Potong Dadu)",
            description = "[DEMO] Media tanam rockwool daya serap tinggi, cocok untuk penyemaian benih hidroponik.",
            price = 25000L,
            unit = "paket",
            stock = 120L,
            freshnessStatus = "Ready Stock",
            deliveryMethods = "Kurir Lokal, Ambil Langsung",
            rating = 4.8,
            reviewCount = 64L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_7",
            sellerId = "demo_supplier_1",
            sellerName = "Tani Hidro Nusantara (DEMO)",
            sellerArea = "Bandung Tengah",
            sellerLat = -6.9218,
            sellerLng = 107.6070,
            mainCategory = MainCategory.PERLENGKAPAN.name,
            subCategory = "Net pot",
            name = "Net Pot Hidroponik 5 cm Tebal",
            description = "[DEMO] Net pot plastik PP kokoh dengan lubang sirkulasi akar optimal.",
            price = 1000L,
            unit = "buah",
            stock = 900L,
            freshnessStatus = "Ready Stock",
            deliveryMethods = "Kurir Lokal, Ekspedisi",
            rating = 4.9,
            reviewCount = 112L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_8",
            sellerId = "demo_supplier_2",
            sellerName = "SmartHydro Tech Jakarta (DEMO)",
            sellerArea = "Kemang, Jakarta Selatan",
            sellerLat = -6.2615,
            sellerLng = 106.8106,
            mainCategory = MainCategory.PERLENGKAPAN.name,
            subCategory = "TDS & pH meter",
            name = "Paket TDS/EC & pH Meter Digital Akurat",
            description = "[DEMO] Alat ukur kadar kepekatan nutrisi dan pH air dengan kalibrasi otomatis.",
            price = 165000L,
            unit = "paket",
            stock = 22L,
            freshnessStatus = "Garansi 6 Bulan",
            deliveryMethods = "Ekspedisi, Kurir Instan",
            rating = 4.8,
            reviewCount = 51L,
            promoted = false,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_9",
            sellerId = "demo_service_1",
            sellerName = "Ahli Instalasi HydroPro (DEMO)",
            sellerArea = "Bandung & Sekitarnya",
            sellerLat = -6.9100,
            sellerLng = 107.6300,
            mainCategory = MainCategory.JASA.name,
            subCategory = "Pembuatan instalasi",
            name = "Jasa Pembuatan Instalasi NFT 60 Lubang",
            description = "[DEMO] Termasuk pipa food-grade, rangka baja ringan, pompa, tandon, dan pelatihan awal.",
            price = 1450000L,
            unit = "paket",
            stock = 8L,
            freshnessStatus = "Jadwal Fleksibel",
            deliveryMethods = "Kunjungan Lokasi",
            rating = 5.0,
            reviewCount = 18L,
            promoted = true,
            active = true,
            isDemo = true
        ),
        MarketplaceProduct(
            id = "demo_prod_10",
            sellerId = "demo_service_1",
            sellerName = "Ahli Instalasi HydroPro (DEMO)",
            sellerArea = "Bandung & Sekitarnya",
            sellerLat = -6.9100,
            sellerLng = 107.6300,
            mainCategory = MainCategory.JASA.name,
            subCategory = "Perawatan instalasi",
            name = "Jasa Perawatan & Kalibrasi Kebun Bulanan",
            description = "[DEMO] Pembersihan pipa, kalibrasi pH/EC, cek pompa, dan pendampingan panen.",
            price = 250000L,
            unit = "kunjungan",
            stock = 15L,
            freshnessStatus = "Tersedia Minggu Ini",
            deliveryMethods = "Kunjungan Lokasi",
            rating = 4.9,
            reviewCount = 14L,
            promoted = false,
            active = true,
            isDemo = true
        )
    )

    val initialOrders = listOf(
        MarketplaceOrder(
            id = "demo_ord_1",
            buyerId = "demo_user_pembeli",
            buyerName = "Resto Salad Segar (DEMO)",
            sellerId = "demo_user_petani",
            sellerName = "Budi Santoso (DEMO)",
            courierId = "demo_user_mitra_pengiriman",
            mainCategory = MainCategory.HASIL_PANEN.name,
            itemsSummary = "Selada Hijau Hidroponik x5 kg",
            subtotal = 100000L,
            shippingFee = 12000L,
            serviceFee = 2000L,
            commissionAmount = 5000L,
            totalAmount = 114000L,
            paymentMethod = "QRIS",
            paymentStatus = PaymentStatus.SIMULASI_DEMO.name,
            orderStatus = OrderStatus.SELESAI.name,
            deliveryMethod = "Kurir Lokal",
            deliveryAddress = "Jl. Riau No. 45, Bandung",
            isDemo = true
        ),
        MarketplaceOrder(
            id = "demo_ord_2",
            buyerId = "demo_user_pembeli",
            buyerName = "Sari Wulandari (DEMO)",
            sellerId = "demo_user_petani",
            sellerName = "Budi Santoso (DEMO)",
            courierId = "demo_user_mitra_pengiriman",
            mainCategory = MainCategory.HASIL_PANEN.name,
            itemsSummary = "Pakcoy Hidroponik x3 kg, Kangkung x2 kg",
            subtotal = 84000L,
            shippingFee = 10000L,
            serviceFee = 2000L,
            commissionAmount = 4200L,
            totalAmount = 96000L,
            paymentMethod = "Transfer Bank",
            paymentStatus = PaymentStatus.SIMULASI_DEMO.name,
            orderStatus = OrderStatus.DIPROSES.name,
            deliveryMethod = "Kurir Lokal",
            deliveryAddress = "Jl. Setiabudi No. 18, Bandung",
            isDemo = true
        ),
        MarketplaceOrder(
            id = "demo_ord_3",
            buyerId = "demo_user_petani",
            buyerName = "Budi Santoso (DEMO)",
            sellerId = "demo_supplier_1",
            sellerName = "Tani Hidro Nusantara (DEMO)",
            courierId = "demo_user_mitra_pengiriman",
            mainCategory = MainCategory.PERLENGKAPAN.name,
            itemsSummary = "Nutrisi AB Mix 5L x1 paket, Rockwool x2 paket",
            subtotal = 130000L,
            shippingFee = 15000L,
            serviceFee = 2000L,
            commissionAmount = 6500L,
            totalAmount = 147000L,
            paymentMethod = "Virtual Account",
            paymentStatus = PaymentStatus.SIMULASI_DEMO.name,
            orderStatus = OrderStatus.DIKIRIM.name,
            deliveryMethod = "Kurir Lokal",
            deliveryAddress = "Jl. Kebun Hidroponik No. 12, Bandung",
            isDemo = true
        )
    )

    val initialExpenses = listOf(
        FinancialExpense(
            id = "demo_exp_1",
            userId = "demo_user_petani",
            entryType = ExpenseType.MODAL.name,
            category = "Pembelian Instalasi",
            title = "Instalasi NFT 200 Lubang + Rangka",
            amount = 3200000L,
            cycleLabel = "Periode Oktober 2026",
            notes = "DEMO: Investasi awal kebun utama",
            dateMillis = now - 45 * DAY_MS
        ),
        FinancialExpense(
            id = "demo_exp_2",
            userId = "demo_user_petani",
            entryType = ExpenseType.PENGELUARAN.name,
            category = "Nutrisi",
            title = "Nutrisi AB Mix Daun 10L",
            amount = 160000L,
            cycleLabel = "Periode November 2026",
            notes = "DEMO: Stok nutrisi bulanan",
            dateMillis = now - 12 * DAY_MS
        ),
        FinancialExpense(
            id = "demo_exp_3",
            userId = "demo_user_petani",
            entryType = ExpenseType.PENGELUARAN.name,
            category = "Benih",
            title = "Benih Selada & Pakcoy Unggul",
            amount = 95000L,
            cycleLabel = "Periode November 2026",
            notes = "DEMO: 2 kaleng benih",
            dateMillis = now - 10 * DAY_MS
        ),
        FinancialExpense(
            id = "demo_exp_4",
            userId = "demo_user_petani",
            entryType = ExpenseType.PENGELUARAN.name,
            category = "Listrik",
            title = "Token Listrik Pompa Sirkulasi",
            amount = 120000L,
            cycleLabel = "Periode November 2026",
            notes = "DEMO: Pemakaian 3 pompa 24 jam",
            dateMillis = now - 5 * DAY_MS
        ),
        FinancialExpense(
            id = "demo_exp_5",
            userId = "demo_user_petani",
            entryType = ExpenseType.PENGELUARAN.name,
            category = "Pengemasan",
            title = "Plastik Kemasan & Stiker Kebun",
            amount = 75000L,
            cycleLabel = "Periode November 2026",
            notes = "DEMO: 200 lembar kemasan sayur",
            dateMillis = now - 3 * DAY_MS
        )
    )

    val initialRevenues = listOf(
        FinancialRevenue(
            id = "demo_rev_1",
            userId = "demo_user_petani",
            category = "Penjualan Hasil Panen",
            title = "Panen Selada Hijau 18.5 kg (Resto)",
            amount = 370000L,
            orderId = "demo_ord_1",
            cycleLabel = "Periode Oktober 2026",
            notes = "DEMO: Penjualan langsung tanpa perantara",
            dateMillis = now - 18 * DAY_MS
        ),
        FinancialRevenue(
            id = "demo_rev_2",
            userId = "demo_user_petani",
            category = "Penjualan Hasil Panen",
            title = "Panen Pakcoy 14.2 kg (Pasar Lokal)",
            amount = 255600L,
            cycleLabel = "Periode Oktober 2026",
            notes = "DEMO: Pesanan rumah tangga & katering",
            dateMillis = now - 9 * DAY_MS
        ),
        FinancialRevenue(
            id = "demo_rev_3",
            userId = "demo_user_petani",
            category = "Penjualan Hasil Panen",
            title = "Penjualan Selada Marketplace (5 kg)",
            amount = 100000L,
            orderId = "demo_ord_1",
            cycleLabel = "Periode November 2026",
            notes = "DEMO: Masuk otomatis dari transaksi selesai",
            dateMillis = now - 2 * DAY_MS
        )
    )

    val initialWholesaleRequests = listOf(
        WholesaleRequest(
            id = "demo_ws_1",
            buyerId = "demo_b2b_hotel",
            buyerCompany = "Hotel Hijau Parahyangan (DEMO)",
            buyerType = "Hotel",
            cropNeeded = "Selada Hijau & Kale",
            quantityKg = 45L,
            targetPricePerKg = 21000L,
            deliverySchedule = "Setiap Senin & Kamis pagi",
            area = "Bandung",
            status = WholesaleStatus.TERBUKA.name,
            open = true
        ),
        WholesaleRequest(
            id = "demo_ws_2",
            buyerId = "demo_b2b_catering",
            buyerCompany = "Katering Sehat Nusantara (DEMO)",
            buyerType = "Katering",
            cropNeeded = "Pakcoy & Kangkung",
            quantityKg = 80L,
            targetPricePerKg = 16500L,
            deliverySchedule = "Kontrak mingguan 20 kg/minggu",
            area = "Cimahi & Bandung",
            status = WholesaleStatus.TERBUKA.name,
            open = true
        ),
        WholesaleRequest(
            id = "demo_ws_3",
            buyerId = "demo_b2b_resto",
            buyerCompany = "Salad Bar Kemang (DEMO)",
            buyerType = "Restoran",
            cropNeeded = "Selada Merah & Romaine",
            quantityKg = 30L,
            targetPricePerKg = 26000L,
            deliverySchedule = "3x seminggu",
            area = "Jakarta Selatan",
            status = WholesaleStatus.TERBUKA.name,
            open = true
        )
    )

    val initialNotifications = listOf(
        AppNotification(
            id = "demo_notif_1",
            userId = "demo_user_petani",
            title = "Jadwal Cek pH Hari Ini",
            body = "DEMO: Cek pH & suhu tandon NFT Rak A (target 5.5 - 6.5).",
            type = "PERAWATAN",
            read = false
        ),
        AppNotification(
            id = "demo_notif_2",
            userId = "demo_user_petani",
            title = "Perkiraan Panen Pakcoy Nauli",
            body = "DEMO: Pakcoy Rak B mendekati siap panen (estimasi 14.4 kg).",
            type = "PANEN",
            read = false
        ),
        AppNotification(
            id = "demo_notif_3",
            userId = "demo_user_petani",
            title = "Pesanan Baru Masuk (#demo_ord_2)",
            body = "DEMO: Sari Wulandari memesan Pakcoy 3 kg & Kangkung 2 kg.",
            type = "PESANAN",
            read = false
        ),
        AppNotification(
            id = "demo_notif_4",
            userId = "demo_user_petani",
            title = "Permintaan Grosir Baru dari Hotel",
            body = "DEMO: Hotel Hijau Parahyangan mencari 45 kg Selada & Kale.",
            type = "GROSIR",
            read = true
        )
    )

    val initialForumPosts = listOf(
        ForumPost(
            id = "demo_forum_1",
            authorId = "demo_user_petani",
            authorName = "Budi Santoso (DEMO)",
            title = "Tips menjaga suhu tandon nutrisi tetap sejuk saat kemarau",
            content = "DEMO: Gunakan penutup styrofoam pada tandon dan pasang instalasi di bawah paranet 50% agar suhu air di bawah 26°C.",
            topic = "Suhu & Akar",
            replyCount = 7L,
            visible = true
        ),
        ForumPost(
            id = "demo_forum_2",
            authorId = "demo_supplier_1",
            authorName = "Tani Hidro Nusantara (DEMO)",
            title = "Kapan waktu terbaik menaikkan EC pada selada fase vegetatif?",
            content = "DEMO: Naikkan bertahap dari 0.8 mS/cm saat pindah tanam menjadi 1.2–1.4 mS/cm di minggu ke-3 agar ujung daun tidak terbakar.",
            topic = "Nutrisi & pH",
            replyCount = 12L,
            visible = true
        )
    )

    val educationArticles = listOf(
        EducationArticle(
            id = "edu_1",
            title = "Panduan Memulai Hidroponik untuk Pemula",
            category = "Dasar Hidroponik",
            readMinutes = 5,
            summary = "Langkah awal memilih sistem, benih, rockwool, dan nutrisi AB Mix.",
            content = "Hidroponik menanam sayuran tanpa tanah dengan memanfaatkan larutan air bernutrisi. Mulai dengan skala kecil 20–40 lubang menggunakan sistem Wick atau DFT. Pastikan lokasi mendapat sinar matahari minimal 5–6 jam per hari dan siapkan alat ukur pH serta TDS/EC meter.",
            videoTitle = "Video: Instalasi Pertama Skala Rumah Tangga",
            videoDuration = "08:24"
        ),
        EducationArticle(
            id = "edu_2",
            title = "Tutorial Lengkap Sistem NFT (Nutrient Film Technique)",
            category = "Sistem Instalasi",
            readMinutes = 6,
            summary = "Kemiringan talang, debit pompa, dan oksigenasi akar pada sistem NFT.",
            content = "Sistem NFT mengalirkan lapisan tipis air nutrisi setebal 2–3 mm melewati akar tanaman. Kemiringan ideal berkisar 3%–5% agar aliran lancar tanpa menggenang. Pompa harus berjalan kontinu, sehingga cadangan daya atau pemantauan aliran sangat dianjurkan.",
            videoTitle = "Video: Merakit Talang NFT 40 Lubang",
            videoDuration = "12:10"
        ),
        EducationArticle(
            id = "edu_3",
            title = "Sistem Wick & DFT: Solusi Hemat Listrik",
            category = "Sistem Instalasi",
            readMinutes = 4,
            summary = "Perbandingan sistem sumbu pasif (Wick) dan genangan dalam (DFT).",
            content = "Sistem Wick menggunakan sumbu kain flanel tanpa pompa listrik, ideal untuk pemula dan kangkung/pakcoy. Sistem DFT (Deep Flow Technique) menyisakan genangan 3–4 cm di dalam pipa sehingga tanaman tetap aman bila listrik padam beberapa jam.",
            videoTitle = "Video: Perbedaan Wick, DFT, dan NFT",
            videoDuration = "06:45"
        ),
        EducationArticle(
            id = "edu_4",
            title = "Panduan Racik & Pakai Nutrisi AB Mix",
            category = "Nutrisi & Air",
            readMinutes = 5,
            summary = "Cara melarutkan pekatan A & B secara terpisah agar tidak mengendap.",
            content = "Selalu larutkan stok A dan stok B di wadah terpisah sebelum dicampur ke tandon air baku. Campurkan masing-masing 5 ml pekatan A dan 5 ml pekatan B per 1 liter air (dosis standar 5:5:1), lalu ukur TDS/EC dan sesuaikan dengan umur tanaman.",
            videoTitle = "Video: Cara Melarutkan AB Mix Tanpa Endapan",
            videoDuration = "07:30"
        ),
        EducationArticle(
            id = "edu_5",
            title = "Teknik Penyemaian & Pindah Tanam Tanpa Stres",
            category = "Budidaya",
            readMinutes = 5,
            summary = "Menjaga kelembapan rockwool hingga muncul 4 daun sejati sebelum pindah tanam.",
            content = "Potong rockwool 2,5 cm, basahi secukupnya tanpa diperas, lalu masukkan 1 benih per lubang. Setelah berkecambah (1–2 hari), segera kenalkan ke sinar matahari pagi agar tidak etiolasi (kutilang). Pindah tanam dilakukan saat muncul 3–4 daun sejati.",
            videoTitle = "Video: Semai Rockwool Anti Kutilang",
            videoDuration = "09:15"
        ),
        EducationArticle(
            id = "edu_6",
            title = "Pengendalian Hama & Penanganan Tanaman Layu",
            category = "Perlindungan Tanaman",
            readMinutes = 6,
            summary = "Deteksi kutu daun, ulat, busuk akar Pythium, dan solusi pH ekstrem.",
            content = "Tanaman layu di siang hari bisa disebabkan oleh suhu larutan >28°C, oksigen terlarut rendah, atau busuk akar. Periksa warna akar (sehat = putih bersih). Gunakan perangkap kuning dan pestisida nabati (minyak mimba/ekstrak bawang) untuk mengendalikan kutu daun tanpa residu kimia.",
            videoTitle = "Video: Mengatasi Akar Cokelat & Daun Layu",
            videoDuration = "10:05"
        ),
        EducationArticle(
            id = "edu_7",
            title = "Teknik Panen, Pengemasan & Pemasaran Hasil Panen",
            category = "Pasca Panen & Bisnis",
            readMinutes = 5,
            summary = "Panen pagi hari, mempertahankan kesegaran dengan akar, dan akses pasar lokal.",
            content = "Panen sebaiknya dilakukan pagi hari (05.30–08.00) saat kadar air daun optimal. Kemas sayuran bersama sedikit rockwool lembap dalam plastik berlubang mikro agar segar hingga 5 hari di lemari es, lalu pasarkan langsung ke pembeli terdekat atau restoran lewat HydroGrow.",
            videoTitle = "Video: Standar Kemasan Sayur Masuk Restoran",
            videoDuration = "08:50"
        )
    )

    val expertConsultants = listOf(
        ExpertConsultant("exp_1", "Ir. Hendra Wijaya (DEMO)", "Sistem NFT Komersial & Otomasi", 9, 150000L, 4.9, "Bandung"),
        ExpertConsultant("exp_2", "Dr. Nadia Kusuma (DEMO)", "Formulasi Nutrisi & Pengendalian Hama", 11, 175000L, 5.0, "Bogor"),
        ExpertConsultant("exp_3", "Raka Pratama (DEMO)", "Pemasaran Sayur B2B & Kemitraan Resto", 6, 120000L, 4.8, "Jakarta Selatan")
    )

    val initialReviews = listOf(
        ProductReview("rev_1", "demo_prod_1", "Dina (DEMO)", 5, "Daun selada sangat renyah, bersih, dan dikirim cepat.", "2 hari lalu"),
        ProductReview("rev_2", "demo_prod_1", "Chef Anton (DEMO)", 5, "Kualitas seragam untuk kebutuhan salad restoran kami.", "5 hari lalu"),
        ProductReview("rev_3", "demo_prod_5", "Kebun Pak Jaya (DEMO)", 5, "Nutrisi larut sempurna, pertumbuhan pakcoy lebih cepat.", "1 minggu lalu")
    )
}
