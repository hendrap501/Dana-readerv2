# 📱 Panduan Build APK dari HP (Tanpa PC/Laptop)

## Metode: GitHub Actions (GRATIS, build di cloud)

---

## Langkah 1 — Buat Akun GitHub

1. Buka browser di HP → **github.com**
2. Tap **"Sign up"**
3. Isi email, password, username
4. Verifikasi email
5. Pilih plan **Free**

---

## Langkah 2 — Buat Repository Baru

1. Login ke GitHub
2. Tap ikon **"+"** di pojok kanan atas
3. Pilih **"New repository"**
4. Isi:
   - Repository name: `dana-reader`
   - Pilih: **Public**
5. Tap **"Create repository"**

---

## Langkah 3 — Upload File via GitHub Web (dari HP)

Setelah repository dibuat, upload file **satu per satu** lewat browser:

### Cara upload file:
1. Tap **"Add file"** → **"Upload files"**
2. Tap **"choose your files"**
3. Pilih file dari penyimpanan HP
4. Tap **"Commit changes"**

### Struktur folder yang harus dibuat:
Gunakan **"Create new file"** untuk membuat folder baru.
Ketik nama file dengan path lengkap, contoh:
```
app/src/main/AndroidManifest.xml
```
GitHub akan otomatis membuat foldernya.

### Daftar file yang harus diupload (salin isinya dari ZIP):

```
📁 .github/
  📁 workflows/
    📄 build.yml

📄 build.gradle
📄 settings.gradle  
📄 gradle.properties
📄 gradlew

📁 gradle/
  📁 wrapper/
    📄 gradle-wrapper.properties

📁 app/
  📄 build.gradle
  📁 src/main/
    📄 AndroidManifest.xml
    📁 java/com/danareader/app/
      📄 MainActivity.kt
      📄 NotificationListenerService.kt
      📄 TransactionReceiver.kt
      📄 BootReceiver.kt
    📁 res/
      📁 layout/
        📄 activity_main.xml
        📄 item_transaction.xml
      📁 values/
        📄 colors.xml
        📄 strings.xml
        📄 themes.xml
      📁 drawable/
        📄 badge_qris.xml
        📄 ic_check_circle.xml
        📄 ic_dana_logo.xml
        📄 ic_warning.xml
        📄 item_transaction_bg.xml
        📄 spinner_background.xml
```

---

## ⚡ Cara Lebih Mudah: Upload ZIP Langsung

Alternatif lebih mudah — gunakan aplikasi **GitHub** di HP:

1. Install app **GitHub** dari Play Store
2. Login dengan akun GitHub
3. Buat repository baru
4. Di repository → tap **"+"** → **"Upload file"**
5. Upload file ZIP yang sudah didownload
6. **Ekstrak dan commit** satu per satu

Atau gunakan website **gitpod.io** atau **github.dev** untuk edit langsung dari browser.

---

## Langkah 4 — Jalankan Build Otomatis

Setelah semua file terupload:

1. Di repository GitHub, tap tab **"Actions"**
2. Anda akan melihat workflow **"Build DANA Reader APK"**
3. Jika belum jalan otomatis, tap nama workflow → **"Run workflow"** → **"Run workflow"**
4. Tunggu **3-5 menit** hingga selesai ✅

---

## Langkah 5 — Download APK

1. Setelah build selesai (tanda ✅ hijau), tap nama build-nya
2. Scroll ke bawah ke bagian **"Artifacts"**
3. Tap **"dana-reader-debug-apk"**
4. File ZIP berisi APK akan terdownload
5. Ekstrak ZIP → install **app-debug.apk** di HP

---

## ⚠️ Sebelum Install APK

Di HP Android, aktifkan **"Install dari sumber tidak dikenal"**:
- **Settings** → **Security** → **Install unknown apps**
- Aktifkan untuk browser/file manager yang Anda gunakan

---

## 🔁 Jika Ingin Update Aplikasi

Cukup edit file di GitHub (tap file → ikon pensil ✏️), 
lalu GitHub Actions akan otomatis build APK baru.

---

## 💬 Butuh Bantuan?

Jika ada error saat build, lihat log di tab **Actions** dan 
tunjukkan pesan errornya — saya bantu perbaiki!
