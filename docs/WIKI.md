# StashFinder — Wiki

> Complete user guide for StashFinder.

---

## 📋 Table of Contents

- [Getting Started](#-getting-started)
- [Search Overlay](#-search-overlay)
- [Chest Records](#-chest-records)
- [Navigation](#-navigation)
- [Search History](#-search-history)
- [Renaming Chests](#-renaming-chests)
- [Deleting Records](#-deleting-records)
- [Dimensions](#-dimensions)
- [Toast Notifications](#-toast-notifications)
- [Tips & Tricks](#-tips--tricks)
- [FAQ](#-faq)

---

## 🚀 Getting Started

StashFinder works completely automatically. You don't need to configure anything or press any buttons to start using it.

**Just play the game normally:**

1. Walk up to a chest and open it
2. StashFinder silently records its contents in the background
3. Close the chest
4. That's it — the chest is now indexed

From this point on, you can search for any item that was inside that chest at any time, even if you are far away from it or in a different dimension.

> The more chests you open, the more useful StashFinder becomes. It gets smarter as you play.

---

## 🔍 Search Overlay

**Open with:** `F`

The search overlay lets you find any item across all your indexed chests instantly.

### How to search

1. Press `F` — a compact search box appears in the center of the screen
2. Start typing the item name — results appear as you type, no need to press Enter
3. Use `↑` `↓` arrow keys to move between results
4. Press `Enter` or click a result to start navigating to that chest
5. Press `Esc` to close without doing anything

### Reading the results

Each result row shows:

```
[Item Icon]  Item Name                    32x
             Chest Name  X, Y, Z         127m ↗
```

| Field | Meaning |
|-------|---------|
| Item icon | Visual icon of the matched item |
| Item name | Display name of the item |
| Chest name | Custom name or auto-assigned number |
| Coordinates | X, Y, Z position of the chest |
| Count | Total quantity of that item in this chest |
| Distance | How far you are from the chest right now |
| Arrow | Rough direction toward the chest |

### Result ordering

- Chests in your **current dimension** appear first, sorted by distance (nearest first)
- Chests in **other dimensions** appear below, dimmed, with "Different dimension" instead of distance

---

## 🗂️ Chest Records

**Open with:** `G`

The chest records screen lets you browse all your indexed chests in detail.

### Left panel — Chest list

- Shows every chest you have ever opened, with name, coordinates and distance
- Scroll with the **mouse wheel** or `↑` `↓` keys
- Click any chest to select it and see its contents on the right
- Chests from other dimensions are dimmed and show "Different dimension"

### Right panel — Chest contents

When you select a chest on the left, the right panel shows:

- **Chest name** and **dimension badge** (Overworld / Nether / The End)
- **Full slot grid** — every slot exactly as it was when you last opened the chest
  - Hover over any item to see its name and quantity
- **Last updated** timestamp — when you last opened this chest

### Action buttons

| Button | What it does |
|--------|-------------|
| `Navigate` | Activates the navigation HUD and closes the screen |
| `Delete` | Removes this chest record after confirmation |

---

## 🧭 Navigation

After selecting a chest from the search overlay or chest records screen, a **compass strip** appears at the top-center of your screen.

### Reading the compass

```
        ◀  W    NW    N    NE  📦  E    SE  ▼
              Weapon Chest  127m
```

| Element | Meaning |
|---------|---------|
| Cardinal letters | The direction you are currently facing |
| `N` in red | Always marks North so you can orient yourself |
| Cyan marker `│` | Points toward your target chest |
| `◀` or `▶` at edge | Target is off-screen in that direction |
| `▼` at center | Marks the direction you are currently facing |
| Info bar below | Shows chest name and distance in meters |

### Arriving at the chest

When you come within **5 blocks** of the target chest:
- A "Destination Reached!" toast appears
- The compass strip disappears automatically

### Cancelling navigation

- Select a different chest from search or records — navigation switches to the new target
- Press `Esc` on the records screen to cancel

---

## 🕐 Search History

When you open the search overlay (`F`) without typing anything, your **recent searches** are shown.

- Up to **8 recent searches** are stored per world
- Click any entry or press `Enter` to re-apply that search instantly
- Press `✕` next to an entry to remove it
- History is saved between sessions — it persists when you close and reopen the game

---

## ✏️ Renaming Chests

You can give any chest a custom name to help you remember what is inside.

1. Open Chest Records with `G`
2. Find the chest you want to rename in the left panel
3. Click the `✏` button next to it
4. Type a new name (max 32 characters)
5. Press `Enter` to save, or `Esc` to cancel

Once renamed, the custom name appears everywhere — in search results, the compass HUD and the records list.

If you clear the name and save, the chest reverts to its auto-assigned number (e.g. `Chest #3`).

---

## 🗑️ Deleting Records

If a chest no longer exists in your world, you can remove its record manually.

1. Open Chest Records with `G`
2. Select the chest you want to remove
3. Click `Delete`
4. Confirm with `Yes, Delete`

> **Note:** Deleting a record only removes it from StashFinder's data. It does not affect the actual chest in the world. If you open the chest again, it will be re-indexed automatically.

---

## 🌍 Dimensions

StashFinder tracks which dimension each chest belongs to.

- **Overworld, Nether and The End** are all supported
- Each chest's dimension is shown as a badge in the records screen
- In search results, chests from your current dimension appear first
- Chests from other dimensions are visible but dimmed — you can still navigate to them if needed
- When you travel between dimensions, distances update automatically to reflect your current position

---

## 🔔 Toast Notifications

A small notification briefly appears on screen when a chest is indexed.

| Notification | Color | When it appears |
|-------------|-------|----------------|
| Chest Indexed | 🟢 Green | First time you open a chest |
| Chest Updated | 🔵 Blue | You opened a previously indexed chest |
| Destination Reached! | 🟢 Green | You arrived within 5 blocks of your target |

If the same notification appears multiple times quickly, it stacks with a `×N` counter instead of showing duplicates.

You can turn toasts off or change their position in the [config file](README.md#-configuration).

---

## 💡 Tips & Tricks

**Build a habit of opening chests before storing items.**
StashFinder only indexes chests you have personally opened. If you place a new chest and put items in it without opening it first, it won't be indexed. Simply open it once and it's recorded.

**Use custom names for important chests.**
Generic names like `Chest #14` are hard to remember. Rename your most important chests — `Diamonds`, `Tools`, `Food` — so they stand out in search results and the compass HUD.

**Search is fuzzy — short queries work well.**
You don't need to type the full item name. Typing `dia` will match `Diamond`, `Diamond Sword`, `Diamond Pickaxe` etc.

**The records screen is great for auditing.**
Use `G` to browse all your chests and spot which ones haven't been updated in a while. If the slot grid looks outdated, go open that chest again to refresh it.

**Navigation works across dimensions.**
You can set a navigation target for a Nether chest while standing in the Overworld. The compass will still point in the right direction — useful when planning a trip.

---

## ❓ FAQ

**Q: I opened a chest but nothing was recorded.**  
A: Make sure you are running the correct version of StashFinder and Fabric API. Also check that only one StashFinder JAR is in your mods folder — multiple versions can cause conflicts.

**Q: The item count shown in search results is wrong.**  
A: StashFinder records contents at the moment you close a chest. If you added or removed items since then, the data will be outdated until you open the chest again.

**Q: Can other players see my chest data?**  
A: No. All data is stored locally on your computer and is never sent to a server or shared with other players.

**Q: Does it work on servers?**  
A: Yes. StashFinder is client-side only and works on any server without any server-side installation.

**Q: Can I use StashFinder with other mods?**  
A: Yes. StashFinder does not modify any game mechanics and should be compatible with most mods. If you encounter a conflict, please [open an issue](https://github.com/muhofy/stashfinder/issues/new?template=bug_report.yml).

**Q: My chest was broken / moved. How do I clean up the old record?**  
A: Open Chest Records (`G`), select the outdated chest and click `Delete`.

**Q: Does StashFinder support Barrels, Shulker Boxes or Ender Chests?**  
A: Not in the current version. Support for these containers is planned for a future release.