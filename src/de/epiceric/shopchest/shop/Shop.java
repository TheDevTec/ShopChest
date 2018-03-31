package de.epiceric.shopchest.shop;

import de.epiceric.shopchest.ShopChest;
import de.epiceric.shopchest.config.Regex;
import de.epiceric.shopchest.exceptions.ChestNotFoundException;
import de.epiceric.shopchest.exceptions.NotEnoughSpaceException;
import de.epiceric.shopchest.language.LanguageUtils;
import de.epiceric.shopchest.language.LocalizedMessage;
import de.epiceric.shopchest.nms.Hologram;
import de.epiceric.shopchest.utils.Utils;
import de.epiceric.shopchest.utils.locationUnloaded;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class Shop {

    private boolean created;
    private int id;
    private ShopChest plugin;
    private OfflinePlayer vendor;
    private ItemStack product;
    private Location location;
    private Hologram hologram;
    private ShopItem item;
    private ShopItem noitem;
    private double buyPrice;
    private double sellPrice;
    private ShopType shopType;
    private long timeend;
    private locationUnloaded locationOffline;
    private boolean _canBuy = true;
    private boolean _canSell = true;
    private long lastedChecked = 0;
    public Shop(int id, ShopChest plugin, OfflinePlayer vendor, ItemStack product, Location location, double buyPrice, double sellPrice, ShopType shopType) {
        this.id = id;
        this.plugin = plugin;
        this.vendor = vendor;
        this.product = product;
        this.location = location;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.shopType = shopType;
        this.timeend = 0;
        if (location != null) {
            this.locationOffline = new locationUnloaded(this.location.getWorld().getName(), this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
        }
        else
        {
            this.locationOffline = new locationUnloaded("world", 0 , 0, 0);
        }
    }
    public void checkShopStatus()
    {
        if (this.getShopType() == ShopType.ADMIN)
        {
            this._canBuy = true;
            this._canSell = true;

            return;
        }
        if (this.isChunkLoaded() && System.currentTimeMillis() - lastedChecked > 300000) {
            lastedChecked = System.currentTimeMillis();
            if (this.getBuyPrice() > 0) {
                if (this.getBuyPrice() > 0) {
                    if (this.getInventoryHolder() != null) {
                        if (Utils.getAmount(this.getInventoryHolder().getInventory(), this.getProduct()) < this.getProduct().getAmount() && this.getShopType() == ShopType.NORMAL) {
                            this._canBuy = false;

                        } else {
                            this._canBuy = true;
                        }
                    }
                }
            }
            if (this.getSellPrice() > 0) {
                if (this.getInventoryHolder() != null) {
                    Inventory inventory = this.getInventoryHolder().getInventory();

                    int freeSpace = Utils.getFreeSpaceForItem(inventory, this.getProduct());
                    if (freeSpace == 0) {
                        this._canSell = false;
                    } else {
                        double amount = ShopChest.getInstance().getEconomy().getBalance(this.getVendor());
                        if (amount < this.getSellPrice()) {
                            this._canSell = false;
                        } else {
                            this._canSell = true;
                        }
                    }
                }
            }
        }
    }
    public Shop(ShopChest plugin, OfflinePlayer vendor, ItemStack product, Location location, double buyPrice, double sellPrice, ShopType shopType) {
        this(-1, plugin, vendor, product, location, buyPrice, sellPrice, shopType);
    }
    public boolean isChunkLoaded()
    {

        int chunkx = this.locationOffline.getX() >> 4;
        int chunkz = this.locationOffline.getZ() >> 4;
        return Bukkit.getWorld(this.locationOffline.getWorld()).isChunkLoaded(chunkx, chunkz);
    }
    public boolean create(boolean showConsoleMessages) {
        if (created) return false;

        plugin.debug("Creating shop (#" + id + ")");

        Block b = location.getBlock();
        if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST) {
            ChestNotFoundException ex = new ChestNotFoundException(String.format("No Chest found in world '%s' at location: %d; %d; %d", b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
            plugin.getShopUtils().removeShop(this, plugin.getShopChestConfig().remove_shop_on_error);
            if (showConsoleMessages) plugin.getLogger().severe(ex.getMessage());
            plugin.debug("Failed to create shop (#" + id + ")");
            plugin.debug(ex);
            return false;
        } else if ((b.getRelative(BlockFace.UP).getType() != Material.AIR) && !((b.getRelative(BlockFace.UP).getType() == Material.SIGN) || (b.getRelative(BlockFace.UP).getType() == Material.SIGN_POST) || (b.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)) && plugin.getShopChestConfig().show_shop_items) {
            NotEnoughSpaceException ex = new NotEnoughSpaceException(String.format("No space above chest in world '%s' at location: %d; %d; %d", b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
            plugin.getShopUtils().removeShop(this, plugin.getShopChestConfig().remove_shop_on_error);
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
    public void removeHologram(boolean useCurrentThread) {
        if (hologram != null && hologram.exists()) {
            plugin.debug("Removing hologram (#" + id + ")");

            for (Player p : Bukkit.getOnlinePlayers()) {
                hologram.hidePlayer(p, useCurrentThread);
            }

            hologram.remove();
        }
    }

    public long getTimeend() {
        return timeend;
    }

    public void setTimeend(long timeend) {
        this.timeend = timeend;
    }

    /**
     * Removes the hologram of the shop
     */
    public void removeHologram() {
        removeHologram(false);
    }

    /**
     * Removes the floating item of the shop
     */
    public void removeItem() {
        if (item != null) {
            plugin.debug("Removing shop item (#" + id + ")");
            item.remove();
        }
        if (noitem != null) {
            noitem.remove();
        }
    }

    /**
     * <p>Creates the floating item of the shop</p>
     * <b>Call this after {@link #createHologram()}, because it depends on the hologram's location</b>
     */
    private void createItem() {
        if (plugin.getShopChestConfig().show_shop_items) {
            plugin.debug("Creating item (#" + id + ")");

            Location itemLocation;
            ItemStack itemStack;

            itemLocation = new Location(location.getWorld(), hologram.getLocation().getX(), location.getY() + 1, hologram.getLocation().getZ());
            itemStack = product.clone();
            itemStack.setAmount(1);

            this.item = new ShopItem(plugin, itemStack, itemLocation);
            this.noitem = new ShopItem(plugin, new ItemStack(Material.BARRIER), itemLocation);
            for (Player p : Bukkit.getOnlinePlayers()) {
                //item.setVisible(p, true);
                noitem.setVisible(p, true);
            }
            this.checkShopStatus();
        }
    }

    /**
     * Creates the hologram of the shop
     */
    public void createHologram() {
        plugin.debug("Creating hologram (#" + id + ")");

        boolean doubleChest;

        Chest[] chests = new Chest[2];
        Block b = location.getBlock();
        InventoryHolder ih = getInventoryHolder();

        if (ih == null) return;

        if (ih instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) ih;

            Chest r = (Chest) dc.getRightSide();
            Chest l = (Chest) dc.getLeftSide();

            chests[0] = r;
            chests[1] = l;

            doubleChest = true;

        } else {
            doubleChest = false;
            chests[0] = (Chest) ih;
        }

        Location holoLocation;
        String[] holoText = new String[plugin.getShopChestConfig().two_line_prices ? 3 : 2];

        if (doubleChest) {

            Chest r = chests[0];
            Chest l = chests[1];

            if (b.getLocation().equals(r.getLocation())) {

                if (r.getX() != l.getX())
                    holoLocation = new Location(b.getWorld(), b.getX(), b.getY() - 0.6, b.getZ() + 0.5);
                else if (r.getZ() != l.getZ())
                    holoLocation = new Location(b.getWorld(), b.getX() + 0.5, b.getY() - 0.6, b.getZ());
                else holoLocation = new Location(b.getWorld(), b.getX() + 0.5, b.getY() - 0.6, b.getZ() + 0.5);

            } else {

                if (r.getX() != l.getX())
                    holoLocation = new Location(b.getWorld(), b.getX() + 1, b.getY() - 0.6, b.getZ() + 0.5);
                else if (r.getZ() != l.getZ())
                    holoLocation = new Location(b.getWorld(), b.getX() + 0.5, b.getY() - 0.6, b.getZ() + 1);
                else holoLocation = new Location(b.getWorld(), b.getX() + 0.5, b.getY() - 0.6, b.getZ() + 0.5);

            }

        } else holoLocation = new Location(b.getWorld(), b.getX() + 0.5, b.getY() - 0.6, b.getZ() + 0.5);

        holoText[0] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_FORMAT, new LocalizedMessage.ReplacedRegex(Regex.AMOUNT, String.valueOf(product.getAmount())),
                new LocalizedMessage.ReplacedRegex(Regex.ITEM_NAME, LanguageUtils.getItemName(product)));

        if ((buyPrice <= 0) && (sellPrice > 0)) {
            holoText[1] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_SELL, new LocalizedMessage.ReplacedRegex(Regex.SELL_PRICE, String.valueOf(sellPrice)));
        } else if ((buyPrice > 0) && (sellPrice <= 0)) {
            holoText[1] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_BUY, new LocalizedMessage.ReplacedRegex(Regex.BUY_PRICE, String.valueOf(buyPrice)));
        } else {
            if (holoText.length == 2) {
                holoText[1] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_BUY_SELL, new LocalizedMessage.ReplacedRegex(Regex.BUY_PRICE, String.valueOf(buyPrice)),
                        new LocalizedMessage.ReplacedRegex(Regex.SELL_PRICE, String.valueOf(sellPrice)));
            } else {
                holoText[1] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_BUY, new LocalizedMessage.ReplacedRegex(Regex.BUY_PRICE, String.valueOf(buyPrice)));
                holoText[2] = LanguageUtils.getMessage(LocalizedMessage.Message.HOLOGRAM_SELL, new LocalizedMessage.ReplacedRegex(Regex.SELL_PRICE, String.valueOf(sellPrice)));
            }
        }

        holoLocation.add(0, plugin.getShopChestConfig().hologram_lift, 0);

        if (plugin.getShopChestConfig().two_line_prices) {
            if (holoText.length == 3 && holoText[2] != null) {
                holoLocation.add(0, plugin.getShopChestConfig().two_line_hologram_lift, 0);
            } else {
                holoLocation.add(0, plugin.getShopChestConfig().one_line_hologram_lift, 0);
            }
        }

        hologram = new Hologram(plugin, holoText, holoLocation);

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
    public locationUnloaded getLocationUnloaded() {
        return this.locationOffline;
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
        /*if (Utils.getAmount(getInventoryHolder().getInventory(), product) < 1)
        {
            return noitem;
        }*/
        return item;
    }

    /**
     * @return Floating {@link ShopItem} of the shop
     */
    public ShopItem getNoItem() {
        /*if (Utils.getAmount(getInventoryHolder().getInventory(), product) < 1)
        {
            return noitem;
        }*/
        return noitem;
    }

    public boolean canBuy() {
        return _canBuy;
    }

    public boolean canSell() {
        return _canSell;
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

    public enum ShopType {
        NORMAL,
        ADMIN
    }

}
