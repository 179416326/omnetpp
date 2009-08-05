//==========================================================================
//  CPROPERTY.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "cproperty.h"
#include "cproperties.h"


cStringPool cProperty::stringPool;


cProperty::cProperty(const char *name, const char *index)
{
    isimplicit = islocked = false;
    ownerp = NULL;
    propname = NULL;
    propindex = NULL;
    propfullname = NULL;
    if (name)
        setName(name);
    if (index)
        setIndex(index);
}

cProperty::~cProperty()
{
    // release pooled strings
    stringPool.release(propname);
    stringPool.release(propindex);
    stringPool.release(propfullname);
    int n = keyv.size();
    for (int i=0; i<n; i++)
    {
        stringPool.release(keyv[i]);
        releaseValues(valuesv[i]);
    }
}

void cProperty::releaseValues(CharPtrVector& vals)
{
    int n = vals.size();
    for (int i=0; i<n; i++)
        stringPool.release(vals[i]);
    vals.clear();
}

cProperty& cProperty::operator=(const cProperty& other)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);

    // note: do NOT copy islocked flag
    setName(other.fullName());
    setIndex(other.index());

    isimplicit = other.isimplicit;

    // release old value
    stringPool.release(propname);
    int n = keyv.size();
    for (int i=0; i<n; i++)
    {
        stringPool.release(keyv[i]);
        releaseValues(valuesv[i]);
    }
    keyv.clear();
    valuesv.clear();

    // copy new value
    int m = other.keyv.size();
    for (int i=0; i<m; i++)
    {
        keyv.push_back(stringPool.get(other.keyv[i]));
        valuesv.push_back(CharPtrVector());
        CharPtrVector& vals = valuesv[i];
        const CharPtrVector& othervals = other.valuesv[i];
        int nn = othervals.size();
        vals.resize(nn);
        for (int j=0; j<nn; j++)
            vals[j] = stringPool.get(othervals[j]);
    }
    return *this;
}

void cProperty::setName(const char *name)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    if (!name)
        name = "";
    if (name[0]=='@')
        throw new cRuntimeError(this,"setName(): property name must be specified without the '@' character");
    if (propname)
        stringPool.release(propname);
    propname = stringPool.get(name);
    updateFullName();
}

void cProperty::updateFullName()
{
    if (propfullname)
		stringPool.release(propfullname);
    if (!propindex)
    {
        propfullname = stringPool.get(propname);
    }
    else
    {
        std::stringstream os;
        os << propname << "[" << propindex << "]";
        std::string res = os.str();
        propfullname = stringPool.get(os.str().c_str());
    }
}

const char *cProperty::fullName() const
{
    return propfullname;
}

std::string cProperty::fullPath() const
{
    return ownerp ? ownerp->fullPath()+"."+fullName() : fullName();
}

std::string cProperty::info() const
{
    std::stringstream os;
    os << "@" << propname;
    if (!keyv.empty())
    {
        os << "(";
        int n = keyv.size();
        for (int i=0; i<n; i++)
        {
            if (i!=0)
                os << ";";
            if (keyv[i] && *keyv[i])
                os << keyv[i] << "=";
            for (int j=0; j<valuesv[i].size(); j++)
                os << (j==0 ? "" : ",") << valuesv[i][j];  //FIXME value may need quoting
        }
        os << ")";
    }
    return os.str();
}

void cProperty::netPack(cCommBuffer *buffer)
{
    //TBD
}

void cProperty::netUnpack(cCommBuffer *buffer)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    //TBD
}

void cProperty::setIndex(const char *index)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    if (propindex)
        stringPool.release(propindex);
    propindex = stringPool.get(index);
    updateFullName();
}

const char *cProperty::index() const
{
    return propindex;
}

void cProperty::setIsImplicit(bool b)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    isimplicit = b;
}

bool cProperty::isImplicit() const
{
    return isimplicit;
}

int cProperty::findKey(const char *key) const
{
    for (int i=0; i<keyv.size(); i++)
        if (!strcmp(key,keyv[i]))
            return i;
    return -1;
}

const std::vector<const char *>& cProperty::keys() const
{
    return keyv;
}

bool cProperty::hasKey(const char *key) const
{
    return findKey(key)!=-1;
}

void cProperty::addKey(const char *key)
{
    int k = findKey(key);
    if (k==-1)
    {
        keyv.push_back(stringPool.get(key));
        valuesv.push_back(CharPtrVector());
    }
}

cProperty::CharPtrVector& cProperty::valuesVector(const char *key) const
{
    int k = findKey(key);
    if (k==-1)
        throw cRuntimeError(this, "property has no key named `%s'", key);
    return const_cast<CharPtrVector&>(valuesv[k]);
}

int cProperty::numValues(const char *key) const
{
    CharPtrVector& v = valuesVector(key);
    return v.size();
}

void cProperty::setNumValues(const char *key, int size)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    CharPtrVector& v = valuesVector(key);
    int oldsize = v.size();

    // if shrink, release extra elements
    for (int i=size; i<oldsize; i++)
        stringPool.release(v[i]);

    // resize
    v.resize(size);

    // if grow, initialize extra elements
    for (int i=oldsize; i<size; i++)
        v[i] = stringPool.get("");
}

const char *cProperty::value(const char *key, int k) const
{
    CharPtrVector& v = valuesVector(key);
    if (k<0 || k>=v.size())
        return "";
    return v[k];
}

void cProperty::setValue(const char *key, int k, const char *value)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);
    CharPtrVector& v = valuesVector(key);
    if (k<0)
        throw cRuntimeError(this,"negative property value index %d for key `%s'", k, key);
    if (k>=v.size())
        setNumValues(key, k+1);
    stringPool.release(v[k]);
    v[k] = stringPool.get(value);
}

void cProperty::erase(const char *key)
{
    if (islocked)
        throw new cRuntimeError(this, eLOCKED);

    // erase
    int k = findKey(key);
    if (k==-1)
    {
        stringPool.release(keyv[k]);
        releaseValues(valuesv[k]);
        keyv.erase(keyv.begin()+k);
        valuesv.erase(valuesv.begin()+k);
    }
}

