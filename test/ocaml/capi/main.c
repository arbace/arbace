/* the main a library of C needs to be linked as a program (test/ocaml/capi/cnodes.ae, when the stubs have none): oi never
   calls it; it initializes musl itself (__init_libc) and runs the static initializers (go/arbace/oicapi) */
int main(void) { return 0; }
