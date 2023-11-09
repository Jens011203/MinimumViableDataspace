./gradlew build -x test
./gradlew -DuseFsVault="true" :launchers:registrationservice:shadowJar
./gradlew :launchers:rest-connector:shadowJar
docker compose --profile ui -f system-tests/docker-compose.yml up --build