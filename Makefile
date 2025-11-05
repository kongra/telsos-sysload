clean:
	@clojure -T:build clean
	@rm -rf .cpcache/

compile:
	@clojure -T:build compile-clj

jar:
	@clojure -T:build jar

install:
	@clojure -T:build install

kaocha:
	@clojure -M:kaocha

# DEPLOYMENT TO CLOJARS
ARTIFACT_BASE = telsos-sysload
POM_FILE = target/classes/META-INF/maven/com.github.kongra/telsos-sysload/pom.xml
REPOSITORY_ID = clojars
REPOSITORY_URL = https://clojars.org/repo
VERSION = $(shell git rev-list HEAD --count)
ARTIFACT_ID = $(ARTIFACT_BASE)-0.1.$(VERSION)
JAR_FILE = target/$(ARTIFACT_ID).jar

deploy-clojars: # make clean compile jar and then:
	@echo "Deploying version 0.1.$(VERSION)"
	@mvn deploy:deploy-file \
		-DpomFile=$(POM_FILE) \
		-Dfile=$(JAR_FILE) \
		-DrepositoryId=$(REPOSITORY_ID) \
		-Durl=$(REPOSITORY_URL)
