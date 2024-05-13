## Структура проекта

```text
/build.gradle.kts - build-скрипт
/docs/openapi - описание openapi спеки
/src/main/kotlin - исходники приложения
/src/main/resources - ресурсы приложения
/src/main/resources/db/changelog - миграции базы
/src/main/resources/application.yaml - базовые настройки проекта
/src/main/resources/application-test.yaml - уточнение настроек проекта для тестовой среды
/src/main/resources/application-prod.yaml - базовые настроек проекта для продакшеновой среды
/src/test/kotlin - тесты приложения
/src/test/resources - ресурсы необходимые для тестов
```

## Как завести проект локально

1. Выполняем, чтобы поставить docker:

```bash
brew install colima
colima start --network-address
docker ps -a
```

2. Возможно еще нужно будет прописать следующее в .zshrc или .bashrc:

```bash
export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address')
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
```

3. Перезагрузить idea, т.к она не подтянет автоматом изменения .zshrc

4. Команда `./gradlew build` должна выполниться успешно

## Как поднять локальный инстанс

### Через IDEA

Запустить через IDEA `ru.itmo.ict.todolist.TestConfig::main`

Если инстанс не запускается и пишет странную ошибку без лога, то надо
сделать `File > Invalidate Caches... > Invalidate and Restart`

### Через Gradle

```bash
./gradlew bootTestRun
```

## Статические ресурсы приложения

`localhost/openapi.yaml` -- смереженная спека openapi

`localhost/` -- html спека openapi

## Автозапуск colima

### Linux

```bash
vi ~/.bash_profile
```

пишем туда `colima start`

### Mac OS

```bash
cat > $HOME/Library/LaunchAgents/com.github.abiosoft.colima.plist <<-EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
  <dict>
    <key>Label</key>
    <string>com.github.abiosoft.colima</string>
    <key>Program</key>
    <string>/usr/local/bin/colima-start-fg</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <false/>
  </dict>
</plist>
EOF

launchctl load -w $HOME/Library/LaunchAgents/com.github.abiosoft.colima.plist
```

## На чем можно в Java/Kotlin запнуться?

В тестах вы используете `import org.junit.Test`. Это не совсем правильно, т.к это от другой версии Junit. Из-за этого у
вас может тест запускаться без спринга (если вы хотели интеграционный тест). И в целом, это фигня сразу не очевидная.
Надо поменять импорт на `import org.junit.jupiter.api.Test`. Это уже импорт от нужной версии Junit