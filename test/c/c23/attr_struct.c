// 6.7.3.2: an attribute on a struct or union, between the keyword and the tag.
struct [[gnu::packed]] s { char c; int i; };
union [[deprecated]] u { int i; };
struct s v;
