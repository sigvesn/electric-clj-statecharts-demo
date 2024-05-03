(ns app.statechart-clj-demo
  (:require [hyperfiddle.electric :as e]
            [statecharts.core :as fsm]
            [hyperfiddle.electric-ui4 :as ui]
            [hyperfiddle.electric-dom2 :as dom]))

(defn traffic-signal
  [id initial]
  (fsm/machine
    {:id      id
     :initial initial
     :states
     {:red    {:on
               {:swap-flow :green}}
      :yellow {:on
               {:swap-flow :red}}
      :green  {:on
               {:swap-flow :green
                :warn-traffic :yellow}}}}))

(defn ped-signal
  [id initial]
  (fsm/machine
    {:id id
     :initial initial
     :states
     {:red {:on
            {:swap-flow :white}}
      :flashing-white {:on
                       {:swap-flow :red}}
      :white {:on
              {:swap-flow :white
               :warn-pedestrians :flashing-white}}}}))

(defn cross-signals
  []
  (let [service (fsm/service
                  {:id       :cross-signals
                   :type     :parallel
                   :regions {:east-west   (traffic-signal :east-west :green)
                             :north-south (traffic-signal :north-south :red)

                             :cross-ew (ped-signal :cross-ew :red)
                             :cross-ns (ped-signal :cross-ns :white)}})]
    (fsm/start service)
    service))

(e/defn Red []
  (e/client
    (dom/span (dom/style {:color "red"})
             (dom/text "Red"))))

(e/defn Green []
  (e/client
    (dom/span (dom/style {:color "green"})
             (dom/text "Green"))))

(e/defn Yellow []
  (e/client
    (dom/span (dom/style {:color "yellow"})
             (dom/text "Yellow"))))

(e/defn White []
  (e/client
    (dom/span (dom/style {:color "gray"})
             (dom/text "white"))))

(e/defn Flashing-White []
  (e/client
    (if (= 0 (int (mod e/system-time-secs 2)))
      (dom/span (dom/style {:color "gray"})
               (dom/text "Flashing white"))
      (dom/span (dom/br)))))

(e/defn Signal
  [signal]
  (case signal
    :red    (Red.)
    :green  (Green.)
    :yellow (Yellow.)
    :white  (White.)
    :flashing-white (Flashing-White.)
    nil))


#?(:clj (def statechart (cross-signals)))
#?(:clj (def !statechart (atom (fsm/value statechart))))
(e/def state (e/server (e/watch !statechart)))

(e/defn TrafficLights []
  (e/server
    (e/client
      (dom/div
        (e/for-by key [[t signal] state]
                  (dom/div
                    (dom/text (name t) ": ")
                    (Signal. signal)))
        (ui/button (e/fn [] (e/server
                              (fsm/send statechart :warn-pedestrians)
                              (reset! !statechart (fsm/value statechart))))
                   (dom/text "warn pedestrians"))
        (ui/button (e/fn [] (e/server
                              (fsm/send statechart :warn-traffic)
                              (reset! !statechart (fsm/value statechart))))
                   (dom/text "warn traffic"))
        (ui/button (e/fn [] (e/server
                              (fsm/send statechart :swap-flow)
                              (reset! !statechart (fsm/value statechart))))
                   (dom/text "swap flow"))))))


(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (dom/h1 (dom/text "Traffic Lights"))
      (TrafficLights.))))
