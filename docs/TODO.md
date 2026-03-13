# TODO.md — ChestMemory Mod
> Fabric 1.21.11 | Geliştirme Sırası: Yukarıdan Aşağıya

---

## 🔧 PHASE 0 — Proje Kurulumu
- [x] Fabric mod template'ini indir (generator.fabricmc.net)
- [x] `build.gradle` yapılandır:
  - [x] `mod_id = chestmemory`
  - [x] `maven_group = com.muhofy`
  - [x] `mod_version = 1.0.0`
- [x] `fabric.mod.json` doldur:
  - [x] id: `chestmemory`
  - [x] name: `ChestMemory`
  - [x] description, authors, license alanları
  - [x] `client` environment işaretle
- [x] Minecraft 1.21.11 + Fabric API dependency ekle
- [x] Proje paket yapısını oluştur: `config`, `data`, `handler`, `ui`
- [x] `ChestMemoryMod.java` entry point yaz (`onInitializeClient`)
- [x] Projeyi build et, boş mod olarak oyunda çalıştığını doğrula

---

## 📦 PHASE 1 — Veri Katmanı (Data Layer)

### ChestItem Model
- [x] `ChestItem.java` oluştur
  - [x] Alanlar: `slot (int)`, `itemId (String)`, `count (int)`, `displayName (String)`
  - [x] Constructor, getter/setter
  - [x] `toString()` override (debug için)

### ChestRecord Model
- [x] `ChestRecord.java` oluştur
  - [x] Alanlar: `id (UUID)`, `customName (String)`, `x, y, z (int)`, `dimension (String)`, `lastUpdated (LocalDateTime)`, `items (List<ChestItem>)`
  - [x] Constructor, getter/setter
  - [x] `distanceTo(x, z)` yardımcı metod (2D mesafe, Y göz ardı)
  - [x] `getDisplayName()` → customName varsa onu, yoksa "Sandık #[otoIndex]" döndür
  - [x] `isInDimension(dimension)` yardımcı metod
  - [x] `toString()` override

### ChestStorage
- [x] `ChestStorage.java` oluştur (singleton pattern)
- [x] Dünya adına göre dosya yolu: `.minecraft/config/chestmemory/<world_name>/chests.json`
- [x] `loadChests(worldName)` → JSON oku, `List<ChestRecord>` döndür
- [x] `saveChests(worldName, List<ChestRecord>)` → JSON yaz
- [x] `addOrUpdateChest(ChestRecord)` → koordinata göre mevcut kaydı güncelle veya yeni ekle
- [x] `deleteChest(UUID)` → sil, kaydet
- [x] `renameChest(UUID, String)` → isim güncelle, kaydet
- [x] `getChestAt(x, y, z, dimension)` → koordinata göre kayıt bul
- [x] `getChestsByDimension(dimension)` → boyut filtreli liste
- [x] `searchItems(query)` → tüm sandıklarda item ismine göre ara, `List<SearchResult>` döndür
- [x] `SearchResult` inner class: `chestRecord`, `item`, `distance` alanları
- [x] Dosya yoksa otomatik oluştur (dizin dahil)
- [x] Bozuk JSON için graceful error handling + `.bak` yedekleme
- [x] Dünya değiştiğinde storage'ı sıfırla ve yeni dünyayı yükle

---

## ⚙️ PHASE 2 — Config Sistemi

- [x] `ChestMemoryConfig.java` oluştur (singleton pattern)
- [x] Config alanları:
  - [x] `toastEnabled` (boolean, varsayılan: true)
  - [x] `toastPosition` (enum: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, varsayılan: BOTTOM_RIGHT)
  - [x] `compassPosition` (enum: TOP_LEFT, TOP_RIGHT, varsayılan: TOP_LEFT)
- [x] Config dosya yolu: `.minecraft/config/chestmemory/config.json`
- [x] `loadConfig()` metodu
- [x] `saveConfig()` metodu
- [x] Config yoksa default değerlerle oluştur

---

## ⌨️ PHASE 3 — Keybind Sistemi

- [x] `KeyHandler.java` oluştur
- [x] Arama overlay keybind: `F` (kategori: "ChestMemory")
- [x] Sandık Kayıtları keybind: `G` (kategori: "ChestMemory")
- [x] `ClientTickEvents.END_CLIENT_TICK` ile key press dinle
- [x] `F` → `SearchOverlay` aç
- [x] `G` → `ChestRecordsScreen` aç
- [x] Oyuncu UI içindeyken keybind çalışmasın (screen check)
- [x] Keybindleri `fabric.mod.json`'a kaydet

---

## 🪤 PHASE 4 — Sandık Açılma Handler

- [x] `ChestOpenHandler.java` oluştur
- [x] Sandık açılma event'ini yakala (Fabric API `ScreenEvents` veya uygun event)
- [x] Açılan ekran `GenericContainerScreen` veya `3x9 container` mı kontrol et
- [x] Sandığın dünya koordinatını al (BlockEntity üzerinden)
- [x] Aktif boyutu al (`client.world.getRegistryKey()`)
- [x] Sandık içeriğini oku: tüm slotları iterate et
- [x] Her slot için `ChestItem` oluştur (itemId, count, displayName)
- [x] `ChestStorage.addOrUpdateChest()` çağır
- [x] Toast tetikle:
  - [x] Yeni sandıksa: `📦 Sandık indexlendi • X item kaydedildi`
  - [x] Güncellemeyse: `📦 Sandık güncellendi • X item`
- [x] `toastEnabled = false` ise toast gösterme
- [x] Boş sandıkları da kaydet (items boş liste)

---

## 🖥️ PHASE 5 — UI Ekranları

### SearchOverlay (Arama)
- [x] `SearchOverlay.java` oluştur (`Screen` extend)
- [x] Arka plan: yarı saydam siyah overlay (tüm ekran), ortada kompakt kutu
- [x] Arama kutusu boyutu: ~420px genişlik
- [x] `TextFieldWidget` ekle — placeholder: "Item ara..."
- [x] Her karakter girişinde `ChestStorage.searchItems()` çağır (canlı filtreleme)
- [x] Sonuç listesi (max 6 satır göster, scroll yok):
  - [x] Her satır: item ikonu + `Xx` adet + koordinat + yön + mesafe
  - [x] Aktif boyuttaki sonuçlar üstte, farklı boyuttakiler altta ve grileştirilmiş
  - [x] Mesafeye göre sırala (yakından uzağa)
- [x] Klavye navigasyonu: `↑↓` ile satır seç
- [x] Seçili satır highlight (yeşil sol border)
- [x] `Enter` veya tıklama → yön göstergeyi aktif et, overlay kapat
- [x] `Escape` → kapat
- [x] Sonuç yoksa: "Hiçbir sandıkta bulunamadı." mesajı
- [x] Alt kısımda kısayol ipuçları: `↑↓ Seç` `Enter Yön göster` `Esc Kapat`

### ChestRecordsScreen (Sandık Kayıtları - Fullscreen)
- [x] `ChestRecordsScreen.java` oluştur (`Screen` extend)
- [x] **Sol panel** (220px genişlik, sabit):
  - [x] Başlık: `📦 SANDIKLAR (X)`
  - [x] Boyut filtre butonları: Tümü | Overworld | Nether | End
  - [x] Sandık listesi (scroll destekli):
    - [x] Her satır: sandık adı, koordinat, mesafe veya "Farklı boyut"
    - [x] Seçili sandık highlight (yeşil sol border)
    - [x] Her satırda ✏️ rename butonu
  - [x] Liste boşsa: "Henüz sandık açılmadı."
- [x] **Sağ panel** (kalan alan):
  - [x] Başlık: `[Sandık Adı] — İçerik` + sağda koordinat tag'i
  - [x] **27 slotluk grid** (Minecraft native slot görünümü):
    - [x] 3 satır x 9 sütun
    - [x] Dolu slotlar: item ikonu + sağ alt köşede adet
    - [x] Boş slotlar: gri, boş
    - [x] Slot hover: item adını tooltip göster
  - [x] Son güncelleme zamanı (küçük, alt kısımda)
  - [x] Alt butonlar:
    - [x] `📍 Yön Göster` → HUD yön göstergesi aktif et, ekranı kapat
    - [x] `🗑 Kaydı Sil` → inline confirm overlay göster
- [x] **Rename işlevi:**
  - [x] ✏️ butonuna tıklanınca sandık adı `TextFieldWidget`'a dönüşür
  - [x] Enter → `ChestStorage.renameChest()` çağır, kaydet
  - [x] Escape → iptal
  - [x] Max 32 karakter
- [x] **Silme onay overlay:**
  - [x] "Bu sandık kaydını silmek istediğine emin misin?"
  - [x] "Evet" / "Hayır" butonları
- [x] `Escape` ile ekranı kapat

---

## 🎨 PHASE 6 — HUD Overlay

- [x] `ChestMemoryHud.java` oluştur (`HudRenderCallback` implement)
- [x] **Toast sistemi:**
  - [x] Toast kuyruğu: `Queue<Toast>`
  - [x] Her toast 2 saniye görünür
  - [x] Slide-in animasyonu
  - [x] `config.toastPosition`'a göre köşeye yerleştir
  - [x] Toast tipleri: SUCCESS (yeşil), INFO (mavi)
- [x] **Pusula widget (yön göstergesi):**
  - [x] Aktif hedef: `ChestRecord activeTarget`
  - [x] `config.compassPosition`'a göre köşeye yerleştir
  - [x] İçerik: `📦 [Sandık adı] • [X] blok` + dönen ok
  - [x] Hedefe **5 blok** kalınca: `✅ Hedefe ulaştın!` toast + widget kaybolur
  - [x] `setTarget(ChestRecord)` → yeni hedef atar
  - [x] `clearTarget()` → hedefi temizler

---

## 🌍 PHASE 7 — Lokalizasyon

- [x] `assets/chestmemory/lang/en_us.json` oluştur
- [x] `assets/chestmemory/lang/tr_tr.json` oluştur
- [x] Key listesi:
  - [x] `chestmemory.toast.indexed`
  - [x] `chestmemory.toast.updated`
  - [x] `chestmemory.toast.arrived`
  - [x] `chestmemory.screen.search.placeholder`
  - [x] `chestmemory.screen.search.empty`
  - [x] `chestmemory.screen.records.title`
  - [x] `chestmemory.screen.records.empty`
  - [x] `chestmemory.screen.records.different_dimension`
  - [x] `chestmemory.screen.records.btn.navigate`
  - [x] `chestmemory.screen.records.btn.delete`
  - [x] `chestmemory.screen.records.confirm_delete`
  - [x] `chestmemory.key.search`
  - [x] `chestmemory.key.records`

---

## 🧪 PHASE 8 — Test

- [ ] Yeni dünya oluştur, sandık aç → JSON dosyası oluştu mu?
- [ ] Toast çıkıyor mu? (ilk açılış "indexlendi", sonraki "güncellendi")
- [ ] Sandık içeriği değişince tekrar aç → JSON güncellendi mi?
- [ ] Arama overlay'i aç → `F` tuşu çalışıyor mu?
- [ ] "diamond" ara → doğru sandıklar listelendi mi?
- [ ] Sonuca tıkla → yön göstergesi aktif oldu mu?
- [ ] Hedefe ulaş → toast çıktı mı, widget kayboldu mu?
- [ ] Sandık Kayıtları aç → `G` tuşu çalışıyor mu?
- [ ] Sol panelde tüm sandıklar listelendi mi?
- [ ] Sağ panelde slot grid doğru görünüyor mu?
- [ ] Rename çalışıyor mu? (✏️ butonu, Enter ile kaydet)
- [ ] Silme onay çalışıyor mu? JSON'dan silindi mi?
- [ ] Nether'a geç → Overworld sandıkları "Farklı boyut" gösteriyor mu?
- [ ] Farklı dünyaya geç → sandıklar karışmıyor mu?
- [ ] Bozuk JSON ile mod açılırsa crash olmuyor mu?
- [ ] 50+ sandık ekle → performans sorun çıkıyor mu?
- [ ] Config sil → default değerlerle devam ediyor mu?

---

## 📦 PHASE 9 — Yayın Hazırlığı

- [x] `README.md` yaz (İngilizce, Modrinth için):
  - [x] Mod ne yapar (kısa açıklama)
  - [x] Nasıl kullanılır (keybindler: F, G)
  - [x] Ekran görüntüleri
  - [x] Config açıklaması
- [x] Modrinth için banner görseli hazırla (1280x640)
- [x] Ekran görüntüleri çek (SearchOverlay, ChestRecordsScreen, Toast, Pusula)
- [x] `build.gradle`'da versiyon `1.0.0` doğrula
- [x] `gradle build` ile JAR üret
- [x] JAR'ı Minecraft'ta son kez test et
- [x] Modrinth'e yükle (Fabric, 1.21.4, client-side only)
- [] CurseForge'a yükle

---

## 🔮 PHASE 10 — Gelecek Versiyonlar (v1.1+)
> Şu an yapılmayacak, ileride eklenebilir

- [ ] Barrel + Shulker Box + Ender Chest desteği
- [ ] Sandık kategorileri / renk etiketleri
- [ ] Xaero's Minimap entegrasyonu (sandıkları haritada göster)
- [ ] Export/Import (JSON paylaşımı)
- [ ] Config ekranı (Mod Menu entegrasyonu)
- [ ] Multiplayer desteği (server-side gerektirir)
- [ ] Sandık içeriği değişince otomatik güncelleme bildirimi
- [ ] `DimensionHelper.java` — tekrar eden dimension metodlarını tek yere topla