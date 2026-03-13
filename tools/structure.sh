#!/bin/bash

# Çıktı dosyasının adı (Zaman damgalı)
OUTPUT_FILE="pr_structure_$(date +'%Y%m%d_%H%M%S').txt"

# Taranacak dizin (Varsayılan: İçinde bulunulan klasör)
TARGET_DIR="."

# Hariç tutulacak dizinler (Regex formatında)
IGNORE_PATTERN="node_modules\|\.git\|__pycache__\|\.venv"

echo "Proje yapısı çıkartılıyor: $TARGET_DIR"
echo "Hedef dosya: $OUTPUT_FILE"

# Başlık ekle
echo "Proje Kök Dizini: $(pwd)" > "$OUTPUT_FILE"
echo "Oluşturulma Tarihi: $(date +'%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_FILE"
echo "----------------------------------------" >> "$OUTPUT_FILE"

# Ağaç yapısını oluştur ve dosyaya yaz
# -print ile listele, grep ile istenmeyenleri ele, sed ile ağaç görünümü ver
find "$TARGET_DIR" -not -path '*/.*' | grep -v "$IGNORE_PATTERN" | sed -e 's/[^-][^\/]*\// |/g' -e 's/|/  /g' -e 's/  \([^ ]\)/|-- \1/' >> "$OUTPUT_FILE"

echo "İşlem tamamlandı!"

