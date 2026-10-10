;; Go-build variant of java.util.concurrent.SubmissionPublisher (C2G-SPEC §4.6, §8.3;
;; doc/go/JRT-NOTES.md, "Concurrency"): read by c2g only. Fields of an interface Go type (two
;; words) that other threads read without a lock are volatile here: the publisher's owner
;; thread (offer's fast path), a subscription's executor and pending error, a consumer's
;; subscription. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.util.concurrent)

(c2g/variant SubmissionPublisher
  (field ^:volatile ^Thread owner))

(c2g/variant SubmissionPublisher$BufferedSubscription
  (field ^:volatile ^Executor executor)
  (field ^:volatile ^Throwable pendingError))

(c2g/variant SubmissionPublisher$ConsumerSubscriber
  (field ^:volatile ^Flow$Subscription subscription))
