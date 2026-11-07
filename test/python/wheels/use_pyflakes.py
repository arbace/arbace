# pyflakes (test/python/wheels.edn): its checker over sources given as text, messages through a reporter on stdout
import io
import sys
from pyflakes import api, reporter

SOURCES = {
    "clean.py": "import os\nprint(os.sep)\n",
    "unused.py": "import os, sys\nfrom json import *\ndef f(a):\n    b = 1\n    return undefined_name\n",
    "redef.py": "def g():\n    pass\ndef g():\n    pass\nx = 1\nclass C:\n    def m(self):\n        return x + y\n",
    "syntax.py": "def h(:\n    pass\n",
    "fstr.py": "v = 1\nprint(f'{v}' f'plain')\nprint('%s %s' % (1,))\n",
}
for name, src in SOURCES.items():
    out, err = io.StringIO(), io.StringIO()
    n = api.check(src, name, reporter.Reporter(out, err))
    print("==", name, n)
    sys.stdout.write(out.getvalue())
    sys.stdout.write(err.getvalue())
