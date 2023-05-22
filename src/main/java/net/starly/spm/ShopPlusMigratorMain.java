package net.starly.spm;

import net.starly.core.bstats.Metrics;
import net.starly.core.data.Config;
import net.starly.core.jb.util.Pair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ShopPlusMigratorMain extends JavaPlugin implements CommandExecutor {

    private static ShopPlusMigratorMain instance;
    public static ShopPlusMigratorMain getInstance() {
        return instance;
    }


    @Override
    public void onEnable() {
        /* DEPENDENCY
         ──────────────────────────────────────────────────────────────────────────────────────────────────────────────── */
        if (!isPluginEnabled("ST-Core")) {
            getServer().getLogger().warning("[" + getName() + "] ST-Core 플러그인이 적용되지 않았습니다! 플러그인을 비활성화합니다.");
            getServer().getLogger().warning("[" + getName() + "] 다운로드 링크 : §fhttp://starly.kr/");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        /* SETUP
         ──────────────────────────────────────────────────────────────────────────────────────────────────────────────── */
        instance = this;

        /* COMMAND
         ──────────────────────────────────────────────────────────────────────────────────────────────────────────────── */
        getServer().getPluginCommand("process").setExecutor(this);
    }

    private boolean isPluginEnabled(String name) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        new Config("", this).getFileNames().forEach(shopName -> {
            Config config = new Config(shopName, this);

            Map<Integer, ItemStack> decoded = decode((byte[]) config.getObject("shop.items"), HashMap.class);
            Map<Pair<Integer, Integer>, ItemStack> migrated = new HashMap<>();
            decoded.forEach((slot, itemStack) -> migrated.put(new Pair<>(1, slot), itemStack));
            config.setObject("shop.items", encode(migrated));

            ConfigurationSection stocksSection = config.getConfigurationSection("shop.stocks");
            config.createSection("shop.stocks");
            config.setObject("shop.stocks.1", stocksSection);
            ConfigurationSection pricesSection = config.getConfigurationSection("shop.prices");
            config.createSection("shop.prices");
            config.setObject("shop.prices.1", pricesSection);




            sender.sendMessage("DONE: " + shopName);
        });
        return true;
    }

    public static byte[] encode(Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); BukkitObjectOutputStream boos = new BukkitObjectOutputStream(bos)) {
            boos.writeObject(obj);
            return bos.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static <T> T decode(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); BukkitObjectInputStream bois = new BukkitObjectInputStream(bis)) {
            return clazz.cast(bois.readObject());
        } catch (NullPointerException ex) {
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
