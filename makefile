clean:
	@clojure -T:build clean

compile:
	@clojure -T:build compile-clj

jar:
	@clojure -T:build jar

install:
	@clojure -T:build install

kaocha:
	@clojure -M:kaocha

deploy-clojars: # make clean compile jar and then:
	@mvn -DaltDeploymentRepository=clojars::https://clojars.org/repo -f target/classes/META-INF/maven/com.github.kongra/telsos-sysload/pom.xml deploy
