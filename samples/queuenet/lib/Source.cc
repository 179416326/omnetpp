//
// Copyright (C) 2006 Rudolf Hornig
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//

#include "Source.h"
#include "Job_m.h"

Define_Module( Source );

void Source::initialize()
{
    jobCounter = 0;
    startTime =  par("startTime");
    stopTime =  par("stopTime"); 
    selfMsg = new cMessage("newJobTimer");
    numJobs =  par("numJobs");
    jobName = par("jobName");
    // if empty, use the module name as the default for message names
    if (strcmp(jobName, "") == 0)
        jobName = name();
    // schedule the first message timer for start time
    scheduleAt(startTime, selfMsg);
}

void Source::handleMessage(cMessage *msg)
{
    if (msg->isSelfMessage() 
        && ((numJobs < 0) || (numJobs > jobCounter)) 
        && ((stopTime < 0.0) || (stopTime > simTime()))) 
    {
        // reschedule the self timer for the next message
        scheduleAt(simTime() + par("interArrivalTime").doubleValue(), msg );
        // create a new message to be sent		
        Job *newJob = new Job();
        newJob->setTimestamp();	
        char buff[1024];
        sprintf(buff, "%.60s %d", jobName, ++jobCounter);
        newJob->setName(buff);
        newJob->setKind(par("jobType"));
        newJob->setPriority(par("jobPriority"));
        send(newJob, "out");
    }
}

void Source::finish()
{
    cancelAndDelete(selfMsg);
    selfMsg = NULL;
}
