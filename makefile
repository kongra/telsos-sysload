.PHONY: clj-clean clj-compile clj-jar clj-install

# CLOJURE BUILDS AND TESTS
clj-clean:   ; clojure -T:build clean
clj-compile: ; clojure -T:build compile-clj
clj-install: ; clojure -T:build install
clj-jar:     ; clojure -T:build jar
clj-test:    ; clojure -M:test
clj-ccjl:      clj-clean clj-compile clj-jar clj-install

# CLOJURE LINTING
clj-kondo:
	clj-kondo --config .clj-kondo/config.edn --lint  src/
	clj-kondo --config .clj-kondo/config.edn --lint test/

cloc:
	cloc . --exclude-list-file=cloc.excluded
