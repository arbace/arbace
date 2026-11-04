# gencorpus.py TOP OUT [--mutants-only N --seed S]: tools/expat/check.sh's corpus (queue 202e): the tarball's XML files,
# hand-written documents for the parts of expat a plain file does not reach (DTDs, entities, namespaces, encodings,
# errors), and 300 seeded mutants of them (truncations, byte flips, insertions); --mutants-only: N mutants of the
# hand-written documents alone, up to three edits each, seeded by S. Deterministic: the same files for the same tarball.
import os, sys, random

top, out = sys.argv[1], sys.argv[2]
only_mutants = int(sys.argv[sys.argv.index("--mutants-only") + 1]) if "--mutants-only" in sys.argv else None
seed = int(sys.argv[sys.argv.index("--seed") + 1]) if "--seed" in sys.argv else 202
os.makedirs(out, exist_ok=True)
docs = []

def add(name, data):
    if isinstance(data, str):
        data = data.encode("utf-8")
    docs.append((name, data))

exts = (".xml", ".xsl", ".dtd", ".plist", ".svg", ".vcxproj", ".props", ".targets", ".wxs", ".manifest")
for d, _, fs in sorted(os.walk(top)):
    for f in sorted(fs):
        if f.endswith(exts) or "fuzz_elementtree" in d:
            p = os.path.join(d, f)
            with open(p, "rb") as fh:
                b = fh.read()
            if len(b) <= 64 * 1024:
                add("t-" + os.path.relpath(p, top).replace("/", "_"), b)

hand = {
 "decl-utf8": '<?xml version="1.0" encoding="utf-8"?>\n<a x="1" y=\'2\'>téxt<b/>&amp;&lt;&gt;&quot;&apos;&#65;&#x42;</a>\n',
 "decl-standalone": '<?xml version="1.0" standalone="yes"?><a/>',
 "decl-bad-version": '<?xml version="2.0"?><a/>',
 "decl-bad": '<?xml encoding="utf-8"?><a/>',
 "pi-comment": '<?pi data here?><!-- c1 --><a><?p2?><!--c2--></a><!--after-->',
 "cdata": '<a><![CDATA[x<y&z]]>tail<![CDATA[]]></a>',
 "cdata-unclosed": '<a><![CDATA[never',
 "crlf": '<a>\r\nline\rx\n</a>\r\n',
 "ws-attr": '<a x=" a\tb\nc\r\nd " y="&#9;&#10;&#13;"/>',
 "ns": '<r xmlns="urn:d" xmlns:p="urn:p"><p:c p:a="1" b="2"><d xmlns="">x</d></p:c><p:e xmlns:p="urn:q"/></r>',
 "ns-unbound": '<p:a/>',
 "ns-xml": '<a xml:lang="en" xmlns:xml="http://www.w3.org/XML/1998/namespace"/>',
 "ns-xmlns-bad": '<a xmlns:xmlns="urn:x"/>',
 "ns-undeclare": '<a xmlns:p="urn:p"><b xmlns:p=""/></a>',
 "ns-dup": '<a xmlns:p="urn:p" xmlns:q="urn:p" p:x="1" q:x="2"/>',
 "dup-attr": '<a x="1" x="2"/>',
 "mismatch": '<a><b></a></b>',
 "junk-after": '<a/><b/>',
 "no-element": '<!-- only -->',
 "empty": '',
 "ws-only": '   \n  ',
 "undefined-entity": '<a>&nope;</a>',
 "bad-charref": '<a>&#0;</a>',
 "bad-charref2": '<a>&#xD800;</a>',
 "lt-in-attr": '<a x="<"/>',
 "rsqb": '<a>]]></a>',
 "dtd-int": '''<?xml version="1.0"?>
<!DOCTYPE r [
  <!ELEMENT r (a|b)*>
  <!ELEMENT a (#PCDATA|b)*>
  <!ELEMENT b EMPTY>
  <!ELEMENT c ANY>
  <!ELEMENT d (a,(b|c)+,a?)>
  <!ATTLIST r id ID #IMPLIED k CDATA "def" e (x|y) "x" f NOTATION (n1) #IMPLIED req CDATA #REQUIRED fix CDATA #FIXED "fx">
  <!ATTLIST a t NMTOKENS "  a   b  " u IDREFS #IMPLIED>
  <!ENTITY e1 "ent &#38;amp; one">
  <!ENTITY e2 "<b/>&e1;">
  <!ENTITY ext SYSTEM "ext.xml">
  <!ENTITY pub PUBLIC "-//X//Y  Z//EN" "pub.xml">
  <!ENTITY unp SYSTEM "u.gif" NDATA gif>
  <!NOTATION gif SYSTEM "image/gif">
  <!NOTATION n1 PUBLIC "-//N1//EN">
  <!ENTITY % pe "<!ENTITY e3 'three'>">
  %pe;
  <?pi-in-dtd x?>
  <!-- comment in dtd -->
]>
<r req="1"><a t="q">&e1;&e2;&e3;</a><b/><a>&ext;&pub;</a></r>
''',
 "dtd-ext-subset": '<!DOCTYPE r SYSTEM "r.dtd"><r>&maybe;</r>',
 "dtd-pubid": '<!DOCTYPE r PUBLIC "-//A//B" "r.dtd"><r/>',
 "dtd-bad-pubid": '<!DOCTYPE r PUBLIC "\\bad{" "r.dtd"><r/>',
 "dtd-recursive": '<!DOCTYPE r [<!ENTITY a "&b;"><!ENTITY b "&a;">]><r>&a;</r>',
 "dtd-attr-entity": '<!DOCTYPE r [<!ENTITY a "x&lt;y"><!ENTITY ex SYSTEM "e">]><r v="&a;" w="&ex;"/>',
 "dtd-unparsed-ref": '<!DOCTYPE r [<!NOTATION n SYSTEM "n"><!ENTITY u SYSTEM "u" NDATA n>]><r>&u;</r>',
 "dtd-cond": '<!DOCTYPE r [<![INCLUDE[<!ENTITY x "1">]]>]><r/>',
 "dtd-pe-in-decl": '<!DOCTYPE r [<!ENTITY % p "CDATA"><!ATTLIST r a %p; #IMPLIED>]><r a="1"/>',
 "dtd-not-standalone": '<?xml version="1.0" standalone="no"?><!DOCTYPE r SYSTEM "x.dtd"><r/>',
 "dtd-standalone-undef": '<?xml version="1.0" standalone="yes"?><!DOCTYPE r SYSTEM "x.dtd"><r>&z;</r>',
 "dtd-bad-model": '<!DOCTYPE r [<!ELEMENT r (a|b,c)>]><r/>',
 "dtd-mixed": '<!DOCTYPE r [<!ELEMENT r (#PCDATA|a|b)*><!ELEMENT a (#PCDATA)>]><r>x<a>y</a></r>',
 "dtd-quant": '<!DOCTYPE r [<!ELEMENT r ((a?,b*)+|c)?><!ELEMENT a EMPTY>]><r/>',
 "dtd-entity-lt": '<!DOCTYPE r [<!ENTITY e "<a>">]><r>&e;</a></r>',
 "dtd-entity-split": '<!DOCTYPE r [<!ENTITY e "<a>x</a>tail">]><r>&e;&e;</r>',
 "dtd-amp-entity": '<!DOCTYPE r [<!ENTITY amp "&#38;#38;">]><r>&amp;</r>',
 "dtd-xml-pi": '<!DOCTYPE r [<?xml version="1.0"?>]><r/>',
 "misplaced-xmldecl": '<a/><?xml version="1.0"?>',
 "bom-utf8": b'\xef\xbb\xbf<a>bom</a>',
 "utf16le": '﻿<?xml version="1.0" encoding="utf-16"?><a x="é">中\U0001F600</a>'.encode("utf-16-le"),
 "utf16be": '﻿<a>be ü</a>'.encode("utf-16-be"),
 "utf16le-nobom": '<?xml version="1.0" encoding="UTF-16LE"?><a>x</a>'.encode("utf-16-le"),
 "latin1": '<?xml version="1.0" encoding="iso-8859-1"?><a v="ÿ">café</a>'.encode("latin-1"),
 "ascii": b'<?xml version="1.0" encoding="us-ascii"?><a>abc</a>',
 "ascii-bad": b'<?xml version="1.0" encoding="us-ascii"?><a>ab\xe9c</a>',
 "cp1252": '<?xml version="1.0" encoding="cp1252"?><a>€ ’</a>'.encode("cp1252"),
 "koi8r": '<?xml version="1.0" encoding="koi8-r"?><a>При</a>'.encode("koi8-r"),
 "unknown-enc": b'<?xml version="1.0" encoding="x-no-such"?><a/>',
 "utf8-bad": b'<a>\xff\xfe</a>',
 "utf8-partial": b'<a>\xe4\xb8',
 "utf8-surrogate": b'<a>\xed\xa0\x80</a>',
 "utf8-4byte": '<a \U00010000="1">\U00010400</a>'.encode(),
 "name-chars": '<élève ·x="1"/>',
 "name-bad": '<1a/>',
 "colon-names": '<a:b:c/>',
 "deep": "<a>" * 200 + "x" + "</a>" * 200,
 "many-attrs": "<a " + " ".join('a%d="%d"' % (i, i) for i in range(60)) + "/>",
 "long-text": "<a>" + ("lorem ipsum &amp; dolor " * 400) + "</a>",
 "long-name": "<" + "n" * 3000 + "/>",
 "tab-col": '<a>\n\t<b\n x="1"\n/>\n</a>',
 "pi-bad-target": '<?xml-foo?><a/>',
 "pi-xml-target": '<a><?xml x?></a>',
 "comment-dashes": '<a><!-- a -- b --></a>',
 "trailing-cr": '<a/>\r',
 "attr-no-quote": '<a x=1/>',
 "attr-no-eq": '<a x/>',
 "tag-unclosed": '<a',
 "end-only": '</a>',
 "text-before": 'text<a/>',
 "doctype-after": '<a/><!DOCTYPE a>',
 "foreign-dtd": '<r>&q;</r>',
}
for k, v in hand.items():
    add("h-" + k, v)

if only_mutants is not None:
    hdocs = [b for n, b in docs if n.startswith("h-") and b]
    r = random.Random(seed)
    ins = [b"<", b">", b"&", b"]]>", b"<!", b"<?", b'"', b"'", b"\x00", b"\xc3", b"\xff", b"&#", b"%", b";", b"=", b"/",
           b":", b"<!ENTITY", b"<!ATTLIST", b"(", b")", b"|", b"*", b"#PCDATA", b"\r\n", b"&e1;", b"%pe;"]
    for i in range(only_mutants):
        b = hdocs[r.randrange(len(hdocs))]
        for _ in range(r.randrange(1, 4)):
            k = r.randrange(4); j = r.randrange(len(b) + 1)
            if k == 0: b = b[:j]
            elif k == 1 and j < len(b): b = b[:j] + bytes([r.randrange(256)]) + b[j + 1:]
            elif k == 2: b = b[:j] + ins[r.randrange(len(ins))] + b[j:]
            else: b = b[:j] + b[j + r.randrange(1, 8):]
            if not b: b = b"<a/>"
        with open(os.path.join(out, "x%04d" % i), "wb") as fh:
            fh.write(b)
    print(only_mutants, "mutants")
    sys.exit(0)

rng = random.Random(seed)
base = [d for d in docs]
mut = []
inserts = [b"<", b">", b"&", b"]]>", b"<!", b"<?", b"\"", b"'", b"\x00", b"\xc3", b"\xff", b"&#", b"<![CDATA[", b"\n", b"\r", b"%", b";", b"=", b"/", b":"]
for i in range(300):
    name, b = base[rng.randrange(len(base))]
    if not b:
        continue
    kind = rng.randrange(4)
    if kind == 0:
        nb = b[: rng.randrange(len(b))]
    elif kind == 1:
        j = rng.randrange(len(b)); nb = b[:j] + bytes([rng.randrange(256)]) + b[j + 1:]
    elif kind == 2:
        j = rng.randrange(len(b) + 1); nb = b[:j] + inserts[rng.randrange(len(inserts))] + b[j:]
    else:
        j = rng.randrange(len(b)); k = min(len(b), j + rng.randrange(1, 12)); nb = b[:j] + b[k:]
    mut.append(("m%03d-%s" % (i, name), nb))
for name, b in docs + mut:
    with open(os.path.join(out, name[:120]), "wb") as fh:
        fh.write(b)
print(len(docs), "documents,", len(mut), "mutants")
