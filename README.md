# Игра «Камень — Ножницы — Бумага»

Добро пожаловать в обновлённую версию проекта! Теперь серверная часть полностью реализована на Java, а основным клиентом выступает Android‑приложение.

---

## Краткое описание

- **Кейс:** мультимедийная игра «Камень — Ножницы — Бумага» с распознаванием жестов и поддержкой мультиплеера.
- **Стек:**
  - **Backend:** Java (Gradle, чистый HTTP API на базе in-memory сервиса из модуля `java-backend`).
  - **Client:** Android-приложение (модуль `android`).
  - **ML:** предобученная модель YOLOv11, используемая на клиентской стороне.

---

## Функциональность

- **Авторизация и управление пользователями** – регистрация, вход, хэширование паролей, выдача токенов.
- **Офлайн и онлайн режимы** – запись результатов одиночных и мультиплеерных игр.
- **Статистика** – хранение побед/поражений, лидерборды.
- **Мобильный клиент** – интерфейс Android, камера, обработка жестов, синхронизация через HTTP API.

**Implemented**
- JWT-based authentication with registration/login endpoints.
- Player profile, avatar, and leaderboard operations backed by an in-memory data store.
- REST CRUD for single-player game history plus duplicate-submission protection.
- WebSocket matchmaking (`/ws/multiplayer`) with battle lifecycle management and result persistence.
- Administrative actions for moderating users and game records.

**To be reimplemented before feature parity**
- Replace the in-memory database with the PostgreSQL schema that existed in the FastAPI service.
- Persist avatars and other binary assets outside of process memory.
- Restore ML-powered inference for `/model/detect` (the current implementation returns a deterministic hash-based stub).
- Reintroduce analytics/log streaming and structured audit trails from the Python stack.

## Как запустить Java backend

1. **Перейдите в директорию сервера**
   ```bash
   cd java-backend
   ```

2. **Соберите и запустите приложение**
   ```bash
   ./gradlew run
   ```
   При первом запуске Gradle загрузит дистрибутив и зависимости (≈130 МБ), поэтому шаг может занять пару минут.
   Скрипт `gradlew` восстанавливает отсутствующий `gradle-wrapper.jar` из текстового `gradle-wrapper.jar.base64` либо, при необходимости, скачивает его из Maven Central (через `curl`/`wget` или PowerShell), поэтому при первом запуске требуется доступ в интернет, если jar ещё не собран локально.
   После сообщения `Java backend started on http://localhost:8080` сервер доступен по `http://localhost:8080`.
   Остановите его сочетанием `Ctrl+C`.
   Если порт 8080 занят, задайте переменную окружения `PORT`, например `env PORT=8081 ./gradlew run`.

3. **Проверьте работу тестов (опционально)**
   ```bash
   ./gradlew test
   ```
4. **Доступные сервисы**
   - REST API стартует на `http://localhost:8080`.
   - Реализованы сервисы авторизации, управления пользователями, записи результатов игр и матчей.

> **Примечание:** backend использует in-memory хранилище (`InMemoryDatabase`). Для постоянного хранения данных подключите собственную базу и реализуйте DAO-интерфейсы по аналогии с текущими сервисами.

---

## Как собрать Android приложение

1. **Подготовьте окружение**
   - Android Studio Flamingo+ или установленный `cmdline-tools`.
   - Java 17 (совместимо с Gradle wrapper).

2. **Укажите адрес backend**
   - Отредактируйте `android/app/src/main/assets/config/backend.json`, установив URL вашего Java-сервера.

3. **Запустите unit-тесты (опционально)**
   ```bash
   ./gradlew test
   ```


4. **Соберите APK**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

5. **Установите на устройство**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
6. **Первый запуск**
   - Предоставьте доступ к камере.
   - Авторизуйтесь или создайте нового пользователя.
   - Запустите офлайн или мультиплеерный матч.


Подробная инструкция по мобильному клиенту находится в `android/README.md`.


---

## Структура репозитория

```
.
├── android/          # Android-приложение (основной клиент)
├── defay_1x9/        # Материалы команды и иллюстрации
├── docs/             # Дополнительная документация
├── java-backend/     # Java backend (Gradle проект)
├── model/            # ML-модель и скрипты проверки
└── README.md         # Текущее описание
```

---

## Команда

<table>
  <tr>
    <td align="center" style="border: 1px solid #555;">
      <img src="defay_1x9/pics_readme/Sasha.jpg" width="100" height="100" style="border-radius: 50%" alt="avatar"><br />
      <b>Александр Штеренфельд</b><br />
      <sub><i>Тимлид, ML Developer, Full-stack разработчик</i></sub>
    </td>
    <td align="center" style="border: 1px solid #555;">
      <img src="defay_1x9/pics_readme/Denis.jpg" width="100" height="100" style="border-radius: 50%" alt="avatar"><br />
      <b>Денис Байрамов</b><br />
      <sub><i>Backend разработчик</i></sub>
    </td>
    <td align="center" style="border: 1px solid #555;">
      <img src="defay_1x9/pics_readme/Max.jpg" width="100" height="100" style="border-radius: 50%" alt="avatar"><br />
      <b>Максим Землянский</b><br />
      <sub><i>Pentester</i></sub>
    </td>
  </tr>
</table>

![alt text](defay_1x9/pics_readme/image.png)

---

## Работа в VS Code

1. **Откройте корень репозитория** (`File → Open Folder…`).
2. **Установите расширения**:
   - *Extension Pack for Java* (для разработки и отладки `java-backend`).
   - *Android Extensions* или Android Studio (для эмуляторов, Logcat и сборок APK).
3. **Запуск backend**:
   - Встроенный терминал VS Code → `cd java-backend && ./gradlew run`.
   - Тесты запускаются той же командой `./gradlew test`.
   - Для дебага добавьте конфигурацию `Java: Launch Program` с главным классом `com.zazeks.app.Application`.
4. **Сборка Android-клиента**:
   - Откройте новый терминал → `cd android && ./gradlew assembleDebug`.
   - Unit-тесты: `./gradlew test`.
   - Для запуска на эмуляторе используйте панель *Run and Debug* → *Run Android on Emulator/Device*.
5. **Рекомендации по отладке**:
   - Используйте вкладку *Logcat* для анализа сетевых запросов и ML-пайплайна.
   - Профилировщик Android Studio помогает отслеживать загрузку камеры и CPU во время распознавания жестов.
=======
## End-to-end testing from VS Code

The goal is to exercise the full stack—backend REST/WebSocket plus the Android client—in one workspace.

1. **Backend verification**
   - `cd java-backend && ./gradlew test` runs the JUnit integration suite (`src/test/java/com/zazeks/web/*IntegrationTest.java`), covering REST endpoints, JWT flows, and WebSocket matchmaking. Failures usually highlight parity gaps with the legacy FastAPI behaviour.

2. **Android unit tests**
   - `cd android && ./gradlew testDebugUnitTest` executes JVM tests for view models, repositories, and HTTP bridges without needing an emulator.

3. **Android UI tests**
   - `cd android && ./gradlew connectedDebugAndroidTest` requires an emulator or device. Launch the emulator via the VS Code device panel, then trigger the task from the Gradle explorer or a custom task entry.

4. **Smoke-testing the full flow**
   - Start the backend (`./gradlew bootRun`).
   - Deploy the debug APK to an emulator using `adb install` or VS Code’s *Run on Android* command.
   - From the emulator, register a user, play a single-player round, then open matchmaking so the WebSocket traffic is exercised. Watch backend logs in the terminal to confirm game persistence.
   - For inference checks, upload static images via the in-app gallery until the real model is restored.

Because the backend stores everything in memory, restarting `bootRun` gives you a clean slate for repeated test loops.

---

## Contributing

1. Fork and clone the repository.
2. Create a feature branch and ensure both backend (`./gradlew test`) and Android (`./gradlew testDebugUnitTest`) checks pass before opening a PR.
3. Update this README or the Android documentation when you add or change API endpoints so the migration guide stays current.


