package ru.eatherbone.panel;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BonesGuard extends JavaPlugin implements Listener {

    // просто конфиги
    private FileConfiguration filterConfig;
    private FileConfiguration muteConfig;
    private FileConfiguration pluginConfig;
    private File filterFile;
    private File muteFile;
    private File configFile;

    // храним проверки и кулдауны
    private final Map<String, Integer> forbiddenWordAttempts = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        loadMainConfig();
        loadFilterConfig();
        loadMuteConfig();

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("bonesguard").setExecutor(this);
        getCommand("bmute").setExecutor(this);
        getCommand("bunmute").setExecutor(this);

        getLogger().info("BonesGuard загружен и готов карать.");
    }

    @Override
    public void onDisable() {
        saveMuteConfig();
        getLogger().info("BonesGuard отключается. Спокойной ночи.");
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("bonesguard.nofilter")) {
            return;
        }

        String originalMessage = LegacyComponentSerializer.legacySection().serialize(event.originalMessage());

        if (isPlayerMuted(player)) {
            player.sendMessage(Component.text("[BoneGuard]: Вы замучены и не можете писать в чат!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (isOnCooldown(player)) {
            sendWarningMessage(player, "Подожди немного, не флуди.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String filteredMessage = filterForbiddenWords(originalMessage);
        String finalMessage = applyAntiCaps(filteredMessage);

        if (!originalMessage.equalsIgnoreCase(filteredMessage)) {
            incrementForbiddenWordAttempts(player);
            int attempts = forbiddenWordAttempts.getOrDefault(player.getName(), 0);
            int attemptsLimit = pluginConfig.getInt("filter.attempts_limit", 4);

            if (attempts >= attemptsLimit) {
                executeAutoMute(player);
                forbiddenWordAttempts.remove(player.getName());
                return;
            } else {
                sendWarningMessage(player, "Замечен бан-ворд. Предупреждений до мута: " + (attemptsLimit - attempts));
            }
        } else {
            forbiddenWordAttempts.put(player.getName(), 0);
        }

        Mentions.sendPersonalizedMessage(player, finalMessage);

        setCooldown(player);
    }

    /**
     * Выполняет кастомную команду наказания из конфига от имени консоли.
     * @param player Игрок, который будет наказан.
     */

    private void executeAutoMute(Player player) {
        String commandTemplate = pluginConfig.getString("auto-mute-settings.command");
        String time = pluginConfig.getString("auto-mute-settings.time", "15m");
        String reason = pluginConfig.getString("auto-mute-settings.reason", "Нарушение правил чата");

        if (commandTemplate == null || commandTemplate.isBlank()) {
            getLogger().warning("Команда для авто-мута не настроена в config.yml! Наказание не будет выдано.");
            return;
        }

        String command = commandTemplate
                .replace("{player}", player.getName())
                .replace("{time}", time)
                .replace("{reason}", reason);

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        final String commandToExecute = command;
        final String playerName = player.getName();

        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
            getLogger().info("Выдано авто-наказание игроку " + playerName + " командой: " + commandToExecute);
        });
    }

    private String filterForbiddenWords(String message) {
        if (filterConfig != null) {
            List<String> forbiddenWords = filterConfig.getStringList("forbidden_words");
            if (forbiddenWords != null) {
                for (String word : forbiddenWords) {
                    // регистрозависимость - Pattern.CASE_INSENSITIVE
                    // чтобы эта хуйня могла с русским работать, используем - Pattern.UNICODE_CASE
                    Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    Matcher matcher = pattern.matcher(message);
                    message = matcher.replaceAll("****");
                }
            }
        }
        return message;
    }

    private String applyAntiCaps(String message) {
        int minLength = pluginConfig.getInt("anticaps.min_length", 10);
        double capsThreshold = pluginConfig.getDouble("anticaps.threshold", 0.3);
        if (message.length() < minLength) {
            return message;
        }
        int letterCount = 0;
        int uppercaseCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
                if (Character.isUpperCase(c)) {
                    uppercaseCount++;
                }
            }
        }
        if (letterCount > 0 && (double) uppercaseCount / letterCount > capsThreshold) {
            return message.toLowerCase();
        }
        return message;
    }

    private void incrementForbiddenWordAttempts(Player player) {
        int attempts = forbiddenWordAttempts.getOrDefault(player.getName(), 0);
        forbiddenWordAttempts.put(player.getName(), attempts + 1);
    }

    private boolean isOnCooldown(Player player) {
        return cooldowns.getOrDefault(player.getName(), 0L) > System.currentTimeMillis();
    }

    private void setCooldown(Player player) {
        long cooldownDuration = pluginConfig.getLong("timeout.softness", 2000L);
        cooldowns.put(player.getName(), System.currentTimeMillis() + cooldownDuration);
    }

    private void sendWarningMessage(Player player, String message) {
        player.sendMessage(Component.text("[BoneGuard]: " + message, NamedTextColor.YELLOW));
    }

    private boolean mutePlayer(Player player, String duration, String reason) {
        long muteTime = parseDuration(duration);
        if (muteTime == -1) return false;
        long muteEndTime = System.currentTimeMillis() + muteTime;
        String uuid = player.getUniqueId().toString();
        muteConfig.set(uuid + ".name", player.getName());
        muteConfig.set(uuid + ".end_time", muteEndTime);
        muteConfig.set(uuid + ".reason", reason);
        saveMuteConfig();
        String readableDuration = formatDuration(duration);
        player.sendMessage(Component.text("[BoneGuard]: Вы получили мут на " + readableDuration + " по причине: " + reason, NamedTextColor.RED));
        return true;
    }

    private boolean unmutePlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (muteConfig.contains(uuid)) {
            muteConfig.set(uuid, null);
            saveMuteConfig();
            return true;
        }
        return false;
    }

    private boolean isPlayerMuted(Player player) {
        String uuid = player.getUniqueId().toString();
        if (muteConfig.contains(uuid)) {
            long endTime = muteConfig.getLong(uuid + ".end_time");
            if (System.currentTimeMillis() < endTime) {
                return true;
            } else {
                muteConfig.set(uuid, null);
                saveMuteConfig();
            }
        }
        return false;
    }

    private long parseDuration(String duration) {
        try {
            char unit = duration.charAt(duration.length() - 1);
            int value = Integer.parseInt(duration.substring(0, duration.length() - 1));
            return switch (unit) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60 * 1000L;
                case 'h' -> value * 3600 * 1000L;
                case 'd' -> value * 86400 * 1000L;
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatDuration(String duration) {
        try {
            char unit = duration.charAt(duration.length() - 1);
            int value = Integer.parseInt(duration.substring(0, duration.length() - 1));
            return switch (unit) {
                case 's' -> value + " " + getPluralForm(value, "секунда", "секунды", "секунд");
                case 'm' -> value + " " + getPluralForm(value, "минута", "минуты", "минут");
                case 'h' -> value + " " + getPluralForm(value, "час", "часа", "часов");
                case 'd' -> value + " " + getPluralForm(value, "день", "дня", "дней");
                default -> duration;
            };
        } catch (Exception e) {
            return duration;
        }
    }

    private String getPluralForm(int number, String form1, String form2, String form5) {
        number = Math.abs(number) % 100;
        int n1 = number % 10;
        if (number > 10 && number < 20) return form5;
        if (n1 > 1 && n1 < 5) return form2;
        if (n1 == 1) return form1;
        return form5;
    }

    private void loadMainConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        pluginConfig = getConfig();
    }

    private void loadFilterConfig() {
        filterFile = new File(getDataFolder(), "filter.yml");
        if (!filterFile.exists()) {
            saveResource("filter.yml", false);
        }
        filterConfig = YamlConfiguration.loadConfiguration(filterFile);
    }

    private void loadMuteConfig() {
        muteFile = new File(getDataFolder(), "mutes.yml");
        if (!muteFile.exists()) {
            try {
                muteFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл mutes.yml: " + e.getMessage());
            }
        }
        muteConfig = YamlConfiguration.loadConfiguration(muteFile);
    }

    private void saveMuteConfig() {
        try {
            muteConfig.save(muteFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить файл mutes.yml: " + e.getMessage());
        }
    }

    private void reloadConfigs() {
        reloadConfig();
        pluginConfig = getConfig();
        filterConfig = YamlConfiguration.loadConfiguration(filterFile);
        muteConfig = YamlConfiguration.loadConfiguration(muteFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bonesguard")) {
            if (args.length == 0) {
                sender.sendMessage(Component.text("Использование: /bonesguard reload или /bonesguard help"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("bonesguard.reload")) {
                    sender.sendMessage(Component.text("Тебе нельзя это использовать. Можешь не пытаться.", NamedTextColor.RED));
                    return true;
                }
                reloadConfigs();
                sender.sendMessage(Component.text("Конфигурация перезагружена.", NamedTextColor.GREEN));
                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(Component.text("/bonesguard reload - Перезагрузить конфигурации"));
                sender.sendMessage(Component.text("/bonesguard help - Показать эту справку"));
                sender.sendMessage(Component.text("/bmute <игрок> <время> [причина] - Замутить игрока"));
                sender.sendMessage(Component.text("/bunmute <игрок> - Размутить игрока"));
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("bmute")) {
            if (!sender.hasPermission("bonesguard.mute")) {
                sender.sendMessage(Component.text("Тебе нельзя это использовать. Можешь не пытаться.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(Component.text("Использование: /bmute <игрок> <время> [причина]", NamedTextColor.RED));
                sender.sendMessage(Component.text("Пример: /bmute EatherBone 1h Оскорбления", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Игрок " + args[0] + " не найден или не в сети.", NamedTextColor.RED));
                return true;
            }

            String duration = args[1];
            String reason = "Нарушение правил сервера";
            if (args.length > 2) {
                reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            }

            if (mutePlayer(target, duration, reason)) {
                String readableDuration = formatDuration(duration);
                Bukkit.broadcast(Component.text()
                        .append(Component.text("[BoneGuard]: ", NamedTextColor.GREEN))
                        .append(Component.text("Игроку " + target.getName() + " выдан мут на " + readableDuration + " по причине: " + reason))
                        .build());
                sender.sendMessage(Component.text("Игрок " + target.getName() + " успешно замучен!", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Неверный формат времени! Используйте: 15m, 1h, 2d и т.д.", NamedTextColor.RED));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("bunmute")) {
            if (!sender.hasPermission("bonesguard.mute")) {
                sender.sendMessage(Component.text("Тебе нельзя это использовать. Можешь не пытаться.", NamedTextColor.RED));
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(Component.text("Использование: /bunmute <игрок>", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Игрок " + args[0] + " не найден (должен быть онлайн)!", NamedTextColor.RED));
                return true;
            }

            if (unmutePlayer(target)) {
                target.sendMessage(Component.text("[BoneGuard]: Вы снова можете писать в чат!", NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Игрок " + target.getName() + " успешно размучен!", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Игрок " + target.getName() + " не был замучен!", NamedTextColor.RED));
            }
            return true;
        }

        return false;
    }
}