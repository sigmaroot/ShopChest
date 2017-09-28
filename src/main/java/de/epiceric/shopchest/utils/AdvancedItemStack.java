package de.epiceric.shopchest.utils;

import org.bukkit.inventory.ItemStack;

public class AdvancedItemStack {
	
	ItemStack itemStack;
	int amount;
	
	public AdvancedItemStack(ItemStack itemStack, int amount) {
		super();
		this.itemStack = itemStack;
		if (this.itemStack != null) {
			this.itemStack.setAmount(1);
		}
		this.amount = amount;
	}
	
	public ItemStack getItemStack() {
		return itemStack;
	}
	
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}
	
	public int getAmount() {
		return amount;
	}
	
	public void setAmount(int amount) {
		this.amount = amount;
	}	

}
