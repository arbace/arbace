# PY6-core: a ParamSpec's variances (test_type_params' test_paramspec_02)
from typing import ParamSpec


def func1[**A]():
    return A


a = func1()
print(type(a).__name__, a.__infer_variance__, a.__covariant__, a.__contravariant__)
P = ParamSpec("P", covariant=True)
print(P.__infer_variance__, P.__covariant__, P.__contravariant__)
