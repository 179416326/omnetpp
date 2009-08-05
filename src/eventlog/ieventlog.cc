//=========================================================================
//  IEVENTLOG.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "ieventlog.h"

IEventLog::IEventLog()
{
    lastNeighbourEventNumber = -1;
    lastNeighbourEvent = NULL;
}

void IEventLog::synchronize()
{
    lastNeighbourEventNumber = -1;
    lastNeighbourEvent = NULL;
}

void IEventLog::printEvents(FILE *file, long fromEventNumber, long toEventNumber, bool outputEventLogMessages)
{
    IEvent *event = fromEventNumber == -1 ? getFirstEvent() : getFirstEventNotBeforeEventNumber(fromEventNumber);

    while (event != NULL && (toEventNumber == -1 || event->getEventNumber() <= toEventNumber))
    {
        event->print(file, outputEventLogMessages);
        event = event->getNextEvent();
    }
}

void IEventLog::print(FILE *file, long fromEventNumber, long toEventNumber, bool outputInitializationEntries, bool outputEventLogMessages)
{
    if (outputInitializationEntries)
        printInitializationLogEntries(file);

    printEvents(file, fromEventNumber, toEventNumber, outputEventLogMessages);
}

IEvent *IEventLog::getNeighbourEvent(IEvent *event, long distance)
{
    Assert(event);
    long neighbourEventNumber = event->getEventNumber() + distance;

    if (lastNeighbourEvent && lastNeighbourEventNumber != -1 && abs(neighbourEventNumber - lastNeighbourEventNumber) < abs(distance))
        return getNeighbourEvent(lastNeighbourEvent, neighbourEventNumber - lastNeighbourEventNumber);

    while (event != NULL && distance != 0)
    {
        if (distance > 0) {
            distance--;
            event = event->getNextEvent();
        }
        else {
            distance++;
            event = event->getPreviousEvent();
        }
    }

    lastNeighbourEventNumber = neighbourEventNumber;
    lastNeighbourEvent = (IEvent *)event;

    return lastNeighbourEvent;
}

double IEventLog::getApproximatePercentageForEventNumber(long eventNumber)
{
    IEvent *firstEvent = getFirstEvent();
    IEvent *lastEvent = getLastEvent();
    IEvent *event = getEventForEventNumber(eventNumber);

    if (firstEvent == NULL)
        return 0;
    else if (event == NULL)
        return 0.5;
    else {
        file_offset_t beginOffset = firstEvent->getBeginOffset();
        file_offset_t endOffset = lastEvent->getBeginOffset();

        double percentage = (double)(event->getBeginOffset() - beginOffset) / (endOffset - beginOffset);

        return std::min(std::max(percentage, 0.0), 1.0);
    }
}
