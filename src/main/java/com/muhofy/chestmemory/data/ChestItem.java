package com.muhofy.chestmemory.data;

public class ChestItem {

    private int slot;
    private String itemId;
    private int count;
    private String displayName;

    public ChestItem() {}

    public ChestItem(int slot, String itemId, int count, String displayName) {
        this.slot = slot;
        this.itemId = itemId;
        this.count = count;
        this.displayName = displayName;
    }

    public int getSlot()             { return slot; }
    public String getItemId()        { return itemId; }
    public int getCount()            { return count; }
    public String getDisplayName()   { return displayName; }

    public void setSlot(int slot)                { this.slot = slot; }
    public void setItemId(String itemId)         { this.itemId = itemId; }
    public void setCount(int count)              { this.count = count; }
    public void setDisplayName(String name)      { this.displayName = name; }

    @Override
    public String toString() {
        return "ChestItem{slot=" + slot + ", itemId='" + itemId + "', count=" + count + ", displayName='" + displayName + "'}";
    }
}