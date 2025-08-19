package ru.eatherbone.panel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor; // боже храни adventure api
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mentions {

    public static void sendPersonalizedMessage(Player sender, String message) {
        Set<String> onlinePlayerNames = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            onlinePlayerNames.add(p.getName());
        }

        if (onlinePlayerNames.isEmpty()) {
            return;
        }

        Pattern pattern = buildPattern(onlinePlayerNames);
        Component senderDisplayName = sender.displayName();

        Bukkit.getConsoleSender().sendMessage(Component.text()
                .append(senderDisplayName)
                .append(Component.text(": " + message))
                .build());

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            Matcher matcher = pattern.matcher(message);
            TextComponent.Builder messageBuilder = Component.text();
            int lastEnd = 0;

            while (matcher.find()) {
                messageBuilder.append(Component.text(message.substring(lastEnd, matcher.start())));

                String matchedName = matcher.group(1);

                if (matchedName.equalsIgnoreCase(recipient.getName())) {
                    TextComponent mentionComponent = Component.text("@" + matchedName) // подставляем @ перед ником (видно если пинганули тебя)
                            .color(NamedTextColor.GOLD) // цвет выделения ника, если тебя пинганули
                            .decorate(TextDecoration.BOLD) // выделяем ник жирным шрифтом
                            .hoverEvent(HoverEvent.showText(Component.text("Написать в ЛС", NamedTextColor.YELLOW)))
                            .clickEvent(ClickEvent.suggestCommand("/msg " + matchedName + " "));

                    messageBuilder.append(mentionComponent);
                    // ВОТ ТУТ "ДЗЫНЬ" КОГДА ТЕБЯ ПИНГУЮТ х)
                    recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } else {
                    messageBuilder.append(Component.text(matchedName));
                }
                lastEnd = matcher.end();
            }

            messageBuilder.append(Component.text(message.substring(lastEnd)));

            // Собираем полное сообщение
            Component fullMessage = Component.text()
                    .append(senderDisplayName)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(messageBuilder.build())
                    .build();

            //  уникальное сообщение получателю
            recipient.sendMessage(fullMessage);
        }
    }

    private static Pattern buildPattern(Set<String> playerNames) {
        StringBuilder regex = new StringBuilder("\\b(");
        boolean first = true;
        for (String name : playerNames) {
            if (!first) {
                regex.append("|");
            }
            regex.append(Pattern.quote(name));
            first = false;
        }
        regex.append(")\\b");
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }
}