# queue 228: the reader's SyntaxWarnings and their errors, PEP 765's, eval and exec of bytes, _tokenize's readline errors
import warnings
def t(s,mode='exec',f='always'):
    with warnings.catch_warnings(record=True) as ws:
        warnings.simplefilter(f)
        try:
            r=compile(s,'<w>',mode); r='ok'
        except SyntaxError as e: r=(e.msg,e.lineno,e.offset,e.end_lineno,e.end_offset,e.text)
    print(repr(s),r,[(w.category.__name__,str(w.message),w.filename,w.lineno) for w in ws])
S=[r"'\z'", "'''\n\\z'''", r"b'\z'", r"'\400'", r"b'\400'",r"f'\z{1}\q'",r"f'{1:\z}'",r"f'\{1}'",'0_7if 1 else 2','1if 1else 2','0xfor x in y','1or 2','1and 2',"'ä\\z'","f'''\n\\z{1}'''", "'\\ä'", r"rb'\z'", "b'''\n\n \\z'''", "x='a' 'b\\zc'", "1is 2", "\n\n 'x\\z'", "1in x","'\\8'","0b1if 1 else 2","1_000else","1.5if x else y","1e5or x"," 'a\\z'"]
for s in S:
    t(s); t(s,f='error')
t("try:\n  pass\nfinally:\n  pass\n1 is 1\n'\\z'\n")
def g(): pass
for s in ["def f():\n  try:pass\n  finally:\n    return 1\n","for x in y:\n  try:pass\n  finally:\n    break\n","for x in y:\n  try:pass\n  finally:\n    continue\n","try:pass\nfinally:\n  def g():\n    return\n"]:
    t(s); t(s,f='error')
print(eval(b'1+1'),eval(bytearray(b' 2')),eval('\t 3'),eval(memoryview(b'4')))
exec(b'print(5)')
for x in [lambda:eval(1),lambda:exec(1),lambda:compile(1,'x','exec'),lambda:eval(b'1\0'),lambda:eval('1\0')]:
    try: x()
    except Exception as e: print(type(e).__name__,e)
import _tokenize
def run(lines,enc=None):
    it=iter(lines)
    def rl():
        x=next(it)
        if isinstance(x,Exception): raise x
        return x
    out=[]
    try:
        for t in _tokenize.TokenizerIter(rl,extra_tokens=True,**({'encoding':enc} if enc else {})):
            out.append(t[:2]+t[2:4])
    except BaseException as e: out.append((type(e).__name__,str(e)))
    print(out)
run(['a = 1\n',ValueError('x'),'b\n'])
run(['if x:\n','  y\n',ValueError('x')])
run(['(a,\n',ValueError('x')])
run(['a\n',1])
run([b'a\n',1],'utf-8')
run([b'a\n','s'],'utf-8')
run([b'a \xff\n'],'utf-8')
run(['a\n',b'b\n'])
