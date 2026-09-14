"""Redirect the retired local Phase 4 admin to the current Phase 5 admin."""
from pathlib import Path
import re,subprocess
ROOT=Path(__file__).resolve().parents[1]
legacy=ROOT.parent/'phase-4-admin'/'infra'/'nginx'/'local.conf'
assert legacy.resolve().is_relative_to(ROOT.parent.resolve())
original=legacy.read_text(encoding='utf-8-sig')
backup=ROOT/'contact-legacy-nginx-backup.log'
if not backup.exists():backup.write_text(original,encoding='utf-8')
updated=re.sub(r'location (= /manage|/manage/assets/|/manage/)\s*\{[^}]*\}',lambda m:'location '+m[1]+' {\n            return 302 http://127.0.0.1:8085'+('/manage/' if m[1]=='= /manage' else '$request_uri')+';\n        }',original)
# Stale tabs must not continue saving to the retired database.
if 'Legacy admin retired' not in updated:
 updated=updated.replace('        location /api/ {','        # Legacy admin retired: reopen the current admin before editing.\n        location ^~ /api/v1/admin/ {\n            default_type application/json;\n            return 409 \'{"code":409,"message":"Please reopen http://127.0.0.1:8085/manage/ to edit the current website","data":null}\';\n        }\n\n        location /api/ {')
legacy.write_text(updated,encoding='utf-8')
result=subprocess.run(['docker','exec','yongtuo-phase4-nginx-1','nginx','-t'],capture_output=True)
if result.returncode:
 legacy.write_text(original,encoding='utf-8');raise RuntimeError('Legacy routing validation failed; original restored.')
subprocess.run(['docker','exec','yongtuo-phase4-nginx-1','nginx','-s','reload'],check=True)
print('Legacy admin now redirects to port 8085; stale admin API calls cannot save to the old database.')
