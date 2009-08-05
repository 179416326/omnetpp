//==========================================================================
//  CMDENV.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//
//  Declaration of the following classes:
//    TCmdenvApp:         command line simulation application
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CMDENV_H
#define __CMDENV_H

#include "csimulation.h"
#include "omnetapp.h"
#include <signal.h>


/**
 * Command line user interface.
 */
class TCmdenvApp : public TOmnetApp
{
   protected:
     // new simulation options:
     opp_string opt_printnumruns;
     opp_string opt_configname;
     opp_string opt_runstoexec;
     bool opt_printconfigdetails;
     bool opt_printconfigdetails2;
     unsigned opt_extrastack_kb;
     opp_string opt_outputfile;

     bool opt_expressmode;
     bool opt_autoflush; // all modes
     bool opt_modulemsgs;  // if normal mode
     bool opt_eventbanners; // if normal mode
     bool opt_eventbanner_details; // if normal mode
     bool opt_messagetrace; // if normal mode
     int opt_status_frequency_ev; // if express mode
     bool opt_perfdisplay; // if express mode

     // set to true on SIGINT/SIGTERM signals
     static bool sigint_received;

     // stream to write output to
     FILE *fout;

   public:
     TCmdenvApp(ArgList *args, cConfiguration *config);
     ~TCmdenvApp();

     // redefined virtual funcs:
     virtual void setup();
     virtual int run();
     virtual void shutdown();
     virtual void printUISpecificHelp();

     virtual void moduleCreated(cModule *newmodule);
     virtual void messageSent_OBSOLETE(cMessage *msg, cGate *directToGate);
     virtual void simulationEvent(cMessage *msg);

     virtual void readOptions();
     virtual void readPerRunOptions();

     // new functions:
     void help();
     void simulate();
     const char *progressPercentage();

     // redefined I/O functions:
     virtual bool isGUI() {return false;}
     virtual void putmsg(const char *s);
     virtual void sputn(const char *s, int n);
     virtual void flush();
     virtual bool gets(const char *promptstr, char *buf, int len=255);
     virtual int  askYesNo(const char *question);

     virtual bool idle();

     virtual unsigned extraStackForEnvir();

     // SIG_USR1 handling
     void setupSignals();
     static void signalHandler(int signum);
};

#endif

