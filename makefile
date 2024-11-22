.PHONY: clean compile jar install

# CLOJURE BUILDS AND TESTS
clean:   ; @clojure -T:build clean
compile: ; @clojure -T:build compile-clj
jar:     ; @clojure -T:build jar
install: ; @clojure -T:build install
kaocha:  ; @clojure -M:kaocha
