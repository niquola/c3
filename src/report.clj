(ns report)

;; {:repo-full-name "foo/bar"
;;  :commit "deadbeef"
;;  :steps [{:name "step1" :state :succeeded}
;;          {:name "step2" :state :running}
;;          {:name "step3" :state :pending}]}

(defn ^:private build-steps-states [steps]
  (->> steps
       (map :state)
       (into #{})))

(defn ^:private build-state [steps]
  (let [states (build-steps-states steps)]
    (cond
      (contains? states :running) :running
      (contains? states :pending) :running
      (and (contains? states :failed)) :failed
      (contains? states :succeeded) :succeeded
      :else :pending)))

(def ^:private build-state-markup
  {:pending "\u231b Pending"
   :succeeded "\u2705 *Succeeded*"
   :failed "\u26d4 *Failed*"
   :running "\u267f Running"})

(def ^:private step-state-markup
  {:pending "    "
   :running "\u21e2 "
   :succeeded "\u2713 "
   :failed "\u2717 "})

(defn ^:private repo-url [repo-full-name]
  (str "https://github.com/" repo-full-name))

(defn ^:private format-repo [repo-full-name]
  (str "[" repo-full-name "](" (repo-url repo-full-name) ")"))

(defn ^:private format-commit [repo-full-name commit]
  (format "[%s](%s/commit/%s)"
          (.substring commit 0 6)
          (repo-url repo-full-name)
          commit))

(defn ^:private format-step [step]
  (str (step-state-markup (:state step)) (:name step)))

(defn ^:private format-steps [steps]
  (->> steps
       (map format-step)
       (interpose "\n")
       (apply str)))

(defn report [{:keys [repo-full-name commit steps]}]
  (str (build-state-markup (build-state steps)) " "
       (format-commit repo-full-name commit) " @ "
       (format-repo repo-full-name) "\n"
       (format-steps steps)))

(comment
  (format-commit "dottedmag/c3"
                 "e7f74e134dc4f6f44b786b8b02b2aa805dd4e599")
  (format-repo "niquola/c3")

  (def sp {:name "step-pending" :state :pending})
  (def sr {:name "step-running" :state :running})
  (def ss {:name "step-succeeded" :state :succeeded})
  (def sf {:name "step-failed" :state :failed})

  (format-step sp)
  (format-step sr)
  (format-step ss)
  (format-step sf)

  (print (format-steps [sp sr ss sf]))

  (= :pending (build-state [sp sp sp sp]))
  (= :running (build-state [ss ss sr sp]))
  (= :failed (build-state [ss ss sf sp]))
  (= :succeeded (build-state [ss ss ss ss]))

  (print (report {:repo-full-name "dottedmag/c3"
                  :commit "e7f74e134dc4f6f44b786b8b02b2aa805dd4e599"
                  :steps [ss ss sf sp]}))

  (print (report {:repo-full-name "dottedmag/c3"
                  :commit "e7f74e134dc4f6f44b786b8b02b2aa805dd4e599"
                  :steps [sp sp sp sp]}))
)
