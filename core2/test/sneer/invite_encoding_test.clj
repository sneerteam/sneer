(ns sneer.invite-encoding-test
  [:require
    [midje.sweet :refer [facts fact]]
    [sneer.midje-util :refer :all]
    [sneer.invite :as subject]])

(facts "Invite Encoding"
  (let [original {:puk         (byte-array (range 32))
                  :name        "WEIRD NAME áéíóú \uD834\uDF06" ;http://stackoverflow.com/questions/19354455/getting-exactly-equal-clojure-clojurescript-strings-in-utf-8
                  :invite-code 1234567}
        encoded (subject/encode original)
        decoded (subject/decode encoded)]
    encoded => "lAHaAC5+YkFBRUNBd1FGQmdjSUNRb0xEQTBPRHhBUkVoTVVGUllYR0JrYUd4d2RIaDg9uldFSVJEIE5BTUUgw6HDqcOtw7PDuiDwnYyGzgAS1oc=" ; Regression
    (seq (decoded :puk))   => (range 32)
    (decoded :name)        => "WEIRD NAME áéíóú \uD834\uDF06"
    (decoded :invite-code) => 1234567))