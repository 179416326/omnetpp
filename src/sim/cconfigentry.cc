//=========================================================================
//  CCONFIGENTRY.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2003-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#include "cconfigentry.h"


cConfigEntry::cConfigEntry(const char *name, const char *section, bool isGlobal, Type type,
                           const char *unit, const char *defaultValue, const char *description) :
cNoncopyableOwnedObject(name)
{
    section_ = section;
    isGlobal_ = isGlobal;
    if (type==CFG_TIME) {
        type_ = CFG_DOUBLE;
        unit_ = "s";
    } else {
        type_ = type;
        unit_ = unit ? unit : "";
    }
    if (type_==CFG_BOOL && defaultValue)
        defaultValue = (defaultValue[0]=='0' || defaultValue[0]=='f') ? "false" : "true";
    defaultValue_ = defaultValue ? defaultValue : "";
    description_ = description ? description : "";
}

const char *cConfigEntry::fullName() const
{
    fullname_ = section_ + "::" + name();
    return fullname_.c_str();
}

std::string cConfigEntry::info() const
{
    std::stringstream out;
    out << (isGlobal_ ? "global" : "per-run");
    out << ", type=" << typeName(type_);
    if (!unit_.empty()) out << ", unit=\"" << unit_ << "\"";
    if (!defaultValue_.empty()) out << ", default=\"" << defaultValue_ << "\"";
    if (!description_.empty()) out << ", hint: " << description_;
    return out.str();
}

const char *cConfigEntry::typeName(Type type)
{
    switch (type)
    {
        case CFG_BOOL:      return "bool";
        case CFG_INT:       return "int";
        case CFG_DOUBLE:    return "double";
        case CFG_STRING:    return "string";
        case CFG_FILENAME:  return "filename";
        case CFG_FILENAMES: return "filenames";
        case CFG_CUSTOM:    return "custom";
        default:            return "???";
    }
}

