//==========================================================================
//  CSTRINGPOOL.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CSTRINGPOOL_H
#define __CSTRINGPOOL_H

#include "defs.h"
#include "util.h"
#include <map>

/**
 * For saving memory on the storage of (largely) constant strings that occur in
 * many instances during runtime: module names, gate names, property names,
 * keys and values, etc. These strings can be stored in a cStringPool as one
 * shared instance. Strings in the cStringPool are refcounted.
 * (See Flyweight GoF pattern.)
 */
class cStringPool
{
  protected:
    struct strless {
        bool operator()(const char *s1, const char *s2) const {
            int d0 = *s1 - *s2;
            if (d0<0) return true; else if (d0>0) return false;
            return strcmp(s1,s2) < 0;  // note: (s1+1,s2+1) not good because either strings may be empty
        }
    };
    typedef std::map<char *,int,strless> StringIntMap;  // map<string,refcount>
    StringIntMap pool;

  public:
    cStringPool();
    ~cStringPool();

    /**
     * Returns pointer to the pooled copy of the given string, and increments
     * its reference count. get() and release() must occur in pairs.
     * Passing NULL is OK.
     */
    const char *get(const char *s);

    /**
     * The parameter must a pointer returned by get(). It decrements the
     * reference count and frees the pooled string if it reaches zero.
     * Passing NULL is OK.
     */
    void release(const char *s);
};

#endif


