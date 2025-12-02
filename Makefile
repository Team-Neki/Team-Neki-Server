.PHONY: help compile build run start stop restart test format clean

.DEFAULT_GOAL := help

# Configuration
SPRING_PROFILES ?= local
PORT ?= 8080

GRADLE := ./gradlew
LOG_DIR := logs
LOG_FILE := $(LOG_DIR)/server.log
PID_FILE := $(LOG_DIR)/server.pid

# Colors
GREEN  := \033[0;32m
YELLOW := \033[1;33m
RED    := \033[0;31m
NC     := \033[0m

help: ## Show available commands
	@echo "$(GREEN)Yapp API Server - Available Commands$(NC)"
	@echo ""
	@echo "$(YELLOW)Usage:$(NC) make <target> SPRING_PROFILES=<profile>"
	@echo "$(YELLOW)Default profile:$(NC) $(SPRING_PROFILES)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-12s$(NC) %s\n", $$1, $$2}'

compile:
	@echo "$(GREEN)Compiling with profile: $(SPRING_PROFILES)$(NC)"
	$(GRADLE) compileJava

build:
	@echo "$(GREEN)code formatting...$(NC)"
	$(GRADLE) spotlessApply
	@echo "$(GREEN)Building with profile: $(SPRING_PROFILES)$(NC)"
	$(GRADLE) clean build

run:
	@echo "$(GREEN)Running with profile: $(SPRING_PROFILES)$(NC)"
	@unset SPRING_PROFILES; \
	$(GRADLE) bootRun --args="--server.port=$(PORT) $(if $(SPRING_PROFILES),--spring.profiles.active=$(SPRING_PROFILES)) $(EXTRA_ARGS)"

start:
	@echo "$(GREEN)Starting with profile: $(SPRING_PROFILES)$(NC)"
	@mkdir -p $(LOG_DIR)
	@$(GRADLE) build
	@JAR=$$(ls build/libs/*.jar 2>/dev/null | head -n 1); \
	if [ -z "$$JAR" ]; then \
		echo "$(RED)Error: No jar file found in build/libs/$(NC)"; \
		exit 1; \
	fi; \
	echo "Starting $$JAR..."; \
	echo "Logging to $(LOG_FILE)"; \
	unset SPRING_PROFILES; \
	nohup java -jar "$$JAR" \
		--server.port=$(PORT) \
		$(if $(SPRING_PROFILES),--spring.profiles.active=$(SPRING_PROFILES)) \
		$(EXTRA_ARGS) \
		> $(LOG_FILE) 2>&1 & \
	echo $$! > $(PID_FILE); \
	echo "$(GREEN)Server started with PID $$(cat $(PID_FILE))$(NC)"

stop:
	@if [ -f "$(PID_FILE)" ]; then \
		PID=$$(cat "$(PID_FILE)"); \
		if kill -0 $$PID 2>/dev/null; then \
			echo "Stopping PID $$PID..."; \
			kill $$PID; \
			sleep 1; \
			if kill -0 $$PID 2>/dev/null; then \
				echo "Force killing PID $$PID..."; \
				kill -9 $$PID || true; \
			fi; \
		else \
			echo "No running process for PID $$PID."; \
		fi; \
		rm -f "$(PID_FILE)"; \
	else \
		echo "No PID file found ($(PID_FILE))."; \
	fi

restart: stop start

test:
	$(GRADLE) test

format:
	@echo "code formatting..."
	$(GRADLE) spotlessApply

clean:
	@echo "cleaning project..."
	$(GRADLE) clean

clear-h2:
	@echo "Removing local H2 files at .h2/ ..."; \
	rm -rf .h2 || true; \
	echo "Done."
