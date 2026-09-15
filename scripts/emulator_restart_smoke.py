"""Native adb smoke test on a freshly installed dedicated emulator; never clears app data."""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

serial = sys.argv[1] if len(sys.argv) > 1 else ""
if not re.fullmatch(r"emulator-\d+", serial):
    raise SystemExit("Pass a dedicated emulator serial. Physical devices are not accepted.")
package = "app.nodenote.worldbuilder.debug"
component = package + "/app.nodenote.worldbuilder.MainActivity"
root = Path(__file__).resolve().parents[1]

def adb(*args, binary=False):
    result = subprocess.run(["adb", "-s", serial, *args], check=True, capture_output=True)
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/Download/nodenote-smoke.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/Download/nodenote-smoke.xml")).iter("node"))

def find(label, seconds=30):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        try:
            found = next((n for n in nodes() if n.get("text") == label), None)
            if found is not None:
                return found
        except (ET.ParseError, subprocess.CalledProcessError):
            pass
        time.sleep(0.5)
    raise AssertionError(f"Native UI did not show {label!r}")

def tap(label):
    node = find(label)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

print(adb("shell", "am", "start", "-W", "-n", component), flush=True)
initial = nodes()
if any(n.get("text") == "Explore sample world" for n in initial):
    tap("Explore sample world")
    find("Lore library", 45)
    tap("Timeline")
    tap("The First Radiance")
elif not any(n.get("text") == "Codex" for n in initial):
    tap("Timeline")
    tap("The First Radiance")
find("Codex")
find("Saved")
before = adb("shell", "pidof", package)
time.sleep(2)  # allow the acknowledged navigation preference write to settle
adb("shell", "am", "force-stop", package)
print(adb("shell", "am", "start", "-W", "-n", component), flush=True)
find("The Ashen Meridian")
find("The First Radiance")
find("Codex")
after = adb("shell", "pidof", package)
assert before and after and before != after, (before, after)
api = adb("shell", "getprop", "ro.build.version.sdk")
out = root / "artifacts/screenshots" / f"11-force-stop-recovery-emulator-api{api}.png"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_bytes(adb("exec-out", "screencap", "-p", binary=True))
print(f"PASS API {api}: process {before} -> {after}; selected world and open Epoch Codex restored.")
print("This checks a settled force-stop/restart, not interruption during a write.")
tap("Done")
tap("Library")
tap("Filters")
tap("Type: All")
tap("Character")
find("Type: Character")
time.sleep(1)
adb("shell", "am", "force-stop", package)
adb("shell", "am", "start", "-W", "-n", component)
find("Lore library")
find("Type: Character")
tap("Filters")
print("PASS: Library destination, expanded filter panel and Character filter survived a second new process.")
