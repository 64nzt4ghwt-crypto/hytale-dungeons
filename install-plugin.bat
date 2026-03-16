@echo off
echo === Dungeon Plugin Installer ===
set "MODDIR=%APPDATA%\Hytale\UserData\Saves\New World\mods\com.howlstudio_DungeonPlugin"
set "CFGDIR=%MODDIR%\DungeonConfigs"
set "DL=%USERPROFILE%\Downloads"

mkdir "%CFGDIR%" 2>nul

copy /Y "%DL%\DungeonPlugin-0.1.0.jar" "%MODDIR%\" 2>nul && echo JAR copied! || echo JAR not found in Downloads
copy /Y "%DL%\goblin_den.json" "%CFGDIR%\" 2>nul && echo goblin_den copied!
copy /Y "%DL%\skeleton_crypt.json" "%CFGDIR%\" 2>nul && echo skeleton_crypt copied!
copy /Y "%DL%\spider_nest.json" "%CFGDIR%\" 2>nul && echo spider_nest copied!

echo.
echo === Done! Restart Hytale to load the plugin ===
pause
