//=========================================================================
//  CARRAY.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//   Member functions of
//    cArray : flexible array to store cObject objects
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>           // sprintf
#include <string.h>          // memcmp, memcpy, memset
#include "carray.h"
#include "globals.h"
#include "cexception.h"

#ifdef WITH_PARSIM
#include "ccommbuffer.h"
#endif

//=== Registration
Register_Class(cArray);

void cArray::Iterator::init(const cArray& a, bool athead)
{
    array = const_cast<cArray *>(&a); // Khmm... I don't want a separate Const_Iterator as well...

    if (athead)
    {
        // fast-forward to first non-empty slot
        k = 0;
        while (!array->get(k) && k<array->items())
            k++;

    }
    else
    {
        // rewind to first non-empty slot
        k = array->items()-1;
        while (!array->get(k) && k>=0)
            k--;
    }
}

cObject *cArray::Iterator::operator++(int)
{
    if (k<0 || k>=array->items())
        return NULL;
    cObject *obj = array->get(k);

    k++;
    while (!array->get(k) && k<array->items())
        k++;
    return obj;
}

cObject *cArray::Iterator::operator--(int)
{
    if (k<0 || k>=array->items())
        return NULL;
    cObject *obj = array->get(k);
    k--;
    while (!array->get(k) && k>=0)
        k--;
    return obj;
}

//----

cArray::cArray(const cArray& list) : cObject()
{
    vect=NULL;
    last=-1;
    setName( list.name() );
    operator=(list);
}

cArray::cArray(const char *name, int siz, int dt) :
cObject( name )
{
    tkownership = true;
    delta = Max(1,dt);
    size = Max(siz,0);
    firstfree = 0;
    last = -1;
    vect = new cObject *[size];
    for (int i=0; i<size; i++) vect[i]=NULL;
}

cArray::~cArray()
{
    clear();
    delete [] vect;
}

cArray& cArray::operator=(const cArray& list)
{
    if (this==&list) return *this;

    clear();

    cObject::operator=( list );

    tkownership = list.tkownership;
    size = list.size;
    delta = list.delta;
    firstfree = list.firstfree;
    last = list.last;
    delete [] vect;
    vect = new cObject *[size];
    if (vect) memcpy( vect, list.vect, size * sizeof(cObject *) );

    for (int i=0; i<=last; i++)
        if (vect[i] && vect[i]->owner()==const_cast<cArray*>(&list))
            take( vect[i] = (cObject *)vect[i]->dup() );
    return *this;
}

std::string cArray::info() const
{
    if (last==-1)
        return std::string("empty");
    std::stringstream out;
    out << "size=" << last+1;
    return out.str();
}

void cArray::forEachChild(cVisitor *v)
{
    for (int i=0; i<=last; i++)
        if (vect[i])
            v->visit(vect[i]);
}

void cArray::netPack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw new cRuntimeError(this,eNOPARSIM);
#else
    cObject::netPack(buffer);

    buffer->pack(size);
    buffer->pack(delta);
    buffer->pack(firstfree);
    buffer->pack(last);

    for (int i = 0; i <= last; i++)
    {
        if (buffer->packFlag(vect[i]!=NULL))
        {
            if (vect[i]->owner() != this)
                throw new cRuntimeError(this,"netPack(): cannot transmit pointer to \"external\" object");
            buffer->packObject(vect[i]);
        }
    }
#endif
}

void cArray::netUnpack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw new cRuntimeError(this,eNOPARSIM);
#else
    cObject::netUnpack(buffer);

    delete [] vect;

    buffer->unpack(size);
    buffer->unpack(delta);
    buffer->unpack(firstfree);
    buffer->unpack(last);

    vect = new cObject *[size];
    for (int i = 0; i <= last; i++)
    {
        if (!buffer->checkFlag())
            vect[i] = NULL;
        else
            take(vect[i] = buffer->unpackObject());
    }
#endif
}

void cArray::clear()
{
    for (int i=0; i<=last; i++)
    {
        if (vect[i] && vect[i]->owner()==this)
            dropAndDelete(vect[i]);
        vect[i] = NULL;  // this is not strictly necessary
    }
    firstfree = 0;
    last = -1;
}

int cArray::add(cObject *obj)
{
    if (!obj)
        throw new cRuntimeError(this,"cannot insert NULL pointer");

    int retval;
    if (takeOwnership()) take( obj );
    if (firstfree < size)  // fits in current vector
    {
        vect[firstfree] = obj;
        retval = firstfree;
        last = Max(last,firstfree);
        do {
            firstfree++;
        } while (firstfree<=last && vect[firstfree]!=NULL);
    }
    else // must allocate bigger vector
    {
        cObject **v = new cObject *[size+delta];
        memcpy(v, vect, sizeof(cObject*)*size );
        memset(v+size, 0, sizeof(cObject*)*delta);
        delete [] vect;
        vect = v;
        vect[size] = obj;
        retval = last = size;
        firstfree = size+1;
        size += delta;
    }
    return retval;
}

int cArray::addAt(int m, cObject *obj)
{
    if (!obj)
        throw new cRuntimeError(this,"cannot insert NULL pointer");

    if (m<size)  // fits in current vector
    {
        if (m<0)
            throw new cRuntimeError(this,"addAt(): negative position %d",m);
        if (vect[m]!=NULL)
            throw new cRuntimeError(this,"addAt(): position %d already used",m);
        vect[m] = obj;
        if (takeOwnership()) take(obj);
        last = Max(m,last);
        if (firstfree==m)
            do {
                firstfree++;
            } while (firstfree<=last && vect[firstfree]!=NULL);
    }
    else // must allocate bigger vector
    {
        cObject **v = new cObject *[m+delta];
        memcpy(v, vect, sizeof(cObject*)*size);
        memset(v+size, 0, sizeof(cObject*)*(m+delta-size));
        delete [] vect;
        vect = v;
        vect[m] = obj;
        if (takeOwnership()) take(obj);
        size = m+delta;
        last = m;
        if (firstfree==m)
            firstfree++;
    }
    return m;
}

int cArray::set(cObject *obj)
{
    if (!obj)
        throw new cRuntimeError(this,"cannot insert NULL pointer");

    int i = find(obj->name());
    if (i<0)
    {
        return add(obj);
    }
    else
    {
        if (vect[i]->owner()==this)
            dropAndDelete(vect[i]);
        vect[i] = obj;
        if (takeOwnership())
            take(obj);
        return i;
    }
}

int cArray::find(cObject *obj) const
{
    int i;
    for (i=0; i<=last; i++)
        if (vect[i]==obj)
            break;
    if (i<=last)
        return i;
    else
        return -1;
}

int cArray::find(const char *objname) const
{
    int i;
    for (i=0; i<=last; i++)
        if (vect[i] && vect[i]->isName(objname))
            break;
    if (i<=last)
        return i;
    else
        return -1;
}

cObject *cArray::get(int m)
{
    if (m>=0 && m<=last && vect[m])
        return vect[m];
    else
        return NULL;
}

const cObject *cArray::get(int m) const
{
    if (m>=0 && m<=last && vect[m])
        return vect[m];
    else
        return NULL;
}

cObject *cArray::get(const char *objname)
{
    int m = find( objname );
    if (m==-1)
        return NULL;
    return get(m);
}

const cObject *cArray::get(const char *objname) const
{
    int m = find( objname );
    if (m==-1)
        return NULL;
    return get(m);
}

cObject *cArray::remove(const char *objname)
{
    int m = find( objname );
    if (m==-1)
        return NULL;
    return remove(m);
}

cObject *cArray::remove(cObject *obj)
{
    if (!obj) return NULL;

    int m = find( obj );
    if (m==-1)
        return NULL;
    return remove(m);
}

cObject *cArray::remove(int m)
{
    if (m<0 || m>last || vect[m]==NULL)
        return NULL;

    cObject *obj = vect[m]; vect[m] = NULL;
    firstfree = Min(firstfree, m);
    if (m==last)
        do {
            last--;
        } while (last>=0 && vect[last]==NULL);
    if (obj->owner()==this)  drop( obj );
    return obj;
}


