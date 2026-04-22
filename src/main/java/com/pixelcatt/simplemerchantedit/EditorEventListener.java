package com.pixelcatt.simplemerchantedit;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;


public class EditorEventListener implements Listener {

    private final SimpleMerchantEdit plugin;
    private final MerchantManager manager;


    public EditorEventListener(SimpleMerchantEdit plugin, MerchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /* ================= OPEN EDITOR ================= */

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!e.getPlayer().isSneaking()) return;

        if (e.getRightClicked() instanceof Villager villager) {
            if (e.getPlayer().hasPermission("simplemerchantedit.edit.villager") && manager.allowEditingVillagers) {
                if (villager.getProfession() == Villager.Profession.NONE) return;
                manager.openEditor(e.getPlayer(), villager);
                e.setCancelled(true);
            }
        }

        if (e.getRightClicked() instanceof WanderingTrader trader) {
            if (e.getPlayer().hasPermission("simplemerchantedit.edit.wandering_trader") && manager.allowEditingWanderingTraders) {
                manager.openWanderingTraderEditor(e.getPlayer(), trader);
                e.setCancelled(true);
            }
        }
    }

    /* ================= INVENTORY CLICK ================= */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();
        int slot = e.getRawSlot();
        if (slot < 0) return;

        /* ===== VILLAGER EDITOR ===== */
        if (title.equals("Edit Villager")) {
            handleVillagerEditorClick(e, player, inv, slot);
            return;
        }

        /* ===== WANDERING TRADER EDITOR ===== */
        if (title.equals("Edit Wandering Trader")) {
            handleWanderingTraderEditorClick(e, player, inv, slot);
            return;
        }

        /* ===== TRADE SETTINGS ===== */
        if (title.equals("Trade Settings")) {
            MerchantManager.SettingsGUIContext ctx = manager.openSettings.get(inv);
            if (ctx == null) return;

            if (ctx.editor != null && Bukkit.getEntity(manager.openEditors.get(ctx.editor)) instanceof Villager) {
                e.setCancelled(slot < e.getView().getTopInventory().getSize());
                handleVillagerSettingsClick(e, inv, slot);
            } else if (ctx.editor != null && Bukkit.getEntity(manager.openEditors.get(ctx.editor)) instanceof WanderingTrader) {
                e.setCancelled(slot < e.getView().getTopInventory().getSize());
                handleWanderingTraderSettingsClick(e, inv, slot);
            }
        }
    }

    /* ================= VILLAGER SETTINGS HANDLER ================= */

    private void handleVillagerSettingsClick(InventoryClickEvent e, Inventory inv, int slot) {
        Player player = (Player) e.getWhoClicked();
        MerchantManager.SettingsGUIContext ctx = manager.openSettings.get(inv);
        if (ctx == null) return;

        if (slot == 45) { // Back
            manager.saveSettings(inv);
            manager.openSettings.remove(inv);
            UUID id = manager.openEditors.get(ctx.editor);
            if (id != null) player.openInventory(ctx.editor);
            return;
        }

        if (slot == 31) { // XP toggle
            ItemStack i = inv.getItem(slot);
            if (i == null) return;
            i.setType(i.getType() == Material.LIME_DYE ? Material.RED_DYE : Material.LIME_DYE);
            return;
        }

        // Number items
        if (slot == 4 || slot == 13 || slot == 22) {
            int delta = e.isShiftClick() ? 5 : 1;
            ItemStack cur = inv.getItem(slot);
            int value = (cur != null && cur.getType() != Material.BARRIER) ? cur.getAmount() : 0;

            if (e.isLeftClick()) value += delta;
            if (e.isRightClick()) value -= delta;

            if (value < 0) value = 0;
            if (value > 64) value = 64;

            Material mat = switch (slot) {
                case 4 -> Material.EMERALD;
                case 13 -> Material.DIAMOND;
                case 22 -> Material.EXPERIENCE_BOTTLE;
                default -> Material.EMERALD;
            };

            inv.setItem(slot, manager.numberItem(manager.numberItemName(slot), value, mat));
        }
    }

    /* ================= WANDERING TRADER SETTINGS HANDLER ================= */

    private void handleWanderingTraderSettingsClick(InventoryClickEvent e, Inventory inv, int slot) {
        Player player = (Player) e.getWhoClicked();
        MerchantManager.SettingsGUIContext ctx = manager.openSettings.get(inv);
        if (ctx == null) return;

        // Back button
        if (slot == 27) {
            manager.saveSettings(inv);
            manager.openSettings.remove(inv);
            UUID id = manager.openEditors.get(ctx.editor);
            if (id != null) player.openInventory(ctx.editor);
            return;
        }

        // XP toggle for WT
        if (slot == 22) {
            ItemStack i = inv.getItem(slot);
            if (i == null) return;
            i.setType(i.getType() == Material.LIME_DYE ? Material.RED_DYE : Material.LIME_DYE);
            return;
        }

        // Number items: maximum uses and current uses only (slots 4 and 13)
        if (slot == 4 || slot == 13) {
            int delta = e.isShiftClick() ? 5 : 1;
            ItemStack cur = inv.getItem(slot);
            int value = (cur != null && cur.getType() != Material.BARRIER) ? cur.getAmount() : 0;

            if (e.isLeftClick()) value += delta;
            if (e.isRightClick()) value -= delta;

            if (value < 0) value = 0;
            if (value > 64) value = 64;

            Material mat = slot == 4 ? Material.EMERALD : Material.DIAMOND;
            inv.setItem(slot, manager.numberItem(manager.numberItemName(slot), value, mat));
        }
    }

    /* ================= INVENTORY CLOSE ================= */

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        String title = e.getView().getTitle();

        if (title.equals("Trade Settings")) {
            manager.saveSettings(inv);
            MerchantManager.SettingsGUIContext ctx = manager.openSettings.remove(inv);
            if (ctx != null) {
                UUID id = manager.openEditors.get(ctx.editor);
                if (id != null) {
                    if (Bukkit.getEntity(id) instanceof Villager) {
                        manager.saveEditor(ctx.editor);
                        manager.updateSettingsButtons(ctx.editor);
                    }
                    if (Bukkit.getEntity(id) instanceof WanderingTrader) {
                        manager.saveWanderingTraderEditor(ctx.editor);
                        manager.updateSettingsButtons(ctx.editor);
                    }
                }
            }
            return;
        }

        if (title.equals("Edit Villager")) {
            manager.saveEditor(inv);
        }

        if (title.equals("Edit Wandering Trader")) {
            manager.saveWanderingTraderEditor(inv);
        }
    }

    /* ================= HELPERS ================= */

    private void handleVillagerEditorClick(InventoryClickEvent e, Player player, Inventory inv, int slot) {
        Villager villager = getVillager(inv);
        if (villager == null) return;

        // Allow moving items only in rows 0-2 (first 3 rows)
        if (slot >= 0 && slot <= 26) {
            // Update trade settings buttons dynamically after any change
            Bukkit.getScheduler().runTask(plugin, () -> manager.updateSettingsButtons(inv));
            return; // allow moving items in these rows
        }

        // Block moving items in other rows
        if (slot >= 27 && slot <= 53) {
            e.setCancelled(true);
        }

        if (slot >= 27 && slot <= 35) { // Trade Settings buttons
            ItemStack i = e.getCurrentItem();
            if (i != null && i.getType() == Material.TEST_BLOCK) {
                manager.openSettings(player, inv, slot - 27);
            }
            return;
        }

        if (slot >= 45 && slot <= 53) { // Bottom control row
            if (slot == 47) { // Profession
                Villager.Profession[] list = Villager.Profession.values();
                int idx = indexOf(list, villager.getProfession());
                int next = e.isRightClick() ? (idx - 1 + list.length) % list.length : (idx + 1) % list.length;
                villager.setProfession(list[next]);
                inv.setItem(47, manager.professionItem(list[next]));
                return;
            }

            if (slot == 49) { // Villager XP
                int xp = manager.getVillagerXPFromItem(inv.getItem(49));
                int delta = e.isShiftClick() ? 5 : 1;
                if (e.isLeftClick()) xp += delta;
                if (e.isRightClick()) xp -= delta;
                xp = Math.max(0, Math.min(250, xp));
                inv.setItem(49, manager.villagerXPItem(xp));
                return;
            }

            if (slot == 51) { // Type
                Villager.Type[] list = Villager.Type.values();
                int idx = indexOf(list, villager.getVillagerType());
                int next = e.isRightClick() ? (idx - 1 + list.length) % list.length : (idx + 1) % list.length;
                villager.setVillagerType(list[next]);
                inv.setItem(51, manager.typeItem(list[next]));
            }
        }

        // Update trade settings buttons after any click affecting inventory
        Bukkit.getScheduler().runTask(plugin, () -> manager.updateSettingsButtons(inv));
    }

    private void handleWanderingTraderEditorClick(InventoryClickEvent e, Player player, Inventory inv, int slot) {
        // Allow all moves in WT trade GUI
        if (slot >= 27 && slot <= 35) { // Trade Settings buttons
            ItemStack i = e.getCurrentItem();
            if (i != null && i.getType() == Material.TEST_BLOCK) {
                manager.openWanderingTraderSettings(player, inv, slot - 27);
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> manager.updateSettingsButtons(inv));
    }

    private Villager getVillager(Inventory editor) {
        UUID id = manager.openEditors.get(editor);
        if (id == null) return null;
        return Bukkit.getEntity(id) instanceof Villager v ? v : null;
    }

    private <T> int indexOf(T[] arr, T value) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == value) return i;
        return 0;
    }
}