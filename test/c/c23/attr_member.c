// 6.7.3.2: attributes on a member, before it and after its declarator.
struct m { [[maybe_unused]] int a; int b [[maybe_unused]]; };
