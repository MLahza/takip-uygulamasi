package com.example.molatakipmobil.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.molatakipmobil.data.Employee
import com.example.molatakipmobil.ui.theme.*
import com.example.molatakipmobil.viewmodel.MolaViewModel
import java.util.Locale

// Aktif çalışanların listelendiği ana ekran bileşeni
@Composable
fun ActiveBreaksScreen(
    viewModel: MolaViewModel,
    onEditClick: (Employee) -> Unit
) {
    // ViewModel'deki verileri "state" olarak dinliyoruz
    val employees by viewModel.employees.collectAsState()
    val currentTimes by viewModel.currentTimes.collectAsState()

    // Sadece molası bitmemiş olanları filtrele
    val activeEmployees = employees.filter { !it.molaFinished }

    if (activeEmployees.isEmpty()) {
        // Liste boşsa kullanıcıya bilgi ver
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextMuted.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Henüz çalışan eklenmemiş.",
                    color = TextMuted,
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activeEmployees) { emp ->
                EmployeeCard(
                    employee = emp,
                    remainingTime = currentTimes[emp.id] ?: 0L,
                    onEditClick = { onEditClick(emp) },
                    onDeleteClick = { viewModel.deleteEmployee(emp.id) },
                    onStartClick = { viewModel.startBreak(emp.id) },
                    onPauseClick = { viewModel.pauseBreak(emp.id, currentTimes[emp.id] ?: 0L) },
                    onResumeClick = { viewModel.resumeBreak(emp.id) },
                    onResetClick = { viewModel.resetBreak(emp.id) }
                )
            }
        }
    }
}

// Her bir çalışanın bilgilerini gösteren kart bileşeni
@Composable
fun EmployeeCard(
    employee: Employee,
    remainingTime: Long,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Üst satır: İsim ve durum bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = employee.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasStarted = employee.molaStartTime != null
                        // Durumuna göre küçük bir renkli nokta göster
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        employee.isPaused -> AccentAmber // Duraklatıldıysa Turuncu
                                        hasStarted -> PrimaryEmerald // Moladaysa Yeşil
                                        else -> TextMuted // Çalışıyorsa Gri
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                employee.isPaused -> "Duraklatıldı"
                                hasStarted -> "Molada"
                                else -> "Çalışıyor"
                            },
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Sayaç Görünümü (Mola başlamışsa göster)
            if (employee.molaStartTime != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(remainingTime), // Saniyeyi 00:00 formatına çevir
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AccentAmber,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (employee.molaStartTime == null) {
                    // Not started
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Başlat")
                    }
                } else {
                    // Started (running or paused)
                    if (employee.isPaused) {
                        Button(
                            onClick = onResumeClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Devam Et")
                        }
                    } else {
                        Button(
                            onClick = onPauseClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duraklat", color = Color.White)
                        }
                    }

                    Button(
                        onClick = onResetClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sıfırla")
                    }
                }
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
