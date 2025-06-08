### Panduan Penggunaan Program Search Engine

## Penting
Enviroment Untuk Development:
JAVA 21

Library yang digunakan:
OpenNLP 2.5.4

Detail lengkapnya bisa cek di (`app/build.gradle.kts`)

Untuk Kerapihan output, disarankan untuk menggunakan Netbeans IDE untuk menjalankan program ini
Namun program tetap bisa dijalankan dengan comandline

## Cara Menjalankan Program

**1. Build Program:**

Buka terminal, navigasikan ke direktori root proyek, lalu jalankan perintah berikut:

*   macOS atau Linux:
    ```bash
    ./gradlew build
    ```
*   Windows:
    ```bash
    gradlew.bat build
    ```
Perintah ini akan mengkompilasi source code, dan menyiapkan aplikasi untuk dijalankan.

**2. Run Program:**

Setelah program berhasil di build, Program dapat dijalankan dengan perintah berikut dari direktori root proyek:

*   macOS atau Linux:
    ```bash
    ./gradlew run
    ```
*   Windows:
    ```bash
    gradlew.bat run
    ```
Perintah ini akan menjalankan metode `main` pada kelas `App.java`, yang akan menampilkan menu interaktif di konsol Anda.

**3. Menu Aplikasi:**

Setelah program berjalan, Anda akan melihat menu berikut di konsol:

```
MAIN MENU
1. Load and preprocess documents
2. View Indexed Document Information
3. Search with Custom Query
4. Batch Search with Query File
5. Switch Retrieval Model (Current: [model saat ini])
6. Evaluate Search Engine
7. Exit
Enter your choice (1-7):
```

Berikut penjelasan untuk setiap opsi menu:

*   **1. Load and Preprocess Documents:**
    *   **Fungsi:** Opsi ini akan memuat dokumen-dokumen dari direktori sumber yang telah ditentukan (terdapat di `app/src/main/resources/documents/`).
    *   Setelah dimuat, dokumen-dokumen tersebut akan melalui proses pre processing (seperti tokenisasi, penghapusan stop-word, stemming, dll.) dan kemudian diindeks untuk pencarian.
    * Proses ini sudah otomatis dilakukan ketika menjalankan program, namun jika ingin tahu detilnya bisa memilih menu ini

*   **2. Display Indexed Document Info:**
    *   **Fungsi:** Menampilkan informasi ringkas mengenai beberapa dokumen pertama yang telah berhasil diindeks.
    *   Informasi yang ditampilkan biasanya meliputi ID dokumen, judul, panjang (jumlah term), dan jumlah term unik.
    * Proses ini sudah otomatis dilakukan ketika menjalankan program, namun jika ingin tahu detilnya bisa memilih menu ini

*   **3. Execute Search Query:**
    *   **Fungsi:** Memungkinkan user untuk memasukkan sebuah query pencarian.
    *   Program akan memproses query user, mencocokkannya dengan indeks dokumen menggunakan model pencarian yang aktif, dan kemudian menampilkan daftar dokumen yang paling relevan beserta skornya.
    *   User juga akan diminta untuk memasukkan jumlah hasil teratas (top K) yang ingin ditampilkan.

*   **4. Batch Search with Query File:**
    *   **Fungsi:** Memungkinkan user untuk melakukan batch processing untuk banyak query.
    *   Program akan memproses query dari `app/src/main/resources/queries`, mencocokkannya dengan indeks dokumen menggunakan model pencarian yang aktif, dan kemudian menampilkan daftar dokumen yang paling relevan beserta skornya.
    *   User juga akan diminta untuk memasukkan jumlah hasil teratas (top K) yang ingin ditampilkan.

*   **5. Switch Retrieval Model (Current: [Nama Model Saat Ini]):**
    *   **Fungsi:** Mengganti model pencarian yang digunakan oleh sistem.
    *   Program ini kemungkinan mendukung beberapa model Information Retrieval (IR) seperti VSM (Vector Space Model) atau BM25. Opsi ini memungkinkan Anda beralih di antara model-model tersebut untuk membandingkan hasil pencarian atau performa.
    *   Nama model yang sedang aktif akan ditampilkan di menu.
    *   Tulis `BM25` atau `VSM`

*   **6. Evaluate Search Engine:**
    *   **Fungsi:** Melakukan evaluasi performa search engine secara otomatis.
    *   Opsi ini akan menjalankan serangkaian query standar (biasanya dimuat dari direktori `app/src/main/resources/queries/`) terhadap koleksi dokumen yang telah diindeks.
    *   Hasil pencarian akan dibandingkan dengan *ground truth* (data relevansi yang sebenarnya, diambil dari file qrels  `app/src/main/resources/qrels/qrels.txt`).
    *   Metrik evaluasi umum seperti Precision@K, Recall@K, F1-Score@K, dan MAP (Mean Average Precision) akan dihitung dan ditampilkan. Anda akan diminta memasukkan nilai K untuk evaluasi.

*   **7. Exit:**
    *   **Fungsi:** Keluar dari aplikasi Search Engine.

**Catatan Penting:**

*   Jika Anda melakukan perubahan pada kode sumber, jangan lupa untuk menjalankan `./gradlew build` lagi sebelum menjalankan aplikasi untuk memastikan perubahan tersebut diterapkan.
