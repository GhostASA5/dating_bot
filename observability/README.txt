Observability stack (Docker Compose)
====================================

Сервисы:
  Grafana       http://localhost:3000   (admin / admin)
  Prometheus    http://localhost:9090
  Loki          http://localhost:3100
  cAdvisor      http://localhost:8089

================================================================================
ГИБРИДНЫЙ РЕЖИМ (микросервисы локально, Prometheus/Grafana в Docker)
================================================================================

Почему пустой дашборд: в prometheus.yml указаны имена userservice:8081 —
они работают только если сервисы тоже в Docker. С хоста Prometheus должен
стучаться в host.docker.internal:8081 и т.д.

1) Поднимите инфраструктуру и observability (без datingbot/userservice в Docker):

   docker compose -f docker-compose.yaml -f docker-compose.local.yml up -d ^
     postgres_user postgres redis kafka zookeeper localstack ^
     loki prometheus grafana promtail cadvisor

   (PowerShell: одна строка или замените ^ на `)

2) Запустите сервисы на хосте (IDE или Maven), порты 8081–8084.

   Переменные для логов в Loki (PowerShell, перед run):
     $env:LOKI_ENABLED="true"
     $env:SPRING_PROFILES_ACTIVE="loki"
     $env:LOKI_PUSH_URL="http://127.0.0.1:3100/loki/api/v1/push"

   Примеры БД/Kafka с портов compose:
     userservice / ratingservice / interactionservice:
       SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/user_db
       SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092  (ratingservice, interactionservice)
       SPRING_DATA_REDIS_HOST=localhost  (ratingservice)
     datingbot:
       SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/bot_db
       USER_SERVICE_URL=http://localhost:8081
       RATING_SERVICE_URL=http://localhost:8083
       INTERACTION_SERVICE_URL=http://localhost:8084

3) Проверка метрик:
   - На хосте: http://localhost:8081/actuator/prometheus (должен открываться текст метрик)
   - В Docker: http://localhost:9090/targets — все job UP (зелёные)
   - Если DOWN: firewall, сервис не запущен, неверный порт

4) Grafana: Dashboards → DatingBot overview. Сгенерируйте HTTP-трафик (health, API, бот).

Логи в гибридном режиме:
  - Spring → Loki напрямую (LOKI_ENABLED=true), запрос: {service="userservice"}
  - Promtail видит только логи контейнеров Docker, не консоль IDE

Переключить Prometheus обратно на «всё в Docker»:
  docker compose up -d prometheus
  (без -f docker-compose.local.yml — используется prometheus.yml)

================================================================================
ВСЁ В DOCKER (полный compose)
================================================================================

  docker compose up -d

  Prometheus: observability/prometheus.yml (targets userservice:8081, …)
  Проверка: http://localhost:9090/targets

Метрики в Grafana
-----------------

1. http://localhost:3000 → DatingBot overview
2. Переменная «Сервис» вверху
3. Explore → Prometheus / Loki

Где смотреть логи
-----------------

  Grafana → Explore → Loki → {service="datingbot"}
  Или панель «Логи» на дашборде
  Docker: docker compose logs -f datingbot
