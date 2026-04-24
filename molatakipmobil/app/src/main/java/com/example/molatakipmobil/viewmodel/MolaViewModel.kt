package com.example.molatakipmobil.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.molatakipmobil.R
import com.example.molatakipmobil.AlarmActivity
import com.example.molatakipmobil.data.Employee
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

// Veri ve iş mantığını tutan, ekran döndürmelerinden etkilenmeyen sınıf
class MolaViewModel(application: Application) : AndroidViewModel(application) {

    // Verileri kalıcı olarak kaydetmek için SharedPreferences kullanıyoruz
    private val prefs = application.getSharedPreferences("mola_prefs", Context.MODE_PRIVATE)
    private val gson = Gson() // Verileri JSON formatına çevirmek için
    
    // Kullanıcının ayarladığı mola süresi (dakika cinsinden, varsayılan 45 dk)
    private val _breakDurationMinutes = MutableStateFlow(
        prefs.getInt("break_duration_minutes", 45)
    )
    val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    // Mola süresini saniyeye çevirip hesaplamalarda kullanıyoruz
    private val breakDurationSeconds: Long
        get() = _breakDurationMinutes.value.toLong() * 60

    // Çalışan listesini tutan ve değişimleri ekrana bildiren StateFlow
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    // Her çalışanın kalan süresini saniye cinsinden tutan Map
    private val _currentTimes = MutableStateFlow<Map<String, Long>>(emptyMap())
    val currentTimes: StateFlow<Map<String, Long>> = _currentTimes.asStateFlow()

    init {
        // Uygulama başladığında ilk hazırlıkları yapıyoruz
        createNotificationChannel() // Bildirim kanalını oluştur
        loadEmployees() // Kayıtlı verileri yükle
        startTimerLoop() // Süre sayacını başlat
    }

    // Telefon hafızasından çalışan listesini yükler
    private fun loadEmployees() {
        val json = prefs.getString("mola_employees", null)
        if (json != null) {
            val type = object : TypeToken<List<Employee>>() {}.type
            _employees.value = gson.fromJson(json, type)
        }
    }

    private fun saveEmployees(list: List<Employee>) {
        _employees.value = list
        prefs.edit().putString("mola_employees", gson.toJson(list)).apply()
    }

    fun addEmployee(name: String) {
        val newEmp = Employee(id = UUID.randomUUID().toString(), name = name)
        saveEmployees(_employees.value + newEmp)
    }

    fun editEmployee(id: String, newName: String) {
        val updatedList = _employees.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
        saveEmployees(updatedList)
    }

    fun deleteEmployee(id: String) {
        saveEmployees(_employees.value.filter { it.id != id })
    }

    fun startBreak(id: String) {
        val updatedList = _employees.value.map {
            if (it.id == id) it.copy(molaStartTime = System.currentTimeMillis(), molaFinished = false, isPaused = false, pausedRemainingSeconds = 0L) else it
        }
        saveEmployees(updatedList)
    }

    fun pauseBreak(id: String, remainingSeconds: Long) {
        val updatedList = _employees.value.map {
            if (it.id == id) it.copy(isPaused = true, pausedRemainingSeconds = remainingSeconds) else it
        }
        saveEmployees(updatedList)
    }

    fun resumeBreak(id: String) {
        val updatedList = _employees.value.map {
            if (it.id == id) {
                val newStartTime = System.currentTimeMillis() - ((breakDurationSeconds - it.pausedRemainingSeconds) * 1000)
                it.copy(molaStartTime = newStartTime, isPaused = false, pausedRemainingSeconds = 0L)
            } else it
        }
        saveEmployees(updatedList)
    }

    fun stopBreak(id: String) {
        val updatedList = _employees.value.map {
            if (it.id == id) it.copy(molaStartTime = null, molaFinished = false, isPaused = false, pausedRemainingSeconds = 0L) else it
        }
        saveEmployees(updatedList)
    }

    // Belirli bir çalışanın mola durumunu tamamen sıfırlar
    fun resetBreak(id: String) {
        stopBreak(id)
    }

    // Kullanıcının mola süresini değiştirmesini sağlayan fonksiyon
    fun setBreakDuration(minutes: Int) {
        _breakDurationMinutes.value = minutes
        // Yeni süreyi kalıcı olarak kaydet
        prefs.edit().putInt("break_duration_minutes", minutes).apply()
    }

    // Arka planda her saniye çalışan süre sayacı döngüsü
    private fun startTimerLoop() {
        viewModelScope.launch {
            while (isActive) {
                var changed = false
                val updatedTimes = mutableMapOf<String, Long>()
                val updatedEmployees = _employees.value.toMutableList()

                for (i in updatedEmployees.indices) {
                    val emp = updatedEmployees[i]
                    // Eğer mola başlamışsa ve henüz bitmemişse hesaplama yap
                    if (emp.molaStartTime != null && !emp.molaFinished) {
                        val remaining = if (emp.isPaused) {
                            emp.pausedRemainingSeconds // Duraklatılmışsa mevcut kalan süreyi al
                        } else {
                            // Geçen süreyi şimdiki zamandan çıkararak hesapla
                            val elapsedSeconds = (System.currentTimeMillis() - emp.molaStartTime) / 1000
                            maxOf(0, breakDurationSeconds - elapsedSeconds)
                        }
                        
                        updatedTimes[emp.id] = remaining

                        // Süre bittiyse çalışanı "mola bitti" olarak işaretle ve bildir
                        if (remaining == 0L && !emp.isPaused) {
                            updatedEmployees[i] = emp.copy(molaFinished = true)
                            changed = true
                            sendNotification(emp.name)
                        }
                    }
                }

                _currentTimes.value = updatedTimes // Ekrana yeni süreleri gönder
                if (changed) {
                    saveEmployees(updatedEmployees) // Değişiklik varsa kaydet
                }
                delay(1000) // 1 saniye bekle
            }
        }
    }

    // Alarm seviyesinde bildirim kanalı oluştur
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Personel Mola Alarmı"
            val descriptionText = "Mola bittiğinde çalan ana alarm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // Kanal ID'sini V3 yaparak sistemi zorlayalım
            val channel = NotificationChannel("MOLA_ALARM_V3", name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                
                // Ses ayarını kanal seviyesinde yapalım
                val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                
                setSound(alarmSound, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Alarm tarzında bildirim gönder
    private fun sendNotification(employeeName: String) {
        val context = getApplication<Application>()

        // Alarm ekranını açacak olan intent
        val alarmIntent = android.content.Intent(context, AlarmActivity::class.java).apply {
            putExtra("EMPLOYEE_NAME", employeeName)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context, employeeName.hashCode(), alarmIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(context, "MOLA_ALARM_V3")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Standart alarm ikonu
            .setContentTitle("⏰ MOLA BİTTİ!")
            .setContentText("$employeeName için mola tamamlandı.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true) 
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(employeeName.hashCode(), builder.build())
    }
}
