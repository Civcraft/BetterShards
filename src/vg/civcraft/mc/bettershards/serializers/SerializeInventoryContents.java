package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializeInventoryContents implements Serializable{

	private static final long serialVersionUID = -3905829616347096087L;
	private Map<Integer, StoredEnchantments> enchants = new HashMap<Integer, StoredEnchantments>(); // Item pos, enchant 
	private Map<Integer, SerializeLore> lore = new HashMap<Integer, SerializeLore>(); //item pos, lore
	private Map<Integer, SerializeBook> books = new HashMap<Integer, SerializeBook>(); //item pos, book
	
	private Map<Integer, Integer> amount = new HashMap<Integer, Integer>(); // place, amount
	private Map<Integer, String> material = new HashMap<Integer, String>(); // place, material
	private Map<Integer, Short> durability = new HashMap<Integer, Short>(); // place, durability
	private Map<Integer, SerializedBooksimplements> enchantedbook = new HashMap<Integer, SerializedBooksimplements>();
	
	private int size;
	
	public SerializeInventoryContents(ItemStack[] contents) {
		int x = 0;
		size = contents.length;
		for (ItemStack item: contents){
			if (item == null){
				x++;
				continue;
			}
			if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK_AND_QUILL){
				BookMeta book = (BookMeta) item.getItemMeta();
				books.put(x, new SerializeBook(book));
			}
			if (item.getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta esm = (EnchantmentStorageMeta) item.getItemMeta();
				enchantedbook.put(x, new SerializedBooksimplements(esm));
			}
			amount.put(x, item.getAmount());
			enchants.put(x, new StoredEnchantments(contents[x]));
			material.put(x, item.getType().toString());
			durability.put(x, item.getDurability());
			lore.put(x, new SerializeLore(item));
			x++;
		}
	}
	
	public ItemStack[] unPackInv(){
		ItemStack[] items = new ItemStack[size];
		for(int x = 0; x < size; x++){
			if (!material.containsKey(x)){
				continue;
			}
			items[x] = new ItemStack(Material.getMaterial(material.get(x)), amount.get(x));
			if (books.containsKey(x)){
				items[x] = books.get(x).unPack(items[x]);
			}
			if (enchantedbook.containsKey(x)){
				EnchantmentStorageMeta esm = (EnchantmentStorageMeta) items[x].getItemMeta();
				SerializedBooksimplements sbs = enchantedbook.get(x);
				Map<String, Integer> data = sbs.getData();
				for (String z: data.keySet()){
					esm.addStoredEnchant(Enchantment.getByName(z), data.get(z), true);
				}
				items[x].setItemMeta(esm);
			}
			items[x].setDurability(durability.get(x));
			Map<String, Integer> en = enchants.get(x).getEnchantments();
			items[x] = getEnchant(items[x], en);
			items[x] = lore.get(x).getLore(items[x]);
		}
		return items;
	}
	public ItemStack getEnchant(ItemStack item, Map<String, Integer> ench){
		for (String id: ench.keySet()){
			item.addEnchantment(Enchantment.getByName(id), ench.get(id));
		}
		return item;
	}
}

