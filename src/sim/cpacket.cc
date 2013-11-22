//========================================================================
//  CPACKET.CC - part of
//                 OMNeT++/OMNEST
//              Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <sstream>
#include "globals.h"
#include "cpacket.h"
#include "csimplemodule.h"

#ifdef WITH_PARSIM
#include "ccommbuffer.h"
#endif

USING_NAMESPACE

using std::ostream;

// comment out to disable reference-counting for encapsulated messages
#define REFCOUNTING

Register_Class(cPacket);

cPacket::cPacket(const cPacket& pkt) : cMessage(pkt)
{
    encapmsg = NULL;
    sharecount = 0;
    copy(pkt);
}

cPacket::cPacket(const char *name, short k, int64 l) : cMessage(name, k)
{
    len = l;
    encapmsg = NULL;
    duration = 0;
    sharecount = 0;
}

cPacket::~cPacket()
{
    if (encapmsg)
#ifdef REFCOUNTING
        _deleteEncapMsg();
#else
        dropAndDelete(encapmsg);
#endif
}

std::string cPacket::info() const  //FIXME revise
{
//    if (tomod<0)
//        return std::string("(new msg)");

    std::stringstream out;
//    const char *deletedstr = "<deleted module>";
//
//    if (delivd > simulation.getSimTime())
//    {
//        // if it arrived in the past, dt is usually unimportant, don't print it
//        out << "at T=" << delivd << ", in dt=" << (delivd - simulation.getSimTime()) << "; ";
//    }
//
//#define MODNAME(modp) ((modp) ? (modp)->getFullPath().c_str() : deletedstr)
//    if (getKind()==MK_STARTER)
//    {
//        cModule *tomodp = simulation.getModule(tomod);
//        out << "starter for " << MODNAME(tomodp) << " (id=" << tomod << ") ";
//    }
//    else if (getKind()==MK_TIMEOUT)
//    {
//        cModule *tomodp = simulation.getModule(tomod);
//        out << "timeoutmsg for " << MODNAME(tomodp) << " (id=" << tomod << ") ";
//    }
//    else if (frommod==tomod)
//    {
//        cModule *tomodp = simulation.getModule(tomod);
//        out << "selfmsg for " << MODNAME(tomodp) << " (id=" << tomod << ") ";
//    }
//    else
//    {
//        cModule *frommodp = simulation.getModule(frommod);
//        cModule *tomodp = simulation.getModule(tomod);
//        out << "src=" << MODNAME(frommodp) << " (id=" << frommod << ") ";
//        out << " dest=" << MODNAME(tomodp) << " (id=" << tomod << ") ";
//    }
//#undef MODNAME
//
//    if (encapmsg)
//        // #ifdef REFCOUNTING const_cast<cPacket *>(this)->_detachEncapMsg();  // see _detachEncapMsg() comment why this might be needed
//        out << "  encapsulates: (" << encapmsg->getClassName() << ")" << encapmsg->getFullName();
//
//    if (ctrlp)
//        out << "  control info: (" << ctrlp->getClassName() << ") " << ctrlp->getFullName() << "\n";
//
    return out.str();
}

void cPacket::forEachChild(cVisitor *v)
{
    cMessage::forEachChild(v);

    if (encapmsg)
    {
#ifdef REFCOUNTING
        _detachEncapMsg();  // see method comment why this is needed
#endif
        v->visit(encapmsg);
    }
}

std::string cPacket::detailedInfo() const
{
    return "";  // all fields are available via reflection, no point in repeating them here
}

void cPacket::parsimPack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw cRuntimeError(this,E_NOPARSIM);
#else
    cMessage::parsimPack(buffer);
    buffer->pack(len);
    buffer->pack(duration);
    if (buffer->packFlag(encapmsg!=NULL))
        buffer->packObject(encapmsg);
#endif
}

void cPacket::parsimUnpack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw cRuntimeError(this,E_NOPARSIM);
#else
    ASSERT(sharecount==0);
    cMessage::parsimUnpack(buffer);
    buffer->unpack(len);
    buffer->unpack(duration);
    if (buffer->checkFlag())
        take(encapmsg = (cPacket *) buffer->unpackObject());
#endif
}

cPacket& cPacket::operator=(const cPacket& msg)
{
    if (this==&msg) return *this;
    cMessage::operator=(msg);
    copy(msg);
    return *this;
}

void cPacket::copy(const cPacket& msg)
{

#ifdef REFCOUNTING
    if (sharecount!=0)
        throw cRuntimeError(this,"operator=(): this message is refcounted (shared between "
                                 "several messages), it is forbidden to change it");
#endif

    len = msg.len;
    duration = msg.duration;

#ifndef REFCOUNTING
    dropAndDelete(encapmsg);
    if (msg.encapmsg)
        take(encapmsg = (cPacket *)msg.encapmsg->dup());
    else
        encapmsg = NULL;
#else
    if (encapmsg)
        _deleteEncapMsg();
    encapmsg = msg.encapmsg;
    if (encapmsg && ++encapmsg->sharecount==0)   // sharecount overflow
    {
        --encapmsg->sharecount;
        take(encapmsg = (cPacket *)encapmsg->dup());
    }
#endif
}

#ifdef REFCOUNTING
void cPacket::_deleteEncapMsg()
{
    if (encapmsg->sharecount>0)
    {
        encapmsg->sharecount--;
        if (encapmsg->ownerp == this)
            encapmsg->ownerp = NULL;
    }
    else
    {
        // note: dropAndDelete(encapmsg) cannot be used, because due to sharecounting
        // ownerp is not valid (may be any former owner, possibly deleted since then)
        encapmsg->ownerp = NULL;
        delete encapmsg;
    }
}
#endif

#ifdef REFCOUNTING
void cPacket::_detachEncapMsg()
{
    if (encapmsg->sharecount>0)
    {
        // "de-share" object - create our own copy
        encapmsg->sharecount--;
        if (encapmsg->ownerp == this)
            encapmsg->ownerp = NULL;
        take(encapmsg = (cPacket *)encapmsg->dup());
    }
    else
    {
        // note: due to sharecounting, ownerp may be pointing to a previous owner -- fix it
        encapmsg->ownerp = this;
    }
}
#endif

void cPacket::setBitLength(int64 l)
{
    if (l<0)
        throw cRuntimeError(this,"setBitLength(): negative length %" INT64_PRINTF_FORMAT "d", l);
    len = l;
}

void cPacket::addBitLength(int64 l)
{
    len += l;
    if (len<0)
        throw cRuntimeError(this,"addBitLength(): length became negative (%" INT64_PRINTF_FORMAT ") after adding %" INT64_PRINTF_FORMAT "d", len, l);
}

void cPacket::encapsulate(cPacket *msg)
{
    if (encapmsg)
        throw cRuntimeError(this,"encapsulate(): another message already encapsulated");

    if (msg)
    {
        if (msg->getOwner()!=simulation.getContextSimpleModule())
            throw cRuntimeError(this,"encapsulate(): not owner of message (%s)%s, owner is (%s)%s",
                                msg->getClassName(), msg->getFullName(),
                                msg->getOwner()->getClassName(), msg->getOwner()->getFullPath().c_str());
        take(encapmsg = msg);
#ifdef REFCOUNTING
        ASSERT(encapmsg->sharecount==0);
#endif
        len += encapmsg->len;
    }
}

cPacket *cPacket::decapsulate()
{
    if (!encapmsg)
        return NULL;
    if (len>0)
        len -= encapmsg->len;
    if (len<0)
        throw cRuntimeError(this,"decapsulate(): packet length is smaller than encapsulated packet");

#ifdef REFCOUNTING
    if (encapmsg->sharecount>0)
    {
        encapmsg->sharecount--;
        if (encapmsg->ownerp == this)
            encapmsg->ownerp = NULL;
        cPacket *msg = encapmsg->dup();
        encapmsg = NULL;
        return msg;
    }
    encapmsg->ownerp = this;
#endif
    cPacket *msg = encapmsg;
    encapmsg = NULL;
    if (msg) drop(msg);
    return msg;
}

cPacket *cPacket::getEncapsulatedPacket() const
{
#ifdef REFCOUNTING
    // encapmsg may be shared (sharecount>0) -- we'll make our own copy,
    // so that other messages are not affected in case the user modifies
    // the encapsulated message via the returned pointer.
    // Trick: this is a const method, so we can only do changes via a
    // non-const copy of the 'this' pointer.
    if (encapmsg)
        const_cast<cPacket *>(this)->_detachEncapMsg();
#endif
    return encapmsg;
}

bool cPacket::hasEncapsulatedPacket() const
{
    return encapmsg != NULL;
}

long cPacket::getEncapsulationId() const
{
    // find innermost msg. Note: don't use getEncapsulatedPacket() because it does copy-on-touch of shared msgs
    const cPacket *msg = this;
    while (msg->encapmsg)
        msg = msg->encapmsg;
    return msg->getId();
}

long cPacket::getEncapsulationTreeId() const
{
    // find innermost msg. Note: don't use getEncapsulatedPacket() because it does copy-on-touch of shared msgs
    const cPacket *msg = this;
    while (msg->encapmsg)
        msg = msg->encapmsg;
    return msg->getTreeId();
}


