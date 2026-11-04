# queue 202 (REQUEST for 202b): sys.set_int_max_str_digits sets the limit int's str and int(str) apply, and
# sys.flags.int_max_str_digits (it was a no-op: test_decimal's PyCoverage sets 7000 and converts a 4,705-digit int)
import sys
print(sys.get_int_max_str_digits(), sys.flags.int_max_str_digits)
x = 1228 ** 1523
try:
    str(x)
except ValueError as e:
    print('ValueError', e)
sys.set_int_max_str_digits(7000)
print(sys.get_int_max_str_digits(), sys.flags.int_max_str_digits, len(str(x)), int('9' * 6000) > 0)
sys.set_int_max_str_digits(maxdigits=640)
for v in (10 ** 700, '1' * 700):
    try:
        print(len(str(v)) if isinstance(v, int) else int(v))
    except ValueError as e:
        print('ValueError', e)
for bad in (639, -1, 1.0, 'x', 2 ** 40):
    try:
        sys.set_int_max_str_digits(bad)
    except (ValueError, TypeError, OverflowError) as e:
        print(type(e).__name__, e)
sys.set_int_max_str_digits(0)
print(sys.get_int_max_str_digits(), len(str(10 ** 5000)))
sys.set_int_max_str_digits(4300)
