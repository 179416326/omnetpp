//==========================================================================
//  ERRMSG.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Error code constants
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __ERRMSG_H
#define __ERRMSG_H

#include "simkerneldefs.h"

NAMESPACE_BEGIN

/**
 * Error codes
 */
enum OppErrorCode
{
    E_OK = 0,  // E_OK must have zero numeric value
    E_BACKSCHED,
    E_BADCAST,
    E_BADEXP,
    E_BADINIT,
    E_CANCEL,
    E_CANTCOPY,
    E_CANTDUP,
    E_CANTPACK,
    E_CUSTOM,
    E_DIMLESS,
    E_EBADARGS,
    E_ECANTCAST,
    E_ENDEDOK,
    E_ENDSIM,
    E_ENOPARENT,
    E_ESTKOFLOW,
    E_ESTKUFLOW,
    E_FINISH,
    E_GATEID,
    E_INFLOOP,
    E_LOCKED,
    E_MODFIN,
    E_MODINI,
    E_NEGTIME,
    E_NEGTOUT,
    E_NOCHANDEF,
    E_NONET,
    E_NOPARENTMOD,
    E_NOPARSIM,
    E_NORECV,
    E_NOSUCHNET,
    E_NUMARGS,
    E_PARAM,
    E_PARNOTSET,
    E_PARAMSNOTREADY,
    E_REALTIME,
    E_SIMTIME,
    E_STATECHG,
    E_STKLOW,
    E_STOPSIMRCVD,
    E_USER,
    E_WRONGSIM
};

/**
 * Provides error messages for error codes.
 *
 * @ingroup Internals
 */
class SIM_API cErrorMessages
{
  public:
    cErrorMessages() {}
    static const char *get(OppErrorCode e);
};

NAMESPACE_END


#endif