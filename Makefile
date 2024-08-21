

.PHONY: build
build:
	@set -e; \
	./gradlew clean build -x test
