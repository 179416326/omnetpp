//
// This file is part of an OMNeT++/OMNEST simulation example.
//
// Copyright (C) 1992-2005 Andras Varga
//
// This file is distributed WITHOUT ANY WARRANTY. See the file
// `license' for details on this and other legal matters.
//


#ifndef __ASERVER_H_
#define __ASERVER_H_

#include <omnetpp.h>


/**
 * Aloha server; see NED file for more info.
 */
class AServer : public cSimpleModule
{
  private:
    // state variables, event pointers
    bool channelBusy;
    cMessage *endRxEvent;
    double txRate;

    long currentCollisionNumFrames;
    simtime_t recvStartTime;

    // statistics
    long totalFrames;
    long collidedFrames;
    simtime_t totalReceiveTime;
    simtime_t totalCollisionTime;
    double currentChannelUtilization;

    cOutVector collisionMultiplicityVector;
    cOutVector collisionLengthVector;
    cOutVector channelUtilizationVector;

  public:
    AServer();
    virtual ~AServer();

  protected:
    virtual void initialize();
    virtual void handleMessage(cMessage *msg);
    virtual void finish();
};

#endif

