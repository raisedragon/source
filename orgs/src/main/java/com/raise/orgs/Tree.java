package com.raise.orgs;

public class Tree
{

	protected String	id;
	protected String	parentId;
	protected String	isLeaf;
	protected Type	type;

	public static enum Type
	{
		Group("group");

		private String	desc;

		Type(String desc)
		{
			this.desc = desc;
		}

		public String getDesc()
		{
			return desc;
		}
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getParentId()
	{
		return parentId;
	}

	public void setParentId(String parentId)
	{
		this.parentId = parentId;
	}

	public String getIsLeaf()
	{
		return isLeaf;
	}

	public void setIsLeaf(String isLeaf)
	{
		this.isLeaf = isLeaf;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

}
