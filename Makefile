.PHONY: servers-restart servers-start servers-stop

COMPOSE_FILE := test-server/docker-compose.yaml

servers-restart:
	docker compose -f $(COMPOSE_FILE) up -d server-1 server-2

servers-start:
	docker compose -f $(COMPOSE_FILE) up -d

servers-stop:
	docker compose -f $(COMPOSE_FILE) down
