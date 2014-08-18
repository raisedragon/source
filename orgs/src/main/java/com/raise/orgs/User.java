package com.raise.orgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.identity.Picture;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.persistence.entity.ByteArrayRef;

public class User implements PersistentObject
{

	protected String				id;
	protected String				name;
	protected String				account;
	protected String				password;
	protected final ByteArrayRef	pictureByteArrayRef	= new ByteArrayRef();
	protected final List<Contact>	contacts			= new ArrayList<Contact>();
	protected final List<Address>	addresses			= new ArrayList<Address>();

	public String getId()
	{
		return this.id;
	}

	public void setId(String id)
	{
		this.id = id;

	}

	public Object getPersistentState()
	{
		Map<String, Object> persistentState = new HashMap<String, Object>();
		persistentState.put("name", name);
		persistentState.put("account", account);
		persistentState.put("password", password);
		persistentState.put("pictureByteArrayId", pictureByteArrayRef.getId());
		return persistentState;
	}

	public Picture getPicture()
	{
		if (pictureByteArrayRef.getId() != null)
		{
			return new Picture(pictureByteArrayRef.getBytes(), pictureByteArrayRef.getName());
		}
		return null;
	}

	public void setPicture(Picture picture)
	{
		pictureByteArrayRef.setValue(picture.getMimeType(), picture.getBytes());
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getAccount()
	{
		return account;
	}

	public void setAccount(String account)
	{
		this.account = account;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public ByteArrayRef getPictureByteArrayRef()
	{
		return pictureByteArrayRef;
	}

	public List<Contact> getContacts()
	{
		return contacts;
	}

	public void addContact(Contact contact){
		if(contact!=null){
			this.contacts.add(contact);
		}
	}
	
	public List<Address> getAddresses()
	{
		return addresses;
	}
	
	public void addAddress(Address address){
		if(address!=null){
			this.addresses.add(address);
		}
	}
	
}
