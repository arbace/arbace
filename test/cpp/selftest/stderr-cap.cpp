// Writes 3 MiB to stderr, then returns 0: the run's stderr is held to 1 MiB (review 018's R7).
extern "C" long write(int, const void*, unsigned long);
static char buf[65536];
int main() {
  for (int i = 0; i < 65536; i++) buf[i] = 'x';
  for (int i = 0; i < 48; i++) write(2, buf, sizeof buf);
  return 0;
}
