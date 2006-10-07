//========================================================================
//  CPOLYMORPHIC.CC - part of
//
//                 OMNeT++/OMNEST
//              Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "cpolymorphic.h"
#include "cexception.h"
#include "cstruct.h"

const char *cPolymorphic::className() const
{
    return opp_typename(typeid(*this));
}

cStructDescriptor *cPolymorphic::createDescriptor()
{
    return cStructDescriptor::createDescriptorFor(className(), (void *)this);
}

const char *cPolymorphic::fullName() const
{
    return "";
}

std::string cPolymorphic::fullPath() const
{
    return std::string(fullName());
}

std::string cPolymorphic::info() const
{
    return std::string();
}

std::string cPolymorphic::detailedInfo() const
{
    return std::string();
}

cPolymorphic *cPolymorphic::dup() const
{
    throw new cRuntimeError("The dup() method, declared in cPolymorphic, is not "
                            "redefined in class %s", className());
}

void cPolymorphic::netPack(cCommBuffer *buffer)
{
}

void cPolymorphic::netUnpack(cCommBuffer *buffer)
{
}

void cPolymorphic::copyNotSupported() const
{
    throw new cRuntimeError(this,eCANTCOPY);
}


