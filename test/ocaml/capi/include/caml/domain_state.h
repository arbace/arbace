/* oi's C API: Arbace's own clean-room <caml/domain_state.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). Caml_state_opt is
   the domain's state while the calling thread holds the runtime lock and NULL inside a blocking section (the manual's 22.12.2:
   between caml_release_runtime_system and caml_acquire_runtime_system a thread must not touch OCaml's values); its fields are
   the runtime's own, which a stub does not read here. */
#ifndef CAML_DOMAIN_STATE_H
#define CAML_DOMAIN_STATE_H
#include "mlvalues.h"
typedef struct caml_domain_state caml_domain_state;
CAMLextern caml_domain_state *caml_state_opt(void);
#define Caml_state_opt (caml_state_opt())
#endif
