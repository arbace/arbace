# xmldiff.py CORPUS [--only NAME] [--dump] [--first N] [--skip N]: each document of CORPUS parsed by pyexpat in
# several configurations, every handler recording its event with the parser's position, and every error's code,
# position and message; one line per document and configuration: NAME CONFIG DIGEST (or, with --dump, the
# events). Run by CPython and by pyi, the outputs compared (queue 202e; tools/expat/check.sh --pyi).
# --ref-pyexpat DIR: pyexpat taken from DIR first (CPython with the tarball's own pyexpat and expat built by gcc).
import os, sys, hashlib
if "--ref-pyexpat" in sys.argv:  # CPython with the tarball's pyexpat built against its own expat 2.9.0
    sys.path.insert(0, sys.argv[sys.argv.index("--ref-pyexpat") + 1])
import pyexpat
from xml.parsers import expat

args = sys.argv[1:]
corpus = args[0]
only = args[args.index("--only") + 1] if "--only" in args else None
dump = "--dump" in args
first = int(args[args.index("--first") + 1]) if "--first" in args else None
skip = int(args[args.index("--skip") + 1]) if "--skip" in args else 0

HANDLERS = ["StartElementHandler", "EndElementHandler", "ProcessingInstructionHandler", "CharacterDataHandler",
            "UnparsedEntityDeclHandler", "NotationDeclHandler", "StartNamespaceDeclHandler",
            "EndNamespaceDeclHandler", "CommentHandler", "StartCdataSectionHandler", "EndCdataSectionHandler",
            "StartDoctypeDeclHandler", "EndDoctypeDeclHandler", "EntityDeclHandler", "XmlDeclHandler",
            "ElementDeclHandler", "AttlistDeclHandler", "SkippedEntityHandler"]

EXT_DTD = b'<!ENTITY maybe "from ext"><!ELEMENT r ANY><!ATTLIST r d CDATA "ext-default">'
EXT_ENT = b'<?xml version="1.0" encoding="utf-8"?>ext <i>text</i>'

# (name, ns separator, ordered, specified, buffer_text, default handler, chunk, external entities)
CONFIGS = [
    ("plain", None, False, False, False, None, 0, False),
    ("ns", "!", True, True, False, None, 0, False),
    ("buf-default", None, False, False, True, "DefaultHandler", 0, False),
    ("chunk7-expand", " ", False, False, False, "DefaultHandlerExpand", 7, False),
    ("ext-chunk1", None, True, False, True, None, 1, True),
]


def parse(data, cfg):
    name, sep, ordered, specified, buffer_text, dflt, chunk, ext = cfg
    ev = []
    p = expat.ParserCreate(namespace_separator=sep) if sep is not None else expat.ParserCreate()
    p.ordered_attributes = ordered
    p.specified_attributes = specified
    p.buffer_text = buffer_text

    def rec(hname, parser):
        def h(*a):
            ev.append((hname, parser.CurrentLineNumber, parser.CurrentColumnNumber, parser.CurrentByteIndex) + a)
        return h

    def install(parser):
        for hname in HANDLERS:
            setattr(parser, hname, rec(hname, parser))
        if dflt:
            setattr(parser, dflt, rec(dflt, parser))
        parser.NotStandaloneHandler = lambda: (ev.append(("NotStandalone",)), 1)[1]

    install(p)
    if ext:
        p.SetParamEntityParsing(expat.XML_PARAM_ENTITY_PARSING_ALWAYS)
        p.UseForeignDTD(True)

        def extref(context, base, systemId, publicId):
            ev.append(("ExternalEntityRef", context, base, systemId, publicId))
            sub = p.ExternalEntityParserCreate(context)
            install(sub)
            text = EXT_DTD if context is None else EXT_ENT
            try:
                sub.Parse(text, True)
            except expat.ExpatError as e:
                ev.append(("sub-error", e.code, e.lineno, e.offset, str(e)))
                return 0
            return 1
        p.ExternalEntityRefHandler = extref
    else:
        p.ExternalEntityRefHandler = lambda *a: (ev.append(("ExternalEntityRef",) + a), 1)[1]
    try:
        if chunk:
            for i in range(0, len(data), chunk):
                p.Parse(data[i:i + chunk], False)
            p.Parse(b"", True)
        else:
            p.Parse(data, True)
        ev.append(("ok", p.CurrentLineNumber, p.CurrentColumnNumber, p.CurrentByteIndex))
    except expat.ExpatError as e:
        ev.append(("error", e.code, e.lineno, e.offset, str(e), p.ErrorCode, p.ErrorLineNumber,
                   p.ErrorColumnNumber, p.ErrorByteIndex, expat.ErrorString(e.code)))
    except Exception as e:  # a handler's or the binding's own exception
        ev.append(("exception", type(e).__name__, str(e)))
    return ev


names = sorted(os.listdir(corpus))
if only:
    names = [n for n in names if n == only]
names = names[skip:]
if first is not None:
    names = names[:first]
for n in names:
    with open(os.path.join(corpus, n), "rb") as fh:
        data = fh.read()
    for cfg in CONFIGS:
        if cfg[6] == 1 and len(data) > 4096:
            continue  # byte by byte only on the small documents
        ev = parse(data, cfg)
        if dump:
            print("==", n, cfg[0])
            for e in ev:
                print("  ", repr(e))
        else:
            print(n, cfg[0], hashlib.sha256(repr(ev).encode()).hexdigest()[:16], ev[-1][0], len(ev))
