// 6.7.7.1: an attribute after a declarator's identifier, which appertains to
// what it declares.
int x [[maybe_unused]] = 1;
int arr [[maybe_unused]] [4];
int f [[deprecated]] (void);
