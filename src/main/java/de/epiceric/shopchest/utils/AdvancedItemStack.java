package de.epiceric.shopchest.utils;

import org.bukkit.inventory.ItemStack;

public class AdvancedItemStack {
    
    ItemStack itemStack;
    int amount;
    
    public AdvancedItemStack(ItemStack itemStack, int amount) {
        super();
        if (itemStack != null) {
            this.itemStack = itemStack.clone();
            this.itemStack.setAmount(1);
        }
        this.amount = amount;
    }
    
    public AdvancedItemStack(ItemStack itemStack) {
        super();
        if (itemStack != null) {
            this.itemStack = itemStack.clone();
            this.itemStack.setAmount(1);
        }
        this.amount = 1;
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
