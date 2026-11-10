#!/usr/bin/env python3
from pathlib import Path
import os,subprocess,urllib.parse,zipfile
root=Path(__file__).resolve().parents[1];os.chdir(root)
for path in Path('.toolchain/test-libs').glob('*.aar'):
    with zipfile.ZipFile(path) as archive:
        if 'classes.jar' in archive.namelist():path.with_suffix('.jar').write_bytes(archive.read('classes.jar'))
jars=sorted(str(p) for p in Path('.toolchain/test-libs').glob('*.jar'))
if not jars:raise SystemExit('Resolve tests/runtime-pom.xml dependencies into .toolchain/test-libs first (README).')
cp=os.pathsep.join(['build/manual/classes','.toolchain/platform/android-36/android.jar']+jars)
subprocess.run(['java','-jar','.toolchain/ecj.jar','-1.8','-warn:none','-encoding','UTF-8','-classpath',cp,'-d','build/native-tests','tests/NativeRuntimeTest.java'],check=True)
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
flags=['--add-opens=java.base/jdk.internal.access=ALL-UNNAMED','--add-opens=java.base/java.io=ALL-UNNAMED','-Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2','-Dmaven.repo.local='+str(Path('.toolchain/m2').resolve()),'-Djava.awt.headless=true']
if proxy.hostname:flags+=['-Dhttps.proxyHost='+proxy.hostname,'-Dhttps.proxyPort='+str(proxy.port)]
result=subprocess.run(['java','-Xmx2g']+flags+['-cp',os.pathsep.join(['build/native-tests','build/manual/classes']+jars),'org.junit.runner.JUnitCore','mx.hecagus.luma.NativeRuntimeTest'])
raise SystemExit(result.returncode)
