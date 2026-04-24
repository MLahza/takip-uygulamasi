package com.example.molatakipmobil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.molatakipmobil.components.AddEmployeeDialog
import com.example.molatakipmobil.data.Employee
import com.example.molatakipmobil.screens.ActiveBreaksScreen
import com.example.molatakipmobil.screens.FinishedBreaksScreen
import com.example.molatakipmobil.screens.SettingsScreen
import com.example.molatakipmobil.ui.theme.*
import com.example.molatakipmobil.viewmodel.MolaViewModel

// Uygulamanın ana giriş noktası olan Activity sınıfı
class MainActivity : ComponentActivity() {
    // Veri ve mantık yönetimini sağlayan ViewModel'i başlatıyoruz
    private val viewModel: MolaViewModel by viewModels()

    // Bildirim izni istemek için kullanılan başlatıcı
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // İzin verildi mi kontrolü burada yapılabilir
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 13 ve üzeri için bildirim izni kontrolü ve isteği
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Compose arayüzünü ayarlıyoruz
        setContent {
            MolatakipmobilTheme {
                // Ana ekran bileşenini çağırıyoruz
                MainAppScreen(viewModel)
            }
        }
    }
}

// Uygulamanın ana ekran yapısını kuran Composable fonksiyon
@Composable
fun MainAppScreen(viewModel: MolaViewModel) {
    // Ekran durumlarını (state) tutan değişkenler
    var selectedTab by remember { mutableStateOf(0) } // Seçili sekme
    var showAddDialog by remember { mutableStateOf(false) } // Ekleme penceresi görünürlüğü
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) } // Düzenlenen çalışan

    // Eğer ekleme/düzenleme penceresi açıksa dialog bileşenini göster
    if (showAddDialog) {
        AddEmployeeDialog(
            initialName = employeeToEdit?.name ?: "",
            onDismiss = {
                showAddDialog = false
                employeeToEdit = null
            },
            onConfirm = { name ->
                if (employeeToEdit != null) {
                    viewModel.editEmployee(employeeToEdit!!.id, name)
                } else {
                    viewModel.addEmployee(name)
                }
                showAddDialog = false
                employeeToEdit = null
            }
        )
    }

    // Android'in standart ekran yapısını (Scaffold) oluşturuyoruz
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = { AppHeader() }, // Üst başlık alanı
        bottomBar = {
            // Alt navigasyon çubuğu
            NavigationBar(
                containerColor = GlassBg,
                contentColor = TextMuted,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Liste") },
                    label = { Text("Çalışanlar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryEmerald,
                        selectedTextColor = PrimaryEmerald,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Mola Sonu") },
                    label = { Text("Mola Sonu") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryEmerald,
                        selectedTextColor = PrimaryEmerald,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
                // 3. sekme: Ayarlar
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarla") },
                    label = { Text("Ayarla") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryEmerald,
                        selectedTextColor = PrimaryEmerald,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryEmerald,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }
        }
    ) { innerPadding ->
        // Ana içerik alanı, sekmelere göre ekran değiştiriyor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ActiveBreaksScreen(viewModel, onEditClick = { emp ->
                    employeeToEdit = emp
                    showAddDialog = true
                })
                1 -> FinishedBreaksScreen(viewModel)
                2 -> SettingsScreen(viewModel)
            }
        }
    }
}

// Uygulamanın en üstündeki başlık bileşeni
@Composable
fun AppHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassBg)
            .statusBarsPadding() // Kamera ve durum çubuğu çentiğini hesaplar
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MOLA TAKİP",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryEmerald, SecondaryIndigo)
                    )
                )
            )
            Text(
                text = "Personel Yönetimi",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}