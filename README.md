# MiniTasker Full-Stack Учебный Проект

MiniTasker — учебный full-stack проект, состоящий из backend-сервиса на Spring Boot и Android-клиента на Kotlin/Jetpack Compose. Приложение реализует требования преподавателя: аутентификация по JWT, разграничение ролей, полный CRUD по сущностям проектов/задач, работу с комментариями и вложениями, а также экран статистики.

## Архитектура backend-проекта
- **Сборка:** Maven (`backend/pom.xml`), Java 17, Spring Boot 3.2.
- **Слои и пакеты:**
  - `controller` — REST-контроллеры (`AuthController`, `ProjectController`, `TaskController`, `CommentController`, `AttachmentController`, `StatsController`, `ReportController`).
  - `service` и `service.impl` — бизнес-логика, проверки прав, логирование ключевых операций.
  - `repository` — `JpaRepository` для каждой сущности.
  - `model`/`entity` и `model.enums` — JPA-сущности и перечисления ролей/статусов/приоритетов.
  - `dto` — DTO для запросов/ответов (auth, проекты, задачи, комментарии, вложения, статистика).
  - `security` — JWT-фильтр, провайдер токенов, настройка Spring Security.
  - `exception` — глобальная обработка ошибок и кастомные исключения.
  - `config`/`util` — конфигурация хранилища файлов и утилиты безопасности.
- **БД:** PostgreSQL (настройки в `application.yml`, `ddl-auto=update`).
- **Безопасность:** Spring Security + JWT, BCrypt для паролей, разграничение прав (USER видит только свои данные, ADMIN — все).
- **Дополнительно:** экспорт CSV, статистика задач, логирование аутентификации/CRUD операций, обработка ошибок через `@ControllerAdvice`.

## Архитектура Android-приложения
- **Сборка:** Gradle (AGP 8.3.2), Kotlin 1.9, Jetpack Compose.
- **Пакеты:**
  - `data/api` — `Retrofit`-интерфейс, `AuthInterceptor`.
  - `data/model` — модели API и DTO.
  - `data/local` — `AuthPreferences` на DataStore для хранения JWT.
  - `data/repository` — репозитории и `ServiceLocator`.
  - `ui` — экраны, компоненты, навигация.
  - `MiniTaskerApplication` — точка входа и ленивые singletons репозиториев.
- **Технологии:** Retrofit+Moshi, OkHttp, Coroutines/Flow, DataStore, Navigation Compose, Material 3, MPAndroidChart для визуализации.
- **Экраны:**
  1. Аутентификация/регистрация.
  2. Список проектов с созданием и выходом.
  3. Список задач проекта с фильтрами и быстрым созданием.
  4. Деталь задачи: данные, смена статуса/приоритета, комментарии, вложения с выбором файла.
  5. Статистика (графики распределения по статусам/приоритетам).
- **Работа с JWT:** токен хранится в DataStore, `AuthInterceptor` автоматически подставляет заголовок Authorization.
- **Обработка ошибок:** `safeCall` возвращает `NetworkResult` для отображения сообщений и состояний загрузки.

## Запуск backend
1. Установите PostgreSQL и создайте БД/пользователя `minitasker`/`minitasker` или измените `backend/src/main/resources/application.yml`.
2. Соберите и запустите сервис:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   (или `mvn spring-boot:run`, если Maven установлен глобально).
3. API доступно на `http://localhost:8080`.

## Запуск backend в Docker
1. Установите [Docker](https://docs.docker.com/get-docker/) и [Docker Compose](https://docs.docker.com/compose/install/).
2. Соберите образ вручную (контекст — каталог `backend`):
   ```bash
   docker build -t minitasker-backend ./backend
   ```
3. Поднимите backend вместе с PostgreSQL при помощи docker compose (команда автоматически стартует и сервис `postgres` благодаря `depends_on`):
   ```bash
   docker compose up backend
   ```
   Образы будут собраны, после чего backend станет доступен, а данные PostgreSQL сохранятся в томе `postgres-data`.
4. API доступно на `http://localhost:8080` (порт проброшен из контейнера `backend`).
5. Для Android-эмулятора ничего менять не нужно — он по-прежнему обращается к `http://10.0.2.2:8080`, поэтому при пробросе `8080:8080` всё работает как и при локальном запуске.

## Сборка и запуск Android-клиента
1. Откройте каталог `android/` в Android Studio (Giraffe+).
2. Убедитесь, что устройство/эмулятор могут обратиться к backend (`10.0.2.2:8080` для эмулятора, при необходимости измените `ServiceLocator.BASE_URL`).
3. Запустите сборку/установку через Android Studio или командой:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

## Структура репозитория
```
backend/  - Spring Boot сервис
android/  - Android-приложение
README.md - этот файл
```

## Дальнейшие шаги
- Добавить unit- и instrumented-тесты.
- Настроить CI-пайплайн.
- Реализовать загрузку файлов в облачное хранилище и предпросмотр вложений в Android.
