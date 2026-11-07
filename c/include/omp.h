/* Arbace's <omp.h> (c/README.md): the OpenMP API's runtime library interface (OpenMP Application Programming Interface
   5.2, §3 and §18: the execution environment, lock and timing routines), declarations only. Arbace's C has no OpenMP:
   its pragmas are ignored (_OPENMP is not defined), and a program that calls these routines does not link. */
#ifndef __ARBACE_OMP_H
#define __ARBACE_OMP_H

typedef struct { unsigned char __arbace_lock[8]; } omp_lock_t;
typedef struct { unsigned char __arbace_lock[16]; } omp_nest_lock_t;

typedef enum omp_sched_t {
  omp_sched_static = 1,
  omp_sched_dynamic = 2,
  omp_sched_guided = 3,
  omp_sched_auto = 4,
  omp_sched_monotonic = 0x80000000u
} omp_sched_t;

typedef enum omp_proc_bind_t {
  omp_proc_bind_false = 0,
  omp_proc_bind_true = 1,
  omp_proc_bind_primary = 2,
  omp_proc_bind_master = 2,
  omp_proc_bind_close = 3,
  omp_proc_bind_spread = 4
} omp_proc_bind_t;

extern void omp_set_num_threads(int);
extern int omp_get_num_threads(void);
extern int omp_get_max_threads(void);
extern int omp_get_thread_num(void);
extern int omp_get_num_procs(void);
extern int omp_in_parallel(void);
extern void omp_set_dynamic(int);
extern int omp_get_dynamic(void);
extern int omp_get_cancellation(void);
extern void omp_set_nested(int);
extern int omp_get_nested(void);
extern void omp_set_schedule(omp_sched_t, int);
extern void omp_get_schedule(omp_sched_t *, int *);
extern int omp_get_thread_limit(void);
extern int omp_get_supported_active_levels(void);
extern void omp_set_max_active_levels(int);
extern int omp_get_max_active_levels(void);
extern int omp_get_level(void);
extern int omp_get_ancestor_thread_num(int);
extern int omp_get_team_size(int);
extern int omp_get_active_level(void);
extern int omp_in_final(void);
extern omp_proc_bind_t omp_get_proc_bind(void);
extern int omp_get_num_places(void);
extern int omp_get_place_num(void);
extern int omp_get_num_devices(void);
extern int omp_get_device_num(void);
extern int omp_get_num_teams(void);
extern int omp_get_team_num(void);
extern int omp_is_initial_device(void);
extern int omp_get_initial_device(void);
extern int omp_get_default_device(void);
extern void omp_set_default_device(int);

extern void omp_init_lock(omp_lock_t *);
extern void omp_destroy_lock(omp_lock_t *);
extern void omp_set_lock(omp_lock_t *);
extern void omp_unset_lock(omp_lock_t *);
extern int omp_test_lock(omp_lock_t *);
extern void omp_init_nest_lock(omp_nest_lock_t *);
extern void omp_destroy_nest_lock(omp_nest_lock_t *);
extern void omp_set_nest_lock(omp_nest_lock_t *);
extern void omp_unset_nest_lock(omp_nest_lock_t *);
extern int omp_test_nest_lock(omp_nest_lock_t *);

extern double omp_get_wtime(void);
extern double omp_get_wtick(void);

#endif
