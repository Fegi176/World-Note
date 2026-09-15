"""Android 14 native UI smoke at 360 dp / 200% text and in landscape; emulator only."""
import re
import subprocess
import struct
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

serial = sys.argv[1] if len(sys.argv) > 1 else ""
if not re.fullmatch(r"emulator-\d+", serial):
    raise SystemExit("A dedicated emulator is required; personal phones are never resized.")
package = "app.nodenote.worldbuilder.debug"
root = Path(__file__).resolve().parents[1]

def adb(*args, binary=False):
    out = subprocess.run(["adb", "-s", serial, *args], capture_output=True, check=True).stdout
    return out if binary else out.decode("utf-8", errors="replace").strip()

def nodes():
    adb("shell", "rm", "-f", "/sdcard/Download/nodenote-matrix.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/Download/nodenote-matrix.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/Download/nodenote-matrix.xml")).iter("node"))

def find(label):
    for _ in range(12):
        n = next((n for n in nodes() if n.get("text") == label), None)
        if n is not None:
            return n
        time.sleep(.4)
    raise AssertionError(f"Missing native UI label: {label}")

def tap(label):
    n = find(label)
    a,b,c,d = map(int,re.findall(r"\d+",n.get("bounds")))
    assert c > a and d > b, (label,n.attrib)
    adb("shell","input","tap",str((a+c)//2),str((b+d)//2))

def shot(name):
    target = root / "artifacts/screenshots" / name
    data = adb("exec-out","screencap","-p",binary=True)
    width, height = struct.unpack(">II", data[16:24])
    assert (width > height) == ("landscape" in name), (name, width, height)
    target.write_bytes(data)
    print(f"{target.name}: {width}x{height}", flush=True)

def restart():
    adb("shell","am","force-stop",package)
    adb("shell","am","start","-W","-n",package+"/app.nodenote.worldbuilder.MainActivity")
    time.sleep(2)

assert adb("shell","getprop","ro.build.version.sdk") == "34"
old_font = adb("shell","settings","get","system","font_scale")
old_accel = adb("shell","settings","get","system","accelerometer_rotation")
old_rotation = adb("shell","settings","get","system","user_rotation")
old_density = adb("shell","wm","density")
old_fixed = adb("shell","wm","fixed-to-user-rotation")
override = re.search(r"Override density: (\d+)", old_density)
try:
    adb("shell","wm","fixed-to-user-rotation","enabled")
    adb("shell","wm","user-rotation","lock","0")
    # Density 480 on the dedicated 1080px AVD yields 360dp in portrait.
    adb("shell","wm","density","480")
    adb("shell","settings","put","system","font_scale","2.0")
    restart()
    labels = {n.get("text") for n in nodes()}
    if "Done" in labels: tap("Done")
    labels = {n.get("text") for n in nodes()}
    if "Library" not in labels and "Boards" in labels: tap("Boards")
    tap("Library")
    find("Lore library")
    shot("12-library-360dp-font200-api34.png")
    tap("Timeline")
    find("The Epoch Codex")
    shot("13-timeline-360dp-font200-api34.png")
    tap("Search")
    find("Search your world")
    shot("14-search-360dp-font200-api34.png")
    adb("shell","settings","put","system","font_scale","1.0")
    adb("shell","wm","density",override.group(1) if override else "reset")
    time.sleep(2)
    restart()
    tap("Boards")
    tap("The living world")
    find("Outline")
    if not any(n.get("content-desc", "").startswith("Board canvas.") for n in nodes()):
        tap("Outline")
    adb("shell","wm","user-rotation","lock","1")
    time.sleep(1)
    find("Outline")
    shot("15-board-landscape-api34.png")
    tap("Outline")
    find("The Ash Physician")
    shot("16-outline-landscape-api34.png")
    print("PASS: 360dp/200% navigation and landscape board/outline, using actual native controls.", flush=True)
finally:
    adb("shell","wm","fixed-to-user-rotation",old_fixed)
    for key,value in [("font_scale",old_font),("accelerometer_rotation",old_accel),("user_rotation",old_rotation)]:
        adb("shell","settings","delete" if value == "null" else "put","system",key,*([] if value == "null" else [value]))
    adb("shell","wm","density",override.group(1) if override else "reset")
