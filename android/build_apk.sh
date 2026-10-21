#!/bin/bash
set -e
cd "$(dirname "$0")"; SDK=../sdk; rm -rf out; mkdir -p out/classes
cp "$1" assets/www/index.html
$SDK/bin/aapt2 compile --dir res -o out/res.zip
$SDK/bin/aapt2 link -o out/base.apk -I $SDK/android.jar --manifest AndroidManifest.xml -A assets out/res.zip --min-sdk-version 24 --target-sdk-version 30 -0 html
javac -source 8 -target 8 -nowarn -encoding UTF-8 -bootclasspath $SDK/android.jar -cp $SDK/android.jar -d out/classes src/kr/stecbest/wbe/*.java 2>&1 | grep -v warning || true
(cd out/classes && jar cf ../classes.jar .)
$SDK/dex-tools-v2.4/d2j-jar2dex.sh -f -o out/classes.dex out/classes.jar >/dev/null
(cd out && zip -q -0 base.apk classes.dex)
python3 - <<'PY'
import zipfile
src=zipfile.ZipFile('out/base.apk'); dst=open('out/aligned.apk','wb'); entries=[]
import struct, zlib
# simple zipalign: rewrite with stored entries padded to 4 bytes via extra field
out=zipfile.ZipFile('out/aligned.apk','w')
for i in src.infolist():
    data=src.read(i.filename)
    zi=zipfile.ZipInfo(i.filename, date_time=(2026,10,21,0,0,0)); zi.compress_type=i.compress_type
    if zi.compress_type==zipfile.ZIP_STORED:
        off=dst.tell() if False else out.fp.tell()
        hdr=30+len(zi.filename.encode())
        pad=(4-((off+hdr)%4))%4
        zi.extra=b'\0'*pad
    out.writestr(zi, data)
out.close()
PY
java -cp $SDK/bundletool.jar:signer Sign out/aligned.apk out/final.apk "$(cat keystore-password.txt)" wbe-release.p12
