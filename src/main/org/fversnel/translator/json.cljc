(ns org.fversnel.translator.json
  #?(:clj (:require [clojure.instant]
                    [clojure.data.json])
     :cljs (:require [cljs.tagged-literals])))

(defprotocol Parse-Json-From
  "Something that can be parsed as JSON into Clojure data"
  (parse-json-from [this opts]))

#?(:clj
   (extend-protocol Parse-Json-From
     java.io.Reader
     (parse-json-from [reader opts]
       (let [opts (merge
                   {:key-fn (if (::keywordize-keys? opts) keyword identity)}
                   opts)]
       (with-open [reader reader]
         (clojure.data.json/read reader opts))))

     java.io.InputStream
     (parse-json-from [stream opts]
       (parse-json-from (java.io.InputStreamReader. stream "UTF-8") opts))

     java.lang.String
     (parse-json-from [string opts]
       (parse-json-from (java.io.StringReader. string) opts)))

   :cljs
   (extend-protocol Parse-Json-From
     js/String
     (parse-json-from [s opts]
       (let [opts (merge 
                   {:keywordize-keys (true? (::keywordize-keys? opts))}
                   opts)]
       (js->clj (.parse js/JSON s) opts)))))

#?(:clj
   (extend-type (Class/forName "[B")
     Parse-Json-From
     (parse-json-from [bytes opts]
       (parse-json-from (java.io.ByteArrayInputStream. bytes) opts))))


(defn translate
  [translation json]
  (cond
    (nil? translation)
    json

    (nil? json)
    nil

    (vector? json)
    (mapv (partial translate translation) json)

    (seq? json)
    (map (partial translate translation) json)

    (map? json)
    (reduce-kv
     (fn [m key value]
       (let [translation (get translation key)
             associate-value (fn [key value]
                               (if value (assoc m key value) m))]
         (cond
           (map? translation)
           (if-not (::merge-with-parent? translation)
             (let [new-key (or (::translated-key translation) key)]
               (associate-value new-key (translate translation value)))
             (merge m (translate translation value)))

           (vector? translation)
           (let [[translation-key translation-fn] translation]
             (associate-value translation-key (translate translation-fn value)))

           (keyword? translation)
           (associate-value translation value)

           :else
           m)))
     {}
     json)

    (fn? translation)
    (translation json)
    
    :else
    json))


(defn parse
  ([x]
   (parse {::translation nil} x))
  ([{::keys [translation] :as opts} x]
   (translate translation (parse-json-from x opts))))

(defn read-instant [s]
  #?(:clj
     (try
       (clojure.instant/read-instant-date s)
       (catch Exception _ nil))
     :cljs
     (try
       (cljs.tagged-literals/read-inst s)
       (catch :default _ nil))))
