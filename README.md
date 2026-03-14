# StashFinder

[![Modrinth](https://img.shields.io/modrinth/v/stashfinder?label=Modrinth&logo=modrinth)](https://modrinth.com/mod/stashfinder)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)](https://fabricmc.net)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![CurseForge](https://img.shields.io/badge/CurseForge-v1.0.1-f16436?style=for-the-badge&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/stashfinder)
---

## 📋 Table of Contents

- [Requirements](#-requirements)
- [Installation](#-installation)
- [Keybinds](#-keybinds)
- [Configuration](#-configuration)
- [Data Storage](#-data-storage)
- [Building from Source](#-building-from-source)
- [Contributing](#-contributing)
- [Bug Reports & Feature Requests](#-bug-reports--feature-requests)
- [License](#-license)

---

## ✅ Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | `1.21.11` |
| Fabric Loader | `>=0.18.1` |
| Fabric API | `0.141.1+1.21.11` |
| Java | `21` |

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest JAR from [Modrinth](https://modrinth.com/mod/stashfinder) or [GitHub Releases](https://github.com/muhofy/stashfinder/releases)
4. Drop the JAR into `.minecraft/mods/`
5. Launch Minecraft

> Client-side only. No server installation required.

---

## ⌨️ Keybinds

| Key | Action | Category |
|-----|--------|----------|
| `F` | Open Search Overlay | ChestMemory |
| `G` | Open Chest Records | ChestMemory |

Keybinds can be remapped in **Options → Controls → Key Binds → StashFinder**.

---

## ⚙️ Configuration

**Location:** `.minecraft/config/stashfinder/config.json`

Created automatically with defaults on first launch.

```json
{
  "toastEnabled": true,
  "toastPosition": "BOTTOM_RIGHT",
  "compassPosition": "TOP_LEFT"
}
```

| Key | Type | Default | Options |
|-----|------|---------|---------|
| `toastEnabled` | `boolean` | `true` | `true` / `false` |
| `toastPosition` | `string` | `BOTTOM_RIGHT` | `TOP_LEFT` `TOP_RIGHT` `BOTTOM_LEFT` `BOTTOM_RIGHT` |
| `compassPosition` | `string` | `TOP_LEFT` | `TOP_LEFT` `TOP_RIGHT` |

---

## 💾 Data Storage

Chest data is stored per-world at:
```
.minecraft/config/stashfinder/<world_name>/chests.json
```

The file is created automatically. If the file becomes corrupted, a backup is saved as `chests.json.bak` and a fresh file is created.

**Data format:**
```json
{
  "chests": [
    {
      "id": "uuid",
      "customName": "My Chest",
      "x": -204, "y": 64, "z": 337,
      "dimension": "minecraft:overworld",
      "lastUpdated": "2026-03-14T00:05:00",
      "isDouble": false,
      "items": [
        { "slot": 0, "itemId": "minecraft:diamond", "count": 32, "displayName": "Diamond" }
      ]
    }
  ],
  "searchHistory": ["diamond", "torch"]
}
```

---

## 🔧 Building from Source

### Requirements
- JDK 21
- Python 3.10+ (optional, for the build CLI)

### Build CLI
```bash
./build.py          # interactive menu
./build.py dev      # dev build     → build/libs/dev/
./build.py beta     # beta build    → build/libs/beta/
./build.py rc       # release cand. → build/libs/rc/
./build.py stable   # release       → build/libs/release/
./build.py info     # build info
./build.py history  # build history
./build.py libs     # clean build/libs/
./build.py mods     # clean mod from mods folder
./build.py clean    # full clean
./build.py reset    # reset build number
```

### Gradle
```bash
./gradlew build
```

### Version Suffix (`gradle.properties`)

| `version_suffix` | Output |
|-----------------|--------|
| *(empty)* | `stashfinder-1.21.11-1.0.0-dev+n5.jar` |
| `beta` | `stashfinder-1.21.11-1.0.0-beta5.jar` |
| `alpha` | `stashfinder-1.21.11-1.0.0-alpha5.jar` |
| `rc` | `stashfinder-1.21.11-1.0.0-rc5.jar` |
| `stable` | `stashfinder-1.21.11-1.0.0.jar` |

### Project Structure
```
src/main/java/com/muhofy/chestmemory/
├── ChestMemoryMod.java
├── config/
│   └── ChestMemoryConfig.java
├── data/
│   ├── ChestItem.java
│   ├── ChestRecord.java
│   └── ChestStorage.java
├── handler/
│   ├── ChestOpenHandler.java
│   ├── KeyHandler.java
│   └── WorldEventHandler.java
└── ui/
    ├── ChestMemoryHud.java
    ├── ChestRecordsScreen.java
    ├── IconManager.java
    └── SearchOverlay.java
```

---

## 🤝 Contributing

Pull requests are welcome. For major changes, open an issue first.

1. Fork the repo
2. Create a branch: `git checkout -b feat/my-feature`
3. Commit: `git commit -m "feat: add my feature"`
4. Push and open a Pull Request

Please keep commits atomic and follow the existing code style.

---

## 🐛 Bug Reports & Feature Requests

- [Report a bug](https://github.com/muhofy/stashfinder/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/muhofy/stashfinder/issues/new?template=feature_request.yml)
- [Ask a question](https://github.com/muhofy/stashfinder/issues/new?template=question.yml)

Please search [existing issues](https://github.com/muhofy/stashfinder/issues) before opening a new one.

---

## 📄 License

This project is licensed under the [GPL-3.0-only License](LICENSE).

```
GPL-3.0-only License — Copyright (c) 2026 Muhofy
```

You are free to use, modify and distribute this project as long as you include the original license notice.