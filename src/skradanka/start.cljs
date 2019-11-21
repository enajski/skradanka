(ns skradanka.start
  (:require [skradanka.core :as c]
            [skradanka.copywrite :as copywrite]
            [play-cljc.gl.core :as pc]
            [goog.events :as events]
            [reagent.core :as reagent])
  (:require-macros [skradanka.music :refer [build-for-cljs]]))


(defonce ui-state (reagent/atom @c/*state))

(defn sync-ui []
  (reset! ui-state @c/*state))

(defn resize [{:keys [context] :as game}]
  (let [display-width context.canvas.clientWidth
        display-height context.canvas.clientHeight]
    (when (or (not= context.canvas.width display-width)
              (not= context.canvas.height display-height))
      (set! context.canvas.width display-width)
      (set! context.canvas.height display-height))))

(defn game-loop [game]
  (resize game)
  (let [game (c/tick game)]
    (js/requestAnimationFrame
      (fn [ts]
        (let [ts (* ts 0.001)]
          (game-loop (assoc game
                            :delta-time (- ts (:total-time game))
                            :total-time ts)))))))

(defn listen-for-mouse [canvas]
  (events/listen js/window "mousemove"
    (fn [event]
      (swap! c/*state
        (fn [state]
          (let [bounds (.getBoundingClientRect canvas)
                x (- (.-clientX event) (.-left bounds))
                y (- (.-clientY event) (.-top bounds))]
            (assoc state :mouse-x x :mouse-y y)))))))

(defn keycode->keyword [keycode]
  (condp = keycode
    37 :left
    39 :right
    38 :up
    nil))

(defn listen-for-keys []
  (events/listen js/window "keydown"
    (fn [event]
      (when-let [k (keycode->keyword (.-keyCode event))]
        (swap! c/*state update :pressed-keys conj k))))
  (events/listen js/window "keyup"
    (fn [event]
      (when-let [k (keycode->keyword (.-keyCode event))]
        (swap! c/*state update :pressed-keys disj k)))))


(defonce textmode-div (js/document.querySelector "#textmode"))

(defonce console-div (js/document.querySelector "#console"))

(defonce choices-div (js/document.querySelector "#choices"))

(declare render-choices)

(defn choice-item [i choice]
  [:button {:data-choice-id i
            :on-click (fn [e]
                        (when-let [choice-id (.-value (.getNamedItem (.-attributes (.-target e)) "data-choice-id" ))]
                          (let [{:keys [choices]} @c/*state
                                choice (get choices (int choice-id))]
                            (c/tick-sim choice)

                            (sync-ui)

                            (let [{textmode-value :textmode
                                   console-value  :console} @c/*state]
                              (set! (.-innerHTML textmode-div) textmode-value)
                              (set! (.-innerHTML console-div) console-value)))))}
   (copywrite/action->button-text choice)])


(defn choices-list [choices]
  [:div
   (map-indexed
    (fn [i choice]
      (choice-item i choice))
    choices)])


(defn root []
  (let [{:keys [choices]} @ui-state]
    (println "choices:" choices)
    [:div#root
     (choices-list choices)]))


(defn render-choices []
  (println "i am render")
  (reagent/render [root]
                  choices-div))




;; start the game

(defonce context
  (let [canvas (js/document.querySelector "canvas")
        context (.getContext canvas "webgl2")
        initial-game (assoc (pc/->game context)
                            :delta-time 0
                            :total-time 0)]
    (listen-for-mouse canvas)
    (listen-for-keys)

    (c/init initial-game)

    (sync-ui)

    (render-choices)
    (game-loop initial-game)
    context))

;; build music, put it in the audio tag, and make the button toggle it on and off

(defonce play-music? (atom false))

(defonce audio (js/document.querySelector "#audio"))
(set! (.-src audio) (build-for-cljs))
(when @play-music? (.play audio))

(defonce audio-button (js/document.querySelector "#audio-button"))
(set! (.-onclick audio-button)
      (fn [e]
        (if (swap! play-music? not)
          (.play audio)
          (.pause audio))))




(defonce next-turn-button (js/document.querySelector "#next-turn-button"))
(set! (.-onclick next-turn-button)
      (fn [e]
        (c/tick-sim)

        (let [{textmode-value :textmode
               console-value  :console} @c/*state]
          (set! (.-innerHTML textmode-div) textmode-value)
          (set! (.-innerHTML console-div) console-value))))

