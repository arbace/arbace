# complex(re, im) keeps a zero's sign, complex_new's combination (PY6g's REQUEST for PY6-core)
import cmath, struct
def h(z): return struct.pack('<dd', z.real, z.imag).hex()
for a, b in ((0.0, 0.0), (0.0, -0.0), (-0.0, 0.0), (-0.0, -0.0)):
    z = complex(a, b)
    print(h(z), h(cmath.acos(z)), h(cmath.sqrt(z)), h(cmath.sqrt(complex(1 - a, -b))))
print(h(complex(1.0, -0.0)), h(complex(-0.0, -0.0)), h(-complex(0.0, 0.0)))
for args in ((), (1,), (1.5, -0.0), (-0.0, -0.0), ('1-0j',), (-0.0,)):
    print(args, h(complex(*args)))
print(h(complex(imag=-0.0)), h(complex(real=-0.0)))
