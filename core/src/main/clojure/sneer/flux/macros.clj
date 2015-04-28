(ns sneer.flux.macros)

(defn- prefix-symbol [s]
  (symbol (str "-interface-" (name s))))

(defn- prefix-method-name [m]
  (update m 1 prefix-symbol))

(defn- inject-parameters [parameters m]
  (if (empty? parameters)
    m
    (let [this (get-in m [2 0])]
      (update m 3 (fn [body]
                    `(let [[~@parameters] (.parameters ~this)]
                       ~body))))))

(defmacro defcomponent

  "(defcomponent sneer.flux.ConversationStore [admin]
     (defn summaries [this] (rx/never))"

  [interface parameters & methods]

  (let [name (str (name interface) "ServiceProvider")
        methods (->> methods (map (comp list* prefix-method-name #(inject-parameters parameters %) vec)))
        parameter-types (map (constantly `Object) parameters)
        init (prefix-symbol 'init)]

    `(do
       (gen-class
        :name ~name
        :implements [~interface]
        :prefix "-interface-"
        :state "parameters"
        :init "init"
        :constructors {[~@parameter-types] []})

       (defn ~init [~@parameters]
         [[] [~@parameters]])

       ~@methods)))
