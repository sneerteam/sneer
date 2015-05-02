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

(defmacro defimpl

  "(defimpl sneer.ConversationList [admin]
     (defn summaries [this] (rx/never))"

  [component-interface parameters & methods]

  (let [name (str (name component-interface) "Impl")
        methods (->> methods (map (comp list* prefix-method-name #(inject-parameters parameters %) vec)))
        parameter-types (map (constantly `Object) parameters)
        init (prefix-symbol 'init)]

    `(do
       (gen-class
        :name ~name
        :implements [~component-interface]
        :prefix "-interface-"
        :state "parameters"
        :init "init"
        :constructors {[~@parameter-types] []})

       (defn ~init [~@parameters]
         [[] [~@parameters]])

       ~@methods)))
