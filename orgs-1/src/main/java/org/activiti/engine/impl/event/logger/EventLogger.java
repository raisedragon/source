package org.activiti.engine.impl.event.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public class EventLogger implements ActivitiEventListener {
	
	private static final Logger logger = LoggerFactory.getLogger(EventLogger.class);
	
	private static final String EVENT_FLUSHER_KEY = "eventFlusher";
	
	protected ObjectMapper objectMapper;
	
	// Mapping of type -> handler
	protected Map<ActivitiEventType, Class<? extends EventLoggerEventHandler>> eventHandlers 
		= new HashMap<ActivitiEventType, Class<? extends EventLoggerEventHandler>>();
	
	// Listeners for new events
	protected List<EventLoggerListener> listeners;
	
	
	@Override
	public void onEvent(ActivitiEvent event) {
		EventLoggerEventHandler eventHandler = getEventHandler(event);
		if (eventHandler != null) {

			// Events are flushed when command context is closed
			CommandContext currentCommandContext = Context.getCommandContext();
			EventFlusher eventFlusher = (EventFlusher) currentCommandContext.getAttribute(EVENT_FLUSHER_KEY);
			
			if (eventHandler != null && eventFlusher == null) {
				
				eventFlusher = createEventFlusher();
				if (eventFlusher == null) {
					eventFlusher = new DatabaseEventFlusher(); // Default
				}
				currentCommandContext.addAttribute(EVENT_FLUSHER_KEY, eventFlusher);
				
				currentCommandContext.addCloseListener(eventFlusher);
				currentCommandContext
				    .addCloseListener(new CommandContextCloseListener() {

					    @Override
					    public void closing(CommandContext commandContext) {
					    }

					    @Override
					    public void closed(CommandContext commandContext) {
						    // For those who are interested: we can now broadcast the events were added
								if (listeners != null) {
									for (EventLoggerListener listener : listeners) {
										listener.eventsAdded(EventLogger.this);
									}
								}
					    }
					    
				    });
			}

			eventFlusher.addEventHandler(eventHandler);
		}
	}
	
	// Subclasses can override this if defaults are not ok
	protected EventLoggerEventHandler getEventHandler(ActivitiEvent event) {

		Class<? extends EventLoggerEventHandler> eventHandlerClass = null;
		if (event.getType().equals(ActivitiEventType.ENTITY_INITIALIZED)) {
			Object entity = ((ActivitiEntityEvent) event).getEntity();
		} else if (event.getType().equals(ActivitiEventType.ENTITY_DELETED)) {
			Object entity = ((ActivitiEntityEvent) event).getEntity();
		} else {
			// Default: dedicated mapper for the type
			eventHandlerClass = eventHandlers.get(event.getType());
		}
		
		if (eventHandlerClass != null) {
			return instantiateEventHandler(event, eventHandlerClass);
		}
		
		return null;
	}

	protected EventLoggerEventHandler instantiateEventHandler(ActivitiEvent event,
      Class<? extends EventLoggerEventHandler> eventHandlerClass) {
		try {
			EventLoggerEventHandler eventHandler = eventHandlerClass.newInstance();
//			eventHandler.setTimeStamp(clock.getCurrentTime());
			eventHandler.setEvent(event);
			eventHandler.setObjectMapper(objectMapper);
			return eventHandler;
		} catch (Exception e) {
			logger.warn("Could not instantiate " + eventHandlerClass + ", this is most likely a programmatic error");
		}
		return null;
  }
	
	@Override
  public boolean isFailOnException() {
		return false;
  }
	
	public void addEventHandler(ActivitiEventType eventType, Class<? extends EventLoggerEventHandler> eventHandlerClass) {
		eventHandlers.put(eventType, eventHandlerClass);
	}
	
	public void addEventLoggerListener(EventLoggerListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<EventLoggerListener>(1);
		}
		listeners.add(listener);
	}
	
	/**
	 * Subclasses that want something else than the database flusher should override this method
	 */
	protected EventFlusher createEventFlusher() {
		return null;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<EventLoggerListener> getListeners() {
		return listeners;
	}

	public void setListeners(List<EventLoggerListener> listeners) {
		this.listeners = listeners;
	}
	
}
