.PHONY: clean compile jar install

# CLOJURE BUILDS AND TESTS
clean:   ; clojure -T:build clean
compile: ; clojure -T:build compile-clj
install: ; clojure -T:build install
jar:     ; clojure -T:build jar
test:    ; clojure -M:test
ccjl:      clean compile jar install

# CLOJURE LINTING
clj-kondo:
	clj-kondo --config .clj-kondo/config.edn --lint  src/
	clj-kondo --config .clj-kondo/config.edn --lint test/

cloc:
	cloc . --exclude-list-file=cloc.excluded
