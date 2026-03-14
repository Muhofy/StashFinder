# CHANGELOG

## v1.0.0  —  2026-03-14

### 🎉 Initial Release

StashFinder is a lightweight, client-side Fabric mod that automatically indexes every chest you open and lets you instantly search for any item — then navigate directly to it.

---

### ✨ Features

#### 📦 Automatic Chest Indexing
- Every chest you open is silently indexed in the background — no extra steps needed
- Chest contents are updated automatically on each open
- Supports single and double chests
- Data is stored per-world in `.minecraft/config/stashfinder/<world>/chests.json`

#### 🔍 Search Overlay  `F`
- Press `F` to open a compact search overlay
- Live filtering as you type — no need to press Enter
- Results show item icon, quantity, chest name, coordinates, distance and direction arrow
- Results from the current dimension are prioritized; other dimensions are shown below
- **Search history** — recent searches are saved per-world and shown when the overlay is empty
  - Click a history entry or press Enter to re-apply it
  - Remove individual entries with the `✕` button

#### 🗂 Chest Records Screen  `G`
- Press `G` to open the full chest records screen
- Left panel: scrollable list of all indexed chests with name, coordinates and distance
- Right panel: 27/54 slot grid showing the exact contents of the selected chest
- Rename any chest with the `✏` button (max 32 characters)
- Delete a chest record with confirmation
- Dimension badge shows Overworld / Nether / The End
- Chests from other dimensions are visually dimmed

#### 🧭 Navigation HUD
- Select a chest from search or records to activate the compass strip
- A top-center compass bar shows cardinal directions and a marker pointing toward the target
- Info strip below shows chest name and distance in meters
- Arrow indicator appears on the edge when the target is off-screen
- Auto-clears when you reach within 5 blocks — shows a "Destination Reached!" toast

#### 🔔 Toast Notifications
- `Chest Indexed` toast on first open (green)
- `Chest Updated` toast on subsequent opens (blue)
- Stacks repeated toasts with a `×N` counter
- Toast visibility and position configurable in `config.json`

---

### ⚙️ Configuration
Config file: `.minecraft/config/stashfinder/config.json`

| Key | Default | Description |
|-----|---------|-------------|
| `toastEnabled` | `true` | Show/hide indexing toasts |
| `toastPosition` | `BOTTOM_RIGHT` | Toast corner position |
| `compassPosition` | `TOP_LEFT` | Compass HUD position |

---

### 🌍 Localization
- English (`en_us`)
- Turkish (`tr_tr`)

---

### 📋 Notes
- Client-side only — no server mod required
- Works in singleplayer and multiplayer
- Barrel, Shulker Box and Ender Chest support planned for v1.1