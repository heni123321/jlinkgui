package jlinkgui;

import java.util.ArrayList;
import java.util.List;

public class Item implements Comparable<Item> {

    private final String name;
    private final List<Item> childrens;
    private Item parent;
    private final int level;
    private final boolean isModule;

    public Item(String name, int level, boolean isModule) {
        this.name = name;
        this.level = level;
        this.isModule = isModule;
        childrens = new ArrayList<>();
    }

    public void setParent(Item parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public List<Item> getChildrens() {
        return childrens;
    }

    public boolean isModule() {
        return isModule;
    }

    @Override
    public String toString() {
        Item current = this;
        String tempStr = current.getName();
        while (current.parent != null) {
            StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(current.parent.name);
			stringBuilder.append(".");
			stringBuilder.append(tempStr);
			tempStr = stringBuilder.toString();
            current = current.parent;
        }
        return tempStr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + level;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Item other = (Item) obj;
        if (level != other.level)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public int compareTo(Item arg0) {
        if (!arg0.childrens.isEmpty() && !childrens.isEmpty()) {
            return getName().compareTo(arg0.getName());
        } else if (!childrens.isEmpty()) {
            return -1;
        } else if (!arg0.childrens.isEmpty()) {
            return 1;
        } else if (!isModule && !arg0.isModule) {
            return getName().compareTo(arg0.getName());
        } else if (!isModule) {
            return -1;
        } else if (!arg0.isModule) {
            return 1;
        } else {
            return toString().compareTo(arg0.toString());
        }
    }

}
