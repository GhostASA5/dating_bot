Нагрузочное тестирование (Apache JMeter)

Предусловия: подняты userservice (8081), ratingservice (8083), interactionservice (8084), PostgreSQL, Redis, Kafka — например через docker compose из корня репозитория.

Запуск (GUI): открыть dating-load.jmx в JMeter 5.6+, задать переменные host/port при необходимости, Run.

Запуск (CLI, из каталога load-tests):
  jmeter -n -t dating-load.jmx -l results.jtl -e -o report-html

Сценарий: опрос health/actuator и ленты ratingservice для userId=1 (при необходимости замените на существующий telegram id в вашей БД).
