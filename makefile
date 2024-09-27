cloc:
	cloc . --exclude-list-file=cloc.excluded

clj-kondo:
	clj-kondo --config .clj-kondo/config.edn --lint  src/
	clj-kondo --config .clj-kondo/config.edn --lint test/

lein-cljfmt-check:
	lein cljfmt check

lein-cljfmt-fix:
	lein cljfmt fix

lein-bikeshed:
	lein bikeshed -m 90

lein-eastwood:
	lein eastwood '{:namespaces [:source-paths]}'

lein-kibit:
	lein kibit

lein-splint:
	lein splint
