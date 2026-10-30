# bin/gate --audit's reducer (doc/GATE-SURVEY.md, GT4, "As built: GT4, GT6"): strace's output
# (-f -y -s 4096, the calls %file, execve, clone, clone3, fork, vfork, chdir), read as it is
# written, reduced to the distinct "OP<TAB>PATH" lines of the paths the traced processes touched,
# absolute and normalized (. and .. resolved; a relative path against its process's directory,
# the checkout's root until a chdir; a child starts in its parent's):
#   X an execve         R an open for reading, readlink    S a stat, access
#   D an open of a directory (O_DIRECTORY)                 W an open for writing, a creation,
#                                                          a removal or a rename
#   H an open for reading by sha256sum (GT7's fix round: hashed only, a key's computation; gate.audit
#     covers it where it is a host's identity check or a build's inner cache key)
# Usage: awk -v root=/path/to/checkout -f test/gate/audit.awk STRACE-OUTPUT > PATHS

function norm(p,   n, a, i, out, k, r) {
  n = split(p, a, "/"); k = 0
  for (i = 1; i <= n; i++) {
    if (a[i] == "" || a[i] == ".") continue
    if (a[i] == "..") { if (k > 0) k--; continue }
    out[++k] = a[i]
  }
  r = ""
  for (i = 1; i <= k; i++) r = r "/" out[i]
  return (r == "" ? "/" : r)
}
function unq(s) { gsub(/\\"/, "\"", s); gsub(/\\\\/, "\\", s); return s }
# git's reads (the version stamp's describe --dirty, status) are not inputs: the commit is not in a key
function emit(op, path, pid,   k) {
  if (exe[pid] ~ /\/git(-[a-z-]+)?$/) return
  if (op == "R" && exe[pid] ~ /\/sha256sum$/) op = "H"
  k = op "\t" path
  if (!(k in seen)) { seen[k] = 1; print k }
}
# the path argument of CALL's text ARGS: [dirfd, ] "path"; returns "" without one. dirfd: AT_FDCWD
# (the process's directory), N<dir> (-y), or a bare N (a descriptor strace could not name)
function target(args, pid, haspathfirst,   m, base, q) {
  if (haspathfirst) {
    if (match(args, /^"([^"\\]|\\.)*"/)) return resolve(unq(substr(args, 2, RLENGTH - 2)), cwd[pid])
    return ""
  }
  base = cwd[pid]
  if (match(args, /^AT_FDCWD<[^>]*>, /)) {
    m = substr(args, 1, RLENGTH); sub(/^AT_FDCWD</, "", m); sub(/>, $/, "", m); base = m
    args = substr(args, RLENGTH + 1)
  } else if (match(args, /^AT_FDCWD, /)) args = substr(args, RLENGTH + 1)
  else if (match(args, /^[0-9]+<[^>]*>, /)) {
    m = substr(args, 1, RLENGTH); sub(/^[0-9]+</, "", m); sub(/>, $/, "", m); base = m
    args = substr(args, RLENGTH + 1)
  } else if (match(args, /^[0-9]+, /)) { return "" }
  if (match(args, /^"([^"\\]|\\.)*"/)) return resolve(unq(substr(args, 2, RLENGTH - 2)), base)
  return ""
}
function resolve(p, base) {
  if (substr(p, 1, 1) == "/") return norm(p)
  if (p == "") return ""
  return norm(base "/" p)
}
function opfor(call, args, flags) {
  if (call ~ /^(unlink|unlinkat|rmdir|rename|renameat|renameat2|mkdir|mkdirat|symlink|symlinkat|link|linkat|chmod|fchmodat|fchmodat2|chown|lchown|fchownat|truncate|utimensat|utimes|futimesat|mknod|mknodat|setxattr|lsetxattr|removexattr|lremovexattr|creat)$/) return "W"
  if (call ~ /^(open|openat|openat2)$/) {
    if (flags ~ /O_DIRECTORY/) return "D"
    if (flags ~ /O_WRONLY|O_RDWR|O_CREAT|O_TRUNC/) return "W"
    return "R"
  }
  if (call ~ /^(readlink|readlinkat)$/) return "R"
  if (call ~ /^execve/) return "X"
  return "S"
}
BEGIN { cwd[0] = root }
{
  line = $0
  if (dbg != "" && line ~ /^[0-9]+ +(\+\+\+|---)/) print line > dbg
  if (!match(line, /^[0-9]+ +/)) next
  pid = substr(line, 1, RLENGTH); sub(/ +$/, "", pid); line = substr(line, RLENGTH + 1)
  if (!(pid in cwd)) cwd[pid] = root
  alive[pid] = 1
  # a call resumed: only clone's result matters (its path arguments came in the unfinished line)
  if (match(line, /^<\.\.\. [a-z_0-9]+ resumed>/)) {
    call = substr(line, 6, RLENGTH - 14)
    if (call ~ /^(clone|clone3|fork|vfork)$/ && match(line, /\) += [0-9]+/)) {
      child = substr(line, RSTART, RLENGTH); sub(/^[^=]*= */, "", child); child += 0
      if (child > 0 && !(child in alive)) { cwd[child] = cwd[pid]; exe[child] = exe[pid] }
    }
    next
  }
  if (!match(line, /^[a-z_0-9]+\(/)) next
  call = substr(line, 1, RLENGTH - 1); args = substr(line, RLENGTH + 1)
  if (call ~ /^(clone|clone3|fork|vfork)$/) {
    if (match(args, /\) += [0-9]+/)) {
      child = substr(args, RSTART, RLENGTH); sub(/^[^=]*= */, "", child); child += 0
      if (child > 0 && !(child in alive)) { cwd[child] = cwd[pid]; exe[child] = exe[pid] }
    }
    next
  }
  if (call == "chdir") {
    p = target(args, pid, 1)
    if (p != "" && args !~ /= -1 /) cwd[pid] = p
    next
  }
  if (call == "execve") { p = target(args, pid, 1); if (p != "") { exe[pid] = p; emit("X", p, pid) }; next }
  if (call == "execveat") { p = target(args, pid, 0); if (p != "") emit("X", p, pid); next }
  # the calls whose first argument is the path
  if (call ~ /^(open|creat|stat|lstat|access|readlink|unlink|rmdir|mkdir|chmod|chown|lchown|truncate|utimes|rename|link|symlink|mknod|setxattr|lsetxattr|removexattr|lremovexattr|getxattr|lgetxattr|listxattr|llistxattr|statfs|utime)$/) {
    p = target(args, pid, 1)
    if (p != "") emit(opfor(call, args, args), p, pid)
    next
  }
  # the calls with a directory descriptor first
  if (call ~ /^(openat|openat2|newfstatat|fstatat64|statx|faccessat|faccessat2|readlinkat|unlinkat|mkdirat|renameat|renameat2|linkat|symlinkat|fchmodat|fchmodat2|fchownat|utimensat|futimesat|mknodat)$/) {
    p = target(args, pid, 0)
    if (p != "") emit(opfor(call, args, args), p, pid)
    next
  }
}
