package de.epiceric.shopchest.shop;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Config;
import de.epiceric.shopchest.config.HologramFormat;
import de.epiceric.shopchest.config.Placeholder;
import de.epiceric.shopchest.exceptions.ChestNotFoundException;
import de.epiceric.shopchest.exceptions.NotEnoughSpaceException;
import de.epiceric.shopchest.language.LanguageUtils;
import de.epiceric.shopchest.nms.Hologram;
import de.epiceric.shopchest.utils.ItemUtils;
import de.epiceric.shopchest.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Shop {

    public enum ShopType {
        NORMAL,
        ADMIN,
    }

    private final ShopChest plugin;
    private final OfflinePlayer vendor;
    private final ItemStack product;
    private final Location location;
    private final double buyPrice;
    private final double sellPrice;
    private final ShopType shopType;

    private boolean created;
    private int id;
    private Hologram hologram;
    private ShopItem item;

    public Shop(int id, ShopChest plugin, OfflinePlayer vendor, ItemStack product, Location location, double buyPrice, double sellPrice, ShopType shopType) {
        this.id = id;
        this.plugin = plugin;
        this.vendor = vendor;
        this.product = product;
        this.location = location;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.shopType = shopType;
    }

    public Shop(ShopChest plugin, OfflinePlayer vendor, ItemStack product, Location location, double buyPrice, double sellPrice, ShopType shopType) {
        this(-1, plugin, vendor, product, location, buyPrice, sellPrice, shopType);
    }

    /**
     * Test if this shop is equals to another
     *
     * @param o Other object to test against
     * @return true if we are sure they are the same, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Shop shop = (Shop) o;

        // id = -1 means temp shop
        return id != -1 && id == shop.id;
    }

    @Override
    public int hashCode() {
        return id != -1 ? id : super.hashCode();
    }

    /**
     * Create the shop
     *
     * @param showConsoleMessages to log exceptions to console
     * @return Whether is was created or not
     */
    public boolean create(boolean showConsoleMessages) {
        if (created) return false;

        plugin.debug("Creating shop (#" + id + ")");

        Block b = location.getBlock();
        if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
            ChestNotFoundException ex = new ChestNotFoundException(String.format("No Chest found in world '%s' at location: %d; %d; %d", b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
            plugin.getShopUtils().removeShop(this, Config.removeShopOnError);
            if (showConsoleMessages) plugin.getLogger().severe(ex.getMessage());
            plugin.debug("Failed to create shop (#" + id + ")");
            plugin.debug(ex);
            return false;
        } else if ((b.getRelative(BlockFace.UP).getType() != Material.AIR) && Config.showShopItems) {
            NotEnoughSpaceException ex = new NotEnoughSpaceException(String.format("No space above chest in world '%s' at location: %d; %d; %d", b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
            plugin.getShopUtils().removeShop(this, Config.removeShopOnError);
            if (showConsoleMessages) plugin.getLogger().severe(ex.getMessage());
            plugin.debug("Failed to create shop (#" + id + ")");
            plugin.debug(ex);
            return false;
        }

        if (hologram == null || !hologram.exists()) createHologram();
        if (item == null) createItem();

        created = true;
        return true;
    }

    /**
     * Removes the hologram of the shop
     */
    public void removeHologram() {
        if (hologram != null && hologram.exists()) {
            plugin.debug("Removing hologram (#" + id + ")");
            hologram.remove();
        }
    }

    /**
     * Removes the floating item of the shop
     */
    public void removeItem() {
        if (item != null) {
            plugin.debug("Removing shop item (#" + id + ")");
            item.remove();
        }
    }

    /**
     * <p>Creates the floating item of the shop</p>
     * <b>Call this after {@link #createHologram()}, because it depends on the hologram's location</b>
     */
    private void createItem() {
        if (Config.showShopItems) {
            plugin.debug("Creating item (#" + id + ")");

            Location itemLocation;
            ItemStack itemStack;

            itemLocation = new Location(location.getWorld(), hologram.getLocation().getX(), location.getY() + 0.9, hologram.getLocation().getZ());
            itemStack = product.clone();
            itemStack.setAmount(1);

            item = new ShopItem(plugin, itemStack, itemLocation);
        }
    }

    /**
     * Creates the hologram of the shop
     */
    private void createHologram() {
        plugin.debug("Creating hologram (#" + id + ")");

        InventoryHolder ih = getInventoryHolder();

        if (ih == null) return;

        Chest[] chests = new Chest[2];
        boolean doubleChest;
        BlockFace face;

        if (ih instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) ih;
            Chest r = (Chest) dc.getRightSide();
            Chest l = (Chest) dc.getLeftSide();

            chests[0] = r;
            chests[1] = l;
            doubleChest = true;
        } else {
            chests[0] = (Chest) ih;
            doubleChest = false;
        }

        face = ((Directional) chests[0].getBlockData()).getFacing();

        String[] holoText = getHologramText();
        Location holoLocation = getHologramLocation(doubleChest, chests, face);

        hologram = new Hologram(plugin, holoText, holoLocation);
    }

    /**
     * Keep hologram text up to date
     */
    public void updateHologramText() {
        String[] lines = getHologramText();
        String[] currentLines = hologram.getLines();

        int max = Math.max(lines.length, currentLines.length);

        for (int i = 0; i < max; i++) {
            if (i < lines.length) {
                hologram.setLine(i, lines[i]);
            } else {
                hologram.removeLine(i);
            }
        }
    }

    private String[] getHologramText() {
        List<String> lines = new ArrayList<>();

        Map<HologramFormat.Requirement, Object> requirements = new EnumMap<>(HologramFormat.Requirement.class);
        requirements.put(HologramFormat.Requirement.VENDOR, getVendor().getName());
        requirements.put(HologramFormat.Requirement.AMOUNT, getProduct().getAmount());
        requirements.put(HologramFormat.Requirement.ITEM_TYPE, getProduct().getType() + (getProduct().getDurability() > 0 ? ":" + getProduct().getDurability() : ""));
        requirements.put(HologramFormat.Requirement.ITEM_NAME, getProduct().hasItemMeta() ? getProduct().getItemMeta().getDisplayName() : null);
        requirements.put(HologramFormat.Requirement.HAS_ENCHANTMENT, !LanguageUtils.getEnchantmentString(ItemUtils.getEnchantments(getProduct())).isEmpty());
        requirements.put(HologramFormat.Requirement.BUY_PRICE, getBuyPrice());
        requirements.put(HologramFormat.Requirement.SELL_PRICE, getSellPrice());
        requirements.put(HologramFormat.Requirement.HAS_POTION_EFFECT, ItemUtils.getPotionEffect(getProduct()) != null);
        requirements.put(HologramFormat.Requirement.IS_MUSIC_DISC, getProduct().getType().isRecord());
        requirements.put(HologramFormat.Requirement.IS_POTION_EXTENDED, ItemUtils.isExtendedPotion(getProduct()));
        requirements.put(HologramFormat.Requirement.IS_WRITTEN_BOOK, getProduct().getType() == Material.WRITTEN_BOOK);
        requirements.put(HologramFormat.Requirement.ADMIN_SHOP, getShopType() == ShopType.ADMIN);
        requirements.put(HologramFormat.Requirement.NORMAL_SHOP, getShopType() == ShopType.NORMAL);
        requirements.put(HologramFormat.Requirement.IN_STOCK, Utils.getAmount(getInventoryHolder().getInventory(), getProduct()));
        requirements.put(HologramFormat.Requirement.MAX_STACK, getProduct().getMaxStackSize());
        requirements.put(HologramFormat.Requirement.CHEST_SPACE, Utils.getFreeSpaceForItem(getInventoryHolder().getInventory(), getProduct()));
        requirements.put(HologramFormat.Requirement.DURABILITY, getProduct().getDurability());

        Map<Placeholder, Object> placeholders = new EnumMap<>(Placeholder.class);
        placeholders.put(Placeholder.VENDOR, getVendor().getName());
        placeholders.put(Placeholder.AMOUNT, getProduct().getAmount());
        placeholders.put(Placeholder.ITEM_NAME, LanguageUtils.getItemName(getProduct()));
        placeholders.put(Placeholder.ENCHANTMENT, LanguageUtils.getEnchantmentString(ItemUtils.getEnchantments(getProduct())));
        placeholders.put(Placeholder.BUY_PRICE, getBuyPrice());
        placeholders.put(Placeholder.SELL_PRICE, getSellPrice());
        placeholders.put(Placeholder.POTION_EFFECT, LanguageUtils.getPotionEffectName(getProduct()));
        placeholders.put(Placeholder.MUSIC_TITLE, LanguageUtils.getMusicDiscName(getProduct().getType()));
        placeholders.put(Placeholder.GENERATION, LanguageUtils.getBookGenerationName(getProduct()));
        placeholders.put(Placeholder.STOCK, Utils.getAmount(getInventoryHolder().getInventory(), getProduct()));
        placeholders.put(Placeholder.MAX_STACK, getProduct().getMaxStackSize());
        placeholders.put(Placeholder.CHEST_SPACE, Utils.getFreeSpaceForItem(getInventoryHolder().getInventory(), getProduct()));
        placeholders.put(Placeholder.DURABILITY, getProduct().getDurability());

        int lineCount = plugin.getHologramFormat().getLineCount();

        for (int i = 0; i < lineCount; i++) {
            String format = plugin.getHologramFormat().getFormat(i, requirements, placeholders);
            for (Placeholder placeholder : placeholders.keySet()) {
                String replace;

                switch (placeholder) {
                    case BUY_PRICE:
                        replace = plugin.getEconomy().format(getBuyPrice());
                        break;
                    case SELL_PRICE:
                        replace = plugin.getEconomy().format(getSellPrice());
                        break;
                    default:
                        replace = String.valueOf(placeholders.get(placeholder));
                }

                format = format.replace(placeholder.toString(), replace);
            }

            if (!format.isEmpty()) {
                lines.add(format);
            }
        }

        return lines.toArray(new String[0]);
    }

    private Location getHologramLocation(boolean doubleChest, Chest[] chests, BlockFace face) {
        Block b = location.getBlock();
        Location holoLocation;

        World w = b.getWorld();
        int x = b.getX();
        int y  = b.getY();
        int z = b.getZ();

        double subtractY = 0.6;

        if (Config.hologramFixedBottom) subtractY = 0.85;

        if (doubleChest) {
            Chest c1 = face == BlockFace.NORTH || face == BlockFace.EAST ? chests[1] : chests[0];
            Chest c2 = face == BlockFace.NORTH || face == BlockFace.EAST ? chests[0] : chests[1];

            if (b.getLocation().equals(c1.getLocation())) {
                if (c1.getX() != c2.getX()) {
                    holoLocation = new Location(w, x, y - subtractY, z + 0.5);
                } else if (c1.getZ() != c2.getZ()) {
                    holoLocation = new Location(w, x + 0.5, y - subtractY, z);
                } else {
                    holoLocation = new Location(w, x + 0.5, y - subtractY, z + 0.5);
                }
            } else {
                if (c1.getX() != c2.getX()) {
                    holoLocation = new Location(w, x + 1, y - subtractY, z + 0.5);
                } else if (c1.getZ() != c2.getZ()) {
                    holoLocation = new Location(w, x + 0.5, y - subtractY, z + 1);
                } else {
                    holoLocation = new Location(w, x + 0.5, y - subtractY, z + 0.5);
                }
            }
        } else {
            holoLocation = new Location(w, x + 0.5, y - subtractY, z + 0.5);
        }

        holoLocation.add(0, Config.hologramLift, 0);

        return holoLocation;
    }

    /**
     * @return Whether an ID has been assigned to the shop
     */
    public boolean hasId() {
        return id != -1;
    }

    /**
     * Assign an ID to the shop. <br/>
     * Only works for the first time!
     */
    public void setId(int id) {
        if (this.id == -1) {
            this.id = id;
        }
    }

    /**
     * @return Whether the shop has already been created
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * @return The ID of the shop
     */
    public int getID() {
        return id;
    }

    /**
     * @return Vendor of the shop; probably the creator of it
     */
    public OfflinePlayer getVendor() {
        return vendor;
    }

    /**
     * @return Product the shop sells (or buys)
     */
    public ItemStack getProduct() {
        return product;
    }

    /**
     * @return Location of (one of) the shop's chest
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return Buy price of the shop
     */
    public double getBuyPrice() {
        return buyPrice;
    }

    /**
     * @return Sell price of the shop
     */
    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * @return Type of the shop
     */
    public ShopType getShopType() {
        return shopType;
    }

    /**
     * @return Hologram of the shop
     */
    public Hologram getHologram() {
        return hologram;
    }

    /**
     * @return Floating {@link ShopItem} of the shop
     */
    public ShopItem getItem() {
        return item;
    }

    public boolean hasHologram() {
        return hologram != null;
    }

    public boolean hasItem() {
        return item != null;
    }

    /**
     * @return {@link InventoryHolder} of the shop or <b>null</b> if the shop has no chest.
     */
    public InventoryHolder getInventoryHolder() {
        Block b = getLocation().getBlock();

        if (b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST) {
            Chest chest = (Chest) b.getState();
            return chest.getInventory().getHolder();
        }

        return null;
    }

}
