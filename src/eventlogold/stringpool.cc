//=========================================================================
//  STRINGPOOL.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "stringpool.h"

StringPool::StringPool()
{
}

StringPool::~StringPool()
{
    for (StringSet::iterator it = pool.begin(); it!=pool.end(); ++it)
        delete [] *it;
    //XXX this one may be faster, test:
    //while (pool.size()>0) {
    //    delete [] *pool.begin();
    //    pool.erase(pool.begin());
    //}
}

const char *StringPool::get(const char *s)
{
    if (s==NULL)
        return ""; // must not be NULL because SWIG-generated code will crash!
    StringSet::iterator it = pool.find(const_cast<char *>(s));
    if (it!=pool.end())
        return *it;
    char *str = new char[strlen(s)+1];
    strcpy(str,s);
    pool.insert(str);
    return str;
}

