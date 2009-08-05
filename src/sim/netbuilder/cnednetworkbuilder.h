//==========================================================================
//  CNEDNETWORKBUILDER.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `terms' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CNETWORKBUILDER_H
#define __CNETWORKBUILDER_H

#include <string>
#include <map>
#include <vector>
#include "cpar.h"
#include "nedelements.h"

class cModule;
class cGate;
class cChannel;
class cNEDDeclaration;

#define MAX_LOOP_NESTING 32


/**
 * Stateless object to assist in building the network, based on NED files.
 *
 * @ingroup NetworkBuilder
 */
class cNEDNetworkBuilder
{
  protected:
    // the current NED declaration we're working with. Stored here to
    // avoid having to pass it around as a parameter.
    cNEDDeclaration *currentDecl;

    // stack of loop variables
    struct {const char *varname; int value;} loopVarStack[MAX_LOOP_NESTING];
    int loopVarSP;

    // submodule pointers. This is an optimization because cModule::submodule()
    // is slow if there are very large submodule vectors.
    typedef std::vector<cModule*> ModulePtrVector;
    typedef std::map<std::string,ModulePtrVector> SubmodMap;
    SubmodMap submodMap;

  protected:
    cModule *_submodule(cModule *parentmodp, const char *submodname, int idx=-1);
    void addSubmodulesAndConnections(cModule *modp);
    bool superTypeAllowsUnconnected() const;
    void buildRecursively(cModule *modp, cNEDDeclaration *decl);
    cModuleType *findAndCheckModuleType(const char *modtypename, cModule *modp, const char *submodname);
    void addSubmodule(cModule *modp, SubmoduleNode *submod);
    void setDisplayString(cModule *module);
    void setConnDisplayString(cGate *srcgatep);
    static cPar::Type translateParamType(int t);
    static cGate::Type translateGateType(int t);
    void doParams(cComponent *component, ParametersNode *paramsNode, bool isSubcomponent);
    void doGates(cModule *component, GatesNode *gatesNode, bool isSubcomponent);
    void assignSubcomponentParams(cComponent *subcomponent, NEDElement *subcomponentNode);
    void setupGateVectors(cModule *submodule, NEDElement *submoduleNode);
    cGate *getFirstUnusedParentModGate(cModule *mod, const char *gatename);
    cGate *getFirstUnusedSubmodGate(cModule *mod, const char *gatename);

    void addConnectionOrConnectionGroup(cModule *modp, NEDElement *connOrConnGroup);
    void doConnOrConnGroupBody(cModule *modp, NEDElement *connOrConnGroup, NEDElement *loopOrCondition);
    void doLoopOrCondition(cModule *modp, NEDElement *loopOrCondition);
    void doAddConnOrConnGroup(cModule *modp, NEDElement *connOrConnGroup);
    void doAddConnection(cModule *modp, ConnectionNode *conn);

    cGate *resolveGate(cModule *modp, const char *modname, ExpressionNode *modindex,
                       const char *gatename, ExpressionNode *gateindex, bool isplusplus);
    cChannel *createChannel(ChannelSpecNode *channelspec, cModule *parentmodp);
    cChannelType *findAndCheckChannelType(const char *channeltypename);
    ExpressionNode *findExpression(NEDElement *node, const char *exprname);
    cParValue *getOrCreateExpression(ExpressionNode *exprNode, cPar::Type type, bool inSubcomponentScope);
    long evaluateAsLong(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope);
    bool evaluateAsBool(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope);
    std::string evaluateAsString(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope);

  public:
    /** Constructor */
    cNEDNetworkBuilder() {}

    /**
     * Adds parameters from the given NED declaration. Invoked from cDynamicModule.
     */
    void addParametersTo(cComponent *component, cNEDDeclaration *decl);

    /**
     * Adds gates to the module from the given NED declaration. This should be
     * called AFTER all parameters have been set, because gate vector sizes
     * may depend on parameter values.
     * Invoked from cDynamicModule.
     */
    void addGatesTo(cModule *module, cNEDDeclaration *decl);

    /**
     * Builds submodules and internal connections, based on the info in the
     * passed NEDElement tree. Invoked from cDynamicModule.
     */
    void buildInside(cModule *module, cNEDDeclaration *decl);
};

#endif





