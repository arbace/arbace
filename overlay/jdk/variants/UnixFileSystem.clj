;; Go-build variant of java.io.UnixFileSystem (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"):
;; read by c2g only. UnixFileSystem is translated from jdk26u (src/java.base/unix/classes); its
;; natives (UnixFileSystem_md.c, canonicalize_md.c) take the File and read its path field
;; through JNI, which a jrt native cannot (jrt builds without the translated classes, C2G-SPEC
;; §9.1). Each native is replaced by a method passing the File's path to a static native of
;; jrt over the host (go/arbace/jrt/filesystem.clj, hostfs.clj), with the same results as the
;; JDK's C. The constructor reads the separators and user.dir through System.getProperty
;; (System.getProperties' Properties is a Hashtable, and StaticProperty, the JDK's snapshot of
;; the properties at start-up, are not in the closed world); initIDs is
;; gone (no JNI field IDs). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.io)

(c2g/variant UnixFileSystem
  (constructor [this]
    (set! slash (.charAt (System/getProperty "file.separator") 0))
    (set! colon (.charAt (System/getProperty "path.separator") 0))
    (set! userDir (System/getProperty "user.dir")))

  (method ^:private canonicalize0 :throws [IOException] ^String [this ^String path]
    (UnixFileSystem/hostCanonicalize path))
  (method ^:private getBooleanAttributes0 ^int [this ^File f]
    (UnixFileSystem/hostBooleanAttributes (.getPath f)))
  (method ^:private checkAccess0 ^boolean [this ^File f ^int access]
    (UnixFileSystem/hostCheckAccess (.getPath f) access))
  (method ^:private getLastModifiedTime0 ^long [this ^File f]
    (UnixFileSystem/hostLastModifiedTime (.getPath f)))
  (method ^:private getLength0 ^long [this ^File f]
    (UnixFileSystem/hostLength (.getPath f)))
  (method ^:private setPermission0 ^boolean [this ^File f ^int access ^boolean enable ^boolean owneronly]
    (UnixFileSystem/hostSetPermission (.getPath f) access enable owneronly))
  (method ^:private createFileExclusively0 :throws [IOException] ^boolean [this ^String path]
    (UnixFileSystem/hostCreateFileExclusively path))
  (method ^:private delete0 ^boolean [this ^File f]
    (UnixFileSystem/hostDelete (.getPath f)))
  (method ^:private list0 ^String/1 [this ^File f]
    (UnixFileSystem/hostList (.getPath f)))
  (method ^:private createDirectory0 ^boolean [this ^File f]
    (UnixFileSystem/hostCreateDirectory (.getPath f)))
  (method ^:private rename0 ^boolean [this ^File f1 ^File f2]
    (UnixFileSystem/hostRename (.getPath f1) (.getPath f2)))
  (method ^:private setLastModifiedTime0 ^boolean [this ^File f ^long time]
    (UnixFileSystem/hostSetLastModifiedTime (.getPath f) time))
  (method ^:private setReadOnly0 ^boolean [this ^File f]
    (UnixFileSystem/hostSetReadOnly (.getPath f)))
  (method ^:private getSpace0 ^long [this ^File f ^int t]
    (UnixFileSystem/hostSpace (.getPath f) t))
  (method ^:private getNameMax0 ^long [this ^String path]
    (UnixFileSystem/hostNameMax path))

  (c2g/cut ^:private ^:static initIDs ^void [])
  (c2g/cut (static-initializer 0))

  (c2g/add (method ^:private ^:static ^:native hostCanonicalize :throws [IOException] ^String [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostBooleanAttributes ^int [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostCheckAccess ^boolean [^String path ^int access]))
  (c2g/add (method ^:private ^:static ^:native hostLastModifiedTime ^long [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostLength ^long [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostSetPermission ^boolean [^String path ^int access ^boolean enable
                                                                   ^boolean owneronly]))
  (c2g/add (method ^:private ^:static ^:native hostCreateFileExclusively :throws [IOException] ^boolean [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostDelete ^boolean [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostList ^String/1 [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostCreateDirectory ^boolean [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostRename ^boolean [^String from ^String to]))
  (c2g/add (method ^:private ^:static ^:native hostSetLastModifiedTime ^boolean [^String path ^long time]))
  (c2g/add (method ^:private ^:static ^:native hostSetReadOnly ^boolean [^String path]))
  (c2g/add (method ^:private ^:static ^:native hostSpace ^long [^String path ^int t]))
  (c2g/add (method ^:private ^:static ^:native hostNameMax ^long [^String path])))
