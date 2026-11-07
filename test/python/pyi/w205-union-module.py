# queue 205 (the wheel check's findings): typing.Union[...] (packaging's tuple[Union[int, str], ...]) and instances
# of module's subclasses (six's _LazyModule/_MovedItems)
import types
import typing
from typing import Optional, Union

T = typing.TypeVar("T")
print(Union[int], Union[int, None], Union[int, "str"], Union[int, int], Union[(int, str)], Union[1])
print(Union[int, list[int]], type(Union[int, str]).__name__, Union[T, int], Union[int, Union[str, bytes]])
print(Union[int, str] == (int | str), Optional[int], tuple[Union[int, str], ...])
try:
    Union[()]
except TypeError as e:
    print("TypeError", e)


class Lazy(types.ModuleType):
    """a module whose attributes are made on first access"""

    def __init__(self, name):
        super().__init__(name)
        self.__doc__ = self.__class__.__doc__

    def __dir__(self):
        return ["made"] + sorted(self.__dict__)


class Descr:
    def __get__(self, obj, tp):
        setattr(obj, "made", 42)
        delattr(tp, "made")
        return 42


class Moved(Lazy):
    made = Descr()


m = Moved("pkg.moves")
print(type(m).__name__, isinstance(m, types.ModuleType), m.__name__, m.__doc__, repr(m))
print(m.made, m.made, "made" in m.__dict__, dir(m)[:3], m.__class__ is Moved, m.__spec__)
m.extra = 1
print(m.extra, sorted(k for k in vars(m) if not k.startswith("__")))
try:
    m.missing
except AttributeError as e:
    print("AttributeError", e)
print(repr(types.ModuleType("plain", "doc")), types.ModuleType("plain", "doc").__doc__)
