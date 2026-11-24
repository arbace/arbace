/* the main a library of C needs to be linked as a program (test/ocaml/capi/cnodes.ae, when the stubs have none): oi never
   calls it; it initializes musl itself and runs the static initializers (go/arbace/oicapi). __oi_no_main marks it as this main,
   not the stubs' own (queue 539: go/arbace/oicapi's OwnMain) */
void __oi_no_main(void) { }
int main(void) { __oi_no_main(); return 0; }
