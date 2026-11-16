#include <spawn.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
extern char **environ;
int main(int argc, char **argv) {
  if (argc > 1 && !strcmp(argv[1], "child")) { printf("child says hi\n"); return 7; }
  printf("parent\n"); fflush(stdout);
  pid_t pid; int st;
  char *a[] = {argv[0], "child", 0};
  int r = posix_spawn(&pid, argv[0], 0, 0, a, environ);
  printf("spawn %d\n", r); fflush(stdout);
  waitpid(pid, &st, 0); printf("child status %d\n", WEXITSTATUS(st));
  int p[2]; pipe(p);
  posix_spawn_file_actions_t fa; posix_spawn_file_actions_init(&fa);
  posix_spawn_file_actions_adddup2(&fa, p[1], 1); posix_spawn_file_actions_addclose(&fa, p[0]);
  char *b[] = {"echo", "piped", 0};
  r = posix_spawnp(&pid, "echo", &fa, 0, b, environ); close(p[1]);
  char buf[64] = {0}; int n = read(p[0], buf, 63); waitpid(pid, &st, 0);
  printf("spawnp %d read %d [%s] status %d\n", r, n, strtok(buf, "\n"), WEXITSTATUS(st));
  r = posix_spawn(&pid, "/nonexistent", 0, 0, b, environ); printf("missing %d\n", r);
  return 0;
}
