package com.leonardobishop.quests.bukkit.hook.itemgetter;

import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemGetterLatest implements ItemGetter {

    private Field profileField;

    /*
     supporting:
      - name
      - material
      - lore
      - enchantments (NamespacedKey)
      - itemflags
      - unbreakable
      - attribute modifier
      - custom model data

      requires at least API version 1.14
      */
    @Override
    public ItemStack getItem(String path, ConfigurationSection config, ItemGetter.Filter... excludes) {
        if (path != null && !path.equals("")) {
            path = path + ".";
        }
        List<Filter> filters = Arrays.asList(excludes);

        String cName = config.getString(path + "name");
        String cType = config.getString(path + "item", config.getString(path + "type", path + "item"));
        boolean hasCustomModelData = config.contains(path + "custommodeldata");
        int customModelData = config.getInt(path + "custommodeldata", 0);
        boolean unbreakable = config.getBoolean(path + "unbreakable", false);
        List<String> cLore = config.getStringList(path + "lore");
        List<String> cItemFlags = config.getStringList(path + "itemflags");
        boolean hasAttributeModifiers = config.contains(path + "attributemodifiers");
        List<Map<?, ?>> cAttributeModifiers = config.getMapList(path + "attributemodifiers");

        // material
        ItemStack is = getItemStack(cType);
        ItemMeta ism = is.getItemMeta();

        // skull
        if (is.getType() == Material.PLAYER_HEAD) {
            SkullMeta sm = (SkullMeta) ism;
            String cOwnerBase64 = config.getString(path + "owner-base64");
            String cOwnerUsername = config.getString(path + "owner-username");
            String cOwnerUuid = config.getString(path + "owner-uuid");
            if (cOwnerBase64 != null || cOwnerUsername != null || cOwnerUuid != null) {
                if (cOwnerUsername != null) {
                    sm.setOwner(cOwnerUsername);
                } else if (cOwnerUuid != null) {
                    try {
                        sm.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(cOwnerUuid)));
                    } catch (IllegalArgumentException ignored) { }
                } else {
                    GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                    profile.getProperties().put("textures", new Property("textures", cOwnerBase64));
                    if (profileField == null) {
                        try {
                            profileField = sm.getClass().getDeclaredField("profile");
                            profileField.setAccessible(true);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        profileField.set(sm, profile);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // name
        if (!filters.contains(Filter.DISPLAY_NAME)) {
            if (cName != null) {
                ism.setDisplayName(Chat.legacyColor(cName));
            }
        }

        // lore
        if (!filters.contains(Filter.LORE)) {
            ism.setLore(Chat.legacyColor(cLore));
        }

        // attribute modifiers
        if (!filters.contains(Filter.ATTRIBUTE_MODIFIER)) {
            if (hasAttributeModifiers) {
                for (Map<?, ?> attr : cAttributeModifiers) {
                    String cAttribute = (String) attr.get("attribute");
                    Attribute attribute = null;
                    for (Attribute enumattr : Attribute.values()) {
                        if (enumattr.toString().equals(cAttribute)) {
                            attribute = enumattr;
                            break;
                        }
                    }

                    if (attribute == null) continue;

                    Map<?, ?> configurationSection = (Map<?, ?>) attr.get("modifier");

                    String cUUID = (String) configurationSection.get("uuid");
                    String cModifierName = (String) configurationSection.get("name");
                    String cModifierOperation = (String) configurationSection.get("operation");
                    double cAmount;
                    try {
                        Object cAmountObj = configurationSection.get("amount");
                        if (cAmountObj instanceof Integer) {
                            cAmount = ((Integer) cAmountObj).doubleValue();
                        } else {
                            cAmount = (Double) cAmountObj;
                        }
                    } catch (Exception e) {
                        cAmount = 1;
                    }
                    String cEquipmentSlot = (String) configurationSection.get("equipmentslot");

                    UUID uuid = null;
                    if (cUUID != null) {
                        try {
                            uuid = UUID.fromString(cUUID);
                        } catch (Exception ignored) {
                            // ignored
                        }
                    }
                    EquipmentSlot equipmentSlot = null;
                    if (cEquipmentSlot != null) {
                        try {
                            equipmentSlot = EquipmentSlot.valueOf(cEquipmentSlot);
                        } catch (Exception ignored) {
                            // ignored
                        }
                    }
                    AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
                    try {
                        operation = AttributeModifier.Operation.valueOf(cModifierOperation);
                    } catch (Exception ignored) {
                        // ignored
                    }

                    AttributeModifier modifier;
                    if (uuid == null) {
                        modifier = new AttributeModifier(cModifierName, cAmount, operation);
                    } else if (equipmentSlot == null) {
                        modifier = new AttributeModifier(uuid, cModifierName, cAmount, operation);
                    } else {
                        modifier = new AttributeModifier(uuid, cModifierName, cAmount, operation, equipmentSlot);
                    }

                    ism.addAttributeModifier(attribute, modifier);
                }
            }
        }

        // item flags
        if (!filters.contains(Filter.ITEM_FLAGS)) {
            if (config.isSet(path + "itemflags")) {
                for (String flag : cItemFlags) {
                    for (ItemFlag iflag : ItemFlag.values()) {
                        if (iflag.toString().equals(flag)) {
                            ism.addItemFlags(iflag);
                            break;
                        }
                    }
                }
            }
        }

        // unbreakable
        if (!filters.contains(Filter.UNBREAKABLE)) {
            ism.setUnbreakable(unbreakable);
        }

        // enchantments
        if (!filters.contains(Filter.ENCHANTMENTS)) {
            if (config.isSet(path + "enchantments")) {
                for (String key : config.getStringList(path + "enchantments")) {
                    String[] split = key.split(":");
                    if (split.length < 2) {
                        continue;
                    }
                    String namespace = split[0];
                    String ench = split[1];
                    String levelName;
                    if (split.length >= 3) {
                        levelName = split[2];
                    } else {
                        levelName = "1";
                    }

                    // TODO i don't know how these namespaces work
//                    NamespacedKey namespacedKey;
//                    try {
//                        namespacedKey = new NamespacedKey(namespace, ench);
//                    } catch (Exception e) {
//                        plugin.getQuestsLogger().debug("Unrecognised namespace: " + namespace);
//                        e.printStackTrace();
//                        continue;
//                    }
                    Enchantment enchantment;
                    if ((enchantment = Enchantment.getByName(ench)) == null) {
                        continue;
                    }

                    int level;
                    try {
                        level = Integer.parseInt(levelName);
                    } catch (NumberFormatException e) {
                        level = 1;
                    }

                    ism.addEnchant(enchantment, level, true);
                }
            }
        }

        is.setItemMeta(ism);
        return is;
    }

    @Override
    public ItemStack getItemStack(String material) {
        Material type;
        try {
            type = Material.valueOf(material);
        } catch (Exception e) {
            type = Material.STONE;
        }
        return new ItemStack(type, 1);
    }

    @Override
    public boolean isValidMaterial(String material) {
        try {
            Material.valueOf(material);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
