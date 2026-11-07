# os.confstr as musl's (PY6g's REQUEST for PY6e): subprocess and platform import
import os
print(sorted(os.confstr_names.items()))
for n in ('CS_GNU_LIBC_VERSION', 'CS_PATH', 'xx', 0, 2, 1.5):
    try: print(repr(os.confstr(n)))
    except Exception as e: print(type(e).__name__, e)
import subprocess, platform
print(subprocess._USE_POSIX_SPAWN)
