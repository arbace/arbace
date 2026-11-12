/* atomic-gcc.c's header (Arbace's own, EPL-1.0): static inline wrappers over gcc's __atomic builtins in the shapes of
   CPython 3.14's Include/cpython/pyatomic_gcc.h (a const-qualified pointer for the loads, the generic __atomic_load and
   __atomic_store for float and double, compare-exchange through the expected value's address, pointer-sized objects) */
static inline int add_int(int *obj, int value) { return __atomic_fetch_add(obj, value, __ATOMIC_SEQ_CST); }
static inline int8_t add_int8(int8_t *obj, int8_t value) { return __atomic_fetch_add(obj, value, __ATOMIC_SEQ_CST); }
static inline uint16_t add_uint16(uint16_t *obj, uint16_t value) { return __atomic_fetch_add(obj, value, __ATOMIC_SEQ_CST); }
static inline int64_t add_int64(int64_t *obj, int64_t value) { return __atomic_fetch_add(obj, value, __ATOMIC_SEQ_CST); }
static inline intptr_t add_intptr(intptr_t *obj, intptr_t value) { return __atomic_fetch_add(obj, value, __ATOMIC_SEQ_CST); }
static inline uint32_t and_uint32(uint32_t *obj, uint32_t value) { return __atomic_fetch_and(obj, value, __ATOMIC_SEQ_CST); }
static inline uint8_t or_uint8(uint8_t *obj, uint8_t value) { return __atomic_fetch_or(obj, value, __ATOMIC_SEQ_CST); }
static inline int cas_int(int *obj, int *expected, int desired)
{ return __atomic_compare_exchange_n(obj, expected, desired, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST); }
static inline int cas_ptr(void *obj, void *expected, void *desired)
{ return __atomic_compare_exchange_n((void **)obj, (void **)expected, desired, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST); }
static inline int exchange_int(int *obj, int value) { return __atomic_exchange_n(obj, value, __ATOMIC_SEQ_CST); }
static inline void *exchange_ptr(void *obj, void *value) { return __atomic_exchange_n((void **)obj, value, __ATOMIC_SEQ_CST); }
static inline int load_int(const int *obj) { return __atomic_load_n(obj, __ATOMIC_SEQ_CST); }
static inline long load_long_relaxed(const long *obj) { return __atomic_load_n(obj, __ATOMIC_RELAXED); }
static inline void *load_ptr_acquire(const void *obj) { return (void *)__atomic_load_n((void * const *)obj, __ATOMIC_ACQUIRE); }
static inline float load_float_relaxed(const float *obj) { float ret; __atomic_load(obj, &ret, __ATOMIC_RELAXED); return ret; }
static inline double load_double_relaxed(const double *obj) { double ret; __atomic_load(obj, &ret, __ATOMIC_RELAXED); return ret; }
static inline void store_int(int *obj, int value) { __atomic_store_n(obj, value, __ATOMIC_SEQ_CST); }
static inline void store_ptr_release(void *obj, void *value) { __atomic_store_n((void **)obj, value, __ATOMIC_RELEASE); }
static inline void store_float_relaxed(float *obj, float value) { __atomic_store(obj, &value, __ATOMIC_RELAXED); }
static inline void store_double_relaxed(double *obj, double value) { __atomic_store(obj, &value, __ATOMIC_RELAXED); }
static inline void fence_seq_cst(void) { __atomic_thread_fence(__ATOMIC_SEQ_CST); }
