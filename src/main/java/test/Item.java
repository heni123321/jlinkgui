package test;

import java.util.ArrayList;
import java.util.List;

public class Item implements Comparable<Item>{

	private final String name;
	private List<Item> childens;
	private Item parent;
	private final int level;
	private final boolean isModule;
	
	public Item(String name, int level, boolean isModule) {
		this.name = name;
		this.level = level;
		this.isModule = isModule;
		childens = new ArrayList<>();
	}
	
	public void setParent(Item parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public List<Item> getChildens() {
		return childens;
	}

	public boolean isModule() {
		return isModule;
	}

	public String getFullName() {
		Item current = this;
		String tempStr = current.getName();
		while(current.parent != null) {
			tempStr =  current.parent.name + "." + tempStr;
			current = current.parent;
		}
		return tempStr;
	}
	
	@Override
	public String toString() {
		return getFullName();
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
		if (arg0.childens.size() > 0 && childens.size() > 0) {
			return getName().compareTo(arg0.getName());
		} else if (childens.size() > 0) {
			return -1;
		} else if (arg0.childens.size() > 0) {
			return 1;
		} else if (!isModule) {
			return -1;
		} else if (!arg0.isModule) {
			return 1;
		} else  if (!isModule && !arg0.isModule) {
			return getName().compareTo(arg0.getName());			
		}else {
			return getFullName().compareTo(arg0.getFullName());
		}
	}
	
}
