"""Verify a genuine 0.1.0 -> 0.2.0 update without uninstalling or clearing data; emulator only."""
import hashlib
import json
import re
import sqlite3
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

serial = sys.argv[1] if len(sys.argv) > 1 else ""
if not re.fullmatch(r"emulator-\d+", serial):
    raise SystemExit("A dedicated emulator serial is required.")
root = Path(__file__).resolve().parents[1]
package = "app.nodenote.worldbuilder.debug"

def adb(*args, binary=False):
    out = subprocess.run(["adb", "-s", serial, *map(str,args)], capture_output=True, check=True).stdout
    return out if binary else out.decode("utf-8", errors="replace").strip()

def nodes():
    adb("shell","uiautomator","dump","/sdcard/Download/upgrade.xml")
    return list(ET.fromstring(adb("shell","cat","/sdcard/Download/upgrade.xml")).iter("node"))

def find(label):
    for _ in range(15):
        n = next((n for n in nodes() if n.get("text") == label),None)
        if n is not None: return n
        time.sleep(.3)
    raise AssertionError(f"Native control missing: {label}")

def tap(label):
    n=find(label)
    a,b,c,d=map(int,re.findall(r"\d+",n.get("bounds")))
    adb("shell","input","tap",(a+c)//2,(b+d)//2)

def start():
    print(adb("shell","am","start","-W","-n",package+"/app.nodenote.worldbuilder.MainActivity"),flush=True)

def snapshot(label):
    folder=root/".tooling"/"upgrade-proof"/label
    folder.mkdir(parents=True,exist_ok=True)
    names=adb("shell","run-as",package,"ls","databases").split()
    for name in names:
        if name in {"worldbuilder.db","worldbuilder.db-wal","worldbuilder.db-shm"}:
            (folder/name).write_bytes(adb("exec-out","run-as",package,"cat","databases/"+name,binary=True))
    with sqlite3.connect(folder/"worldbuilder.db") as db:
        data={table:sorted(db.execute("SELECT * FROM "+table).fetchall()) for table in ["worlds","records","fields","refs","times"]}
    assets={}
    for name in adb("shell","run-as",package,"ls","files/assets").split():
        assert re.fullmatch(r"[a-fA-F0-9-]{36}",name)
        assets[name]=hashlib.sha256(adb("exec-out","run-as",package,"cat","files/assets/"+name,binary=True)).hexdigest()
    return {"rows":{k:len(v) for k,v in data.items()},"contentSha256":hashlib.sha256(json.dumps(data,sort_keys=True).encode()).hexdigest(),"assets":assets}

assert adb("shell","getprop","ro.build.version.sdk") == "34"
assert ("package:"+package) not in adb("shell","pm","list","packages",package).splitlines(), "Use a fresh dedicated emulator installation after connected tests; no data will be cleared."
old=root/"artifacts/v0.1.0/NodeNote-Worldbuilder-debug.apk"
new=root/"app/build/outputs/apk/debug/app-debug.apk"
print(adb("install",old),flush=True)
start()
tap("Explore sample world")
find("Lore library")
tap("Timeline")
tap("The First Radiance")
find("Saved")
time.sleep(1)
adb("shell","am","force-stop",package)
before=snapshot("before")
print(adb("install","-r",new),flush=True)
start()
find("The First Radiance")
find("Codex")
time.sleep(1)
adb("shell","am","force-stop",package)
after=snapshot("after")
assert before == after, "Upgrade changed authored rows or original assets"
(root/"artifacts/upgrade-api34.json").write_text(json.dumps({"result":"PASS","from":"0.1.0 (2)","to":"0.2.0 (3)","preserved":after},indent=2),encoding="utf-8")
print("PASS: update-in-place preserved every world/record/field/reference/time row and all original image hashes.",flush=True)
start()
