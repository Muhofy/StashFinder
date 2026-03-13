#!/usr/bin/env python3
"""
╔══════════════════════════════════════════════════════════════╗
║           StashFinder Build CLI  —  by Muhofy               ║
╚══════════════════════════════════════════════════════════════╝
  python build.py          → interactive menu
  python build.py dev      → quick dev build
  python build.py beta     → beta build
  python build.py rc       → release candidate
  python build.py stable   → stable release
  python build.py info     → show build info
  python build.py history  → show build history
  python build.py clean    → clean build artifacts
  python build.py reset    → reset build number
"""

import platform
import os
import sys
import json
import shutil
import subprocess
from datetime import datetime
from pathlib import Path

# ─────────────────────────────────────────────────────────────
#  ANSI COLORS
# ─────────────────────────────────────────────────────────────
R  = "\033[38;5;203m"   # red
G  = "\033[38;5;83m"    # green
Y  = "\033[38;5;227m"   # yellow
C  = "\033[38;5;87m"    # cyan
B  = "\033[38;5;69m"    # blue
M  = "\033[38;5;177m"   # magenta
W  = "\033[38;5;255m"   # white
DM = "\033[38;5;242m"   # dark gray
BLD= "\033[1m"          # bold
DIM= "\033[2m"          # dim
RST= "\033[0m"          # reset

# ─────────────────────────────────────────────────────────────
#  PATHS
# ─────────────────────────────────────────────────────────────
ROOT          = Path(__file__).parent
GRADLE_PROPS  = ROOT / "gradle.properties"
BUILD_NUM     = ROOT / "build-number.txt"
BUILD_HISTORY = ROOT / ".build-history.json"
BUILD_DIR     = ROOT / "build" / "libs"
GRADLEW       = ROOT / ("gradlew.bat" if platform.system() == "Windows" else "gradlew")

# ─────────────────────────────────────────────────────────────
#  HELPERS
# ─────────────────────────────────────────────────────────────
def clear():
    os.system("clear")

def read_props() -> dict:
    props = {}
    if not GRADLE_PROPS.exists():
        die("gradle.properties not found!")
    for line in GRADLE_PROPS.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, _, v = line.partition("=")
            props[k.strip()] = v.strip()
    return props

def write_suffix(suffix: str):
    text = GRADLE_PROPS.read_text(encoding="utf-8")
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("version_suffix"):
            lines.append(f"version_suffix={suffix}")
        else:
            lines.append(line)
    GRADLE_PROPS.write_text("\n".join(lines) + "\n", encoding="utf-8")

def get_build_number() -> int:
    if BUILD_NUM.exists():
        return int(BUILD_NUM.read_text().strip())
    return 1

def compute_version(mod_ver: str, suffix: str, build_num: int) -> tuple[str, str]:
    """Returns (full_version, jar_suffix)"""
    s = suffix.lower()
    if s == "stable":
        return mod_ver, ""
    elif s in ("beta", "alpha", "rc"):
        return f"{mod_ver}-{s}{build_num}", f"-{s}{build_num}"
    else:
        return f"{mod_ver}-dev+n{build_num}", f"-dev+n{build_num}"

def get_release_dir(suffix: str) -> str:
    s = suffix.lower()
    mapping = {"stable": "release", "beta": "beta", "alpha": "alpha", "rc": "rc"}
    return mapping.get(s, "dev")

def die(msg: str):
    print(f"\n{R}✖ {msg}{RST}\n")
    sys.exit(1)

def box(lines: list[str], color: str = C, width: int = 56):
    print(f"{color}╔{'═' * width}╗{RST}")
    for line in lines:
        pad = width - len(strip_ansi(line))
        print(f"{color}║{RST} {line}{' ' * (pad - 1)}{color}║{RST}")
    print(f"{color}╚{'═' * width}╝{RST}")

def strip_ansi(s: str) -> str:
    import re
    return re.sub(r'\033\[[0-9;]*m', '', s)

def separator(char="─", width=58, color=DM):
    print(f"{color}{char * width}{RST}")

# ─────────────────────────────────────────────────────────────
#  BUILD HISTORY
# ─────────────────────────────────────────────────────────────
def load_history() -> list:
    if BUILD_HISTORY.exists():
        try:
            return json.loads(BUILD_HISTORY.read_text(encoding="utf-8"))
        except Exception:
            return []
    return []

def save_history(entry: dict):
    history = load_history()
    history.insert(0, entry)
    history = history[:50]  # max 50 entry
    BUILD_HISTORY.write_text(json.dumps(history, indent=2, ensure_ascii=False), encoding="utf-8")

# ─────────────────────────────────────────────────────────────
#  HEADER
# ─────────────────────────────────────────────────────────────
def print_header():
    props      = read_props()
    build_num  = get_build_number()
    mod_ver    = props.get("mod_version", "?")
    mc_ver     = props.get("minecraft_version", "?")
    suffix     = props.get("version_suffix", "")
    full_ver, _= compute_version(mod_ver, suffix, build_num)
    rel_dir    = get_release_dir(suffix)

    suffix_display = {
        "stable": f"{G}● STABLE{RST}",
        "beta":   f"{Y}◐ BETA{RST}",
        "alpha":  f"{M}◑ ALPHA{RST}",
        "rc":     f"{B}◉ RC{RST}",
        "":       f"{DM}○ DEV{RST}",
    }.get(suffix.lower(), f"{DM}○ DEV{RST}")

    print()
    box([
        f"{BLD}{W}StashFinder  Build CLI{RST}",
        "",
        f"  {DM}Mod Version {RST}{W}{full_ver}{RST}",
        f"  {DM}MC          {RST}{W}{mc_ver}{RST}",
        f"  {DM}Build #     {RST}{C}n{build_num}{RST}",
        f"  {DM}Channel     {RST}{suffix_display}",
        f"  {DM}Output      {RST}{DM}build/libs/{rel_dir}/{RST}",
    ], color=C)
    print()

# ─────────────────────────────────────────────────────────────
#  RUN GRADLE
# ─────────────────────────────────────────────────────────────
def run_gradle(suffix: str) -> bool:
    write_suffix(suffix)

    props      = read_props()
    build_num  = get_build_number()
    mod_ver    = props.get("mod_version", "?")
    mc_ver     = props.get("minecraft_version", "?")
    full_ver, jar_sfx = compute_version(mod_ver, suffix, build_num)
    rel_dir    = get_release_dir(suffix)
    jar_name   = f"stashfinder-{mc_ver}-{mod_ver}{jar_sfx}.jar"

    print(f"\n{C}▶ Running gradle build...{RST}\n")
    separator()

    cmd = [str(GRADLEW), "build", "--stacktrace"] if "--debug" in sys.argv \
          else [str(GRADLEW), "build"]

    start = datetime.now()
    result = subprocess.run(cmd, cwd=ROOT)
    elapsed = (datetime.now() - start).total_seconds()

    separator()

    success = result.returncode == 0

    if success:
        jar_path = BUILD_DIR / rel_dir / jar_name
        size_kb  = round(jar_path.stat().st_size / 1024, 1) if jar_path.exists() else 0

        print(f"\n{G}✔ Build successful!{RST}  {DM}({elapsed:.1f}s){RST}\n")
        box([
            f"  {DM}JAR      {RST}{W}{jar_name}{RST}",
            f"  {DM}Size     {RST}{W}{size_kb} KB{RST}",
            f"  {DM}Output   {RST}{G}build/libs/{rel_dir}/{RST}",
            f"  {DM}Version  {RST}{C}{full_ver}{RST}",
            f"  {DM}Built at {RST}{DM}{datetime.now().strftime('%H:%M:%S')}{RST}",
        ], color=G)

        prompt_git_tag(full_ver)

        save_history({
            "version":   full_ver,
            "suffix":    suffix or "dev",
            "build_num": build_num,
            "jar":       jar_name,
            "size_kb":   size_kb,
            "elapsed":   elapsed,
            "timestamp": datetime.now().isoformat(),
            "success":   True,
        })
    else:
        print(f"\n{R}✖ Build failed!{RST}  {DM}({elapsed:.1f}s){RST}")
        print(f"{DM}  Check output above for errors.{RST}\n")

        save_history({
            "version":   full_ver,
            "suffix":    suffix or "dev",
            "build_num": build_num,
            "jar":       jar_name,
            "elapsed":   elapsed,
            "timestamp": datetime.now().isoformat(),
            "success":   False,
        })

    return success

# ─────────────────────────────────────────────────────────────
#  COMMANDS
# ─────────────────────────────────────────────────────────────
def prompt_git_tag(version: str):
    tag_name = f"v{version}"

    # ── Git repo mu? ──────────────────────────────────────────
    is_git = subprocess.run(["git", "rev-parse", "--git-dir"],
                            cwd=ROOT, capture_output=True).returncode == 0

    if is_git:
        _tag_git(tag_name)
    else:
        _tag_changelog(tag_name, version)


def _tag_git(tag_name: str):
    # Tag zaten var mı?
    existing = subprocess.run(["git", "tag", "-l", tag_name],
                              cwd=ROOT, capture_output=True, text=True)
    if existing.stdout.strip() == tag_name:
        print(f"\n  {DM}Tag {tag_name} already exists, skipping.{RST}")
        return

    print(f"\n  {Y}Git tag oluşturmak ister misin?{RST}  {DM}({tag_name}){RST}")
    if input(f"  {W}[y/N]:{RST} ").strip().lower() != "y":
        print(f"  {DM}Tag oluşturulmadı.{RST}")
        return

    print(f"  {DM}Tag mesajı (Release {tag_name}):{RST} ", end="")
    msg = input().strip() or f"Release {tag_name}"

    r = subprocess.run(["git", "tag", "-a", tag_name, "-m", msg], cwd=ROOT)
    if r.returncode == 0:
        print(f"  {G}✔ Tag oluşturuldu:{RST} {W}{tag_name}{RST}  {DM}\"{msg}\"{RST}")
        print(f"  {DM}Push için: git push origin {tag_name}{RST}")
    else:
        print(f"  {R}✖ Tag oluşturulamadı.{RST}")


def _tag_changelog(tag_name: str, version: str):
    changelog = ROOT / "CHANGELOG.md"

    print(f"\n  {Y}CHANGELOG.md'ye tag eklemek ister misin?{RST}  {DM}({tag_name}){RST}")
    if input(f"  {W}[y/N]:{RST} ").strip().lower() != "y":
        print(f"  {DM}Tag eklenmedi.{RST}")
        return

    print(f"  {DM}Açıklama (boş bırakabilirsin):{RST} ", end="")
    msg = input().strip() or "No description."

    date_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    entry    = f"\n## {tag_name}  —  {date_str}\n{msg}\n"

    # Dosya yoksa başlıkla oluştur
    if not changelog.exists():
        changelog.write_text(f"# CHANGELOG\n{entry}", encoding="utf-8")
    else:
        # En üste ekle (başlığın hemen altına)
        content = changelog.read_text(encoding="utf-8")
        if content.startswith("# CHANGELOG"):
            content = content.replace("# CHANGELOG\n", f"# CHANGELOG\n{entry}", 1)
        else:
            content = entry + content
        changelog.write_text(content, encoding="utf-8")

    print(f"  {G}✔ CHANGELOG.md güncellendi:{RST} {W}{tag_name}{RST}  {DM}\"{msg}\"{RST}")
    print(f"  {DM}Dosya: {changelog}{RST}")



    print_header()
    props     = read_props()
    build_num = get_build_number()
    history   = load_history()

    print(f"{DM}  gradle.properties{RST}")
    separator(width=40)
    for k, v in props.items():
        if not k.startswith("#"):
            print(f"  {DM}{k:<25}{RST}{W}{v}{RST}")
    print()

    if history:
        last = history[0]
        status = f"{G}✔ success{RST}" if last.get("success") else f"{R}✖ failed{RST}"
        print(f"{DM}  Last build:{RST} {W}{last['version']}{RST}  {status}  {DM}{last['timestamp'][:16]}{RST}")
    print()

def cmd_history():
    clear()
    print_header()
    history = load_history()

    if not history:
        print(f"  {DM}No build history yet.{RST}\n")
        return

    print(f"  {BLD}{W}Build History{RST}  {DM}(last {len(history)} builds){RST}\n")
    separator()

    for i, entry in enumerate(history):
        status  = f"{G}✔{RST}" if entry.get("success") else f"{R}✖{RST}"
        suffix  = entry.get("suffix", "dev")
        ver     = entry.get("version", "?")
        ts      = entry.get("timestamp", "")[:16]
        elapsed = entry.get("elapsed", 0)
        size    = entry.get("size_kb", 0)

        channel_color = {
            "stable": G, "beta": Y, "alpha": M, "rc": B, "dev": DM
        }.get(suffix, DM)

        line  = f"  {status} {W}{ver:<30}{RST}"
        line += f" {channel_color}{suffix:<8}{RST}"
        line += f" {DM}{size:>6} KB  {elapsed:>5.1f}s  {ts}{RST}"
        print(line)

        if i < len(history) - 1:
            separator(char="·", width=58)

    print()

def cmd_clean():
    print(f"\n{Y}⚠ Clean build artifacts{RST}\n")

    targets = [
        (ROOT / "build", "build/  (all compiled output)"),
        (ROOT / ".gradle", ".gradle/  (gradle cache)"),
    ]

    for path, label in targets:
        exists = path.exists()
        mark   = f"{W}●{RST}" if exists else f"{DM}○{RST}"
        print(f"  {mark}  {label}")

    print()
    confirm = input(f"  {Y}Delete all? (y/N):{RST} ").strip().lower()
    if confirm != "y":
        print(f"\n  {DM}Cancelled.{RST}\n")
        return

    for path, label in targets:
        if path.exists():
            shutil.rmtree(path)
            print(f"  {G}✔ Removed:{RST} {label}")

    print(f"\n{G}✔ Clean complete.{RST}\n")

def cmd_reset():
    current = get_build_number()
    print(f"\n  Current build number: {C}n{current}{RST}")
    confirm = input(f"  {Y}Reset to 1? (y/N):{RST} ").strip().lower()
    if confirm == "y":
        BUILD_NUM.write_text("1")
        print(f"\n  {G}✔ Build number reset to n1.{RST}\n")
    else:
        print(f"\n  {DM}Cancelled.{RST}\n")

def cmd_build(suffix: str):
    clear()
    print_header()
    run_gradle(suffix)
    print()
    input(f"  {DM}Press Enter to continue...{RST}")

# ─────────────────────────────────────────────────────────────
#  INTERACTIVE MENU
# ─────────────────────────────────────────────────────────────
MENU_ITEMS = [
    ("1", "dev",    f"{DM}○  Dev Build      {RST}", "Quick dev build, auto-increments n"),
    ("2", "beta",   f"{Y}◐  Beta Build     {RST}", "Beta release, e.g. 1.0.0-beta5"),
    ("3", "alpha",  f"{M}◑  Alpha Build    {RST}", "Alpha release, e.g. 1.0.0-alpha5"),
    ("4", "rc",     f"{B}◉  Release Cand.  {RST}", "RC build, e.g. 1.0.0-rc5"),
    ("5", "stable", f"{G}●  Stable Release {RST}", "Clean release JAR, e.g. 1.0.0"),
    ("─", None,     None, None),
    ("i", "info",   f"{C}   Info           {RST}", "Show current build info"),
    ("h", "hist",   f"{C}   History        {RST}", "Show build history"),
    ("c", "clean",  f"{Y}   Clean          {RST}", "Remove build artifacts"),
    ("r", "reset",  f"{R}   Reset n        {RST}", "Reset build number to 1"),
    ("q", "quit",   f"{DM}   Quit           {RST}", "Exit"),
]

def interactive_menu():
    while True:
        clear()
        print_header()
        print(f"  {BLD}{W}Select build type:{RST}\n")

        for key, _, label, desc in MENU_ITEMS:
            if key == "─":
                separator(char="·", width=44, color=DM)
                continue
            print(f"  {C}[{key}]{RST}  {label}  {DM}{desc}{RST}")

        print()
        choice = input(f"  {W}>{RST} ").strip().lower()
        print()

        if choice in ("q", "quit", "exit"):
            print(f"  {DM}Bye!{RST}\n")
            break
        elif choice in ("1", "dev", ""):
            cmd_build("")
        elif choice in ("2", "beta"):
            cmd_build("beta")
        elif choice in ("3", "alpha"):
            cmd_build("alpha")
        elif choice in ("4", "rc"):
            cmd_build("rc")
        elif choice in ("5", "stable"):
            # Stable için ekstra onay iste
            clear()
            print_header()
            print(f"  {G}● Stable Release Build{RST}\n")
            print(f"  {Y}⚠ This will produce the final release JAR.{RST}")
            print(f"  {DM}  Output: build/libs/release/{RST}\n")
            confirm = input(f"  {Y}Confirm stable build? (y/N):{RST} ").strip().lower()
            if confirm == "y":
                run_gradle("stable")
                print()
                input(f"  {DM}Press Enter to continue...{RST}")
            else:
                print(f"\n  {DM}Cancelled.{RST}")
                import time; time.sleep(1)
        elif choice in ("i", "info"):
            clear()
            cmd_info()
            input(f"  {DM}Press Enter to continue...{RST}")
        elif choice in ("h", "hist", "history"):
            cmd_history()
            input(f"  {DM}Press Enter to continue...{RST}")
        elif choice in ("c", "clean"):
            cmd_clean()
            input(f"  {DM}Press Enter to continue...{RST}")
        elif choice in ("r", "reset"):
            cmd_reset()
            input(f"  {DM}Press Enter to continue...{RST}")
        else:
            print(f"  {R}Unknown option: {choice}{RST}")
            import time; time.sleep(1)

# ─────────────────────────────────────────────────────────────
#  ENTRY POINT
# ─────────────────────────────────────────────────────────────
def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]

    if not args:
        interactive_menu()
        return

    cmd = args[0].lower()

    match cmd:
        case "dev":     cmd_build("")
        case "beta":    cmd_build("beta")
        case "alpha":   cmd_build("alpha")
        case "rc":      cmd_build("rc")
        case "stable":  cmd_build("stable")
        case "info":    cmd_info()
        case "history": cmd_history()
        case "clean":   cmd_clean()
        case "reset":   cmd_reset()
        case "help" | "--help" | "-h":
            print(__doc__)
        case _:
            print(f"\n{R}Unknown command: {cmd}{RST}")
            print(f"{DM}Run without arguments for interactive menu.{RST}\n")
            sys.exit(1)

if __name__ == "__main__":
    main()