//==========================================================================
//  CSTRUCT.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//
//  Declaration of the following classes:
//    cStructDescriptor : meta-info about structures
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>   // sprintf
#include <stdlib.h>  // atol
#include <string.h>
#include <assert.h>
#include "cstruct.h"
#include "carray.h"
#include "cenum.h"
#include "util.h"
#include "cclassfactory.h"  // createOne()


void cStructDescriptor::long2string(long l, char *buf, int bufsize)
{
    assert(bufsize>=10);
    sprintf(buf, "%ld", l);
}

void cStructDescriptor::ulong2string(unsigned long l, char *buf, int bufsize)
{
    assert(bufsize>=10);
    sprintf(buf, "%lu", l);
}


long cStructDescriptor::string2long(const char *s)
{
    return atol(s);
}


unsigned long cStructDescriptor::string2ulong(const char *s)
{
    return (unsigned long) atol(s);
}


void cStructDescriptor::bool2string(bool b, char *buf, int bufsize)
{
    assert(bufsize>=7);
    strcpy(buf, b ? "true" : "false");
}


bool cStructDescriptor::string2bool(const char *s)
{
    return s[0]=='t' || s[0]=='T' || s[0]=='y' || s[0]=='Y' || s[0]=='1';
}


void cStructDescriptor::double2string(double d, char *buf, int bufsize)
{
    assert(bufsize>=20);
    sprintf(buf, "%f", d);
}


double cStructDescriptor::string2double(const char *s)
{
    return atof(s);
}


void cStructDescriptor::enum2string(long e, const char *enumname, char *buf, int bufsize)
{
    assert(bufsize>=30); // FIXME: very crude check

    sprintf(buf,"%ld",e);
    cEnum *enump = cEnum::find(enumname);
    if (!enump) {
        // this enum type is not registered
        return;
    }
    const char *s = enump->stringFor(e);
    if (!s) {
        // no string for this numeric value
        sprintf(buf+strlen(buf), " (unknown)");
        return;
    }
    sprintf(buf+strlen(buf), " (%s)",s);
}


long cStructDescriptor::string2enum(const char *s, const char *enumname)
{
    // return zero if string cannot be parsed

    // try to interpret as numeric value
    if (s[0]>=0 && s[0]<=9) {
        return atol(s);
    }

    // try to recognize string
    cEnum *enump = cEnum::find(enumname);
    if (!enump) {
        // this enum type is not registered
        return 0;
    }

    // TBD should strip possible spaces, parens etc.
    return enump->lookup(s,0);
}

void cStructDescriptor::oppstring2string(const opp_string& str, char *buf, int buflen)
{
    strncpy(buf, str.c_str(), buflen);
    buf[buflen-1] = '\0';
}

void cStructDescriptor::string2oppstring(const char *s, opp_string& str)
{
    str = s;
}


//-----------------------------------------------------------

cStructDescriptor::cStructDescriptor(const char *_baseclassname)
{
    p = NULL;
    baseclassname = _baseclassname ? _baseclassname : "";
    baseclassdesc = _baseclassname ? createDescriptorFor(_baseclassname, NULL) : NULL;
}

cStructDescriptor::~cStructDescriptor()
{
    delete baseclassdesc;
}

void cStructDescriptor::setStruct(void *_p)
{
    p = _p;
    if (baseclassdesc)
        baseclassdesc->setStruct(p);
}

cStructDescriptor *cStructDescriptor::createDescriptorFor(const char *classname, void *p)
{
    // produce name of struct descriptor class
    std::string sdclass = std::string(classname) + "Descriptor";

    // create and initialize a structure descriptor object
    cStructDescriptor *sd = (cStructDescriptor *) createOneIfClassIsKnown(sdclass.c_str());
    if (!sd)
        return NULL;
    sd->setStruct(p);
    return sd;
}


