/* include-in-body.c's declarations and a pragma (Arbace's own, EPL-1.0) */
int inc_local = 40;
#pragma pack(push, 1)
struct IncPacked { char c; int i; };
#pragma pack(pop)
inc_local += 2;
