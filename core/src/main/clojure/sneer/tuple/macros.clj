(ns sneer.tuple.macros)

(defmacro tuple-getter [g]
  `(~g [~'this]
       (get ~'tuple ~(name g))))

(defmacro with-field [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))
