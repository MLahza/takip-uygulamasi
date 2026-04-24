package com.example.molatakipmobil.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.molatakipmobil.ui.theme.*
import com.example.molatakipmobil.viewmodel.MolaViewModel

// Kullanıcının mola süresini ayarlayabileceği Ayarlar ekranı
@Composable
fun SettingsScreen(viewModel: MolaViewModel) {
    // ViewModel'den mevcut süreyi dinliyoruz
    val currentDuration by viewModel.breakDurationMinutes.collectAsState()
    // Kullanıcının girdiği değeri geçici olarak tutan state
    var inputValue by remember { mutableStateOf(currentDuration.toString()) }
    // Başarı mesajı göstermek için state
    var showSuccess by remember { mutableStateOf(false) }

    // currentDuration değişirse input'u güncelle
    LaunchedEffect(currentDuration) {
        inputValue = currentDuration.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Süre ayarlama kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Başlık satırı
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Mola Süresi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mevcut süre gösterimi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mevcut: $currentDuration dakika",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryEmerald
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hızlı seçim butonları
                Text(
                    text = "Hızlı Seçim",
                    fontSize = 14.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // İlk satır butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hızlı seçim butonları - sık kullanılan süreler
                    listOf(5, 10, 15, 30).forEach { minutes ->
                        QuickSelectButton(
                            minutes = minutes,
                            isSelected = currentDuration == minutes,
                            onClick = {
                                viewModel.setBreakDuration(minutes)
                                inputValue = minutes.toString()
                                showSuccess = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // İkinci satır butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(45, 60, 90, 120).forEach { minutes ->
                        QuickSelectButton(
                            minutes = minutes,
                            isSelected = currentDuration == minutes,
                            onClick = {
                                viewModel.setBreakDuration(minutes)
                                inputValue = minutes.toString()
                                showSuccess = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Manuel giriş alanı
                Text(
                    text = "Manuel Giriş (dakika)",
                    fontSize = 14.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sayı giriş alanı
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { newVal ->
                            // Sadece sayı girişine izin ver
                            if (newVal.all { it.isDigit() } && newVal.length <= 3) {
                                inputValue = newVal
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                            cursorColor = PrimaryEmerald
                        ),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = {
                            Text("Dakika girin", color = TextMuted.copy(alpha = 0.5f))
                        }
                    )

                    // Kaydet butonu
                    Button(
                        onClick = {
                            val minutes = inputValue.toIntOrNull()
                            // Geçerli bir değer mi kontrol et (1-999 arası)
                            if (minutes != null && minutes in 1..999) {
                                viewModel.setBreakDuration(minutes)
                                showSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kaydet")
                    }
                }

                // Başarı mesajı
                if (showSuccess) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryEmerald.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mola süresi $currentDuration dakika olarak ayarlandı!",
                                color = PrimaryEmerald,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Bilgi notu kartı
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryIndigo.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, SecondaryIndigo.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SecondaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Süre değişikliği yeni başlatılan molalar için geçerli olur. Devam eden molalar etkilenmez.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Hızlı süre seçim butonu bileşeni
@Composable
fun QuickSelectButton(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            // Seçili ise yeşil, değilse koyu arka plan
            containerColor = if (isSelected) PrimaryEmerald else BorderColor,
            contentColor = if (isSelected) Color.White else TextMuted
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(
            text = "${minutes}dk",
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
