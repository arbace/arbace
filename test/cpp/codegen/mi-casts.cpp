// multiple inheritance: pointer and reference conversions between the bases and the derived class, comparisons of
// pointers to different bases of one object, static_cast down from the second base, the bases' members by name
#include <cstdio>
struct In { int fd = 0; int read() const { return fd + 100; } };
struct Out { int fd = 1; int written = 0; void write(int n) { written += n; } };
struct IO : In, Out { IO() { In::fd = 3; Out::fd = 4; } int both() const { return In::fd * 10 + Out::fd; } };
void pump(In& i, Out& o) { o.write(i.read()); }
int main() {
  IO io;
  In* ip = &io; Out* op = &io;
  printf("%d %d %d\n", ip->read(), op->fd, io.both());
  pump(io, io);
  printf("written %d\n", io.written);
  IO* back = static_cast<IO*>(op);
  printf("%d %d\n", back == &io, (void*)op == (void*)&io);
  Out& ro = io;
  IO& rback = static_cast<IO&>(ro);
  printf("%d\n", rback.both());
  printf("%zu %zu\n", sizeof(IO), (char*)op - (char*)ip);
  return 0;
}
