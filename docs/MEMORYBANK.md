# MEMORY_BANK.md — ChestMemory Mod

## 🧠 Proje Bağlamı
Bu dosya, yeni bir Claude sohbetinde projeyi sıfırdan anlatmak zorunda kalmamak için hazırlanmıştır.
Tüm kararlar, kapsam, mimari ve detaylar burada mevcuttur. Kodlamaya başlamadan önce bu dosyayı ve TODO.md'yi oku.

---

## 👤 Geliştirici
- **İsim:** Muhammed (Muhofy)
- **Rol:** Mod sahibi ve baş geliştirici
- **Asistan Rolü:** Senior Software Developer + Architect olarak davran

---

## 📌 Proje Özeti
**ChestMemory**, Minecraft Survival oyuncularının dünyalarındaki tüm sandıkları otomatik olarak indexlemesini, daha sonra item ismiyle arama yapmasını ve sandığın konumuna yön göstergesi ile ulaşmasını sağlayan, tamamen **client-side**, **lightweight** bir Fabric modudur.

---

## 🎯 Çözdüğü Problem
Survival oyuncuları ilerledikçe onlarca sandık biriktirir. Zamanla:
- Hangi sandıkta ne olduğunu unuturlar
- "Diamond nerede koymuştum?" diye tüm sandıkları tek tek açarlar
- Organizasyon için zaman kaybederler

Mevcut çözümler yetersiz:
- **ItemScroller / Inventory Profiles Next** → sadece inventory sort eder, sandık aramaz
- **JEI / REI** → sadece recipe gösterir, dünya içi sandık içeriğini bilmez
- **Chest labels modları** → sadece isim koyar, arama yapmaz

**ChestMemory** bu boşluğu doldurur: sandıkları otomatik indexle, istediğin itemi saniyeler içinde bul.

---

## 🛠️ Teknik Stack
| Parametre | Değer |
|---|---|
| Minecraft Versiyonu | 1.21.11 |
| Mod Loader | Fabric |
| Dil | Java |
| Minimum Fabric API | Fabric API for 1.21.4 |
| Hedef Platform | Client-side only (server gerekmez) |
| JSON Kütüphanesi | Gson (Minecraft içinde mevcut) |

---

## ✅ Kesinleşmiş Özellikler

### 1. Otomatik Index Sistemi
- Oyuncu bir sandığı açtığında mod içeriği **otomatik ve sessizce** kaydeder
- Oyuncu hiçbir şey yapmak zorunda değildir — tamamen pasif
- Sandık her açıldığında içerik **güncellenir** (eski snapshot'ın üzerine yazar)
- Index sadece **Chest** için geçerlidir — Barrel, Shulker Box, Ender Chest kapsam dışıdır
- Sandık kırılırsa veya içerik değişirse bir sonraki açılışta otomatik güncellenir

### 2. Toast Bildirimi (Sandık Açılınca)
- Sandık ilk kez açıldığında: `📦 Sandık indexlendi • X item kaydedildi`
- Daha önce açılmış sandık güncellendiğinde: `📦 Sandık güncellendi • X item`
- Toast **2 saniye** görünür, kaybolur
- Toast pozisyonu config'den ayarlanabilir (sağ alt varsayılan)

### 3. Arama Overlay (F tuşu)
- `F` tuşuna basınca ekranın **ortasında kompakt** bir arama kutusu açılır
- Oyuncu item ismini yazar — **canlı filtreleme** (Enter gerekmez)
- Sonuçlar şunu gösterir:
  - Item ikonu
  - Adet (örn. `32x`)
  - Sandık koordinatı (örn. `-204, 64, 337`)
  - Yön ve mesafe (örn. `127 blok ↗`)
- Sonuca **tıklanınca veya Enter'a basılınca** HUD yön göstergesi aktif olur
- `Escape` ile kapanır
- Aktif boyuttaki sandıklar önce sıralanır, farklı boyuttakiler grileşir

### 4. Sandık Kayıtları Ekranı (Fullscreen)
- Ayrı bir keybind ile açılır (varsayılan: `G`)
- **Sol panel:** Tüm indexlenmiş sandıkların listesi
  - Sandık adı (varsayılan: "Sandık #1", #2, #3...)
  - Koordinat + boyut
  - Mesafe (aktif boyuttaysa blok cinsinden, değilse "Farklı boyut")
  - ✏️ butonu ile sandığa özel isim verilebilir
- **Sağ panel:** Seçilen sandığın detayı
  - 27 slotluk Minecraft native slot grid görünümü
  - Dolu slotlarda item ikonu + adet
  - Boş slotlar gri görünür
  - Koordinat + boyut + son güncelleme zamanı
- **Alt butonlar:**
  - `📍 Yön Göster` → HUD yön göstergesi aktif olur, ekran kapanır
  - `🔄 Güncelle` → "Sandığı tekrar aç ki güncellensin" uyarısı gösterir
  - `🗑 Kaydı Sil` → Onay sorar, sandık kaydını siler

### 5. Sandık İsimlendirme
- Sandık Kayıtları ekranında her sandığın yanında ✏️ butonu bulunur
- Tıklayınca sandık adı alanı **editable** olur
- Enter ile kaydedilir, Escape ile iptal edilir
- İsim verilmemişse otomatik numara gösterilir: `Sandık #1`
- İsim verilmişse özel isim gösterilir: `Silah Sandığı`
- İsim max **32 karakter**

### 6. HUD Yön Göstergesi
- Arama overlay veya Sandık Kayıtları'ndan bir sandık seçilince aktif olur
- Ekranın sol üst köşesinde küçük bir **pusula widget'ı** gösterilir
- İçerik: `📦 [Sandık adı] • [X] blok` + dönen ok
- Hedefe **5 blok** yaklaşılınca: `✅ Hedefe ulaştın!` toast'ı çıkar, gösterge kaybolur
- Başka bir sandık seçilirse önceki hedef iptal olur
- `Escape` veya tekrar aynı tuşa basınca iptal edilebilir

### 7. Veri Saklama
- Her dünya için ayrı JSON dosyası
- Dosya yolu: `.minecraft/config/chestmemory/<world_name>/chests.json`
- JSON yapısı:
```json
{
  "chests": [
    {
      "id": "uuid",
      "customName": "Silah Sandığı",
      "x": -204,
      "y": 64,
      "z": 337,
      "dimension": "minecraft:overworld",
      "lastUpdated": "2025-03-07T12:00:00",
      "items": [
        {
          "slot": 0,
          "itemId": "minecraft:diamond",
          "count": 32,
          "displayName": "Diamond"
        }
      ]
    }
  ]
}
```

### 8. Config Sistemi
- Config dosyası: `.minecraft/config/chestmemory/config.json`
- Config alanları:
  - `toastEnabled` (boolean, varsayılan: true)
  - `toastPosition` (enum: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, varsayılan: BOTTOM_RIGHT)
  - `searchKey` (varsayılan: F)
  - `recordsKey` (varsayılan: G)
  - `compassPosition` (enum: TOP_LEFT, TOP_RIGHT, varsayılan: TOP_LEFT)

---

## ❌ Kapsam Dışı (Yapılmayacaklar)
- Barrel, Shulker Box, Ender Chest desteği — v1'de yok, v2'de eklenebilir
- Server-side senkronizasyon (multiplayer'da paylaşım)
- Harita entegrasyonu (Xaero's, Journey Map)
- Sandık içeriğini uzaktan değiştirme
- Çoklu dünya/profil desteği (her dünya zaten ayrı dosya)
- Sandık kategorileri / renk sistemi

---

## 🎨 UI/UX Prensipleri
- **Vanilla hissettirmeli** — Minecraft'ın slot görünümü, font, buton stili kullanılmalı
- **Pasif ve non-intrusive** — oyuncu hiçbir şey yapmadan sistem çalışmalı
- **Hız öncelikli** — arama overlay kompakt ve hızlı olmalı, fullscreen değil
- **Sandık Kayıtları fullscreen** — detaylı browse için geniş alan
- **Klavye öncelikli** — mouse gerektirmeden her şey yapılabilmeli

---

## 📁 Planlanan Proje Yapısı
```
chestmemory/
├── src/main/java/com/muhofy/chestmemory/
│   ├── ChestMemoryMod.java           # Mod entry point
│   ├── config/
│   │   └── ChestMemoryConfig.java    # Config yönetimi
│   ├── data/
│   │   ├── ChestRecord.java          # Sandık model
│   │   ├── ChestItem.java            # Item model
│   │   └── ChestStorage.java         # JSON okuma/yazma
│   ├── handler/
│   │   ├── KeyHandler.java           # Keybind handler
│   │   └── ChestOpenHandler.java     # Sandık açılma event'i
│   └── ui/
│       ├── SearchOverlay.java        # Arama overlay
│       ├── ChestRecordsScreen.java   # Fullscreen kayıtlar
│       └── ChestMemoryHud.java       # Toast + pusula widget
├── src/main/resources/
│   ├── fabric.mod.json
│   └── assets/chestmemory/
│       └── lang/
│           ├── en_us.json
│           └── tr_tr.json
└── build.gradle
```

---

## 🚀 Yayın Planı
- **Platform:** Modrinth (öncelikli), CurseForge (ikincil)
- **Lisans:** MIT
- **Versiyon:** 1.0.0 ile başla
- **Mod ID:** `chestmemory`
- **Maven Group:** `com.muhofy`

---

## 💬 Geliştirici Notları
- Bu mod AutoNote'taki `NoteHud.java` toast sistemi burada referans alınabilir
- Sandık açılma event'i için `ScreenEvents` veya `ClientPlayNetworking` kullanılabilir — araştırılmalı
- Item ID'leri Minecraft Registry'den alınmalı, hardcode edilmemeli
- Bu dosya her yeni sohbette Claude'a verilmelidir