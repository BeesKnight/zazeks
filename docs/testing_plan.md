# План автоматизированного тестирования

## Цели

- Быстро обнаруживать регрессии в доменных правилах игры и инфраструктурных интеграциях.
- Поддерживать стабильность клиентских приложений (Android, Web) и серверной части.
- Гарантировать корректность моделей машинного зрения и их интеграции с приложением.

## Уровни и типы тестов

| Уровень | Подсистема | Цель | Инструменты |
|---------|------------|------|-------------|
| Unit | `domain`, `model`, `backend/app/services` | Проверка бизнес-правил (подсчёт результатов, валидация жестов, расчёт рейтингов). | `pytest`, `unittest`, `Kotest` (Kotlin) / `JUnit` (Java). |
| Unit | `data`, `backend/app/repositories` | Проверка преобразования DTO↔️доменные сущности, обработка ошибок сети/БД. | `pytest` с `responses`, `Mockito`/`MockK`, `SQLDelight` in-memory. |
| Instrumented UI | `android/ui`, `frontend/src` | Проверка сценариев взаимодействия пользователей, переходов по экранам, сохранения состояния. | `Espresso`, `UIAutomator`, `Playwright`/`Cypress`. |
| Integration | `backend` | Проверка REST и WebSocket API, взаимодействия с БД и очередями. | `pytest` + `httpx.AsyncClient`, `docker-compose`, `pytest-asyncio`. |
| Contract | `backend` ↔ `frontend` | Проверка совместимости схем API и DTO. | `schemathesis`, `Pact` (consumer-driven). |
| E2E | Веб + backend | Проверка пользовательских сценариев (регистрация, игра, лидерборды). | `Playwright`, `Docker` окружение. |
| ML Evaluation | `model` | Проверка точности и производительности TFLite/ONNX моделей на валидационных наборах. | `pytest`, `onnxruntime`, `tensorflow` (`tflite_runtime`). |
| Load/Stress | `backend`, WebSocket | Проверка масштабируемости матчмейкинга и API. | `Locust`, `k6`. |

## Организация пайплайна

1. **Pre-commit:** статический анализ (`ruff`, `mypy`, `eslint`, `ktlint`) и быстрые unit-тесты `pytest -m "fast"`, `./gradlew testDebugUnitTest`.
2. **CI (PR):**
   - Запуск всех unit- и интеграционных тестов backend (`pytest`), проверка миграций БД через `alembic upgrade head --sql`.
   - Сборка Android (`./gradlew assembleDebug`) + unit-тесты (`./gradlew test`).
   - Линтеры фронтенда + unit (`npm test -- --watch=false`).
   - Генерация отчётов покрытий (`pytest --cov`, `jacoco`, `nyc`).
3. **Nightly:**
   - Полные E2E сценарии в Docker Compose (`playwright test`).
   - Нагрузочные проверки (`locust -f locustfile.py --headless`).
   - Прогон датасета жестов, расчёт метрик (`python model/eval.py`).

## Тегирование и данные

- Все тесты маркируются по подсистемам (`@pytest.mark.domain`, `@Tag("ui")`) для избирательного запуска.
- Для интеграционных тестов backend используется тестовая PostgreSQL через `docker-compose -f docker-compose.test.yml`.
- Mock-сервисы (например, для аналитики) поднимаются через `wiremock`/`httptest`.
- Для ML-тестов датасеты хранятся в `model/datasets/test` и подключаются как внешние артефакты в CI.

## Артефакты и отчётность

- Отчёты покрытий публикуются в CI (Codecov/SonarQube) и анализируются по порогам (`coverage ≥ 75%` на доменном слое).
- Скриншоты/видео из UI-тестов сохраняются как артефакты GitHub Actions.
- Результаты нагрузочных тестов загружаются в Grafana для трендового анализа.

## Ответственность

- **Backend-группа:** поддерживает `pytest`-сценарии, проверяет миграции, следит за контрактами API.
- **Android-группа:** владеет unit/UI тестами, обновляет mock-серверы для Espresso.
- **Frontend-группа:** поддерживает Playwright/Cypress, контролирует визуальные регрессии.
- **ML-группа:** обеспечивает актуальность датасетов и порогов точности.
- **QA:** оркестрирует E2E и нагрузочные проверки, следит за метриками стабильности релизов.

