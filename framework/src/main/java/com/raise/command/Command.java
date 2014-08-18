package com.raise.command;

public interface Command<T>
{
	public T execute(CommandContext commandContext);
}
