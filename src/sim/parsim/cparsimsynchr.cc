//=========================================================================
//  CPARSIMSYNCHR.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga, 2012
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2003-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#include "csimulation.h"
#include "cparsimsynchr.h"

USING_NAMESPACE

cEvent *cParsimSynchronizer::guessNextEvent()
{
    return sim->msgQueue.peekFirst();
}
