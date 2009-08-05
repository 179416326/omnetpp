package org.omnetpp.eventlogtable.widgets;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.eventlog.EventLogInput;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.virtualtable.IVirtualTableRowRenderer;
import org.omnetpp.eventlog.engine.BeginSendEntry;
import org.omnetpp.eventlog.engine.BubbleEntry;
import org.omnetpp.eventlog.engine.CancelEventEntry;
import org.omnetpp.eventlog.engine.ConnectionCreatedEntry;
import org.omnetpp.eventlog.engine.ConnectionDeletedEntry;
import org.omnetpp.eventlog.engine.ConnectionDisplayStringChangedEntry;
import org.omnetpp.eventlog.engine.DeleteMessageEntry;
import org.omnetpp.eventlog.engine.EndSendEntry;
import org.omnetpp.eventlog.engine.Event;
import org.omnetpp.eventlog.engine.EventEntry;
import org.omnetpp.eventlog.engine.EventLogEntry;
import org.omnetpp.eventlog.engine.EventLogMessageEntry;
import org.omnetpp.eventlog.engine.IEvent;
import org.omnetpp.eventlog.engine.IEventLog;
import org.omnetpp.eventlog.engine.MessageDependency;
import org.omnetpp.eventlog.engine.ModuleCreatedEntry;
import org.omnetpp.eventlog.engine.ModuleDeletedEntry;
import org.omnetpp.eventlog.engine.ModuleDisplayStringChangedEntry;
import org.omnetpp.eventlog.engine.ModuleMethodBeginEntry;
import org.omnetpp.eventlog.engine.ModuleMethodEndEntry;
import org.omnetpp.eventlog.engine.ModuleReparentedEntry;
import org.omnetpp.eventlog.engine.PStringVector;
import org.omnetpp.eventlog.engine.SendDirectEntry;
import org.omnetpp.eventlog.engine.SendHopEntry;

public class EventLogTableRowRenderer implements IVirtualTableRowRenderer<EventLogEntry> {
	private static final Color DARKBLUE = new Color(null, 0, 0, 192);
	private static final Color DARKRED = new Color(null, 127, 0, 85);
	private static final Color RED = new Color(null, 240, 0, 0);
	private static final Color BLACK = new Color(null, 0, 0, 0);
	private static final Color LIGHTGREY = new Color(null, 211, 211, 211);
	
	private static final Color BOOKMARK_COLOR = ColorFactory.asColor("lightCyan");

	private static final Color EVENT_ENTRY_EVENT_NUMBER_COLOR = BLACK;

	private static final Color EVENT_LOG_ENTRY_EVENT_NUMBER_COLOR = LIGHTGREY;

	private static final Color EVENT_ENTRY_SIMULATION_TIME_COLOR = BLACK;

	private static final Color EVENT_LOG_ENTRY_SIMULATION_TIME_COLOR = LIGHTGREY;

	private static final Color CONSTANT_TEXT_COLOR = BLACK;

	private static final Color RAW_VALUE_COLOR = DARKBLUE;

	private static final Color TYPE_COLOR = DARKBLUE;

	private static final Color NAME_COLOR = DARKBLUE;

	private static final Color EVENT_LOG_MESSAGE_COLOR = DARKRED;

	private static final Color BUBBLE_ENTRY_COLOR = RED;

	private static final Color DATA_COLOR = DARKBLUE;

	private static final int HORIZONTAL_SPACING = 4;

	private static final int INDENT_SPACING = HORIZONTAL_SPACING * 4;

	private static final int VERTICAL_SPACING = 3;

	protected EventLogInput eventLogInput;

	protected Font font = JFaceResources.getDefaultFont();

	protected int fontHeight;

	/**
	 * The current GC we are drawing to.
	 */
	protected GC gc;
	
	/**
	 * The next x position where drawing continues.
	 */
	protected int x;

	public void setInput(Object eventLogInput) {
		this.eventLogInput = (EventLogInput)eventLogInput;
	}

	public int getRowHeight(GC gc) {
		if (fontHeight == 0) {
			Font oldFont = gc.getFont();
			gc.setFont(font);
			fontHeight = gc.getFontMetrics().getHeight();
			gc.setFont(oldFont);
		}

		return fontHeight + VERTICAL_SPACING;
	}

	public void drawCell(GC gc, EventLogEntry element, int index) {
		Assert.isTrue(element != null);
		
		this.x = HORIZONTAL_SPACING;
		this.gc = gc;
		gc.setAntialias(SWT.OFF);

		EventLogEntry eventLogEntry = (EventLogEntry)element;
		Event event = eventLogEntry.getEvent();
		boolean isEventLogEntry = eventLogEntry instanceof EventEntry;

		try {
			if (eventLogInput.getFile() != null) {
				IMarker[] markers = eventLogInput.getFile().findMarkers(IMarker.BOOKMARK, true, IResource.DEPTH_ZERO);
				boolean marked = false;
				for (int i = 0; i < markers.length; i++)
					if (markers[i].getAttribute("EventNumber", -1) == event.getEventNumber()) {
						marked = true;
						break;
					}
				
				if (marked && 
					(gc.getBackground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND)) ||
					 gc.getBackground().equals(BOOKMARK_COLOR))) {
					gc.setBackground(BOOKMARK_COLOR);
					gc.fillRectangle(gc.getClipping());
				}
			}
		}
		catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		switch (index) {
			case 0:
				drawText("#" + event.getEventNumber(), isEventLogEntry ? EVENT_ENTRY_EVENT_NUMBER_COLOR : EVENT_LOG_ENTRY_EVENT_NUMBER_COLOR, false);
				break;
			case 1:
				drawText(event.getSimulationTime() + "s", isEventLogEntry ? EVENT_ENTRY_SIMULATION_TIME_COLOR : EVENT_LOG_ENTRY_SIMULATION_TIME_COLOR, false); 
				break;
			case 2:
				x += eventLogEntry.getLevel() * INDENT_SPACING;
				
				if (!(eventLogEntry instanceof EventEntry))
					x += INDENT_SPACING;

				if (eventLogEntry instanceof SendHopEntry || eventLogEntry instanceof SendDirectEntry)
					x += INDENT_SPACING;

				Image image = getEventLogEntryImage(eventLogEntry);
				gc.drawImage(image, x, 0);
				x += image.getBounds().width + HORIZONTAL_SPACING;
				
				switch (eventLogInput.getEventLogTableFacade().getDisplayMode()) {
					case 0:
						if (eventLogEntry instanceof EventEntry) {
							MessageDependency cause = event.getCause();
				
							drawText("Event in ", CONSTANT_TEXT_COLOR);
							drawModuleDescription(event.getModuleId());
							
							BeginSendEntry beginSendEntry = cause != null ? cause.getCauseBeginSendEntry() : null;
							if (beginSendEntry != null) {
								drawText(" on arrival of ", CONSTANT_TEXT_COLOR);
								
								if (event.isSelfEvent())
									drawText("self ", CONSTANT_TEXT_COLOR);

								drawMessageDescription(beginSendEntry);

								if (!event.isSelfEvent()) {
									drawText(" from ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(beginSendEntry.getContextModuleId());
								}
								
								IEvent causeEvent = cause.getCauseEvent();
								if (causeEvent != null && causeEvent.getModuleId() != beginSendEntry.getContextModuleId()) {
									drawText(" called from ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(causeEvent.getModuleId());
								}
							}
						}
						else {
							if (eventLogEntry instanceof EventLogMessageEntry) {
								EventLogMessageEntry eventLogMessageEntry = (EventLogMessageEntry)eventLogEntry;
								drawText(eventLogMessageEntry.getText(), EVENT_LOG_MESSAGE_COLOR);
							}
							else if (eventLogEntry instanceof BubbleEntry) {
								BubbleEntry bubbleEntry = (BubbleEntry)eventLogEntry;
								drawText("Bubble", CONSTANT_TEXT_COLOR);

								if (event.getModuleId() != bubbleEntry.getContextModuleId()) {
									drawText(" in ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(bubbleEntry.getContextModuleId());
								}

								drawText(": ", CONSTANT_TEXT_COLOR);
								drawText(bubbleEntry.getText(), BUBBLE_ENTRY_COLOR);
							}
							else if (eventLogEntry instanceof ModuleMethodBeginEntry) {
								ModuleMethodBeginEntry moduleMethodBeginEntry = (ModuleMethodBeginEntry)eventLogEntry;
								drawText("Begin calling ", CONSTANT_TEXT_COLOR);
								drawText(moduleMethodBeginEntry.getMethod(), DATA_COLOR);
								drawText(" in ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleMethodBeginEntry.getToModuleId());

								if (event.getModuleId() != moduleMethodBeginEntry.getContextModuleId()) {
									drawText(" from ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(moduleMethodBeginEntry.getFromModuleId());
								}
							}
							else if (eventLogEntry instanceof ModuleMethodEndEntry) {
								drawText("End calling module", CONSTANT_TEXT_COLOR);
							}
							else if (eventLogEntry instanceof ModuleCreatedEntry) {
								ModuleCreatedEntry moduleCreatedEntry = (ModuleCreatedEntry)eventLogEntry;
								drawText("Creating ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleCreatedEntry);
								drawText(" under ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleCreatedEntry.getParentModuleId());
							}
							else if (eventLogEntry instanceof ModuleDeletedEntry) {
								ModuleDeletedEntry moduleDeletedEntry = (ModuleDeletedEntry)eventLogEntry;
								drawText("Deleting ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleDeletedEntry.getModuleId());
							}
							else if (eventLogEntry instanceof ModuleReparentedEntry) {
								ModuleReparentedEntry moduleReparentedEntry = (ModuleReparentedEntry)eventLogEntry;
								drawText("Reparenting ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleReparentedEntry.getModuleId());
								drawText(" under ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(moduleReparentedEntry.getNewParentModuleId());
							}
							else if (eventLogEntry instanceof ConnectionCreatedEntry) {
								ConnectionCreatedEntry connectionCreatedEntry = (ConnectionCreatedEntry)eventLogEntry;
								drawText("Createding ", CONSTANT_TEXT_COLOR);
								drawConnectionDescription(connectionCreatedEntry.getSourceModuleId(), connectionCreatedEntry.getSourceGateFullName(),
									connectionCreatedEntry.getDestModuleId(), connectionCreatedEntry.getDestGateFullName());
							}
							else if (eventLogEntry instanceof ConnectionDeletedEntry) {
								ConnectionDeletedEntry connectionDeletedEntry = (ConnectionDeletedEntry)eventLogEntry;
								drawText("Deleting ", CONSTANT_TEXT_COLOR);
								drawConnectionDescription(connectionDeletedEntry.getSourceModuleId(), connectionDeletedEntry.getSourceGateId());
							}
							else if (eventLogEntry instanceof ConnectionDisplayStringChangedEntry) {
								// TODO: print connection info
								ConnectionDisplayStringChangedEntry connectionDisplayStringChangedEntry = (ConnectionDisplayStringChangedEntry)eventLogEntry;
								drawText("Connection display string changed to ", CONSTANT_TEXT_COLOR);
								drawText(connectionDisplayStringChangedEntry.getDisplayString(), DATA_COLOR);
							}
							else if (eventLogEntry instanceof ModuleDisplayStringChangedEntry) {
								ModuleDisplayStringChangedEntry moduleDisplayStringChangedEntry = (ModuleDisplayStringChangedEntry)eventLogEntry;
								drawText("Display string changed", CONSTANT_TEXT_COLOR);

								if (event.getModuleId() != moduleDisplayStringChangedEntry.getContextModuleId())	{
									drawText("for ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(moduleDisplayStringChangedEntry.getContextModuleId());
								}

								drawText(" to ", CONSTANT_TEXT_COLOR);							
								drawText(moduleDisplayStringChangedEntry.getDisplayString(), DATA_COLOR);
							}
							else if (eventLogEntry instanceof CancelEventEntry) {
								CancelEventEntry cancelEventEntry = (CancelEventEntry)eventLogEntry;
								drawText("Cancelling self ", CONSTANT_TEXT_COLOR);
								drawMessageDescription(findBeginSendEntry(cancelEventEntry.getPreviousEventNumber(), cancelEventEntry.getMessageId()));
							}
							else if (eventLogEntry instanceof BeginSendEntry) {
								// TODO: complete message stuff, tooltip with all data?
								BeginSendEntry beginSendEntry = (BeginSendEntry)eventLogEntry;
								drawText("Begin sending of ", CONSTANT_TEXT_COLOR);
								drawMessageDescription(beginSendEntry);
								drawText(" kind = ", CONSTANT_TEXT_COLOR);
								drawText(String.valueOf(beginSendEntry.getMessageKind()), DATA_COLOR);
								drawText(" length = ", CONSTANT_TEXT_COLOR);
								drawText(String.valueOf(beginSendEntry.getMessageLength()), DATA_COLOR);
							}
							else if (eventLogEntry instanceof EndSendEntry) {
								EndSendEntry endSendEntry = (EndSendEntry)eventLogEntry;
								drawText("End sending at ", CONSTANT_TEXT_COLOR);
								drawText(endSendEntry.getArrivalTime() + "s", DATA_COLOR);
							}
							else if (eventLogEntry instanceof SendHopEntry) {
								// TODO: add senderGateId
								SendHopEntry sendHopEntry = (SendHopEntry)eventLogEntry;
								drawText("Sending from ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(sendHopEntry.getSenderModuleId());
								drawText(" with transmission delay ", CONSTANT_TEXT_COLOR);
								drawText(sendHopEntry.getTransmissionDelay() + "s", DATA_COLOR);
								drawText(" and propagation delay ", CONSTANT_TEXT_COLOR);
								drawText(sendHopEntry.getPropagationDelay() + "s", DATA_COLOR);
							}
							else if (eventLogEntry instanceof SendDirectEntry) {
								// TODO: add destGate name
								SendDirectEntry sendDirectEntry = (SendDirectEntry)eventLogEntry;
								drawText("Sending direct message ", CONSTANT_TEXT_COLOR);

								if (event.getModuleId() != sendDirectEntry.getContextModuleId())	{
									drawText("from ", CONSTANT_TEXT_COLOR);
									drawModuleDescription(sendDirectEntry.getSenderModuleId());
								}

								drawText(" to ", CONSTANT_TEXT_COLOR);
								drawModuleDescription(sendDirectEntry.getDestModuleId());
								drawText(" with transmission delay ", CONSTANT_TEXT_COLOR);
								drawText(sendDirectEntry.getTransmissionDelay() + "s", DATA_COLOR);
								drawText(" and propagation delay ", CONSTANT_TEXT_COLOR);
								drawText(sendDirectEntry.getPropagationDelay() + "s", DATA_COLOR);
							}
							else if (eventLogEntry instanceof DeleteMessageEntry) {
								DeleteMessageEntry deleteMessageEntry = (DeleteMessageEntry)eventLogEntry;
								drawText("Deleting ", CONSTANT_TEXT_COLOR);
								drawMessageDescription(findBeginSendEntry(deleteMessageEntry.getPreviousEventNumber(), deleteMessageEntry.getMessageId()));
							}
							else
								throw new RuntimeException("Unknown event log entry: " + eventLogEntry.getClassName());
						}
						break;
					case 1:
						drawRawEntry(eventLogEntry);
						break;
					default:
						throw new RuntimeException("Unknown display mode");
				}
		}
	}

	private BeginSendEntry findBeginSendEntry(int previousEventNumber, int messageId) {
		if (previousEventNumber != -1) {
			IEvent event = eventLogInput.getEventLog().getEventForEventNumber(previousEventNumber);
	
			if (event != null) {
				int index = event.findBeginSendEntryIndex(messageId);
				
				if (index != -1)
					return (BeginSendEntry)event.getEventLogEntry(index);
			}
		}
		
		return null;
	}

	private void drawModuleDescription(int moduleId) {
		drawModuleDescription(eventLogInput.getEventLog().getModuleCreatedEntry(moduleId));
	}

	private void drawModuleDescription(ModuleCreatedEntry moduleCreatedEntry) {
		drawText("module ", CONSTANT_TEXT_COLOR);

		if (moduleCreatedEntry != null) {
			// TODO: print submodule (fullName), parent (fullName) or module (fullPath)
			drawText("(" + moduleCreatedEntry.getModuleClassName() + ") ", TYPE_COLOR);
			drawText(moduleCreatedEntry.getFullName(), NAME_COLOR, true);
		}
		else
			drawText("<unknown>", CONSTANT_TEXT_COLOR);
	}
	
	private void drawConnectionDescription(int sourceModuleId, String sourceGateFullName, int destModuleId, String destGateFullName) {
		IEventLog eventLog = eventLogInput.getEventLog();
		drawConnectionDescription(eventLog.getModuleCreatedEntry(sourceModuleId), sourceGateFullName, eventLog.getModuleCreatedEntry(destModuleId), destGateFullName);
	}

	private void drawConnectionDescription(int sourceModuleId, int sourceGateId) {
		IEventLog eventLog = eventLogInput.getEventLog();
		// TODO: find out source gate name and dest stuff
		drawConnectionDescription(eventLog.getModuleCreatedEntry(sourceModuleId), String.valueOf(sourceGateId), null, null);
	}

	private void drawConnectionDescription(ModuleCreatedEntry sourceModuleCreatedEntry, String sourceGateFullName, ModuleCreatedEntry destModuleCreatedEntry, String destGateFullName) {
		drawText("connection from ", CONSTANT_TEXT_COLOR);

		if (sourceModuleCreatedEntry != null) {
			drawModuleDescription(sourceModuleCreatedEntry);
			drawText(" gate ", CONSTANT_TEXT_COLOR);
			drawText(sourceGateFullName, DATA_COLOR);
		}

		drawText(" to ", CONSTANT_TEXT_COLOR);

		if (destModuleCreatedEntry != null) {
			drawModuleDescription(destModuleCreatedEntry);
			drawText(" gate ", CONSTANT_TEXT_COLOR);
			drawText(destGateFullName, DATA_COLOR);
		}
	}

	private void drawMessageDescription(BeginSendEntry beginSendEntry) {
		if (beginSendEntry != null) {
			drawText("message ", CONSTANT_TEXT_COLOR);
			drawText("(" + beginSendEntry.getMessageClassName() + ") ", TYPE_COLOR);
			drawText(beginSendEntry.getMessageFullName(), NAME_COLOR, true);
		}
		else
			drawText("message <unknown>", CONSTANT_TEXT_COLOR);
	}
	
	private void drawRawEntry(EventLogEntry eventLogEntry) {

		if (!(eventLogEntry instanceof EventLogMessageEntry)) {
			drawText(eventLogEntry.getDefaultAttribute() + " ", CONSTANT_TEXT_COLOR, true);
		}

		PStringVector stringVector = eventLogEntry.getAttributeNames();
		for (int i = 0; i < stringVector.size(); i++)
		{
			String name = stringVector.get(i);
			drawText(name + " ", CONSTANT_TEXT_COLOR);
			drawText(eventLogEntry.getAttribute(name) + " ", RAW_VALUE_COLOR);
		}
	}
	
	private void drawText(String text, Color color) {
		drawText(text, color, false);
	}
	
	private void drawText(String text, Color color, boolean bold) {
		Font oldFont = gc.getFont();
		FontData fontData = font.getFontData()[0];
		Font newFont = new Font(oldFont.getDevice(), fontData.getName(), fontData.getHeight(), bold ? SWT.BOLD : SWT.NORMAL);
		gc.setFont(newFont);

		if (color != null && !gc.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT)))
			gc.setForeground(color);

		gc.drawText(text, x, VERTICAL_SPACING/2);
		x += gc.textExtent(text).x;
		newFont.dispose();
		gc.setFont(oldFont);
	}
	
	private Image getEventLogEntryImage(EventLogEntry eventLogEntry) {
		String className = eventLogEntry.getClassName();
		
		if (eventLogEntry instanceof EventEntry && eventLogEntry.getEvent().isSelfEvent())
			return ImageFactory.getImage(ImageFactory.EVENLOG_IMAGE_SELF_EVENT);
		else if (eventLogEntry instanceof BeginSendEntry) {
			EventLogEntry nextEventLogEntry = eventLogEntry.getEvent().getEventLogEntry(eventLogEntry.getIndex() + 1);

			if (nextEventLogEntry instanceof EndSendEntry)
				return ImageFactory.getImage(ImageFactory.EVENLOG_IMAGE_SCHEDULE_AT);
		}

		return ImageFactory.getImage(ImageFactory.EVENTLOG_IMAGE_DIR + className.substring(0, className.length() - 5));
	}
}
