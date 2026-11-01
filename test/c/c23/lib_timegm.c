// 7.29.2.6, 7.29.3.4: timegm and gmtime_r in <time.h>.
#include <time.h>
time_t round_trip(time_t t) { struct tm tm; gmtime_r(&t, &tm); return timegm(&tm); }
