/* oi's C API: <caml/runtime_events_consumer.h> for C stubs on ci (test/ocaml/capi/include/caml/mlvalues.h's notes), its
   declarations OCaml 5.5.0's otherlibs/runtime_events/caml/runtime_events_consumer.h's (LGPL-2.1 with the OCaml linking exception,
   LICENSE.md; queue 478). test/ocaml/capi/runtime.c implements them over oi's ring (go/arbace/oi/ext_runtime_events.ae): a cursor
   on the process's own ring (another process's ring is not mapped, as the OCaml cursor's, OC219), read_poll calling the callbacks
   set for each message from the ring's start, a callback's 0 stopping the poll after that message. A user events callback is kept
   and not called (OC219's ring holds the user events for the OCaml consumer). */
#ifndef CAML_RUNTIME_EVENTS_CONSUMER_H
#define CAML_RUNTIME_EVENTS_CONSUMER_H
#include "runtime_events.h"

struct caml_runtime_events_cursor;

CAMLextern runtime_events_error caml_runtime_events_create_cursor(const char_os *runtime_events_path, int pid,
                                                                  struct caml_runtime_events_cursor **cursor_res);
CAMLextern void caml_runtime_events_set_runtime_begin(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, uint64_t timestamp, ev_runtime_phase phase));
CAMLextern void caml_runtime_events_set_runtime_end(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, uint64_t timestamp, ev_runtime_phase phase));
CAMLextern void caml_runtime_events_set_runtime_counter(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, uint64_t timestamp, ev_runtime_counter counter, uint64_t val));
CAMLextern void caml_runtime_events_set_runtime_alloc(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, uint64_t timestamp, uint64_t *sz));
CAMLextern void caml_runtime_events_set_lifecycle(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, int64_t timestamp, ev_lifecycle lifecycle, int64_t data));
CAMLextern void caml_runtime_events_set_lost_events(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, int lost_words));
CAMLextern void caml_runtime_events_set_user_events(struct caml_runtime_events_cursor *cursor,
    int (*f)(int domain_id, void *callback_data, int64_t timestamp, value event, uintnat event_data_len, uint64_t *event_data));
CAMLextern void caml_runtime_events_free_cursor(struct caml_runtime_events_cursor *cursor);
CAMLextern runtime_events_error caml_runtime_events_read_poll(struct caml_runtime_events_cursor *cursor, void *callback_data,
                                                              uintnat max_events, uintnat *events_consumed);
#endif
