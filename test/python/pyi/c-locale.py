# PY6c: _locale over musl's model and the locale module over it
import _locale as L, locale

print(L.CHAR_MAX in (127, 255), sorted(n for n in dir(L) if not n.startswith("__")) == sorted(n for n in dir(L) if not n.startswith("__")))
for n in ["C", "POSIX", "C.UTF-8", "fr_FR", "en_US.UTF-8", "de_DE.ISO8859-1", "a" * 30, "a/b"]:
    print(repr(n), L.setlocale(L.LC_ALL, n), L.setlocale(L.LC_CTYPE), L.nl_langinfo(L.CODESET))
L.setlocale(L.LC_ALL, "C")
L.setlocale(L.LC_NUMERIC, "fr_FR")
print(L.setlocale(L.LC_ALL), L.setlocale(L.LC_NUMERIC))
L.setlocale(L.LC_ALL, L.setlocale(L.LC_ALL))
print(L.setlocale(L.LC_ALL))
conv = L.localeconv()
print(sorted((k, v) for k, v in conv.items() if v != L.CHAR_MAX))
print(L.strcoll("a", "b"), L.strcoll("b", "a"), L.strcoll("\xe9", "f"), L.strcoll("ab", "a"), L.strxfrm("abc"))
for a in [(99,), (L.LC_ALL, 5), (-1, None), (99, "C")]:
    try:
        print(L.setlocale(*a))
    except Exception as e:
        print(type(e).__name__, e)
for f, a in [(L.nl_langinfo, (9999,)), (L.strcoll, ("a\0", "b")), (L.setlocale, (L.LC_ALL, "a\0"))]:
    try:
        f(*a)
    except Exception as e:
        print(type(e).__name__, e)
print([L.nl_langinfo(getattr(L, k)) for k in sorted(dir(L)) if k.isupper() and not k.startswith("LC_") and k != "CHAR_MAX"])
print(L.gettext("hi"), L.dgettext(None, "a"), L.dcgettext("d", "b", L.LC_MESSAGES), L.textdomain(None), L.textdomain("foo"),
      L.textdomain(None), L.bindtextdomain("x", "/nonexist"), L.bind_textdomain_codeset("x", None))
print(L.Error, issubclass(L.Error, ValueError), locale.Error is L.Error)
L.setlocale(L.LC_ALL, "en_US.UTF-8")
print(locale.getlocale(), locale.format_string("%.2f", 1234.5, grouping=True), locale.atof("1.5"), locale.str(2.5),
      locale.currency(1.5) if conv["int_frac_digits"] != L.CHAR_MAX else "no currency", locale.normalize("fr_FR"))
L.setlocale(L.LC_ALL, "C")
print(locale.getlocale(), L.getencoding())
