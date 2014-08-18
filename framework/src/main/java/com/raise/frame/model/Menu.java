package com.raise.frame.model;

public class Menu extends Tree
{
	protected boolean folder;
	
	protected boolean expanse;
	
	protected String windowId;
	
	public boolean isFolder()
	{
		return folder;
	}
	public void setFolder(boolean folder)
	{
		this.folder = folder;
	}
	public boolean isExpanse()
	{
		return expanse;
	}
	public void setExpanse(boolean expanse)
	{
		this.expanse = expanse;
	}
	
}
