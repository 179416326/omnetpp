//==========================================================================
//   CCOMPONENT.H  -  header for
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CCOMPONENT_H
#define __CCOMPONENT_H

#include <vector>
#include "defs.h"
#include "cownedobject.h"
#include "cpar.h"
#include "cdefaultlist.h"

class cComponentType;
class cProperties;
class cRNG;

/**
 * Common base for module and channel classes: cModule and cChannel.
 * cComponent provides parameters, properties and RNG mapping.
 *
 * @ingroup SimCore
 */
class SIM_API cComponent : public cDefaultList // noncopyable
{
    friend class cPar; // needs to call handleParameterChange()
  protected:
    cComponentType *componenttype;  // component type object

    short rngmapsize;  // size of rngmap array (RNGs>=rngmapsize are mapped one-to-one to physical RNGs)
    int *rngmap;       // maps local RNG numbers (may be NULL if rngmapsize==0)
    bool ev_enabled;   // if output from ev<< is enabled   FIXME utilize cOwnedObject::flags

    std::vector<cPar> paramv;  // stores the parameters of this component

  public:
    // internal: currently used by Cmdenv
    void setEvEnabled(bool e)  {ev_enabled = e;}
    bool isEvEnabled() {return ev_enabled;}

    // internal: invoked from within cEnvir::getRNGMappingFor(component)
    void setRNGMap(short size, int *map) {rngmapsize=size; rngmap=map;}

    // internal: sets associated cComponentType for the component;
    // called as part of the creation process.
    virtual void setComponentType(cComponentType *componenttype);

    // internal: adds a new parameter to the component; called as part of the creation process
//FIXME ADDING NEW PARAMETERS AT RUNTIME HAS TO BE PROHIBITED!!!!
    virtual void addPar(cParValue *value);

    // internal: invokes the read() method on all unset parameters
    virtual void readParams();

  protected:
    /** @name Initialization, finish and parameter change hooks.
     *
     * Initialize and finish functions may be provided by the user,
     * to perform special tasks at the beginning and the end of the simulation.
     * The functions are made protected because they are supposed
     * to be called only via callInitialize() and callFinish().
     *
     * The initialization process was designed to support multi-stage
     * initialization of compound modules (i.e. initialization in several
     * 'waves'). (Calling the initialize() function of a simple module is
     * hence a special case). The initialization process is performed
     * on a module like this. First, the number of necessary initialization
     * stages is determined by calling numInitStages(), then initialize(stage)
     * is called with <tt>0,1,...numstages-1</tt> as argument. The default
     * implementation of numInitStages() and initialize(stage) provided here
     * defaults to single-stage initialization, that is, numInitStages()
     * returns 1 and initialize(stage) simply calls initialize() if stage is 0.
     */
    //@{

    /**
     * Multi-stage initialization hook. This default implementation does
     * single-stage init, that is, calls initialize() if stage is 0.
     */
    virtual void initialize(int stage) {if (stage==0) initialize();}

    /**
     * Multi-stage initialization hook, should be redefined to return the
     * number of initialization stages required. This default implementation
     * does single-stage init, that is, returns 1.
     */
    virtual int numInitStages() const  {return 1;}

    /**
     * Single-stage initialization hook. This default implementation
     * does nothing.
     */
    virtual void initialize();

    /**
     * Finish hook. finish() is called after end of simulation, if it
     * terminated without error. This default implementation does nothing.
     */
    virtual void finish();

    /**
     * This method is called by the simulation kernel to notify the module or
     * channel that the value of an existing parameter got changed.
     * Redefining this method allows simple modules and channels to be react on
     * parameter changes, for example by re-reading the value.
     * This default implementation does nothing.
     *
     * To make it easier to write predictable simple modules, the function does
     * NOT get called during initialize() or finish(). If you need
     * notifications within those two functions as well, add the following
     * code into your initialize() and/or finish() methods:
     *
     * <pre>
     * for (int i=0; i<params(); i++)
     *     handleParameterChange(par(i).name());
     * </pre>
     *
     * Also, one must be extremely careful when changing parameters from inside
     * handleParameterChange(), to avoid creating an infinite notification loop.
     */
    virtual void handleParameterChange(const char *parname);
    //@}

  public:
    /** @name Constructors, destructor, assignment. */
    //@{
    /**
     * FIXME revise comment!!!
     * Constructor. Note that module objects should not be created directly,
     * only via their cComponentType objects. cComponentType::create() will do
     * all housekeeping tasks associated with module creation (assigning
     * an ID to the module, inserting it into the global <tt>simulation</tt>
     * object (see cSimulation), etc.).
     */
    cComponent(const char *name = NULL);

    /**
     * Destructor.
     */
    virtual ~cComponent();
    //@}

    /** @name Redefined cObject functions */
    //@{
    /**
     * Lie about the class name: we return the NED type name instead of the
     * real one, that is, "MobileHost" instead of "cCompoundModule" for example.
     */
    virtual const char *className() const;

    /**
     * Redefined to include component parameters in the traversal as well.
     */
    virtual void forEachChild(cVisitor *v);
    //@}

    /** @name Misc. */
    //@{
    /**
     * Return the properties for this component. Properties cannot be changed
     * at runtime.
     */
    cProperties *properties() const;

    /**
     * Returns the associated component type.
     */
    cComponentType *componentType() const  {return componenttype;}

    /**
     * Redefined to return true in cModule and subclasses, otherwise returns false.
     */
    virtual bool isModule() const  {return false;}

    /**
     * Returns the module containing this module/channel. This is not necessarily
     * the same object as owner(), especially for channel objects. For the system
     * module, it returns NULL.
     */
    virtual cModule *parentModule() const = 0;

    /**
     * Returns the global RNG mapped to local RNG number k. For large indices
     * (k >= map size) the global RNG k is returned, provided it exists.
     */
    cRNG *rng(int k) const  {return ev.rng(k<rngmapsize ? rngmap[k] : k);}
    //@}

    /** @name Interface for calling initialize()/finish().
     * Those functions may not be called directly, only via
     * callInitialize() and callFinish() provided here.
     */
    //@{

    /**
     * Interface for calling initialize() from outside.
     * Implements full multi-stage init for this module and its submodules.
     */
    virtual void callInitialize();

    /**
     * Interface for calling initialize() from outside.  This method includes
     * calling initialize() of contained components (submodules, channels) as well.
     * It does a single stage of initialization, and returns <tt>true</tt>
     * if more stages are required.
     */
    virtual bool callInitialize(int stage) = 0;

    /**
     * Interface for calling finish() from outside. This method includes
     * calling finish() of contained components (submodules, channels) as well.
     */
    virtual void callFinish() = 0;
    //@}

    /** @name Properties. */
    //@{
    /**
     * Return the properties for this component. Properties are locked
     * against modifications, because properties() returns a shared copy.
     */
    virtual cProperties *properties();
    //@}

    /** @name Parameters. */
    //@{

    /**
     * Returns total number of the component's parameters.
     */
    virtual int params() const  {return paramv.size();} //XXX rename to numParams

    /**
     * Returns reference to the parameter identified with its
     * index k. Throws an error if the parameter does not exist.
     */
    virtual cPar& par(int k);

    /**
     * Returns reference to the parameter identified with its
     * index k. Throws an error if the parameter does not exist.
     */
    const cPar& par(int k) const  {return const_cast<cComponent *>(this)->par(k);}

    /**
     * Returns reference to the parameter specified with its name.
     * Throws an error if the parameter does not exist.
     */
    virtual cPar& par(const char *parname);

    /**
     * Returns reference to the parameter specified with its name.
     * Throws an error if the parameter does not exist.
     */
    const cPar& par(const char *parname) const  {return const_cast<cComponent *>(this)->par(parname);}

    /**
     * Returns index of the parameter specified with its name.
     * Returns -1 if the object doesn't exist.
     */
    virtual int findPar(const char *parname) const;

    /**
     * Check if a parameter exists.
     */
    bool hasPar(const char *s) const {return findPar(s)>=0;}
    //@}
};

#endif

