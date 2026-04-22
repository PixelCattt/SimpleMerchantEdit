package com.pixelcatt.simplemerchantedit;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;


public class MerchantManager {

    public final SimpleMerchantEdit plugin;

    public boolean allowEditingVillagers = true;
    public boolean allowEditingWanderingTraders = true;

    public final Map<Inventory, UUID> openEditors = new HashMap<>();
    public final Map<Inventory, Map<Integer, SettingsContext>> columnSettings = new HashMap<>();
    public final Map<Inventory, SettingsGUIContext> openSettings = new HashMap<>();


    public MerchantManager(SimpleMerchantEdit plugin) {
        this.plugin = plugin;
    }

    /* ================= CONTEXT ================= */

    public static class SettingsContext {
        public int maxUses;
        public int uses;
        public int villagerXP;
        public boolean rewardXP;

        public SettingsContext(int maxUses, int uses, int villagerXP, boolean rewardXP) {
            this.maxUses = maxUses;
            this.uses = uses;
            this.villagerXP = villagerXP;
            this.rewardXP = rewardXP;
        }
    }

    public static class SettingsGUIContext {
        public final Inventory editor;
        public final int column;

        public SettingsGUIContext(Inventory editor, int column) {
            this.editor = editor;
            this.column = column;
        }
    }

    /* ================= VILLAGER EDITOR ================= */

    public void openEditor(Player player, Villager villager) {
        if (villager.getProfession() == Villager.Profession.NONE) return;

        Inventory inv = Bukkit.createInventory(null, 54, "Edit Villager");

        Map<Integer, SettingsContext> settings =
                columnSettings.computeIfAbsent(inv, k -> new HashMap<>());

        List<MerchantRecipe> recipes = villager.getRecipes();

        for (int col = 0; col < recipes.size() && col < 9; col++) {
            MerchantRecipe r = recipes.get(col);

            if (!r.getIngredients().isEmpty())
                inv.setItem(col, r.getIngredients().get(0).clone());
            if (r.getIngredients().size() > 1)
                inv.setItem(9 + col, r.getIngredients().get(1).clone());

            inv.setItem(18 + col, r.getResult().clone());

            settings.put(col, new SettingsContext(
                    r.getMaxUses(),
                    r.getUses(),
                    r.getVillagerExperience(),
                    r.hasExperienceReward()
            ));
        }

        inv.setItem(47, professionItem(villager.getProfession()));
        inv.setItem(49, villagerXPItem(villager.getVillagerExperience()));
        inv.setItem(51, typeItem(villager.getVillagerType()));

        updateSettingsButtons(inv);

        openEditors.put(inv, villager.getUniqueId());
        player.openInventory(inv);
    }

    public void saveEditor(Inventory editor) {
        UUID id = openEditors.get(editor);
        if (id == null) return;
        if (!(Bukkit.getEntity(id) instanceof Villager villager)) return;

        Villager.Profession profession = getProfessionFromItem(editor.getItem(47));
        villager.setProfession(profession);

        if (profession == Villager.Profession.NONE) {
            villager.setRecipes(Collections.emptyList());
            return;
        }

        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<Integer, SettingsContext> map = columnSettings.getOrDefault(editor, Map.of());

        for (int col = 0; col < 9; col++) {
            ItemStack buy1 = editor.getItem(col);
            ItemStack buy2 = editor.getItem(9 + col);
            ItemStack sell = editor.getItem(18 + col);

            if (!isTradeValid(buy1, sell)) continue;

            SettingsContext s = map.getOrDefault(col, new SettingsContext(0, 0, 0, false));

            MerchantRecipe r = new MerchantRecipe(sell.clone(), s.maxUses);
            r.addIngredient(buy1.clone());
            if (buy2 != null && buy2.getType() != Material.AIR)
                r.addIngredient(buy2.clone());

            r.setUses(s.uses);
            r.setVillagerExperience(s.villagerXP);
            r.setExperienceReward(s.rewardXP);

            recipes.add(r);
        }

        villager.setRecipes(recipes);

        // Update XP & level
        int totalXP = getVillagerXPFromItem(editor.getItem(49));
        villager.setVillagerExperience(totalXP);

        villager.setVillagerType(getTypeFromItem(editor.getItem(51)));
    }

    /* ================= WANDERING TRADER EDITOR ================= */

    public void openWanderingTraderEditor(Player player, WanderingTrader trader) {
        Inventory inv = Bukkit.createInventory(null, 36, "Edit Wandering Trader"); // 9x4

        Map<Integer, SettingsContext> settings =
                columnSettings.computeIfAbsent(inv, k -> new HashMap<>());

        List<MerchantRecipe> recipes = trader.getRecipes();

        for (int col = 0; col < recipes.size() && col < 9; col++) {
            MerchantRecipe r = recipes.get(col);

            if (!r.getIngredients().isEmpty())
                inv.setItem(col, r.getIngredients().get(0).clone());
            if (r.getIngredients().size() > 1)
                inv.setItem(9 + col, r.getIngredients().get(1).clone());

            inv.setItem(18 + col, r.getResult().clone());

            settings.put(col, new SettingsContext(
                    r.getMaxUses(),
                    r.getUses(),
                    0, // no XP for wandering trader
                    r.hasExperienceReward()
            ));
        }

        updateSettingsButtons(inv);

        openEditors.put(inv, trader.getUniqueId());
        player.openInventory(inv);
    }

    public void saveWanderingTraderEditor(Inventory editor) {
        UUID id = openEditors.get(editor);
        if (id == null) return;
        if (!(Bukkit.getEntity(id) instanceof WanderingTrader trader)) return;

        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<Integer, SettingsContext> map = columnSettings.getOrDefault(editor, Map.of());

        for (int col = 0; col < 9; col++) {
            ItemStack buy1 = editor.getItem(col);
            ItemStack buy2 = editor.getItem(9 + col);
            ItemStack sell = editor.getItem(18 + col);

            if (!isTradeValid(buy1, sell)) continue;

            SettingsContext s = map.getOrDefault(col, new SettingsContext(0, 0, 0, false));

            MerchantRecipe r = new MerchantRecipe(sell.clone(), s.maxUses);
            r.addIngredient(buy1.clone());
            if (buy2 != null && buy2.getType() != Material.AIR)
                r.addIngredient(buy2.clone());

            r.setUses(s.uses);
            r.setExperienceReward(s.rewardXP);

            recipes.add(r);
        }

        trader.setRecipes(recipes);
    }

    /* ================= SETTINGS ================= */

    public void openSettings(Player player, Inventory editor, int col) {
        Inventory inv = Bukkit.createInventory(null, 54, "Trade Settings");

        SettingsContext ctx = columnSettings
                .computeIfAbsent(editor, k -> new HashMap<>())
                .computeIfAbsent(col, k -> new SettingsContext(0, 0, 0, false));

        inv.setItem(4, numberItem("Maximum Uses", ctx.maxUses, Material.EMERALD));
        inv.setItem(13, numberItem("Current Uses", ctx.uses, Material.DIAMOND));
        inv.setItem(22, numberItem("Villager XP Reward", ctx.villagerXP, Material.EXPERIENCE_BOTTLE));

        ItemStack reward = new ItemStack(ctx.rewardXP ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta rm = reward.getItemMeta();
        rm.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_GREEN + "Reward XP Orbs");
        reward.setItemMeta(rm);
        inv.setItem(31, reward);

        ItemStack back = new ItemStack(Material.SPRUCE_DOOR);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Back");
        back.setItemMeta(bm);
        inv.setItem(45, back);

        openSettings.put(inv, new SettingsGUIContext(editor, col));
        player.openInventory(inv);
    }

    public void openWanderingTraderSettings(Player player, Inventory editor, int col) {
        Inventory inv = Bukkit.createInventory(null, 9 * 4, "Trade Settings");

        SettingsContext ctx = columnSettings
                .computeIfAbsent(editor, k -> new HashMap<>())
                .computeIfAbsent(col, k -> new SettingsContext(0, 0, 0, false));

        inv.setItem(4, numberItem("Maximum Uses", ctx.maxUses, Material.EMERALD));
        inv.setItem(13, numberItem("Current Uses", ctx.uses, Material.DIAMOND));

        // Reward XP (just visual toggle)
        ItemStack reward = new ItemStack(ctx.rewardXP ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta rm = reward.getItemMeta();
        rm.setDisplayName(ChatColor.RESET + "" + ChatColor.DARK_GREEN + "Reward XP Orbs");
        reward.setItemMeta(rm);
        inv.setItem(22, reward);

        ItemStack back = new ItemStack(Material.SPRUCE_DOOR);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Back");
        back.setItemMeta(bm);
        inv.setItem(27, back);

        openSettings.put(inv, new SettingsGUIContext(editor, col));
        player.openInventory(inv);
    }

    /* ================= ITEMS ================= */

    public ItemStack professionItem(Villager.Profession profession) {
        String name = profession.name();
        Material mat;

        switch (name) {
            case "ARMORER": mat = Material.BLAST_FURNACE; break;
            case "BUTCHER": mat = Material.SMOKER; break;
            case "CARTOGRAPHER": mat = Material.CARTOGRAPHY_TABLE; break;
            case "CLERIC": mat = Material.BREWING_STAND; break;
            case "FARMER": mat = Material.COMPOSTER; break;
            case "FISHERMAN": mat = Material.BARREL; break;
            case "FLETCHER": mat = Material.FLETCHING_TABLE; break;
            case "LEATHERWORKER": mat = Material.CAULDRON; break;
            case "LIBRARIAN": mat = Material.LECTERN; break;
            case "MASON": mat = Material.STONECUTTER; break;
            case "SHEPHERD": mat = Material.LOOM; break;
            case "TOOLSMITH": mat = Material.SMITHING_TABLE; break;
            case "WEAPONSMITH": mat = Material.GRINDSTONE; break;
            case "NITWIT": mat = Material.MANGROVE_PROPAGULE; break;
            default: mat = Material.BARRIER;
        }

        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE + "Profession: " + titleCase(name));
        i.setItemMeta(m);
        return i;
    }

    public ItemStack typeItem(Villager.Type type) {
        String name = type.name();
        Material mat;

        switch (name) {
            case "PLAINS": mat = Material.GRASS_BLOCK; break;
            case "JUNGLE": mat = Material.VINE; break;
            case "TAIGA": mat = Material.PODZOL; break;
            case "DESERT": mat = Material.SAND; break;
            case "SAVANNA": mat = Material.ACACIA_LOG; break;
            case "SWAMP": mat = Material.LILY_PAD; break;
            case "SNOW": mat = Material.SNOWBALL; break;
            default: mat = Material.GRASS_BLOCK;
        }

        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE + "Type: " + titleCase(name));
        i.setItemMeta(m);
        return i;
    }

    public ItemStack villagerXPItem(int xp) {
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta m = bottle.getItemMeta();
        m.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Total Villager XP: " + xp);
        bottle.setItemMeta(m);
        return bottle;
    }

    public ItemStack numberItem(String name, int value, Material mat) {
        if (value <= 0) {
            ItemStack b = new ItemStack(Material.BARRIER);
            ItemMeta m = b.getItemMeta();
            m.setDisplayName(ChatColor.RESET + name);
            b.setItemMeta(m);
            return b;
        }
        if (value > 64) value = 64;
        ItemStack e = new ItemStack(mat, value);
        ItemMeta m = e.getItemMeta();
        m.setDisplayName(ChatColor.RESET + name);
        e.setItemMeta(m);
        return e;
    }

    public int getItemAmount(ItemStack i) {
        if (i == null || i.getType() == Material.BARRIER) return 0;
        return Math.min(i.getAmount(), 64);
    }

    /* ================= HELPERS ================= */

    private boolean isTradeValid(ItemStack buy, ItemStack sell) {
        return buy != null && sell != null &&
                buy.getType() != Material.AIR &&
                sell.getType() != Material.AIR;
    }

    public void updateSettingsButtons(Inventory inv) {
        for (int col = 0; col < 9; col++) {
            ItemStack buy1 = inv.getItem(col);       // Row 0
            ItemStack sell = inv.getItem(18 + col); // Row 2

            // Show button only if both row 0 and row 2 have valid items
            if (isTradeValid(buy1, sell)) {
                ItemStack b = new ItemStack(Material.TEST_BLOCK);
                ItemMeta m = b.getItemMeta();
                m.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Trade Settings");
                b.setItemMeta(m);
                inv.setItem(27 + col, b);
            } else {
                inv.setItem(27 + col, null);
            }
        }
    }

    public int getNumberFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (!name.contains(": ")) return 0;
        try {
            return Integer.parseInt(name.split(": ")[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getVillagerXPFromItem(ItemStack i) {
        return getNumberFromItem(i);
    }

    private Villager.Profession getProfessionFromItem(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return Villager.Profession.NONE;
        try {
            String name = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            name = name.replace("Profession: ", "").toUpperCase();
            return Villager.Profession.valueOf(name);
        } catch (Exception e) {
            return Villager.Profession.NONE;
        }
    }

    private Villager.Type getTypeFromItem(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return Villager.Type.PLAINS;
        try {
            String name = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            name = name.replace("Type: ", "").toUpperCase();
            return Villager.Type.valueOf(name);
        } catch (Exception e) {
            return Villager.Type.PLAINS;
        }
    }

    private String titleCase(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    public String numberItemName(int slot) {
        return switch (slot) {
            case 4 -> ChatColor.RESET + "" + ChatColor.AQUA + "Maximum Uses";
            case 13 -> ChatColor.RESET + "" + ChatColor.BLUE + "Current Uses";
            case 22 -> ChatColor.RESET + "" + ChatColor.GREEN + "Villager XP Reward";
            default -> "Value";
        };
    }

    public void saveSettings(Inventory inv) {
        SettingsGUIContext ctx = openSettings.get(inv);
        if (ctx == null) return;

        SettingsContext s = columnSettings
                .computeIfAbsent(ctx.editor, k -> new HashMap<>())
                .computeIfAbsent(ctx.column, k -> new SettingsContext(0, 0, 0, false));

        s.maxUses = getItemAmount(inv.getItem(4));
        s.uses = getItemAmount(inv.getItem(13));
        s.villagerXP = getItemAmount(inv.getItem(22));

        ItemStack rewardItem = inv.getItem(31);
        if (rewardItem == null) rewardItem = inv.getItem(22); // Wandering Trader slot
        s.rewardXP = rewardItem != null && rewardItem.getType() == Material.LIME_DYE;
    }
}