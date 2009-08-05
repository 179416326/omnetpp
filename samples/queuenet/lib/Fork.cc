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

#include <stdio.h>
#include "Fork.h"
#include "Job_m.h"

Define_Module( Fork );

void Fork::initialize() 
{
    changeMsgNames = par("changeMsgNames");
}
    
void Fork::handleMessage(cMessage *msg)
{
    Job *job = check_and_cast<Job *>(msg);
    if (job == NULL)
        error("Non-job message received");
        
    // increment the generation counter
    job->setGeneration(job->getGeneration()+1);    

    // if there are more than one out gate, send a duplicate on each outgate (index>0)
    for (int i = 1; i < gateSize("out"); ++i)
    {
        cMessage *dupMsg = msg->dup();
        if (changeMsgNames) {
            char buff[1024];
            sprintf(buff, "%.60s.%d", msg->name(), i);
            dupMsg->setName(buff);
        } 
        send(dupMsg, "out", i);
    }
    // send out the original message on out[0]
    if (changeMsgNames) {
        char buff[1024];
        sprintf(buff, "%.60s.0", msg->name());
        msg->setName(buff);
    } 
    send(msg, "out", 0);
}