(ns skradanka.textmode
  (:require [clojure.string :as str]))

(def glyphs {:empty  "·"
             :player "@"
             :npc    "Å"})

(defn calculate-textmode [state]
  (let [row    (apply str (take 5 (repeat (:empty glyphs))))
        grid   (take 5 (repeat row))
        result (str/join "\n" grid)]
    result))
