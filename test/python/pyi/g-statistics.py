# _statistics (PY6g): _normal_dist_inv_cdf as Modules/_statisticsmodule.c, results as bits; NormalDist uses it
import _statistics, statistics, struct
h = lambda x: struct.pack('<d', x).hex()
ps = [1e-300, 1e-20, 1e-10, 0.001, 0.02425, 0.075, 0.1, 0.3, 0.5, 0.5 + 1e-12, 0.7, 0.9, 0.925, 0.97575, 0.999, 1 - 1e-12, 1 - 1e-16]
print([h(_statistics._normal_dist_inv_cdf(p, 0.0, 1.0)) for p in ps])
print([h(_statistics._normal_dist_inv_cdf(p, 3.5, 2.25)) for p in ps])
for a in ((0.0, 0, 1), (1.0, 0, 1), (-1, 0, 1), (float('nan'), 0, 1), ('x', 0, 1), (0.5, 0)):
    try: print(_statistics._normal_dist_inv_cdf(*a))
    except Exception as e: print(type(e).__name__, e)
print(statistics.NormalDist(10, 2).inv_cdf(0.25), statistics.NormalDist().quantiles(5))
