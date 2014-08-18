package com.raise.command;

public interface CommandExecutor
{
	T execute(Command<T> )
}
