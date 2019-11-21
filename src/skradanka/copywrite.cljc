(ns skradanka.copywrite)

(def action-texts
  {:disparage-others-value    "disparages value held by"
   :discuss-shared-value      "discusses a shared value with"
   :praise-others-value       "praises value held by"
   :make-request              "makes a request of"
   :praise-own-value          "praises their own value to"
   :notice-presence           "notices the presence of"
   :ignore                    "is ignoring"
   :insult                    "insults"
   :accuse-of-violating-value "thinks that a value has been violated by"
   :compliment                "compliments"
   :confide-in                "confides in"
   :confess-to                "confesses to"
   :accuse-of-hypocrisy       "accuses the hypocritical"
   :assert-authority          "asserts their authority before"
   :undermine-authority       "undermines the authority of"
   :assist-with-research      "assists with research done by"
   :interfere-with-research   "interferes with research done by"})

(defmulti action->button-text  (fn [action]
                                 (:name action)))

(defmethod action->button-text :default
  [{:keys [type
           name
           harms
           harms-value
           significance
           helps
           helps-value
           vars]}]
  (let [[c1-id c1-name c2-id c2-name] vars]
    (str c1-name " " name " " c2-name)))

(defmethod action->button-text :disparage-others-value
  [{:keys [type
           name
           harms
           harms-value
           significance
           helps
           helps-value
           vars]}]
  (let [[c1-id c1-name c2-id c2-name] vars]
    (str c1-name " " name " " c2-name)))
