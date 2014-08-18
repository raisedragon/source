package com.raise.command;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandContext
{

	private static Logger									log				= LoggerFactory.getLogger(CommandContext.class.getName());

	private static final ThreadLocal<Stack<CommandContext>>	txContextStacks	= new ThreadLocal<Stack<CommandContext>>();

	protected Command<?>									command;
	protected Throwable										exception		= null;

	public static void setCurrentCommandContext(CommandContext commandContext)
	{
		getContextStack(true).push(commandContext);
	}

	public static void removeCurrentCommandContext()
	{
		getContextStack(true).pop();
	}

	public static CommandContext getCurrent()
	{
		Stack<CommandContext> contextStack = getContextStack(false);
		if ((contextStack == null) || (contextStack.isEmpty()))
		{
			return null;
		}
		return contextStack.peek();// 在非出栈情况下获取栈顶的CommandContext
	}

	private static Stack<CommandContext> getContextStack(boolean isInitializationRequired)
	{
		Stack<CommandContext> txContextStack = txContextStacks.get();// 获取当前线程一个栈变量副本
		if (txContextStack == null && isInitializationRequired)
		{// 初始化栈
			txContextStack = new Stack<CommandContext>();
			txContextStacks.set(txContextStack);
		}

		return txContextStack;

	}

	public void close()
	{
		try
		{
			try
			{

				try
				{

					if (exception == null)
					{
						flushSessions();// 会话提交

					}

				}
				catch (Throwable exception)
				{

					exception(exception);

				}
				finally
				{

				}

			}
			catch (Throwable exception)
			{

				exception(exception);

			}
			finally
			{

				closeSessions();// 关闭会话

			}

		}
		catch (Throwable exception)
		{

			exception(exception);

		}

		// rethrow the original exception if there was one

		if (exception != null)
		{

			if (exception instanceof Error)
			{

				throw (Error) exception;

			}
			else if (exception instanceof RuntimeException)
			{

				throw (RuntimeException) exception;

			}
			else
			{

			}

		}

	}

	private void closeSessions()
	{
		// TODO Auto-generated method stub

	}

	private void exception(Throwable exception2)
	{
		// TODO Auto-generated method stub

	}

	private void flushSessions()
	{
		// TODO Auto-generated method stub

	}
}
