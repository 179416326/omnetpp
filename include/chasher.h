//==========================================================================
//   CHASHER.H  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CHASHER_H
#define __CHASHER_H

#include <string.h>
#include <stdlib.h>
#include "defs.h"
#include "cobject.h"
#include "cexception.h"

typedef __int64 int64;
typedef unsigned __int64 uint64;
typedef unsigned int uint32;   //FIXME into defs.h or something???

/**
 * Utility class to calculate the "fingerprint" of a simulation.
 *
 * We're trying to achieve that the same simulation gives the same fingerprint
 * on a 32-bit machine and on a 64-bit machine. Longs can be either 32-bit or
 * 64-bit, so we always convert them to 64 bits. We don't try to convert
 * endianness, it would be too costly.
 */
class SIM_API cHasher : noncopyable
{
  private:
    uint32 value;
    void merge(uint32 x) {
        // rotate value left by one bit, and xor with new data
        uint32 carry = (value & 0x80000000U) >> 31;
        value = ((value<<1)|carry) ^ x;
    }

    void merge2(uint64 x) {
        uint32 *p = (uint32 *)&x;
        merge(p[0]);
        merge(p[1]);
    }

  public:
    /**
     * Constructor.
     */
    cHasher() {ASSERT(sizeof(uint32)==4); ASSERT(sizeof(double)==8); value = 0;}

    /** @name Updating the hash */
    //@{
    void reset() {value = 0;}
    void add(const char *p, size_t length);
    void add(int d)   {merge((uint32)d);}
    void add(short d) {merge((uint32)d);}
    void add(char d)  {merge((uint32)d);}
    void add(long d)  {merge((uint64)d);}
    void add(unsigned int d)   {merge((uint32)d);}
    void add(unsigned short d) {merge((uint32)d);}
    void add(unsigned char d)  {merge((uint32)d);}
    void add(unsigned long d)  {merge((uint64)(int64)d);} // sign! TODO regression test! as this is tricky
    void add(double d)  {merge(*(uint64 *)&d);}
    void add(const char *s)  {if (s) add(s, strlen(s)+1); else add(0);}
    //@}

    /** @name Obtaining the result */
    //@{
    /**
     * Returns the hash value.
     */
    uint32 getHash() const {return value;}

    /**
     * Converts the given string to a numeric fingerprint value. The object is
     * not changed. Throws an error if the string does not contain a valid
     * fingerprint.
     */
    uint32 parse(const char *fingerprint) const;

    /**
     * Parses the given fingerprint string, and compares it to the stored hash.
     */
    bool equals(const char *fingerprint) const;

    /**
     * Returns the textual representation (hex string) of the stored hash.
     */
    std::string toString() const;
    //@}
};

#endif


