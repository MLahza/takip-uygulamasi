package com.example.molatakipmobil.data

// Her bir çalışanın verilerini temsil eden model sınıfı
data class Employee(
    val id: String, // Benzersiz kimlik
    val name: String, // İsim soyisim
    val molaStartTime: Long? = null, // Molanın başladığı zaman damgası
    val molaFinished: Boolean = false, // Mola bitti mi?
    val isPaused: Boolean = false, // Mola duraklatıldı mı?
    val pausedRemainingSeconds: Long = 0L // Duraklatıldığında kalan süre
) {
    // Mola şu an aktif mi (başlamış, bitmemiş ve duraklatılmamış)
    val isMolaActive: Boolean
        get() = molaStartTime != null && !molaFinished && !isPaused
}
