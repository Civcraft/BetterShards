package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializeBook implements Serializable{
	private String author;
	private String title;
	private String display;
	private List<String> pages;
	private List<String> lore;
	/**
	 * 
	 */
	private static final long serialVersionUID = 6931550901915595804L;

	public SerializeBook(BookMeta book){
		author = book.getAuthor();
		title = book.getTitle();
		display = book.getDisplayName();
		pages = book.getPages();
		lore = book.getLore();
	}
	public ItemStack unPack(ItemStack stack){
		BookMeta book = (BookMeta) stack.getItemMeta();
		book.setAuthor(author);
		book.setTitle(title);
		book.setDisplayName(display);
		book.setPages(pages);
		book.setLore(lore);
		stack.setItemMeta(book);
		return stack;
	}
}

