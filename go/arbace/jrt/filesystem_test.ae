;; jrt's tests: the file system natives under java.io.File (filesystem.clj) over the host's
;; HostFS (hostfs.clj, hostfs_linux.clj), in a temporary directory; doc/go/JRT-NOTES.md, "Files".
(in-ns 'go.arbace.jrt)

(go/file "filesystem_test.go"
  :imports [[os "os"] [filepath "path/filepath"] [testing "testing"]])

(go/func TestCollapsePath
  "collapsePath removes . and name/.. as path_util.c's collapse does (a single name is left as it is).\n"
  [^{:tag (* testing/T)} t]
  (let [cases (lit (slice string) "/a/b" "/a/b" "/a/./b" "/a/b" "/a/../b" "/b" "/../a" "/a"
                   "a/../../b" "../b" "/a/b/.." "/a" "/." "/." "/" "/" "a" "a")]
    (for [i 0] (< i (len cases)) (set! i + 2)
      (let [got (collapsePath (aget cases i))]
        (when (!= got (aget cases (+ i 1)))
          (.Errorf t "collapsePath(%q) = %q, want %q" (aget cases i) got (aget cases (+ i 1))))))))

(go/func TestFileSystemNatives
  "The natives of UnixFileSystem's variant on a temporary directory: attributes, creation,
listing, renaming, times, permissions, canonical paths, space.\n"
  [^{:tag (* testing/T)} t]
  (let [dir (.TempDir t)
        (values real err) (filepath/EvalSymlinks dir)]
    (when (!= err nil)
      (.Fatal t err))
    (let [a (Str (+ dir "/a"))
          b (Str (+ dir "/b"))
          sub (Str (+ dir "/sub"))]
      (when (!= (UnixFileSystem_HostBooleanAttributes_String__I_native a) 0)
        (.Error t "attributes of a missing file"))
      (when (not (UnixFileSystem_HostCreateFileExclusively_String__Z_native a))
        (.Error t "create"))
      (when (UnixFileSystem_HostCreateFileExclusively_String__Z_native a)
        (.Error t "create of an existing file"))
      (when (!= (UnixFileSystem_HostBooleanAttributes_String__I_native a) (bit-or baExists baRegular))
        (.Error t "attributes of a file"))
      (os/WriteFile (+ dir "/a") (conv (slice byte) "hello") 0644)
      (when (!= (UnixFileSystem_HostLength_String__J_native a) 5)
        (.Error t "length"))
      (when (not (UnixFileSystem_HostCreateDirectory_String__Z_native sub))
        (.Error t "mkdir"))
      (when (UnixFileSystem_HostCreateDirectory_String__Z_native sub)
        (.Error t "mkdir of an existing directory"))
      (when (!= (UnixFileSystem_HostBooleanAttributes_String__I_native sub) (bit-or baExists baDirectory))
        (.Error t "attributes of a directory"))
      (let [names (UnixFileSystem_HostList_String__String1_native (Str dir))]
        (when (or (== names nil) (!= (len (.-A names)) 2)
                  (!= (.String (assert (* String) (aget (.-A names) 0))) "a")
                  (!= (.String (assert (* String) (aget (.-A names) 1))) "sub"))
          (.Errorf t "list: %v" names)))
      (when (!= (UnixFileSystem_HostList_String__String1_native b) nil)
        (.Error t "list of a missing directory"))
      (when (not (UnixFileSystem_HostRename_String_String__Z_native a b))
        (.Error t "rename"))
      (when (UnixFileSystem_HostDelete_String__Z_native a)
        (.Error t "delete of a renamed file"))
      (when (not (UnixFileSystem_HostSetLastModifiedTime_String_J__Z_native b 1234567890123))
        (.Error t "set the modification time"))
      (when (!= (UnixFileSystem_HostLastModifiedTime_String__J_native b) 1234567890123)
        (.Errorf t "modification time: %d" (UnixFileSystem_HostLastModifiedTime_String__J_native b)))
      (when (not (UnixFileSystem_HostSetPermission_String_I_Z_Z__Z_native b 1 true false))
        (.Error t "set execute"))
      (let [(values fi _) (os/Stat (+ dir "/b"))]
        (when (!= (bit-and (.Mode fi) 0111) 0111)
          (.Errorf t "execute for everyone: %v" (.Mode fi))))
      (when (not (UnixFileSystem_HostSetPermission_String_I_Z_Z__Z_native b 1 false true))
        (.Error t "clear the owner's execute"))
      (let [(values fi _) (os/Stat (+ dir "/b"))]
        (when (!= (bit-and (.Mode fi) 0111) 0011)
          (.Errorf t "the owner's execute cleared: %v" (.Mode fi))))
      (when (not (UnixFileSystem_HostSetReadOnly_String__Z_native b))
        (.Error t "read-only"))
      (let [(values fi _) (os/Stat (+ dir "/b"))]
        (when (!= (bit-and (.Mode fi) 0222) 0)
          (.Errorf t "read-only: %v" (.Mode fi))))
      (when (not (UnixFileSystem_HostCheckAccess_String_I__Z_native b 4))
        (.Error t "readable"))
      (when (UnixFileSystem_HostCheckAccess_String_I__Z_native (Str (+ dir "/none")) 4)
        (.Error t "a missing file readable"))
      (let [c (.String (UnixFileSystem_HostCanonicalize_String__String_native (Str (+ dir "/sub/../b"))))]
        (when (!= c (+ real "/b"))
          (.Errorf t "canonical: %q" c)))
      (let [c (.String (UnixFileSystem_HostCanonicalize_String__String_native (Str (+ dir "/none/./x/../y"))))]
        (when (!= c (+ real "/none/y"))
          (.Errorf t "canonical of a missing path: %q" c)))
      (when (<= (UnixFileSystem_HostSpace_String_I__J_native (Str dir) spaceTotal) 0)
        (.Error t "total space"))
      (when (<= (UnixFileSystem_HostNameMax_String__J_native (Str dir)) 0)
        (.Error t "name max"))
      (when (UnixFileSystem_HostDelete_String__Z_native (Str dir))
        (.Error t "delete of a directory that is not empty"))
      (when (not (and (UnixFileSystem_HostDelete_String__Z_native b) (UnixFileSystem_HostDelete_String__Z_native sub)))
        (.Error t "delete")))))
