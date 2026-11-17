# a64i's program (AR5): logical immediate: and, orr, eor, ands over element sizes 2-64
# with: lib/out.s
# exit: 0
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "7fff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "7ffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "0000000055555555\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "00000000fffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000f0\n"
# stdout: "00000000000000f0\n"
# stdout: "123400009abc0000\n"
# stdout: "000000009abc0000\n"
# stdout: "1014545010145450\n"
# stdout: "0000000010145450\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "123456789abcdef0\n"
# stdout: "000000001abcdef0\n"
# stdout: "10341438183c1c30\n"
# stdout: "00000000183c1c30\n"
# stdout: "123456789abcdef0\n"
# stdout: "000000009abcdef0\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0001\n"
# stdout: "00000000ffff0001\n"
# stdout: "5555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000001\n"
# stdout: "0000000080000001\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "3c3c3c3c3c3c3c3d\n"
# stdout: "000000003c3c3c3d\n"
# stdout: "fffffffffffffff1\n"
# stdout: "00000000fffffff1\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "80000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "d555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "bc3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffff0000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "55555555ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "80000000ffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "7fffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "3c3c3c3cffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "ffffffffffffffff\n"
# stdout: "00000000ffffffff\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "00000000800000ff\n"
# stdout: "00000000800000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "55555555d5555555\n"
# stdout: "00000000d5555555\n"
# stdout: "8000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "3c3c3c3cbc3c3c3c\n"
# stdout: "00000000bc3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "123456789abcdef1\n"
# stdout: "000000009abcdef1\n"
# stdout: "123456789abcdeff\n"
# stdout: "000000009abcdeff\n"
# stdout: "ffff5678ffffdef0\n"
# stdout: "00000000ffffdef0\n"
# stdout: "5775577ddffddff5\n"
# stdout: "00000000dffddff5\n"
# stdout: "923456789abcdef0\n"
# stdout: "000000009abcdef0\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "3e3c7e7cbebcfefc\n"
# stdout: "00000000bebcfefc\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000fe\n"
# stdout: "00000000000000fe\n"
# stdout: "ffff0000ffff0001\n"
# stdout: "00000000ffff0001\n"
# stdout: "5555555555555554\n"
# stdout: "0000000055555554\n"
# stdout: "8000000000000001\n"
# stdout: "0000000080000001\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "3c3c3c3c3c3c3c3d\n"
# stdout: "000000003c3c3c3d\n"
# stdout: "fffffffffffffff1\n"
# stdout: "00000000fffffff1\n"
# stdout: "fffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "ffffffffffffff00\n"
# stdout: "00000000ffffff00\n"
# stdout: "0000ffff0000ffff\n"
# stdout: "000000000000ffff\n"
# stdout: "aaaaaaaaaaaaaaaa\n"
# stdout: "00000000aaaaaaaa\n"
# stdout: "7fffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "8000000000000001\n"
# stdout: "0000000080000001\n"
# stdout: "c3c3c3c3c3c3c3c3\n"
# stdout: "00000000c3c3c3c3\n"
# stdout: "000000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "7fffffffffffff00\n"
# stdout: "00000000ffffff00\n"
# stdout: "8000ffff0000ffff\n"
# stdout: "000000000000ffff\n"
# stdout: "2aaaaaaaaaaaaaaa\n"
# stdout: "00000000aaaaaaaa\n"
# stdout: "ffffffffffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "0000000000000001\n"
# stdout: "0000000080000001\n"
# stdout: "43c3c3c3c3c3c3c3\n"
# stdout: "00000000c3c3c3c3\n"
# stdout: "800000000000000f\n"
# stdout: "000000000000000f\n"
# stdout: "8000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "80000000000000ff\n"
# stdout: "00000000000000ff\n"
# stdout: "7fff0000ffff0000\n"
# stdout: "00000000ffff0000\n"
# stdout: "d555555555555555\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "fffffffffffffffe\n"
# stdout: "000000007ffffffe\n"
# stdout: "bc3c3c3c3c3c3c3c\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "7ffffffffffffff0\n"
# stdout: "00000000fffffff0\n"
# stdout: "00000000fffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "00000000ffffff00\n"
# stdout: "00000000ffffff00\n"
# stdout: "ffff00000000ffff\n"
# stdout: "000000000000ffff\n"
# stdout: "55555555aaaaaaaa\n"
# stdout: "00000000aaaaaaaa\n"
# stdout: "80000000ffffffff\n"
# stdout: "000000007fffffff\n"
# stdout: "7fffffff00000001\n"
# stdout: "0000000080000001\n"
# stdout: "3c3c3c3cc3c3c3c3\n"
# stdout: "00000000c3c3c3c3\n"
# stdout: "ffffffff0000000f\n"
# stdout: "000000000000000f\n"
# stdout: "0000000080000001\n"
# stdout: "0000000080000001\n"
# stdout: "00000000800000ff\n"
# stdout: "00000000800000ff\n"
# stdout: "ffff00007fff0000\n"
# stdout: "000000007fff0000\n"
# stdout: "55555555d5555555\n"
# stdout: "00000000d5555555\n"
# stdout: "8000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "7fffffff7ffffffe\n"
# stdout: "00000000fffffffe\n"
# stdout: "3c3c3c3cbc3c3c3c\n"
# stdout: "00000000bc3c3c3c\n"
# stdout: "ffffffff7ffffff0\n"
# stdout: "000000007ffffff0\n"
# stdout: "123456789abcdef1\n"
# stdout: "000000009abcdef1\n"
# stdout: "123456789abcde0f\n"
# stdout: "000000009abcde0f\n"
# stdout: "edcb56786543def0\n"
# stdout: "000000006543def0\n"
# stdout: "4761032dcfe98ba5\n"
# stdout: "00000000cfe98ba5\n"
# stdout: "923456789abcdef0\n"
# stdout: "000000001abcdef0\n"
# stdout: "6dcba9876543210e\n"
# stdout: "00000000e543210e\n"
# stdout: "2e086a44a680e2cc\n"
# stdout: "00000000a680e2cc\n"
# stdout: "edcba98765432100\n"
# stdout: "0000000065432100\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "ffff0000ffff0000\n"
# stdout: "0000000000000008\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000055555555\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "0000000000000000\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "fffffffffffffff0\n"
# stdout: "0000000000000008\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "7fff0000ffff0000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffff0000\n"
# stdout: "5555555555555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000080000000\n"
# stdout: "7ffffffffffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "000000007ffffffe\n"
# stdout: "3c3c3c3c3c3c3c3c\n"
# stdout: "0000000000000000\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "7ffffffffffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "8000000000000000\n"
# stdout: "0000000000000008\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000001\n"
# stdout: "00000000000000ff\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000ff\n"
# stdout: "00000000ffff0000\n"
# stdout: "0000000000000000\n"
# stdout: "00000000ffff0000\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000055555555\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000080000000\n"
# stdout: "00000000fffffffe\n"
# stdout: "0000000000000000\n"
# stdout: "000000007ffffffe\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "0000000000000000\n"
# stdout: "000000003c3c3c3c\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "00000000fffffff0\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000080000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000080000000\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000f0\n"
# stdout: "0000000000000000\n"
# stdout: "00000000000000f0\n"
# stdout: "123400009abc0000\n"
# stdout: "0000000000000000\n"
# stdout: "000000009abc0000\n"
# stdout: "1014545010145450\n"
# stdout: "0000000000000000\n"
# stdout: "0000000010145450\n"
# stdout: "0000000000000000\n"
# stdout: "0000000000000004\n"
# stdout: "0000000080000000\n"
# stdout: "123456789abcdef0\n"
# stdout: "0000000000000000\n"
# stdout: "000000001abcdef0\n"
# stdout: "10341438183c1c30\n"
# stdout: "0000000000000000\n"
# stdout: "00000000183c1c30\n"
# stdout: "123456789abcdef0\n"
# stdout: "0000000000000000\n"
# stdout: "000000009abcdef0\n"
	.text
	.globl	_start
_start:
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xff
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	and x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	and w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xff
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	orr x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	orr w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0x1
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0xff
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xff
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0xffff0000ffff0000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0x5555555555555555
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0x8000000000000000
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	eor x0, x1, #0xfffffffffffffff0
	bl	put_hex
	mov	x0, #-1
	eor w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #1
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #65535, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #65535, lsl #32
	movk	x1, #32767, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #0, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #32768, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #65535
	movk	x1, #65535, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #0
	movk	x1, #32768, lsl #16
	movk	x1, #0, lsl #32
	movk	x1, #0, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0x1
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x1
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0xff
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xff
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0xffff0000ffff0000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xffff0000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0x5555555555555555
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x55555555
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0x8000000000000000
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x80000000
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0x7ffffffffffffffe
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x7ffffffe
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0x3c3c3c3c3c3c3c3c
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0x3c3c3c3c
	bl	put_hex
	movz	x1, #57072
	movk	x1, #39612, lsl #16
	movk	x1, #22136, lsl #32
	movk	x1, #4660, lsl #48
	ands x0, x1, #0xfffffffffffffff0
	bl	put_hex
	bl	put_nzcv
	mov	x0, #-1
	ands w0, w1, #0xfffffff0
	bl	put_hex
	b	exit0
