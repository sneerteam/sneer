(ns sneer.server.core
  (:require
   [clojure.core.async :as async :refer [chan >! <! >!! <!! alts!! timeout]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]))


(defmacro while-let
  "Makes it easy to continue processing an expression as long as it is true"
  [binding & forms]
  `(loop []
     (when-let ~binding
       ~@forms
       (recur))))

(defmacro go!
  [& forms]
  `(async/go
     (try
       ~@forms
       (catch java.lang.Exception ~'e
         (println "GO ERROR" ~'e)
         (. ~'e printStackTrace)))))

(defmacro go-while-let
  "Makes it easy to continue processing data from a channel until it closes"
  [binding & forms]
  `(go!
     (while-let ~binding
                ~@forms)))

(defn prefixes [path]
  "(1 2 3) -> ([1] [1 2] [1 2 3])"
  (map #(vec (take % path)) (range 1 (inc (count path)))))

(defprotocol PubStorage
  (-exists? [this path])
  (-read [this path])
  (-write [this path value])
  (-children [this path]))

(defn ->file [file path & more]
  (java.io.File.
   file
   (apply str (interpose java.io.File/separatorChar (concat path more)))))

(defn ->value-file [file path]
  (->file file path "value.edn"))

(defn ->path-file [dir]
  (java.io.File. dir "path.edn"))

(defn slurp-edn [file]
  (edn/read-string (slurp file)))

(defn spit-edn [file value]
  (spit file (pr-str value)))

(defn make-parents-recording-path [this path]
  (doseq [prefix (prefixes path)]
    (let [path-file (->path-file (->file this prefix))]
      (io/make-parents path-file)
      (when-not (. path-file exists)
        (spit-edn path-file prefix)))))

(extend-protocol PubStorage
  java.io.File
  (-exists? [this path]
    (. (->file this path) exists))

  (-write [this path value]
    (make-parents-recording-path this path)
    (let [file (->value-file this path)]
      (spit-edn file value)))

  (-read [this path]
    (let [file (->value-file this path)]
      (if (. file exists)
        (slurp-edn file)
        ::missing)))

  (-children [this path]
    (let [file (->file this path)]
      (->> (. file listFiles)
           (filter #(. % isDirectory))
           (map ->path-file)
           (filter #(. % exists))
           (map slurp-edn)))))

(defn try-to-write [storage path value]
  (try
    (-write storage path value)
    true
    (catch java.io.IOException e
      (println e)
      false)))

(defonce log
  (let [ch (async/chan 1)]
    (go-while-let
     [msg (<! log)]
     (println msg))
    ch))


(defn announce [to-subs new-paths path value]
  (let [new-pubs (mapv (fn [p] [p ::new]) new-paths)
        all-pubs (conj new-pubs [path value])]
    (go!
     (doseq [[path value] all-pubs]
       (>! log (str "publishing " path " " value))
       (>! to-subs {:tag :notification :path path :value value})))))

(defn pub-loop [storage pubs to-subs]
  (let [new-path? (complement (partial -exists? storage))]
    (go-while-let
     [{:keys [path value]} (<! pubs)]
     (let [new-paths (filterv new-path? (prefixes path))
           value (or value ::missing)]
       (when (try-to-write storage path value)
         (announce to-subs new-paths path value))))))


(defn notify [[id channel] path value]
  (>!! channel {:tag :notification :id id :path path :value value}))

(defn notify-from-storage [storage path sub]
  (when (-exists? storage path)
    (let [children (-children storage path)
          value (-read storage path)]

      (doseq [child children]
        (>!! log (str "publishing from storage " [child ::new]))
        (notify sub child ::new))

      (when-not (= ::missing value)
        (notify sub path value)))))

(defn add-to-trie [trie path sub]
  (let [subs-path (concat path '(::subs))]
    (assoc-in trie subs-path
              (conj (or (get-in trie subs-path) []) sub))))

(defn ->sub [{:keys [id channel]}]
  [id channel])

(defn sub-loop [storage channel]
  (let [trie (atom {})
        subs-for (fn [path] (::subs (get-in @trie path)))]
    (go-while-let
     [{:keys [tag path value] :as message} (<! channel)]

     (case tag
       :notification
       (let [sub-path (if (= ::new value) (butlast path) path)]
         (when-let [subs (subs-for sub-path)]
           (doseq [sub subs]
             (notify sub path value))))

       :sub
       (let [sub (->sub message)]
         (when-not (some (partial = sub) (subs-for path))
           (swap! trie add-to-trie path sub)
           (notify-from-storage storage path sub)))))))

(defn start-server! [storage]
  (let [pubs (async/chan 1)
        subs (async/chan 1)]
    (pub-loop storage pubs subs)
    (sub-loop storage subs)
    {:pubs pubs :subs subs}))

(defn- service-client [server seal in out]
  (let [{:keys [pubs subs]} server]
    (assert pubs)
    (assert subs)

    (go-while-let
     [{:keys [tag path] :as message} (<! in)]

     (>! log (str seal "=>" message))

     (case tag
       :pub (>! pubs (assoc message :path (cons seal path)))
       :sub (>! subs (assoc message :channel out))))))


(defn start-client! [server seal in out]
  (service-client server seal out in)
  {:in in :out out})

(defn post [{:keys [out]} message]
  (>!! out message))

(defn pub [client path & [value]]
  (post client {:tag :pub :path path :value value}))

(defn sub [client path id]
  (post client {:tag :sub :path path :id id}))
