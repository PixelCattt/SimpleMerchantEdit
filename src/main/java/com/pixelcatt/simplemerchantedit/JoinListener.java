package com.pixelcatt.simplemerchantedit;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.ChatColor;


public class JoinListener implements Listener {

    private final SimpleMerchantEdit plugin;
    private final MerchantManager manager;

    private boolean sendUpdateNotification = false;


    public JoinListener(SimpleMerchantEdit plugin, MerchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.sendUpdateNotification = updateAvailable;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (sendUpdateNotification && player.isOp()) {
            // Prefix
            TextComponent prefix = new TextComponent();

            TextComponent prefixPart1 = new TextComponent("[");
            prefixPart1.setColor(ChatColor.RED);
            prefixPart1.setBold(true);
            prefix.addExtra(prefixPart1);

            TextComponent prefixPart2 = new TextComponent("Simple");
            prefixPart2.setColor(ChatColor.GREEN);
            prefixPart2.setBold(true);
            prefix.addExtra(prefixPart2);

            TextComponent prefixPart3 = new TextComponent("Merchant");
            prefixPart3.setColor(ChatColor.BLUE);
            prefixPart3.setBold(true);
            prefix.addExtra(prefixPart3);

            TextComponent prefixPart4 = new TextComponent("Edit");
            prefixPart4.setColor(ChatColor.GREEN);
            prefixPart4.setBold(true);
            prefix.addExtra(prefixPart4);

            TextComponent prefixPart5 = new TextComponent("]");
            prefixPart5.setColor(ChatColor.RED);
            prefixPart5.setBold(true);
            prefix.addExtra(prefixPart5);

            TextComponent prefixPart6 = new TextComponent(" ");
            prefixPart6.setColor(ChatColor.WHITE);
            prefixPart6.setBold(false);
            prefix.addExtra(prefixPart6);


            // Message 1
            TextComponent message1 = new TextComponent();

            message1.addExtra(prefix);

            TextComponent Text1 = new TextComponent("A new Update is available!");
            Text1.setColor(ChatColor.GOLD);
            message1.addExtra(Text1);

            player.spigot().sendMessage(message1);


            // Message 2
            TextComponent message2 = new TextComponent();

            message2.addExtra(prefix);

            TextComponent Text2 = new TextComponent("Click here to Download it!");
            Text2.setColor(ChatColor.AQUA);
            Text2.setUnderlined(true);
            Text2.setClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL,
                "https://modrinth.com/plugin/simple_ban/versions"
            ));
            message2.addExtra(Text2);

            player.spigot().sendMessage(message2);
        }
    }
}