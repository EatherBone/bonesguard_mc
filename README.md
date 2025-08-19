## EN
# BoneGuard Plugin
Lightweight chat moderation plugin for Minecraft Paper 1.21 server. 

Simple plugin that:

1. Replace the ban-words in chat with "****" using the blacklist from **filter.yml**. 
2. Notifies player that use the ban words too often and mutes for X amount of time if player use ban words off limit.
3. Auto-mute command constructor in **config.yml**. If you use external plugin for muting players - you might need this setting.
4. Auto-CAPS moderation will format player's message to lowercase if message includes more than X% of caps in one word using the **min_length** and **threshold** settings from **config.yml**.
5. Ping nicknames in chat! If Player1 mentions Player2 in chat - Player2 gets a sound notification (xp orb sound) and can see own nickname in chat formatted in color.
6. Fast response to player who pinged you in chat! Hover and click on colored nickname in chat to open new msg/whisper tab leads to original message sender.

### Commands
/bonesguard <reload|help>
/bmute <player> <time> [reason] - mutes player
/bunmute <player> - force unmute player

### Permissions
1. bonesguard.mute - allows muting players
2. bonesguard.reload - allows reloading plugin configuration
3. bonesguard.nofilter - allows to bypass filters

## RU
# BoneGuard Плагин

Лёгкий плагин для модерации чата на сервере Minecraft Paper 1.21.

Простой плагин, который:

1. Заменяет запрещённые слова в чате на "****", используя чёрный список из filter.yml.
2. Уведомляет игрока, который слишком часто использует запрещённые слова, и автоматически мутит его на X времени, если превышен лимит.
3. Конструктор команды автоматического мута в config.yml. Если вы используете внешний плагин для мута игроков – вам может понадобиться эта настройка.
4. Авто-модерация CAPS: сообщение игрока будет автоматически переведено в нижний регистр, если в нём есть слово с количеством заглавных букв более X% согласно настройкам min_length и threshold в config.yml.
5. Упоминание ников в чате! Если Player1 упомянет Player2 в чате – Player2 услышит звуковое уведомление (звук xp orb) и увидит свой ник в чате выделенным цветом.
6. Быстрый ответ игроку, который упомянул вас в чате! Наведите курсор и кликните на выделенный цветом ник в чате, чтобы открыть новое окно msg/whisper, которое ведёт к отправителю исходного сообщения.

### Команды
/bonesguard <reload|help>
/bmute <player> <time> [reason] - замутить игрока
/bunmute <player> - размутить игрока

### Permissions
1. bonesguard.mute - позволяет мутить игроков
2. bonesguard.reload - позволяет обновлять конфигурацию плагина
3. bonesguard.nofilter - позволяет обходить все проверки чата
