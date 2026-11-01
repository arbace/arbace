// 6.7.13.2: a vendor's attribute, prefixed, and the double-underscore spellings.
[[gnu::always_inline]] static inline int f(void) { return 0; }
[[__gnu__::__cold__]] void g(void);
[[__deprecated__]] int h(void);
