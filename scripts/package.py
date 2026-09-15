"""Package only the genuine Gradle APK and an allowlisted, secret-free source tree."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root=Path(__file__).resolve().parents[1]
art=root/'artifacts'
art.mkdir(exist_ok=True)
apk=root/'app/build/outputs/apk/debug/app-debug.apk'
if not apk.is_file():
    raise SystemExit('No Gradle debug APK. Run verification first; packaging cannot fabricate a build.')
shutil.copy2(apk,art/'World-Note-debug.apk')
allowed_dirs={'app','core','gradle','scripts','docs','.github'}
allowed_root={'README.md','CONTRIBUTING.md','CHANGELOG.md','.gitignore','.gitattributes','settings.gradle.kts','build.gradle.kts','gradle.properties','gradlew','gradlew.bat'}
with zipfile.ZipFile(art/'World-Note-source.zip','w',zipfile.ZIP_DEFLATED) as archive:
    candidates = [root / name for name in allowed_root]
    for directory in sorted(allowed_dirs):
        candidates.extend((root / directory).rglob('*'))
    for path in sorted(candidates):
        if not path.is_file(): continue
        relative=path.relative_to(root)
        if relative.parts[0] not in allowed_dirs and relative.as_posix() not in allowed_root: continue
        if any(part in {'build','.gradle','.kotlin','__pycache__'} for part in relative.parts): continue
        if path.suffix.lower() in {'.jks','.keystore','.apk','.db'} or path.name in {'local.properties','signing.properties'}: continue
        info=zipfile.ZipInfo(relative.as_posix())
        info.external_attr=((0o100755 if path.name in {'gradlew','verify.sh'} else 0o100644)<<16)
        info.compress_type=zipfile.ZIP_DEFLATED
        archive.writestr(info,path.read_bytes())
files=[art/'World-Note-debug.apk',art/'World-Note-source.zip']
(art/'SHA256SUMS').write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n' for p in files),encoding='utf-8')
for path in files: print(path, path.stat().st_size)
