;; jrt: the natives of jdk.internal.misc.CDS (doc/go/JRT-NOTES.md, "Text and locales"), which
;; jdk26u's sun.util.locale.BaseLocale and the referenced-key caches call: the Go build has no
;; class data sharing archive, so the configuration is empty (nothing dumped, no archive used)
;; and a class has no archived statics to initialize from.
(in-ns 'go.arbace.jrt)

(go/file "text.go")

(go/func CDS_GetCDSConfigStatus__I_native
  "CDS_GetCDSConfigStatus__I_native is CDS.getCDSConfigStatus: 0, no archive.\n"
  ^int32 []
  0)

(go/func CDS_InitializeFromArchive_Class__V_native
  "CDS_InitializeFromArchive_Class__V_native is CDS.initializeFromArchive: nothing archived.\n"
  [^{:tag (* Class)} c])
