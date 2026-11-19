# Chastify Device-Owner Bridge

LSPosed module that makes the system treat the **Chastify** app
(`net.chastify.app`) as **device owner** — with no real provisioning and no
second app (Owndroid).

**[⬇ Download the APK from Releases](https://github.com/mishkarala/Lsposed-module-chastify/releases/latest)**

*(Русская версия ниже — [Russian version below](#chastify-device-owner-bridge-ru))*

---

## How it works

The module loads into `system_server` (scope = `system`) and hooks the internal
owner checks of `DevicePolicyManagerService`:

- `isDeviceOwner(CallerIdentity …)`
- `isProfileOwner(CallerIdentity …)`
- overloads of those taking a `ComponentName`

The hook fires **only when the caller is Chastify** — then the check returns
`true` and the system actually applies Chastify's request (`addUserRestriction`,
etc.). For every other app and system component the original method runs, so
there are no side effects.

`DevicePolicyManagerService`'s methods are internal, non-SDK and version-specific,
so they are resolved **reflectively at runtime** (by name + argument type) rather
than referenced at compile time. That keeps the module from being pinned to one
Android version.

## Before you use it

1. **Chastify still has to be enabled as a normal device-admin**
   (Settings → device admin apps). The module only fakes the "admin → owner"
   elevation, not the admin registration itself — otherwise the system can't find
   its `ActiveAdmin` and crashes before the owner check.
2. **Android 14+**: some DPC calls moved to permission-based access
   (`MANAGE_DEVICE_POLICY_*`) instead of the owner check. User-restrictions still
   go through the old path for now; if a specific call fails with
   `SecurityException`, a second hook on the permission check is needed — see
   `logcat | grep ChastifyBridge`.

## Install

1. Install the APK (from **Releases**).
2. Enable the module in LSPosed; keep the scope on **System Framework**
   (`system`).
3. Reboot.
4. Enable Chastify as a device administrator.
5. Check `logcat | grep ChastifyBridge` — you should see
   `installed N owner-check hook(s)`.

## Build

GitHub Actions builds a debug APK on every push (Actions → artifacts). Pushing a
tag like `v1.0` builds the APK and attaches it to a **Release**. Locally:

```
./gradlew assembleDebug
```

## Dependencies

Built against the classic Xposed API (`de.robv.android.xposed:api:82`, bundled as
`app/libs/api-82.jar`, `compileOnly`). The classic API is recognized and loaded by
every LSPosed build; the newer libxposed API is not listed by some LSPosed
variants, which is why this module uses the classic one. Entry point is declared
in `assets/xposed_init`.

---

<a name="chastify-device-owner-bridge-ru"></a>

# Chastify Device-Owner Bridge (RU)

LSPosed-модуль, который заставляет систему считать приложение **Chastify**
(`net.chastify.app`) **владельцем устройства** — без реального provisioning и без
второго приложения (Owndroid).

**[⬇ Скачать APK из Releases](https://github.com/mishkarala/Lsposed-module-chastify/releases/latest)**

## Как это работает

Модуль грузится в `system_server` (scope = `system`) и хукает внутренние
проверки владельца в `DevicePolicyManagerService`:

- `isDeviceOwner(CallerIdentity …)`
- `isProfileOwner(CallerIdentity …)`
- перегрузки этих методов, принимающие `ComponentName`

Хук срабатывает **только когда вызывающий — Chastify**: тогда проверка
возвращает `true`, и система реально применяет запрос Chastify
(`addUserRestriction` и т.п.). Для всех остальных приложений и компонентов
системы вызывается оригинальный метод, так что побочных эффектов нет.

Методы `DevicePolicyManagerService` — внутренние, не из SDK, их сигнатуры меняются
между версиями Android, поэтому они ищутся **рефлексивно во время выполнения**
(по имени + типу аргумента), а не указываются при компиляции. За счёт этого модуль
не привязан к одной версии Android.

## Важно перед использованием

1. **Chastify всё равно нужно включить как обычного device-admin**
   (Настройки → приложения администратора устройства). Модуль подделывает только
   повышение «админ → владелец», но не саму регистрацию админа — иначе система не
   найдёт `ActiveAdmin` и упадёт ещё до проверки владельца.
2. **Android 14+**: часть DPC-вызовов переведена на модель permission-based access
   (разрешения `MANAGE_DEVICE_POLICY_*`) вместо проверки владельца. Для
   user-restrictions старый путь пока работает; если конкретный вызов упадёт с
   `SecurityException`, нужно добавить ещё один хук на проверку разрешения — смотри
   `logcat | grep ChastifyBridge`.

## Установка

1. Установить APK (из **Releases**).
2. В LSPosed включить модуль, в области действия оставить **System Framework**
   (`system`).
3. Перезагрузиться.
4. Включить Chastify как администратора устройства.
5. Проверить `logcat | grep ChastifyBridge` — должно быть
   `installed N owner-check hook(s)`.

## Сборка

GitHub Actions собирает debug-APK на каждый push (Actions → артефакты). Пуш тега
вида `v1.0` собирает APK и прикладывает его к **Release**. Локально:

```
./gradlew assembleDebug
```
