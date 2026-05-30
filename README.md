# DANA Reader - Aplikasi Pembaca Transaksi QRIS

Aplikasi Android untuk membaca notifikasi transaksi masuk dari aplikasi DANA secara otomatis menggunakan Text-to-Speech.

## 🚀 Fitur
- **Deteksi Otomatis** transaksi QRIS masuk dari DANA
- **Text-to-Speech** membacakan nominal dan nama pengirim dalam Bahasa Indonesia atau English
- **Statistik Harian** total uang masuk dan jumlah transaksi
- **Log Transaksi** riwayat semua transaksi yang terbaca
- **Pengaturan Suara** kecepatan bicara dan pilihan bahasa
- **Auto-start** setelah HP restart

## 📋 Cara Pasang di Android Studio

### Persyaratan
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- Android SDK 34
- Kotlin 1.9.x
- HP Android dengan minimal Android 5.0 (API 21)

### Langkah-langkah

1. **Buka Android Studio**
   - File → Open → pilih folder `dana-reader`

2. **Tunggu Gradle Sync**
   - Biarkan Android Studio mengunduh dependencies
   - Pastikan koneksi internet aktif

3. **Build & Install**
   - Hubungkan HP Android dengan USB Debugging aktif
   - Klik tombol Run (▶) atau tekan Shift+F10
   - Pilih device Anda

4. **Konfigurasi di HP**
   - Buka app setelah terinstall
   - Tap "Berikan Izin Notifikasi"
   - Cari "DANA Reader" dalam daftar dan aktifkan
   - Kembali ke app

## ⚙️ Cara Kerja

```
Notifikasi DANA masuk
        ↓
NotificationListenerService mendeteksi
        ↓
Cek apakah ada keyword QRIS: "pembayaran qris", "qris masuk", dll
        ↓
Extract nominal: Regex "Rp[\d.,]+"
        ↓
Extract pengirim: Regex "dari [Nama]"
        ↓
Convert angka ke kata: 50000 → "lima puluh ribu"
        ↓
TextToSpeech: "Pembayaran QRIS masuk. Lima puluh ribu rupiah. Dari Budi."
        ↓
Update UI log & statistik
```

## 📱 Contoh Notifikasi DANA yang Terdeteksi

App akan mendeteksi notifikasi dengan teks seperti:
- "Pembayaran QRIS diterima Rp50.000 dari Budi Santoso"
- "QRIS masuk! Rp150.000 dari Toko Maju"
- "Berhasil menerima Rp25.000 dari Ahmad"
- "Terima pembayaran Rp100.000 dari Customer"

## 🔧 Kustomisasi

### Menambah pattern keyword baru
Edit di `NotificationListenerService.kt`:
```kotlin
private val QRIS_KEYWORDS = listOf(
    "pembayaran qris",
    "tambahkan keyword baru disini",  // ← tambah di sini
    ...
)
```

### Menambah package name DANA versi lain
```kotlin
private val DANA_PACKAGES = setOf(
    "id.dana",
    "com.dana.android.merchant", // ← tambah di sini
    ...
)
```

## 🛡️ Izin yang Diperlukan

| Izin | Kegunaan |
|------|---------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Membaca notifikasi |
| `RECEIVE_BOOT_COMPLETED` | Auto-start setelah restart |
| `FOREGROUND_SERVICE` | Jalan di background |
| `VIBRATE` | Getaran (opsional) |

## ❓ Troubleshooting

**Suara tidak keluar?**
- Pastikan volume HP tidak di-mute
- Test dengan tombol "Test Suara"
- Pastikan Bahasa Indonesia tersedia di pengaturan TTS HP

**Transaksi tidak terbaca?**
- Pastikan izin notifikasi sudah diberikan
- Pastikan notifikasi DANA tidak diblokir
- Cek apakah switch "Aktif" sudah ON

**Di beberapa HP (MIUI, EMUI, dll):**
- Battery optimization perlu dinonaktifkan untuk DANA Reader
- Beri izin "Auto-start" di pengaturan HP

## 📦 Struktur Project

```
dana-reader/
├── app/
│   ├── src/main/
│   │   ├── java/com/danareader/app/
│   │   │   ├── MainActivity.kt          # UI utama
│   │   │   ├── NotificationListenerService.kt  # Core service
│   │   │   ├── TransactionReceiver.kt   # Update UI dari service
│   │   │   └── BootReceiver.kt          # Auto-start
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    # Layout utama
│   │   │   │   └── item_transaction.xml # Item log
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/               # Icons & shapes
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```
