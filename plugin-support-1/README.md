# Plugin support

Перед вами музыкальный плеер с поддержкой плагинов.

Задачи:

* Дополните реализацию `MusicApp` кодом для загрузки плагинов и управления их жизненным циклом.
    * Изучите KDoc (комментарии с документацией) к имеющимся типам. Контракты описаны в документации.
    * Плагины должны загружаться в раздельных class loader'ах, дочерних по отношению к загрузчику самого приложения.
      На каждый `MusicPluginPath` – по одному загрузчику классов.
* Творческая часть: напишите свой плагин (или плагины)
    * Можно реализовать один плагин с интересной функциональностью, которую можно найти в популярных плеерах 
      (как `UsageStatsPlugin`). 
      А можно реализовать несколько несложных плагинов, которые взаимодействуют друг с другом и предоставляют
      "точки расширения" для других плагинов (как `ConsoleControlsPlugin` + `ConsoleHandlerPlugin` + 
      `BasicConsoleControlsPlugin`). Во втором случае общие интерфейсы можно добавить в главный модуль, 
      чтобы они оказались в родительском загрузчике и были видны всем плагинам.
    * Хотя бы один написанный плагин должен поставляться отдельным модулем
        * Посмотрите, как оформлен `third-party-plugin`, и добавьте новый модуль таким же образом. Обратите внимание на
          свойство `third-party-plugin-file` и то, как оно задаётся в разных случаях.
    * Напишите тесты для новых плагинов.
    
* Требование: программа должна запускаться (и работать корректно) в том числе из "дистрибутива", который можно собрать с помощью 
  `./gradlew installDist`. Собранный "дистрибутив" можно найти по пути `build/install/plugin-support-hw` и запустить из 
  него `./plugin-support-hw` (в UNIX-подобных ОС) или `plugin-support-hw.bat` (Windows). Если программе 
  нужны дополнительные ресурсы, настройте их упаковку в `bulid.gradle.kts` в секции `application {
  distributions { ... } }`