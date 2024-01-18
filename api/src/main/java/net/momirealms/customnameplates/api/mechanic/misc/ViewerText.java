package net.momirealms.customnameplates.api.mechanic.misc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.momirealms.customnameplates.api.CustomNameplatesPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewerText {

    private final Player owner;
    private String processedText;
    private final ClaimedText[] placeholders;
    private final ConcurrentHashMap<UUID, String> valueMap;

    public ViewerText(Player owner, String rawText) {
        this.processedText = rawText;
        this.valueMap = new ConcurrentHashMap<>();
        this.owner = owner;
        List<String> placeholders = CustomNameplatesPlugin.get().getPlaceholderManager().detectPlaceholders(rawText);
        this.placeholders = new ClaimedText[placeholders.size()];
        int i = 0;
        for (String placeholder : placeholders) {
            processedText = processedText.replace(placeholder, "%s");
            if (placeholder.startsWith("%viewer_")) {
                this.placeholders[i] = new ClaimedText(null, "%" + placeholder.substring("%viewer_".length()));
            } else {
                this.placeholders[i] = new ClaimedText(owner, placeholder);
            }
            i++;
        }
    }

    public void updateForOwner() {
        for (ClaimedText text : placeholders) {
            text.update();
        }
    }

    public boolean updateForViewer(Player viewer) {
        String string;
        if ("%s".equals(processedText)) {
            string = placeholders[0].getValue(viewer);
        } else if (placeholders.length != 0) {
            Object[] values = new String[placeholders.length];
            for (int i = 0; i < placeholders.length; i++) {
                values[i] = placeholders[i].getValue(viewer);
            }
            string = String.format(processedText, values);
        } else {
            string = processedText;
        }
        var uuid = viewer.getUniqueId();
        if (!valueMap.containsKey(uuid)) {
            valueMap.put(uuid, string);
            return true;
        }
        String previousValue = valueMap.get(uuid);
        if (!previousValue.equals(string)) {
            valueMap.put(uuid, string);
            return true;
        }
        return false;
    }

    public void removeViewer(Player viewer) {
        valueMap.remove(viewer.getUniqueId());
    }

    public String getProcessedText() {
        return processedText;
    }

    public String getLatestValue(Player viewer) {
        return valueMap.get(viewer.getUniqueId());
    }

    public Entity getOwner() {
        return owner;
    }

    public static class ClaimedText {

        private final String placeholder;
        private final Player owner;
        private String latestValue;

        public ClaimedText(Player owner, String placeholder) {
            this.placeholder = placeholder;
            this.owner = owner;
            this.latestValue = null;
            this.update();
        }

        public void update() {
            if (owner == null) return;
            this.latestValue = PlaceholderAPI.setPlaceholders(owner, placeholder);
        }

        public String getValue(Player viewer) {
            return Objects.requireNonNullElseGet(
                    latestValue,
                    () -> PlaceholderAPI.setPlaceholders(owner == null ? viewer : owner, placeholder)
            );
        }
    }
}