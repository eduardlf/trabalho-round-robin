@echo off

docker compose up -d --build

docker compose exec app mvn -q clean compile exec:java -Dexec.mainClass=com.trabalho.br.round_robin.Round_robin

pause