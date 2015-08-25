.PHONY: build
build:
	docker build -t sneerteam/android-studio .
	docker tag -f sneerteam/android-studio sneerteam/android-studio:latest

.PHONY: hack
hack: build
	./android-studio
