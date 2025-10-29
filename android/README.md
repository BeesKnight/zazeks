# Мобильное приложение Zazeks

Этот документ описывает процесс подготовки и сборки Android-приложения проекта, включая
настройку тестовой подписи, проверку встроенных ресурсов и запуск.

## Предварительные требования

- Android Studio Iguana (или новее) **или** установленный Gradle 8.5+
- Android SDK 34, платформа-инструменты и эмулятор/устройство с Android 7.0+
- Java 17 (рекомендуется использовать комплект JDK, поставляемый с Android Studio)

## Структура проекта

```
android/
├── build.gradle          # Модульное описание Gradle
├── signing/              # Настройки подписи (шаблон и ваши ключи)
├── src/main/assets/      # Ресурсы, конфигурации и модель
└── src/main/java/...     # Исходный код приложения
```

Каталог `src/main/assets` содержит пример конфигурации backend, демонстрационные данные модели
и служебные файлы камеры. Они автоматически упаковываются в APK для режимов debug и release.

## Настройка подписи APK

В каталоге `signing/` лежит файл-шаблон `signing.properties.example`.
Скопируйте его в `signing/signing.properties` и заполните своими данными.
Если у вас ещё нет keystore, создайте его (например, командой ниже) и пропишите путь и пароли:

```bash
keytool -genkeypair -v \
  -storetype PKCS12 \
  -keystore signing/my-release-key.jks \
  -alias my-key-alias \
  -keyalg RSA -keysize 2048 -validity 3650
```

После генерации укажите значения в `signing/signing.properties`:

```
storeFile=signing/my-release-key.jks
storePassword=<пароль к хранилищу>
keyAlias=<алиас ключа>
keyPassword=<пароль ключа>
```

Gradle применит подпись и для debug, и для release сборки, если файл `signing/signing.properties` существует.

## Обновление конфигурации backend

Приложение читает параметры окружения из `src/main/assets/config/backend.json`. Перед сборкой
обновите адреса под своё окружение. Пример содержимого:

```json
{
  "backendBaseUrl": "http://10.0.2.2:8000",
  "detectionEndpoint": "/model/detect",
  "timeoutSeconds": 15
}
```

Для удобства рядом лежит шаблон `backend.example.json`.

## Сборка из командной строки

```bash
cd android
./gradlew assembleDebug   # сборка debug и генерация app-debug.apk
./gradlew assembleRelease # сборка release и генерация app-release.apk
```

> Если Gradle wrapper ещё не инициализирован в вашей среде, запустите `gradle wrapper` один раз или
> воспользуйтесь Android Studio (она создаст wrapper автоматически).

Готовые APK можно найти в `android/build/outputs/apk/<buildType>/`.

## Установка APK на устройство

1. Включите режим разработчика и «Отладку по USB» на устройстве.
2. Подключите устройство к компьютеру и убедитесь, что оно определяется командой `adb devices`.
3. Установите нужную сборку:
   ```bash
   adb install -r build/outputs/apk/debug/android-debug.apk
   ```
4. Запустите приложение «Zazeks» на устройстве/эмуляторе.

## Проверка встроенных файлов

- В Android Studio откройте вкладку *Build* → *Analyze APK* и выберите собранный APK.
- Убедитесь, что внутри присутствуют каталоги:
  - `assets/config/backend.json`
  - `assets/model/gesture_labels.json`
  - `assets/resources/camera_presets.json`
- Для автоматизации можно использовать команду `./gradlew verifyReleaseResources`, которая
  проверит наличие ресурсов на этапе сборки.

## Запуск демонстрации

1. Убедитесь, что backend (FastAPI) запущен и доступен по адресу, указанному в `backend.json`.
2. Запустите приложение и выберите «Новая игра» в главном меню.
3. Нажмите «Старт» — камера активируется и начнёт отправлять кадры в backend.
4. Показывайте жесты «камень», «ножницы» или «бумага» перед камерой.
5. Наблюдайте за результатами раунда и статистикой побед в приложении.
6. Для завершения сеанса вернитесь в главное меню и откройте «Результаты».

Дополнительный сценарий презентации размещён в `docs/android/demo_script.md`.
