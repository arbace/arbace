// 6.7.13.2: another vendor's attribute, its arguments a balanced token
// sequence that need not be expressions, and an attribute list's empty element.
[[vendor::thing(1, [2], {x})]] int a;
[[deprecated,]] int b;
