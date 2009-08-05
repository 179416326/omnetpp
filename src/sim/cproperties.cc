//==========================================================================
//  CPROPERTIES.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <algorithm>
#include "cproperties.h"
#include "cproperty.h"


cProperties::cProperties()
{
    islocked = false;
    refcount = 0;
}

cProperties::~cProperties()
{
    for (int i=0; i<propv.size(); i++)
        delete propv[i];
}

cProperties& cProperties::operator=(const cProperties& other)
{
    if (islocked)
        throw cRuntimeError(this, eLOCKED);

    // note: do NOT copy islocked flag

    // delete existing contents
    for (int i=0; i<propv.size(); i++)
        delete propv[i];
    propv.clear();

    // copy properties from other
    for (int i=0; i<other.propv.size(); i++)
    {
        cProperty *p = other.propv[i]->dup();
        propv.push_back(p);
    }
    return *this;
}

std::string cProperties::info() const
{
    if (propv.empty())
        return "";
    std::stringstream out;
    //out << "size=" << propv.size();
    for (int i=0; i<propv.size(); i++)
        out << (i==0 ? "" : " ") << propv[i]->info();
    return out.str();
}

void cProperties::netPack(cCommBuffer *buffer)
{
    // TBD
}

void cProperties::netUnpack(cCommBuffer *buffer)
{
    if (islocked)
        throw cRuntimeError(this, eLOCKED);
    // TBD
}

cProperty *cProperties::get(int k) const
{
    if (k<0 || k>=propv.size())
        throw cRuntimeError(this, "property index %d out of range", k);
    return propv[k];
}

cProperty *cProperties::get(const char *name, const char *index) const
{
    for (int i=0; i<propv.size(); i++)
        if (!strcmp(propv[i]->name(), name) && !opp_strcmp(index, propv[i]->index()))
            return propv[i];
    return NULL;
}

void cProperties::add(cProperty *p)
{
    if (islocked)
        throw cRuntimeError(this, eLOCKED);
    propv.push_back(p);
    p->setOwner(this);
}

void cProperties::remove(int k)
{
    if (islocked)
        throw cRuntimeError(this, eLOCKED);

    if (k<0 || k>=propv.size())
        throw cRuntimeError(this, "property index %d out of range", k);

    delete propv[k];
    propv.erase(propv.begin()+k);
}

const std::vector<const char *> cProperties::getNames() const
{
    std::vector<const char *> v;
    for (int i=0; i<propv.size(); i++)
    {
        const char *s = propv[i]->name();
        if (std::find(v.begin(), v.end(), s) != v.end())
            v.push_back(s);
    }
    return v;
}

const std::vector<const char *> cProperties::getIndicesFor(const char *name) const
{
    std::vector<const char *> v;
    for (int i=0; i<propv.size(); i++)
    {
        if (!strcmp(propv[i]->name(), name))
        {
            const char *s = propv[i]->index();
            if (std::find(v.begin(), v.end(), s) != v.end())
                v.push_back(s);
        }
    }
    return v;
}

void cProperties::lock()
{
    islocked = true;
    for (int i=0; i<propv.size(); i++)
        propv[i]->lock();
}


