.EXPORT_ALL_VARIABLES:
.PHONY: test


C3_IMAGE  = c3/c3

repl:
	source .env && clj -A:nrepl -e "(-main)" -r 

jar:
	clj -A:build

docker:
	docker build -t ${C3_IMAGE} .

all: jar docker

test:
	clj -A:test


