package com.raise.orgs;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.db.PersistentObject;

public class Group implements PersistentObject
{

	protected String	id;
	protected String	name;
	protected String	type;
	

	public Object getPersistentState()
	{
		Map<String, Object> persistentState = new HashMap<String, Object>();
		persistentState.put("name", name);
		persistentState.put("type", type);
		return persistentState;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}


	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

}
