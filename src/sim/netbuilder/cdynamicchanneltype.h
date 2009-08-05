//==========================================================================
//  CDYNAMICCHANNELTYPE.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `terms' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CDYNAMICCHANNELTYPE_H
#define __CDYNAMICCHANNELTYPE_H

#include "cchannel.h"
#include "ccomponenttype.h"
#include "cnednetworkbuilder.h"
#include "cneddeclaration.h"


/**Declaration();
 * NEDXML-based cChannelType: takes all info from cNEDLoader
 */
class cDynamicChannelType : public cChannelType
{
  protected:
    /** Redefined from cChannelType */
    virtual cChannel *createChannelObject();

    /** Redefined from cChannelType */
    virtual void addParametersTo(cChannel *module);

    // internal utility function
    cNEDDeclaration *getDecl() const;

  public:
    /**
     * Constructor.
     */
    cDynamicChannelType(const char *name);

    /**
     * Produces a one-line description.
     */
    virtual std::string info() const;

    /**
     * Produces a detailed, multi-line description.
     */
    virtual std::string detailedInfo() const;
    //@}

    /**
     * Returns the NED text, if available.
     */
    virtual cNEDDeclarationBase *declaration() const {return getDecl();} //XXX

};


#endif


