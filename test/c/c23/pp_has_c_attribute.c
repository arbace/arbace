// 6.10.1: __has_c_attribute names the standard attributes and their versions.
#if __has_c_attribute(nodiscard) >= 202003L
[[nodiscard]] int f(void);
#endif
#if __has_c_attribute(gnu::packed)
struct [[gnu::packed]] s { char c; int i; };
#endif
#if __has_c_attribute(no_such_attribute)
#error "no such attribute"
#endif
