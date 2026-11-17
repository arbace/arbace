/* the main a library of C needs to be linked as a program (bin/hsi cnodes, when the test's C has none): hsi never calls it,
   it calls musl's __init_libc and the static initializers itself */
int main(void) { return 0; }
