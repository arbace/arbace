;; Go-build variant of java.time.format.DateTimeFormatterBuilder (C2G-SPEC §4.6;
;; doc/go/JRT-NOTES.md, "Time"): read by c2g only. The localized patterns come from the locale's
;; resources (jrt's LocaleResources over java.base's CLDR data) directly, where the JDK asks the
;; adapter for its JavaTimeDateTimePatternProvider (a service provider class the Go build does
;; not have), whose implementation asks the same resources. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'java.time.format)

(c2g/variant DateTimeFormatterBuilder
  (method ^:public ^:static getLocalizedDateTimePattern
    ^String [^FormatStyle dateStyle ^FormatStyle timeStyle ^Chronology chrono ^Locale locale]
    (Objects/requireNonNull locale "locale")
    (Objects/requireNonNull chrono "chrono")
    (when (and (nil? dateStyle) (nil? timeStyle))
      (throw (IllegalArgumentException. "Either dateStyle or timeStyle must be non-null")))
    (.getJavaTimeDateTimePattern
      (.getLocaleResources (sun.util.locale.provider.LocaleProviderAdapter/getResourceBundleBased)
                           (sun.util.locale.provider.CalendarDataUtility/findRegionOverride locale))
      (DateTimeFormatterBuilder/convertStyle timeStyle)
      (DateTimeFormatterBuilder/convertStyle dateStyle)
      (.getCalendarType chrono)))

  (method ^:public ^:static getLocalizedDateTimePattern
    ^String [^String requestedTemplate ^Chronology chrono ^Locale locale]
    (Objects/requireNonNull requestedTemplate "requestedTemplate")
    (Objects/requireNonNull chrono "chrono")
    (Objects/requireNonNull locale "locale")
    (sun.util.locale.provider.LocaleResources/getJavaTimeDateTimePattern
      requestedTemplate (.getCalendarType chrono)
      (sun.util.locale.provider.CalendarDataUtility/findRegionOverride locale))))
