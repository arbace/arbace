;; Go-build variant of java.time.zone.ZoneRulesProvider (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Time"): read by c2g only. The provider list is a synchronized ArrayList (CopyOnWriteArrayList
;; is not in the closed world; providers are registered rarely and the list is only iterated by
;; refresh), and the static initializer registers the time-zone database's provider alone:
;; there is no ServiceLoader in the Go build, and the property
;; java.time.zone.DefaultZoneRulesProvider (a class to load by name) is not read. Copyright (c)
;; the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.time.zone)

(c2g/variant ZoneRulesProvider
  (field ^:private ^:static ^:final ^java.util.List PROVIDERS
    (java.util.Collections/synchronizedList (java.util.ArrayList.)))

  (static-initializer
    (ZoneRulesProvider/registerProvider (TzdbZoneRulesProvider.))))
