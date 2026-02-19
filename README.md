# 📁 A3 Mart - Management System 🛒

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Material3](https://img.shields.io/badge/Design-Material--3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)
![Automation](https://img.shields.io/badge/Workflow-GitHub--Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

**A3 Mart** adalah aplikasi manajemen inventaris dan transaksi ritel modern yang dirancang untuk efisiensi operasional toko. Aplikasi ini menggabungkan antarmuka **Material Design 3** yang intuitif dengan sistem distribusi otomatis.

---

## ✨ Fitur Utama

* **📦 Manajemen Produk:** Pantau stok, harga, dan kategori produk secara real-time.
* **📊 Transaksi Cepat:** Antarmuka transaksi yang efisien dengan fitur *plus-minus* jumlah beli.
* **🚀 Auto-Update System:** Integrasi dengan **GitHub Actions** untuk distribusi pembaruan aplikasi otomatis melalui file manifest `update.json`.
* **🎨 Material 3 UI:** Desain modern menggunakan komponen terbaru seperti *MaterialCardView*, *BottomSheets*, dan *Tonal Buttons*.
* **🔍 Tracking System:** Riwayat transaksi dan status pembayaran (Sudah/Belum Bayar) yang terorganisir.

---

## 🛠️ Arsitektur & Teknologi

* **Language:** Java (Android SDK)
* **UI Framework:** Google Material Design 3 (M3)
* **Automation:** GitHub Actions (CI/CD) untuk Sinkronisasi Manifest.
* **Data Format:** JSON & YAML untuk konfigurasi remote.
* **Libraries:** Glide (Image Loading), Material Components, dsb.

---

## 🚀 Mekanisme CI/CD Manifest

Project ini menggunakan otomatisasi tingkat lanjut untuk mengelola siklus rilis:
1.  **Trigger:** Setiap kali rilis baru dipublikasikan di GitHub.
2.  **Action:** GitHub Actions secara otomatis menghitung ukuran APK, mengambil link download terbaru, dan menyusun changelog.
3.  **Output:** Memperbarui file `update.json` di cabang utama tanpa merusak urutan `versionCode` manual.



---

## 📸 Tampilan Aplikasi

| Dashboard | Buat Transaksi | Produk |
| :---: | :---: | :---: |
| ![Dashboard](https://raw.githubusercontent.com/dedemardiyanto10/A3Mart-App/refs/heads/main/screenshots/Screenshot_20260219-230739.A3Mart.png) | ![Transaksi](https://raw.githubusercontent.com/dedemardiyanto10/A3Mart-App/refs/heads/main/screenshots/Screenshot_20260219-230744.A3Mart.png) | ![Produk](https://raw.githubusercontent.com/dedemardiyanto10/A3Mart-App/refs/heads/main/screenshots/Screenshot_20260219-230756.A3Mart.png) |



---

## 📦 Cara Instalasi

1.  Kunjungi tab [Releases](https://github.com/dedemardiyanto10/A3Mart-App/releases).
2.  Unduh file `.apk` terbaru.
3.  Izinkan instalasi dari sumber tidak dikenal di pengaturan Android lo.
4.  Buka aplikasi dan mulai kelola toko lo!

---

## 🤝 Kontribusi

Ingin membantu pengembangan **A3 Mart**?
1.  **Fork** repository ini.
2.  Buat **Branch** fitur baru (`git checkout -b fitur/FiturKeren`).
3.  **Commit** perubahan lo (`git commit -m 'Add: Fitur Keren'`).
4.  **Push** ke branch (`git push origin fitur/FiturKeren`).
5.  Buka **Pull Request**.

---

**Developed with ❤️ by [Dede Mardiyanto](https://github.com/dedemardiyanto10)**
