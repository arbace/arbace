// 6.10.2: #elifdef and #elifndef take the group whose macro is (or is not) defined.
#define HAVE_A 1
#ifdef NOPE
int x = 1;
#elifdef HAVE_A
int x = 2;
#else
int x = 3;
#endif
#ifdef NOPE
int y = 1;
#elifndef ALSO_NOPE
int y = 2;
#endif
