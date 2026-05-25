REMOTE  := tmb@trust
APP_DIR := /home/tmb/mtz
SERVICE := mtzion
JAR     := target/mtzion.jar

.PHONY: css js build deploy

css:
	clj -M:run css

js:
	npm run build

build: js css
	clj -T:build uber

deploy: build
	scp $(JAR) $(REMOTE):$(APP_DIR)/mtzion.jar.new
	ssh $(REMOTE) "mv $(APP_DIR)/mtzion.jar.new $(APP_DIR)/mtzion.jar && sudo systemctl restart $(SERVICE) && sudo systemctl status $(SERVICE) --no-pager"
