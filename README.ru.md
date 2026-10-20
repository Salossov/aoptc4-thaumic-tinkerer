# Thaumic Tinkerer

[English](README.md)

Я поддерживаю неофициальный порт **Thaumic Tinkerer 2.5-1.7.10-164** на **Minecraft 1.21.1 / NeoForge** через публичный API AOP: Thaumcraft 4.

## Содержимое

В порт входят исходные устройства, набалдашники, зачарования, насыщенные посевы и огонь, управление големами и периферия ComputerCraft. KAMI включает материалы, снаряжение, пробуждённые инструменты, крылья, сумку, строительное зеркало, врата, Recall, Shadowbeam и бедроковое измерение.

Необязательные интеграции: CC: Tweaked, Applied Energistics 2, Botania, EnderStorage, CB Multipart и неофициальный порт Blood Magic. Tinkers' Construct, IndustrialCraft 2 и OpenComputers не включены. Старый VersionChecker удалён; замена механизма обновлений запланирована.

## Установка

Нужны Minecraft **1.21.1**, Java **21**, NeoForge **от 21.1.252**, **Curios от 9.5.1+1.21.1** и совместимая сборка **AOP: Thaumcraft 4 1.0.0**. Все зависимости должны быть для Minecraft 1.21.1 / NeoForge. Игровой JAR устанавливается на клиент и сервер. Отдельный API JAR Thaumcraft устанавливать как мод не нужно.

Аддон использует добавленные публичные контракты ядра: старый JAR с тем же номером версии может оказаться несовместимым. Обновляйте ядро и аддон вместе.

Необязательные моды устанавливаются со своими зависимостями. **AOP:TC4 Compact** отдельно добавляет рецепты JEI/EMI и подсказки Jade/WTHIT, включая устройства Tinkerer. Сам Tinkerer не требует этих просмотрщиков.

## Сборка

Установите JDK 21. Сначала соберите API и игровой мод из совместимых исходников AOP: Thaumcraft 4:

```powershell
./gradlew.ps1 :api:assemble :mod:assemble
```

В этом репозитории явно укажите API JAR:

```powershell
./gradlew.ps1 build -PthaumcraftApiJar=/path/to/thaumcraft-api-1.0.0.jar
```

Можно также поместить его в `libs/`. В Linux/macOS используйте `sh ./gradlew`. Необязательные API загружаются из Maven-репозиториев их авторов. Для разработки доступны явные параметры `-PbotaniaJar`, `-PenderStorageJar`, `-PcodeChickenLibJar` и `-PmultipartJar`.

Игровой файл: `build/libs/ThaumicTinkerer-1.21.1-2.5.164-port.1.jar`. Проверочный код и классы сторонних модов в него не входят.

## Проверки

`build` компилирует проверочные исходники и выполняет отдельные проверки данных и отображения. Нативные GameTest запускаются отдельно и требуют совместимого ядра через `-PthaumcraftRuntimeJar=/path/to/core.jar`. Интеграции включаются параметрами `-PwithComputerCraft=true`, `-PwithAppliedEnergistics=true`, `-PwithBotania=true`, `-PwithEnderStorage=true` и `-PwithMultipart=true`. Для профиля Blood Magic JAR самого мода и Modopedia указываются явно.

Ручная проверка игровых, графических и многопользовательских сценариев продолжается. В сообщении об ошибке укажите версии, способ воспроизведения и соответствующий лог. GitHub workflow собирает изменения кода; правки только документации сборку не запускают.

## Авторы и лицензия

- [Vazkii](https://github.com/Vazkii/ThaumicTinkerer), pixlepix, nekosune и участники оригинала — Thaumic Tinkerer.
- [Azanor](https://www.curseforge.com/minecraft/mc-mods/thaumcraft) — Thaumcraft.
- [Salossov](https://github.com/Salossov) — порт на Minecraft 1.21.1 с применением ИИ при реализации и проверке.

Я сохраняю авторство и условия **CC BY-NC-SA 3.0**; см. [NOTICE.md](NOTICE.md). Лицензия Tinkerer не разрешает распространять сам Thaumcraft. Порт неофициальный.

## Настройка CI

Переменные репозитория `THAUMCRAFT_REPOSITORY` и `THAUMCRAFT_REF` выбирают исходники ядра (по умолчанию `Salossov/aop-thaumcraft-4`, `main`). Используйте совместимые ревизии ядра и аддона. Для приватных зависимостей checkout требует токен с правом чтения.
