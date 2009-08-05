%module Simkernel

//
// PROBLEMS:
//
// Crash scenario:
// 1. create a new cDisplayString() object in Java
// 2. pass it to cModule::setDisplayString()
// 3. when the Java object gets garbage collected, it'll delete the underlying C++ object
// 4. cModule will crash when it tries to access the display string object
// Solution: disown typemap or obj.disown() java method
//
// Memory leak (reverse scenario of the above):
// 1. call a C++ method from Java
// 2. C++ method creates and returns a new object
// 3. its Java proxy won't be owner, so C++ object will never get deleted
// Solution: use %newobject
//

%pragma(java) jniclasscode=%{
  static {
    try {
      System.loadLibrary("simkernel");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load. \n" + e);
      System.exit(1);
    }
  }
%}


%{
#include "omnetpp.h"
#include "src/javaenv/javaenv.h"

// accessor for global vars
inline cSimulation *getSimulation() {return &simulation;}
inline cEnvir *getEV() {return &ev;}

#include <direct.h>
inline void changeToDir(const char *dir)  //XXX
{
    printf("changing to: %s\n", dir);
    _chdir(dir);

    //char buffer[_MAX_PATH];
    //if (_getcwd( buffer, _MAX_PATH)==NULL)
    //   strcpy(buffer,"???");
    //printf("current working directory: %s\n", buffer);
}

inline void evSetup(const char *inifile) { //XXX
    char *argv[] = {"exe", "-f", (char *)inifile, NULL};
    int argc = 3;
    ev.setup(argc, argv);
}

%}

%include "std_common.i"
%include "std_string.i"

#pragma SWIG nowarn=516;  // "Overloaded method x ignored. Method y used."

// ignore/rename some operators (some have method equivalents)
%rename(assign) operator=;
%rename(plusPlus) operator++;
%ignore operator +=;
%ignore operator [];
%ignore operator <<;
%ignore operator ();

// ignore conversion operators (they all have method equivalents)
%ignore operator bool;
%ignore operator const char *;
%ignore operator char;
%ignore operator unsigned char;
%ignore operator int;
%ignore operator unsigned int;
%ignore operator long;
%ignore operator unsigned long;
%ignore operator double;
%ignore operator long double;
%ignore operator void *;
%ignore operator cObject *;
%ignore operator cXMLElement *;
%ignore cSimulation::operator=;
%ignore cEnvir::printf;

// ignore methods that are useless from Java
%ignore writeContents;
%ignore netPack;
%ignore netUnpack;

// ignore non-inspectable classes
%ignore cCommBuffer;
%ignore cContextSwitcher;
%ignore cContextTypeSwitcher;
//%ignore cEnvir;
%ignore cConfiguration;
%ignore cOutputVectorManager;
%ignore cOutputScalarManager;
%ignore cOutputSnapshotManager;
%ignore cScheduler;
%ignore cRealTimeScheduler;
%ignore cParsimCommunications;
%ignore ModNameParamResolver;
%ignore StringMapParamResolver;

%ignore simulation;
%ignore ev;


// typemaps to wrap Javaenv::setJCallback(JNIEnv *jenv, jobject jcallbackobj):
// %typemap(in, numinputs=0): unfortunately, generated java code doesn't compile
%typemap(in) JNIEnv *jenv {
    $1 = jenv;
}
%typemap(in) jobject jcallbackobj {
    $1 = j$1;
}

%rename cObject::name getName;
%rename cObject::fullName getFullName;
%rename cObject::fullPath getFullPath;

%typemap(javainterfaces) cSimulation "org.omnetpp.common.simulation.model.IRuntimeSimulation";
%rename cSimulation::systemModule getRootModule;
%rename cSimulation::moduleByPath getModuleByPath;
%rename cSimulation::module getModuleByID;

%typemap(javainterfaces) cModule "org.omnetpp.common.simulation.model.IRuntimeModule";
%rename cModule::id getId;
%rename cModule::index getIndex;
%rename cModule::size getSize;
%rename cModule::parentModule getParentModule;
%rename cModule::submodule getSubmodule;
%rename cModule::gates getNumGates;
%rename cModule::gate getGate;
%typemap(javaimports) cModule
  "import org.omnetpp.common.displaymodel.DisplayString;\n"
  "import org.omnetpp.common.displaymodel.IDisplayString;";
%extend cModule {
  const char *getTypeName() {
    return self->moduleType()->name();
  }
}
%typemap(javacode) cModule %{
  public IDisplayString getDisplayString() {
    return new DisplayString(null, null, displayString().getString());
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + getId();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final cModule other = (cModule) obj;
    if (getId() != other.getId())
      return false;
    return true;
  }
%};

%typemap(javainterfaces) cGate "org.omnetpp.common.simulation.model.IRuntimeGate";
%rename cGate::id getId;
%rename cGate::index getIndex;
%rename cGate::size getSize;
%rename cGate::ownerModule getOwnerModule;
%typemap(javaimports) cGate
  "import org.omnetpp.common.displaymodel.DisplayString;\n"
  "import org.omnetpp.common.displaymodel.IDisplayString;";
%typemap(javacode) cGate %{
  public IDisplayString getDisplayString() {
    return new DisplayString(null, null, displayString().getString());
  }
%};

%typemap(javainterfaces) cMessage "org.omnetpp.common.simulation.model.IRuntimeMessage";
%rename cMessage::kind getKind;
%rename cMessage::priority getPriority;
%rename cMessage::length getLength;
%rename cMessage::senderModuleId getSenderModuleId;
%rename cMessage::senderGateId getSenderGateId;
%rename cMessage::arrivalModuleId getArrivalModuleId;
%rename cMessage::arrivalGateId getArrivalGateId;
%rename cMessage::sendingTime getSendingTime;
%rename cMessage::arrivalTime getArrivalTime;
%rename cMessage::id getId;
%rename cMessage::treeId getTreeId;
%rename cMessage::encapsulationId getEncapsulationId;
%rename cMessage::encapsulationTreeId getEncapsulationTreeId;

// SWIG doesn't understand nested classes, turn off corresponding warnings
//%warnfilter(312) cTopology::Node; -- this doesn't seem to work
//%warnfilter(312) cTopology; -- nor this

// now include all header files
%include "defs.h"
%include "cpolymorphic.h"
%include "cobject.h"
%include "cvisitor.h"
//%include "opp_string.h"
//%include "random.h"
//%include "distrib.h"
%include "cexception.h"
%include "cdefaultlist.h"
%include "csimul.h"
//%include "ctypes.h"
//%include "carray.h"
//%include "cqueue.h"
//%include "cllist.h"
%include "globals.h"
//%include "cpar.h"
%include "cgate.h"
%include "cmessage.h"
//%include "cpacket.h"
//%include "cmsgheap.h"
%include "cmodule.h"
%include "csimplemodule.h"
//%include "cstat.h"
//%include "cdensity.h"
//%include "chist.h"
//%include "cvarhist.h"
//%include "cpsquare.h"
//%include "cksplit.h"
//%include "coutvect.h"
//%include "cdetect.h"
//%include "ctopo.h"
//%include "cfsm.h"
//%include "protocol.h"
//%include "cenum.h"
//%include "cstruct.h"
//%include "cchannel.h"
%include "cdispstr.h"
//%include "cxmlelement.h"
%include "cenvir.h"

%include "src/javaenv/javaenv.h"

//%include "util.h" -- no need to wrap
//%include "macros.h" -- no need to wrap
//%include "cwatch.h" -- no need to wrap
//%include "cstlwatch.h" -- no need to wrap
//%include "onstartup.h" -- no need to wrap
//%include "envirext.h" -- no need to wrap
//%include "cconfig.h" -- no need to wrap
//%include "cstrtokenizer.h" -- no need to wrap
//%include "cscheduler.h" -- no need to wrap
//%include "compat.h" -- no need to wrap
//%include "cparsimcomm.h" -- no need to wrap
//%include "ccommbuffer.h" -- no need to wrap
//%include "crng.h" -- no need to wrap


// refinements
cSimulation *getSimulation();
cEnvir *getEV();
void evSetup(const char *inifile); //XXX
void changeToDir(const char *dir); //XXX

