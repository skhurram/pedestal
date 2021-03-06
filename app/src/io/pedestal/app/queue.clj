; Copyright 2013 Relevance, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(ns ^:shared io.pedestal.app.queue
  "A very simple application message queue implementation which can be used from both
  Clojure and ClojureScript. In the future, there will be both Clojure and ClojureScript
  implementations of this queue which will take advantage of the capabilities of each
  platform."
  (:require [io.pedestal.app.protocols :as p]
            [io.pedestal.app.util.platform :as platform]))

(defn- pop-message-internal [queue-state]
  (let [queue (:queue queue-state)]
    (assoc queue-state
      :item (first queue)
      :queue (vec (rest queue)))))

(defn- process-next-item [queue f]
  (if (seq (:queue @queue))
    (when-let [item (:item (swap! queue pop-message-internal))]
      (platform/log-exceptions f item))
    (platform/create-timeout 1 (fn [] (process-next-item queue f)))))

(defrecord AppMessageQueue [state]
  p/PutMessage
  (put-message [this message]
    (let [q (swap! state update-in [:queue] conj message)]
      (count (:queue q))))
  p/TakeMessage
  (pop-message [this]
    (:item (swap! state pop-message-internal)))
  (take-message [this f]
    (process-next-item state f)))

(defn queue [name]
  (->AppMessageQueue (atom {:queue [] :item nil :name name})))
