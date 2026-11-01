// 6.7.13.7: [[noreturn]] on a declaration and a definition.
[[noreturn]] void die(void);
[[noreturn]] void die(void) { for (;;) { } }
