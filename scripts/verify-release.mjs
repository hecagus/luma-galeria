import {readFileSync,existsSync,readdirSync,lstatSync} from 'node:fs';
import assert from 'node:assert/strict';
const release=JSON.parse(readFileSync('site/release.json','utf8'));
const html=readFileSync('site/index.html','utf8');
const manifest=readFileSync('app/src/main/AndroidManifest.xml','utf8');
assert.equal(release.version,'1.0.0');assert.equal(release.size,41429);
assert.equal(release.sha256,'187e8ce7789c61f094d48eb75a813ea33dc8a7653d91a6873a5e6bc54b9305df');
assert.match(release.download,/^https:\/\/github\.com\/hecagus\/luma-galeria\/releases\/download\/v1\.0\.0\/Luma-1\.0\.0\.apk$/);
assert.ok(html.includes(release.download)&&html.includes(release.sha256));
assert.ok(!manifest.includes('android.permission.INTERNET'));
for(const name of ['photos','albums','permission'])assert.ok(existsSync(`site/assets/screens/${name}.png`),`Missing screenshot ${name}`);
for(const root of ['site','app','scripts','tests','verificacion']){
  function walk(path){for(const item of readdirSync(path,{withFileTypes:true})){const full=`${path}/${item.name}`;if(item.isDirectory()){if(item.name==='__pycache__')continue;walk(full);}else{assert.ok(!/\.(p12|jks|keystore|pem|key)$|password\.txt|firma-privada\.zip/i.test(item.name),`Private artifact: ${full}`);}}}
  walk(root);
}
assert.ok(!existsSync('site/signing'));
console.log('Luma 1.0.0 landing, screenshots, Android permissions and release metadata verified.');
