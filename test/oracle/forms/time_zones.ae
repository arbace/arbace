;; The names of every time zone (doc/go/JRT-NOTES.md, "Time"): for each ID of
;; TimeZone/getAvailableIDs (the time-zone database's regions and the old short IDs), its
;; standard and daylight names, long and short (TimeZone.getDisplayName), and its generic names
;; (ZoneId.getDisplayName, for the region IDs), in the US English and the root locales; in
;; groups of 25 IDs in sorted order. The names come from java.base's CLDR data and the JDK's
;; derivations where CLDR has none.

(import '(java.time ZoneId)
        '(java.time.format TextStyle)
        '(java.util Locale TimeZone))

(def zone-ids (vec (sort (TimeZone/getAvailableIDs))))
(def region-ids (set (ZoneId/getAvailableZoneIds)))

(defn zone-names [group ^Locale l]
  (vec (for [id (take 25 (drop (* 25 group) zone-ids))]
         (let [tz (TimeZone/getTimeZone ^String id)]
           (into [id (.getDisplayName tz false TimeZone/LONG l) (.getDisplayName tz false TimeZone/SHORT l)
                  (.getDisplayName tz true TimeZone/LONG l) (.getDisplayName tz true TimeZone/SHORT l)]
                 (when (region-ids id)
                   (let [z (ZoneId/of id)]
                     [(.getDisplayName z TextStyle/FULL l) (.getDisplayName z TextStyle/SHORT l)])))))))

(count zone-ids)
(zone-names 0 Locale/US)
(zone-names 1 Locale/US)
(zone-names 2 Locale/US)
(zone-names 3 Locale/US)
(zone-names 4 Locale/US)
(zone-names 5 Locale/US)
(zone-names 6 Locale/US)
(zone-names 7 Locale/US)
(zone-names 8 Locale/US)
(zone-names 9 Locale/US)
(zone-names 10 Locale/US)
(zone-names 11 Locale/US)
(zone-names 12 Locale/US)
(zone-names 13 Locale/US)
(zone-names 14 Locale/US)
(zone-names 15 Locale/US)
(zone-names 16 Locale/US)
(zone-names 17 Locale/US)
(zone-names 18 Locale/US)
(zone-names 19 Locale/US)
(zone-names 20 Locale/US)
(zone-names 21 Locale/US)
(zone-names 22 Locale/US)
(zone-names 23 Locale/US)
(zone-names 24 Locale/US)
(zone-names 25 Locale/US)
(zone-names 0 Locale/ROOT)
(zone-names 1 Locale/ROOT)
(zone-names 2 Locale/ROOT)
(zone-names 3 Locale/ROOT)
(zone-names 4 Locale/ROOT)
(zone-names 5 Locale/ROOT)
(zone-names 6 Locale/ROOT)
(zone-names 7 Locale/ROOT)
(zone-names 8 Locale/ROOT)
(zone-names 9 Locale/ROOT)
(zone-names 10 Locale/ROOT)
(zone-names 11 Locale/ROOT)
(zone-names 12 Locale/ROOT)
(zone-names 13 Locale/ROOT)
(zone-names 14 Locale/ROOT)
(zone-names 15 Locale/ROOT)
(zone-names 16 Locale/ROOT)
(zone-names 17 Locale/ROOT)
(zone-names 18 Locale/ROOT)
(zone-names 19 Locale/ROOT)
(zone-names 20 Locale/ROOT)
(zone-names 21 Locale/ROOT)
(zone-names 22 Locale/ROOT)
(zone-names 23 Locale/ROOT)
(zone-names 24 Locale/ROOT)
(zone-names 25 Locale/ROOT)
