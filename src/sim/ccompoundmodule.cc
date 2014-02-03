//======================================================================
//  CCOMPOUNDMODULE.CC - part of
//
//                 OMNeT++/OMNEST
//              Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//======================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>
#include <string.h>
#include <sstream>
#include "ccompoundmodule.h"
#include "ccomponenttype.h"
#include "cmessage.h"

NAMESPACE_BEGIN


Register_Class(cCompoundModule);


cCompoundModule::cCompoundModule()
{
}

cCompoundModule::~cCompoundModule()
{
}

std::string cCompoundModule::info() const
{
    std::stringstream out;
    out << "id=" << getId();
    return out.str();
}

void cCompoundModule::arrived(cMessage *msg, cGate *ongate, simtime_t)
{
    throw cRuntimeError("Gate `%s' of compound module (%s)%s is not connected on the %s, "
                        "upon arrival of message (%s)%s",
                        ongate->getFullName(),
                        getClassName(), getFullPath().c_str(),
                        (ongate->isConnectedOutside() ? "inside" : "outside"),
                        msg->getClassName(), msg->getName());
}

NAMESPACE_END

