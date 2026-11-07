# PY6-core: the compiler's SyntaxWarnings (codegen's _PyCompile_Warn) emitted by compile() through warnings, a
# SyntaxWarning made an error raising the SyntaxError of its location (test_grammar's test_assert_syntax_warnings,
# test_comparison_is_literal, test_warn_missed_comma, test_assert_warning_promotes_to_syntax_error)
import warnings

cases = ['assert(x, "msg")', 'assert(False,)', 'assert x, "msg"', 'x is 1', 'x is "thing"', '1 is x', 'x is y is 1',
         'x is not (1, 2)', '(1, 2) is not x', 'None is 1', 'x == "thing" is y', 'x is None', 'x is ...', 'x is (a, b)',
         '[(1, 2) (3, 4)]', '[(x, y) (3, 4)]', '[{1: 2} (3, 4)]', '[(i for i in range(5)) (3, 4)]', '[f"{x}" (3, 4)]',
         '["abc" (3, 4)]', '[b"abc" (3, 4)]', '[12.3j (3, 4)]', '[None (3, 4)]', '[... (3, 4)]', '[t"{x}" (3, 4)]',
         '[(lambda: 1) (3, 4)]', '[{1, 2} [i, j]]', '[(lambda x, y: x) [i, j]]', '[123 [i, j]]', '[True [i, j]]',
         '[(1, 2) [i, j]]', '[[1, 2] [i, j]]', '["abc" [i, j]]', '[[1, 2] [{3: 4}]]', '[[1, 2] [(i for i in "")]]',
         '[[1, 2] [f"{x}"]]', '[[1, 2] [t"{x}"]]', '[[1, 2] [None]]', '[[1, 2] [1]]', '[[1, 2] [1:2]]', 'x[1, 2]',
         'f(x) (1)', '\n\nx is 1']
for src in cases:
    with warnings.catch_warnings(record=True) as ws:
        warnings.simplefilter("always", SyntaxWarning)
        compile(src, "<testcase>", "exec")
    print(repr(src), [(w.category.__name__, str(w.message), w.filename, w.lineno) for w in ws])
    with warnings.catch_warnings():
        warnings.simplefilter("error", SyntaxWarning)
        try:
            compile(src, "<testcase>", "exec")
        except SyntaxError as e:
            print("  SyntaxError", e.msg, e.lineno, e.offset, e.text)
