# Игра «Камень — Ножницы — Бумага» (Zazeks RPS)

Обновлённый README с акцентом на **деплой backend** и **сборку/публикацию Android‑APK**. Описаны реальные проблемы, которые встречались при настройке (JDK/Gradle/SDK/AndroidX) и готовые решения.

---

## TL;DR (самый короткий путь)

1. **Backend (Java/Spring Boot):**

   ```bash
   cd java-backend
   ./gradlew bootRun        # Windows: .\gradlew.bat bootRun
   # или собрать jar
   ./gradlew bootJar && java -jar build/libs/*-SNAPSHOT.jar
   ```

   По умолчанию: `http://0.0.0.0:8080` (можно задать `PORT=8081`).

2. **Android (Debug):**

   ```bash
   cd android
   # настроить SDK путь: android/local.properties → sdk.dir=...
   # включить AndroidX: android/gradle.properties → см. ниже
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   В `android/app/src/main/assets/config/backend.json` укажи URL бэкенда:

   * Эмулятор: `{"baseUrl":"http://10.0.2.2:8080"}`
   * Физ. устройство (USB): `adb reverse tcp:8080 tcp:8080` и `{"baseUrl":"http://127.0.0.1:8080"}`
   * Публичный сервер: `{"baseUrl":"https://your-domain.tld"}`

3. **Android (Release + подпись):**

   ```bash
   cd android
   ./gradlew assembleRelease  # соберёт app-release-unsigned.apk, если не настроена подпись

   # если нужен быстрый ручной вариант подписи:
   keytool -genkeypair -v -keystore signing-release.jks -alias zazeks-release -keyalg RSA -keysize 2048 -validity 10000
   ./gradlew assembleRelease \
     -Pandroid.injected.signing.store.file=signing-release.jks \
     -Pandroid.injected.signing.store.password=****** \
     -Pandroid.injected.signing.key.alias=zazeks-release \
     -Pandroid.injected.signing.key.password=******
   # Итоговый APK: app/build/outputs/apk/release/app-release.apk (или app-release-unsigned.apk, если без параметров подписи)
   ```

---

## Архитектура проекта (кратко)

* **Backend:** Java (Spring Boot) + WebSocket матчмейкинг. Сейчас хранилище in-memory.
* **Android‑клиент:** Камера, жесты, HTTP/WS с backend, ML‑bridge для `/model/detect`.
* **ML:** на клиенте мост к детектору; на сервере сейчас заглушка для инференса (восстановить позже).

Основные эндпоинты backend: `/auth`, `/users`, `/games`, `/multiplayer` (WS: `/ws/multiplayer`), `/admin`, `/model/detect`.

---

## Предустановки и версии

### Java/JDK

* **Backend** стабильно запускается на **JDK 21 LTS** (Temurin/OpenJDK).
* **Android‑сборка** через Android Gradle Plugin (AGP) требует **JDK 17**.

  > Вывод: держи обе версии рядом. Перед сборкой Android переключайся на 17, для backend можешь использовать 21.

Примеры путей (Windows):

```
C:\Program Files\Eclipse Adoptium\jdk-21.0.x
C:\Program Files\Eclipse Adoptium\jdk-17.0.x
```

Переключение JDK (PowerShell, только для текущего окна):

```powershell
# для backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# для Android
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### Gradle Wrapper

В репозитории должен быть **wrapper** (скрипты `gradlew*` + `gradle/wrapper/*`). Если в модуле `android/` wrapper отсутствует, можно:

```bash
# из android/
gradle wrapper --gradle-version 8.10.2   # если установлен standalone Gradle
```

Или скопировать wrapper из соседнего модуля (как делалось при отладке).

### Android SDK

* Установить через **Android Studio → More Actions → SDK Manager**.
* В корне `android/` создать файл `local.properties`:

  ```
  sdk.dir=C:/Users/<USERNAME>/AppData/Local/Android/Sdk
  ```
* Докачать платформы/Build‑Tools под твой `compileSdk` (например 34) и `build-tools` (например 34.0.0).

### AndroidX (обязательно)

Если проект/зависимости используют AndroidX, включить флаги в `android/gradle.properties`:

```
android.useAndroidX=true
android.enableJetifier=true
```

---

## Backend — запуск локально (Dev)

```bash
cd java-backend

# Быстрый старт
./gradlew bootRun                   # Windows: .\gradlew.bat bootRun
# или
./gradlew run

# Сборка jar и запуск как отдельного процесса
./gradlew bootJar
java -jar build/libs/*-SNAPSHOT.jar

# Изменить порт (по умолчанию 8080)
PORT=8081 ./gradlew bootRun         # Windows PowerShell: $env:PORT=8081; .\gradlew.bat bootRun
```

Проверка:

```
GET http://localhost:8080/actuator/health    (если включён actuator)
WS  ws://localhost:8080/ws/multiplayer
```

> **Примечание:** хранилище пока in‑memory (`InMemoryDatabase`). Для персистентности подключить PostgreSQL и реализовать DAO.

### Модель жестов (YOLO)

* При старте backend загружает веса детектора жестов из файла `best.pt` (YOLOv8). По умолчанию используется
  `../model/learning/runs/detect/train5/weights/best.pt` (путь относительно каталога `java-backend`).
* Путь можно переопределить через переменную окружения `YOLO_WEIGHTS_PATH` или property `app.yolo.weights-path`.
* Если рядом с весами присутствует экспорт в формате ONNX (`best.onnx`), backend сразу использует [ONNX Runtime](https://onnxruntime.ai/).
  Если `.onnx` отсутствует, сервис попытается **автоматически экспортировать** его при помощи `python3 -m ultralytics export ...`.
  Для этого необходим установленный Python (3.9+) и пакет `ultralytics` (`pip install ultralytics`). При необходимости можно задать путь к
  интерпретатору через `YOLO_PYTHON_BIN`/`app.yolo.python-executable` и таймаут экспорта `YOLO_EXPORT_TIMEOUT`.
* Если экспорт не удался, backend продолжит работу в резервном режиме (по отпечаткам тестовых изображений) — жесты будут определяться
  только для образцов из тестового набора. Чтобы получить полноценное распознавание и bounding box'ы, убедитесь, что ONNX экспорт успешен.
* Интеграционные тесты сервиса отправляют образец `model/test_model/images.jpg` на `/model/detect` и ожидают распознавание жеста «Rock».

---

## Backend — продакшн деплой (VPS/сервер)

### Вариант A: «fat‑jar» + reverse proxy

1. Собрать jar:

   ```bash
   cd java-backend
   ./gradlew bootJar
   scp build/libs/*-SNAPSHOT.jar user@server:/opt/zazeks/app.jar
   ```
2. Systemd‑юнит (Linux): `/etc/systemd/system/zazeks.service`

   ```ini
   [Unit]
   Description=Zazeks RPS Backend
   After=network.target

   [Service]
   User=zazeks
   WorkingDirectory=/opt/zazeks
   Environment=PORT=8080
   ExecStart=/usr/bin/java -jar /opt/zazeks/app.jar
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now zazeks
   ```
3. Reverse proxy (Caddy — авто‑TLS): `/etc/caddy/Caddyfile`

   ```
   your-domain.tld {
     reverse_proxy 127.0.0.1:8080
   }
   ```

   > Nginx тоже подойдёт. Не забудь включить **WebSocket** проксирование (`Upgrade/Connection`), если настраиваешь вручную.

### Вариант B: Запуск на Windows

* Сервис через NSSM или Task Scheduler.
* Открыть порт в брандмауэре или повесить Caddy/Nginx с TLS поверх Java процесса.

### CORS/HTTPS

* Для релизного клиента желательно HTTPS‑домен. Если держишь HTTP, и клиент на Android 9+, включи в манифесте `usesCleartextTraffic=true` + `networkSecurityConfig` (см. раздел «Troubleshooting»).

---

## Android — настройка клиента

Файл конфигурации: `android/app/src/main/assets/config/backend.json`.

Примеры:

```json
{ "baseUrl": "http://10.0.2.2:8080" }   // эмулятор Android
{ "baseUrl": "http://192.168.1.100:8080" }   // физ. устройство в одной сети с ПК
{ "baseUrl": "https://your-domain.tld" }     // публичный бэкенд
```

USB‑устройство без локальной сети:

```bash
adb reverse tcp:8080 tcp:8080
# тогда можно указать http://127.0.0.1:8080
```

---

## Android — сборка и установка

### Debug

```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release (подписанный APK)

1. Сгенерировать ключ (однократно):

```bash
keytool -genkeypair -v \
  -keystore signing-release.jks \
  -alias zazeks-release \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. Собрать и подписать (быстрый способ через параметры):

```bash
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=signing-release.jks \
  -Pandroid.injected.signing.store.password=****** \
  -Pandroid.injected.signing.key.alias=zazeks-release \
  -Pandroid.injected.signing.key.password=******
```

3. Установить:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

> Никогда не коммить `*.jks` в репозиторий. Храни отдельно.

---

## Частые ошибки и быстрые решения

### 1) `ERROR: JAVA_HOME is set to an invalid directory`

* Проверь, что `JAVA_HOME` указывает на реальную папку с `bin/java(.exe)`.
* Для Android используй **JDK 17**; для backend допустим **JDK 21**.

### 2) `Android Gradle plugin requires Java 17`

* Переключись на JDK 17 перед сборкой Android.

### 3) `SDK location not found. Define location with sdk.dir...`

* Создай/поправь `android/local.properties`: `sdk.dir=C:/Users/<YOU>/AppData/Local/Android/Sdk`.
* Установи через SDK Manager платформу `compileSdk` (например 34) и Build‑Tools.

### 4) `Set android.useAndroidX=true ...`

* Добавь в `android/gradle.properties`:

  ```
  android.useAndroidX=true
  android.enableJetifier=true
  ```

### 5) Эмулятор не видит `localhost`

* На эмуляторе используй `http://10.0.2.2:<port>`.

### 6) Физ. устройство не достаёт ПК

* В одной сети: укажи IP ПК.
* Без сети: `adb reverse tcp:8080 tcp:8080` и `http://127.0.0.1:8080` в конфиге.

### 7) `CLEARTEXT communication not permitted` (HTTP на Android 9+)

* В `AndroidManifest.xml` внутри `<application>`:

  ```xml
  <application
      android:usesCleartextTraffic="true"
      android:networkSecurityConfig="@xml/network_security_config"
      ...>
  </application>
  ```
* Файл `res/xml/network_security_config.xml`:

  ```xml
  <network-security-config>
    <base-config cleartextTrafficPermitted="true" />
  </network-security-config>
  ```
* В продакшене лучше перейти на HTTPS.

---

## Полезные команды

```bash
# Backend health (если actuator подключён)
curl -s http://localhost:8080/actuator/health

# Проверка детектора с ПК
curl -F "file=@sample.jpg" http://localhost:8080/model/detect

# Логи приложения Android
adb logcat | findstr /i "zazeks okhttp websocket"

# Запуск MainActivity вручную
adb shell cmd package resolve-activity --brief com.example.zazeks
adb shell am start -n com.example.zazeks/.MainActivity
```

---

## Безопасность и .gitignore

* Не коммить `local.properties`, ключи `*.jks`, приватные пароли.
* Пример `.gitignore` для корня (Android + Java/Gradle):

  ```gitignore
  .DS_Store
  Thumbs.db
  .idea/
  .vscode/
  *.iml
  *.log
  .gradle/
  **/.gradle/
  build/
  **/build/
  gradle/wrapper/dists/
  **/gradle/wrapper/dists/
  out/
  bin/
  *.class
  android/local.properties
  android/.cxx/
  android/.externalNativeBuild/
  captures/
  *.jks
  *.keystore
  *.keystore.properties
  *.signing.properties
  signing/**
  !signing/.gitkeep
  !signing/README.md
  node_modules/
  .venv/
  venv/
  __pycache__/
  *.pyc
  ```

---

## Дорожная карта (что ещё сделать позже)

* Перевести хранилище на PostgreSQL.
* Отдельное хранение аватаров/бинари вне памяти процесса.
* Вернуть реальный ML‑инференс для `/model/detect`.
* Логи/аудит (stream/analytics) из старого Python‑стека.

---

Готово: по этому файлу можно поднять backend на сервере, собрать подписанный APK и отдать его пользователю, чтобы тот просто установил и «потыкал». Если нужен auto‑deploy (GitHub Actions → VPS) или Docker‑вариант — добавлю.
