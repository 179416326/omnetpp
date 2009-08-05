//==========================================================================
//   CNEDNETWORKBUILDER.CC
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


#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <iostream>

#include "cmodule.h"
#include "cgate.h"
#include "cchannel.h"
#include "cbasicchannel.h"
#include "ccomponenttype.h"
#include "clongpar.h"
#include "cboolpar.h"
#include "cstringpar.h"

#include "nedelements.h"
#include "nederror.h"

#include "nedparser.h"
#include "nedxmlparser.h"
#include "neddtdvalidator.h"
#include "nedbasicvalidator.h"
#include "nedsemanticvalidator.h"
#include "xmlgenerator.h"  // for debugging

#include "cneddeclaration.h"
#include "cnednetworkbuilder.h"
#include "cnedloader.h"
#include "cexpressionbuilder.h"
#include "nedsupport.h"
#include "commonutil.h"  // TRACE()

/*
  FIXME FIXME FIXME

  a toplevel modulra nem hivodnak meg a kovetkezok:

        setDisplayString(submodp);
        assignSubcomponentParams(submodp, submod);  --> ez kell?
        submodp->readParams();  --> ez most mar OK, finalizeParameters()-ben van

  mivel ezek az addSubmodule()-ban vannak!

  atrakni a buildInside()-ba???


  TODO connection display string
*/

inline bool strnull(const char *s)
{
    return !s || !s[0];
}

static void dump(NEDElement *node)
{
    generateXML(std::cout, node, false);
    std::cout.flush();
}

void cNEDNetworkBuilder::addParametersTo(cComponent *component, cNEDDeclaration *decl)
{
    //TRACE("addParametersTo(%s), decl=%s", component->fullPath().c_str(), decl->name()); //XXX

    // recursively add and assign super types' parameters
    if (decl->numExtendsNames() > 0)
    {
        const char *superName = decl->extendsName(0);
        cNEDDeclaration *superDecl = cNEDLoader::instance()->getDecl(superName);
        addParametersTo(component, superDecl);
    }

    // add this decl parameters / assignments
    ParametersNode *paramsNode = decl->getParametersNode();
    if (paramsNode)
    {
        currentDecl = decl; // switch "context"
        doParams(component, paramsNode, false);
    }
}

cPar::Type cNEDNetworkBuilder::translateParamType(int t)
{
    return t==NED_PARTYPE_DOUBLE ? cPar::DOUBLE :
           t==NED_PARTYPE_INT ? cPar::LONG :
           t==NED_PARTYPE_STRING ? cPar::STRING :
           t==NED_PARTYPE_BOOL ? cPar::BOOL :
           t==NED_PARTYPE_XML ? cPar::XML :
           (cPar::Type)-1;
}

cGate::Type cNEDNetworkBuilder::translateGateType(int t)
{
    return t==NED_GATETYPE_INPUT ? cGate::INPUT :
           t==NED_GATETYPE_OUTPUT ? cGate::OUTPUT :
           t==NED_GATETYPE_INOUT ? cGate::INOUT :
           (cGate::Type)-1;
}

void cNEDNetworkBuilder::doParams(cComponent *component, ParametersNode *paramsNode, bool isSubcomponent)
{
    ASSERT(paramsNode!=NULL);
    for (NEDElement *child=paramsNode->getFirstChildWithTag(NED_PARAM); child; child=child->getNextSiblingWithTag(NED_PARAM))
    {
        ParamNode *paramNode = (ParamNode *)child;
        const char *paramName = paramNode->getName();
        ExpressionNode *exprNode = paramNode->getFirstExpressionChild();

        cParValue *value = NULL;
        if (exprNode)
        {
            value = currentDecl->getCachedExpression(exprNode);
            ASSERT(!value || value->isName(paramName));
            if (!value)
            {
                cPar::Type parType = paramNode->getType()==NED_PARTYPE_NONE
                    ? component->par(paramName).type()
                    : translateParamType(paramNode->getType());

                bool isVolatile = paramNode->getType()==NED_PARTYPE_NONE
                    ? component->par(paramName).isVolatile()
                    : paramNode->getIsVolatile();

                cDynamicExpression *dynamicExpr = cExpressionBuilder().process(exprNode, isSubcomponent);
                value = cParValue::createWithType(parType);
                cExpressionBuilder::assign(value, dynamicExpr);
                value->setName(paramName);
                value->setIsShared(true);
                value->setIsInput(paramNode->getIsDefault());
                value->setIsVolatile(isVolatile);
                currentDecl->putCachedExpression(exprNode, value);
            }
        }

        // find or add parameter
        if (isSubcomponent || paramNode->getType()==NED_PARTYPE_NONE)
        {
            if (value)
            {
                // parameter must exist already
                cPar &par = component->par(paramName);
                par.reassign(value);
            }
        }
        else
        {
            // add it now
            if (!value)
            {
                value = cParValue::createWithType(translateParamType(paramNode->getType()));
                value->setName(paramName);
                value->setIsShared(false); //XXX cannot cache: there'no ExpressionNode to piggyback on!!!
                value->setIsInput(true);
                value->setIsVolatile(paramNode->getIsVolatile());
            }
            //XXX printf("   +++ adding param %s\n", paramName);
            component->addPar(value);
        }
    }
}

void cNEDNetworkBuilder::doGates(cModule *module, GatesNode *gatesNode, bool isSubcomponent)
{
    ASSERT(gatesNode!=NULL);
    for (NEDElement *child=gatesNode->getFirstChildWithTag(NED_GATE); child; child=child->getNextSiblingWithTag(NED_GATE))
    {
        GateNode *gateNode = (GateNode *)child;
        const char *gateName = gateNode->getName();
        ExpressionNode *exprNode = gateNode->getFirstExpressionChild();

        // determine gatesize
        int gatesize = -1;
        if (gateNode->getIsVector() && exprNode)
        {
            cParValue *value = currentDecl->getCachedExpression(exprNode);
            if (!value)
            {
                cDynamicExpression *dynamicExpr = cExpressionBuilder().process(exprNode, isSubcomponent);
                value = new cLongPar();
                value->setName("gatesize-expression");
                cExpressionBuilder::assign(value, dynamicExpr);
                currentDecl->putCachedExpression(exprNode, value);
            }
            gatesize = value->longValue(module);
        }

        // add gate if it's declared here
        if (!isSubcomponent && gateNode->getType()!=NED_GATETYPE_NONE)
            module->addGate(gateName, translateGateType(gateNode->getType()), gateNode->getIsVector());

        // set gate vector size
        if (gatesize!=-1)
            module->setGateSize(gateName, gatesize);
    }
}

cModule *cNEDNetworkBuilder::_submodule(cModule *, const char *submodname, int idx)
{
    SubmodMap::iterator i = submodMap.find(std::string(submodname));
    if (i==submodMap.end())
        return NULL;

    ModulePtrVector& v = i->second;
    if (idx<0)
        return (v.size()!=1 || v[0]->isVector()) ? NULL : v[0];
    else
        return ((unsigned)idx>=v.size()) ? NULL : v[idx];
}

void cNEDNetworkBuilder::addGatesTo(cModule *module, cNEDDeclaration *decl)
{
    //TRACE("addGatesTo(%s), decl=%s", module->fullPath().c_str(), decl->name()); //XXX

    // recursively add and assign super types' gates
    if (decl->numExtendsNames() > 0)
    {
        const char *superName = decl->extendsName(0);
        cNEDDeclaration *superDecl = cNEDLoader::instance()->getDecl(superName);
        addGatesTo(module, superDecl);
    }

    // add this decl gates / gatesizes
    GatesNode *gatesNode = decl->getGatesNode();
    if (gatesNode)
    {
        currentDecl = decl; // switch "context"
        doGates(module, gatesNode, false);
    }
}

void cNEDNetworkBuilder::buildInside(cModule *modp, cNEDDeclaration *decl)
{
    //TRACE("buildinside(%s), decl=%s", modp->fullPath().c_str(), decl->name());  //XXX

    // add submodules and connections. Submodules and connections are inherited:
    // we need to start start with the the base classes, and do this compound
    // module last.
    submodMap.clear();
    buildRecursively(modp, decl);

    // recursively build the submodules too (top-down)
    currentDecl = decl;
    for (cSubModIterator submod(*modp); !submod.end(); submod++)
    {
       cModule *m = submod();
       m->buildInside();
    }
}

void cNEDNetworkBuilder::buildRecursively(cModule *modp, cNEDDeclaration *decl)
{
    // recursively add super types' submodules and connections
    if (decl->numExtendsNames() > 0)
    {
        const char *superName = decl->extendsName(0);
        cNEDDeclaration *superDecl = cNEDLoader::instance()->getDecl(superName);
        buildRecursively(modp, superDecl);
    }

    currentDecl = decl; // switch "context"
    addSubmodulesAndConnections(modp);
}

void cNEDNetworkBuilder::addSubmodulesAndConnections(cModule *modp)
{
    //TRACE("addSubmodulesAndConnections(%s), decl=%s", modp->fullPath().c_str(), currentDecl->name()); //XXX
    //dump(currentDecl->getTree()); XXX

    SubmodulesNode *submods = currentDecl->getSubmodulesNode();
    if (submods)
    {
        for (SubmoduleNode *submod=submods->getFirstSubmoduleChild(); submod; submod=submod->getNextSubmoduleNodeSibling())
        {
            addSubmodule(modp, submod);
        }
    }

    // loop through connections and add them
    ConnectionsNode *conns = currentDecl->getConnectionsNode();
    if (conns)
    {
        for (NEDElement *child=conns->getFirstChild(); child; child=child->getNextSibling())
        {
            if (child->getTagCode()==NED_CONNECTION || child->getTagCode()==NED_CONNECTION_GROUP)
                addConnectionOrConnectionGroup(modp, child);
        }
    }

    // check if there are unconnected gates left -- unless unconnected gates were already permitted in the super type
    if ((!conns || !conns->getAllowUnconnected()) && !superTypeAllowsUnconnected())
        modp->checkInternalConnections();
}

bool cNEDNetworkBuilder::superTypeAllowsUnconnected() const
{
    // follow through the inheritance chain, and return true if we find an "allowunconnected" anywhere
    cNEDDeclaration *decl = currentDecl;
    while (decl->numExtendsNames() > 0)
    {
        const char *superName = decl->extendsName(0);
        decl = cNEDLoader::instance()->getDecl(superName);
        ASSERT(decl); // all super classes must be loaded before we start building
        ConnectionsNode *conns = decl->getConnectionsNode();
        if (conns && conns->getAllowUnconnected())
            return true;
    }
    return false;
}

cModuleType *cNEDNetworkBuilder::findAndCheckModuleType(const char *modtypename, cModule *modp, const char *submodname)
{
    // try first in local scope, then in global scope
    std::string localTypeName = std::string(currentDecl->name())+"::"+modtypename;
    cModuleType *modtype = cModuleType::find(localTypeName.c_str());
    if (!modtype)
        modtype = cModuleType::find(modtypename);
    if (!modtype)
        throw cRuntimeError("dynamic module builder: module type definition `%s' for submodule %s "
                            "in (%s)%s not found (not in the loaded NED files?)",
                            modtypename, submodname, modp->className(), modp->fullPath().c_str());
    return modtype;
}

void cNEDNetworkBuilder::addSubmodule(cModule *modp, SubmoduleNode *submod)
{
    // create submodule
    const char *submodname = submod->getName();
    std::string submodtypename;
    if (strnull(submod->getLikeType()))
    {
        submodtypename = submod->getType();
    }
    else
    {
        // type may be given either in ExpressionNode child or "like-param" attribute
        if (!strnull(submod->getLikeParam()))
        {
            submodtypename = modp->par(submod->getLikeParam()).stringValue();
        }
        else
        {
            ExpressionNode *likeParamExpr = findExpression(submod, "like-param");
            ASSERT(likeParamExpr); // either attr or expr must be there
            //FIXME if module vector: store it as expression, don't evaluate it now,
            //      because it might be random etc!!!
            submodtypename = evaluateAsString(likeParamExpr, modp, false);
        }

    }

    ExpressionNode *vectorsizeexpr = findExpression(submod, "vector-size");

    if (!vectorsizeexpr)
    {
        cModuleType *submodtype = findAndCheckModuleType(submodtypename.c_str(), modp, submodname);
        //FIXME: check submodtype contains likeType as interfaceName!
        cModule *submodp = submodtype->create(submodname, modp);
        ModulePtrVector& v = submodMap[submodname];
        v.push_back(submodp);

        cContextSwitcher __ctx(submodp); // params need to be evaluated in the module's context
        setDisplayString(submodp);
        assignSubcomponentParams(submodp, submod);
        submodp->finalizeParameters(); // also sets up type's gates
        setupGateVectors(submodp, submod);
    }
    else
    {
        int vectorsize = (int) evaluateAsLong(vectorsizeexpr, modp, false);
        ModulePtrVector& v = submodMap[submodname];
        cModuleType *submodtype = NULL;
        for (int i=0; i<vectorsize; i++)
        {
            if (!submodtype)
                submodtype = findAndCheckModuleType(submodtypename.c_str(), modp, submodname);
            cModule *submodp = submodtype->create(submodname, modp, vectorsize, i);
            v.push_back(submodp);

            cContextSwitcher __ctx(submodp); // params need to be evaluated in the module's context
            setDisplayString(submodp);
            assignSubcomponentParams(submodp, submod);
            submodp->finalizeParameters(); // also sets up type's gates
            setupGateVectors(submodp, submod);
        }
    }

    // Note: buildInside() will be called when connections have been built out
    // on this level too.
}


void cNEDNetworkBuilder::setDisplayString(cModule *module)
{
    cProperties *props = module->properties();
    cProperty *prop = props->get("display");
    const char *propValue = prop ? prop->value(cProperty::DEFAULTKEY) : NULL;
    if (propValue)
    {
        module->setDisplayString(propValue);
    }
}

void cNEDNetworkBuilder::setConnDisplayString(cGate *srcgatep)
{
/*XXX
    DisplayStringNode *dispstrnode = conn->getFirstDisplayStringChild();
    if (dispstrnode)
    {
        const char *dispstr = dispstrnode->getValue();
        srcgatep->setDisplayString(dispstr);
    }
*/
}

void cNEDNetworkBuilder::assignSubcomponentParams(cComponent *subcomponent, NEDElement *subcomponentNode)
{
    ParametersNode *paramsNode = (ParametersNode *) subcomponentNode->getFirstChildWithTag(NED_PARAMETERS);
    if (paramsNode)
        doParams(subcomponent, paramsNode, true);
}

void cNEDNetworkBuilder::setupGateVectors(cModule *submodule, NEDElement *submoduleNode)
{
    GatesNode *gatesNode = (GatesNode *) submoduleNode->getFirstChildWithTag(NED_GATES);
    if (gatesNode)
        doGates(submodule, gatesNode, true);
}

void cNEDNetworkBuilder::addConnectionOrConnectionGroup(cModule *modp, NEDElement *connOrConnGroup)
{
    // this is tricky: elements representing "for" and "if" in NED are children
    // of the ConnectionNode or ConnectionGroupNode. So, first we have to go through
    // and execute the LoopNode and ConditionNode children recursively to get
    // nested loops etc, then after (inside) the last one create the connection(s)
    // themselves, which is (are) then parent of the LoopNode/ConditionNode.
    NEDSupport::LoopVar::reset();
    doConnOrConnGroupBody(modp, connOrConnGroup, connOrConnGroup->getFirstChild());
}

void cNEDNetworkBuilder::doConnOrConnGroupBody(cModule *modp, NEDElement *connOrConnGroup, NEDElement *loopOrCondition)
{
    // find first "for" or "if" at loopOrCondition (or among its next siblings)
    while (loopOrCondition && loopOrCondition->getTagCode()!=NED_LOOP && loopOrCondition->getTagCode()!=NED_CONDITION)
        loopOrCondition = loopOrCondition->getNextSibling();

    // if there's a "for" or "if", do that, otherwise create the connection(s) themselves
    if (loopOrCondition)
        doLoopOrCondition(modp, loopOrCondition);
    else
        doAddConnOrConnGroup(modp, connOrConnGroup);
}


void cNEDNetworkBuilder::doLoopOrCondition(cModule *modp, NEDElement *loopOrCondition)
{
    if (loopOrCondition->getTagCode()==NED_CONDITION)
    {
        // check condition
        ConditionNode *condition = (ConditionNode *)loopOrCondition;
        ExpressionNode *condexpr = findExpression(condition, "condition");
        if (condexpr && evaluateAsBool(condexpr, modp, false)==true)
        {
            // do the body of the "if": either further "for"'s and "if"'s, or
            // the connection(group) itself that we are children of.
            doConnOrConnGroupBody(modp, loopOrCondition->getParent(), loopOrCondition->getNextSibling());
        }
    }
    else if (loopOrCondition->getTagCode()==NED_LOOP)
    {
        LoopNode *loop = (LoopNode *)loopOrCondition;
        long start = evaluateAsLong(findExpression(loop, "from-value"), modp, false);
        long end = evaluateAsLong(findExpression(loop, "to-value"), modp, false);

        // do loop
        long& i = NEDSupport::LoopVar::pushVar(loop->getParamName());
        for (i=start; i<=end; i++)
        {
            // do the body of the "if": either further "for"'s and "if"'s, or
            // the connection(group) itself that we are children of.
            doConnOrConnGroupBody(modp, loopOrCondition->getParent(), loopOrCondition->getNextSibling());
        }
        NEDSupport::LoopVar::popVar();
    }
    else
    {
        ASSERT(false);
    }
}

void cNEDNetworkBuilder::doAddConnOrConnGroup(cModule *modp, NEDElement *connOrConnGroup)
{
    if (connOrConnGroup->getTagCode()==NED_CONNECTION)
    {
        doAddConnection(modp, (ConnectionNode *)connOrConnGroup);
    }
    else if (connOrConnGroup->getTagCode()==NED_CONNECTION_GROUP)
    {
        ConnectionGroupNode *conngroup = (ConnectionGroupNode *)connOrConnGroup;
        for (ConnectionNode *conn=conngroup->getFirstConnectionChild(); conn; conn=conn->getNextConnectionNodeSibling())
        {
            doConnOrConnGroupBody(modp, conn, conn->getFirstChild());
        }
    }
    else
    {
        ASSERT(false);
    }
}

void cNEDNetworkBuilder::doAddConnection(cModule *modp, ConnectionNode *conn)
{
//FIXME spurious error message comes when trying to connect INOUT gate with "-->"
    if (conn->getArrowDirection()!=NED_ARROWDIR_BIDIR)
    {
        // find gates and create connection
        cGate *srcg = resolveGate(modp, conn->getSrcModule(), findExpression(conn, "src-module-index"),
                                        conn->getSrcGate(), findExpression(conn, "src-gate-index"),
                                        conn->getSrcGatePlusplus());
        cGate *destg = resolveGate(modp, conn->getDestModule(), findExpression(conn, "dest-module-index"),
                                         conn->getDestGate(), findExpression(conn, "dest-gate-index"),
                                         conn->getDestGatePlusplus());

        //TRACE("doAddConnection(): %s --> %s", srcg->fullPath().c_str(), destg->fullPath().c_str());

        // check directions
        cGate *errg = NULL;
        if (srcg->ownerModule()==modp ? srcg->type()!=cGate::INPUT : srcg->type()!=cGate::OUTPUT)
            errg = srcg;
        if (destg->ownerModule()==modp ? destg->type()!=cGate::OUTPUT : destg->type()!=cGate::INPUT)
            errg = destg;
        if (errg)
            throw cRuntimeError("dynamic module builder: gate %s in (%s)%s is being "
                                "connected the wrong way: directions don't match",
                                errg->fullPath().c_str(), modp->className(), modp->fullPath().c_str());

        doConnectGates(modp, srcg, destg, conn);
    }
    else
    {
        // find gates and create connection in both ways
        cGate *srcgi, *srcgo, *destgi, *destgo;
        resolveInoutGate(modp, conn->getSrcModule(), findExpression(conn, "src-module-index"),
                         conn->getSrcGate(), findExpression(conn, "src-gate-index"),
                         conn->getSrcGatePlusplus(),
                         srcgi, srcgo);
        resolveInoutGate(modp, conn->getDestModule(), findExpression(conn, "dest-module-index"),
                         conn->getDestGate(), findExpression(conn, "dest-gate-index"),
                         conn->getDestGatePlusplus(),
                         destgi, destgo);

        doConnectGates(modp, srcgo, destgi, conn);
        doConnectGates(modp, destgo, srcgi, conn);
    }
}

void cNEDNetworkBuilder::doConnectGates(cModule *modp, cGate *srcg, cGate *destg, ConnectionNode *conn)
{
    // connect
    ChannelSpecNode *channelspec = conn->getFirstChannelSpecChild();
    if (!channelspec)
    {
        srcg->connectTo(destg);
    }
    else
    {
        cChannel *channel = createChannel(channelspec, modp);
        srcg->connectTo(destg, channel);
        assignSubcomponentParams(channel, channelspec);
        channel->finalizeParameters();

        //XXX display string
    }
}

cGate *cNEDNetworkBuilder::resolveGate(cModule *parentmodp,
                                       const char *modname, ExpressionNode *modindexp,
                                       const char *gatename, ExpressionNode *gateindexp,
                                       bool isplusplus)
{
    if (isplusplus && gateindexp)
        throw cRuntimeError("dynamic module builder: \"++\" and gate index expression cannot exist together");

    cModule *modp = resolveModuleForConnection(parentmodp, modname, modindexp);

    cGate *gatep = NULL;
    if (!gateindexp && !isplusplus)
    {
        gatep = modp->gate(gatename);
    }
    else if (isplusplus)
    {
        if (modp == parentmodp)
            gatep = getFirstUnusedParentModGate(modp, gatename);
        else
            gatep = getFirstUnusedSubmodGate(modp, gatename);
    }
    else // (gateindexp)
    {
        int gateindex = (int) evaluateAsLong(gateindexp, parentmodp, false);
        gatep = modp->gate(gatename, gateindex);
    }
    return gatep;
}

void cNEDNetworkBuilder::resolveInoutGate(cModule *parentmodp,
                                          const char *modname, ExpressionNode *modindexp,
                                          const char *gatename, ExpressionNode *gateindexp,
                                          bool isplusplus,
                                          cGate *&gatein, cGate *&gateout)
{
    if (isplusplus && gateindexp)
        throw cRuntimeError("dynamic module builder: \"++\" and gate index expression cannot exist together");

    cModule *modp = resolveModuleForConnection(parentmodp, modname, modindexp);

    gatein = gateout = NULL;
    if (!gateindexp && !isplusplus)
    {
        gatein = modp->gateHalf(gatename, cGate::INPUT);
        gateout = modp->gateHalf(gatename, cGate::OUTPUT);
    }
    else if (isplusplus)
    {
        if (modp == parentmodp)
            getFirstUnusedParentModInoutGate(modp, gatename, gatein, gateout);
        else
            getFirstUnusedSubmodInoutGate(modp, gatename, gatein, gateout);
    }
    else // (gateindexp)
    {
        int gateindex = (int) evaluateAsLong(gateindexp, parentmodp, false);
        gatein = modp->gateHalf(gatename, cGate::INPUT, gateindex);
        gateout = modp->gateHalf(gatename, cGate::OUTPUT, gateindex);
    }
}

cModule *cNEDNetworkBuilder::resolveModuleForConnection(cModule *parentmodp, const char *modname, ExpressionNode *modindexp)
{
    if (strnull(modname))
    {
        return parentmodp;
    }
    else
    {
        int modindex = !modindexp ? 0 : (int) evaluateAsLong(modindexp, parentmodp, false);
        cModule *modp = _submodule(parentmodp, modname,modindex);
        if (!modp)
        {
            if (!modindexp)
                throw cRuntimeError("dynamic module builder: submodule `%s' in (%s)%s not found",
                                        modname, parentmodp->className(), parentmodp->fullPath().c_str());
            else
                throw cRuntimeError("dynamic module builder: submodule `%s[%d]' in (%s)%s not found",
                                        modname, modindex, parentmodp->className(), parentmodp->fullPath().c_str());
        }
        return modp;
    }
}

cGate *cNEDNetworkBuilder::getFirstUnusedParentModGate(cModule *modp, const char *gatename)
{
    //FIXME revise! probably won't work
    int baseId = modp->findGate(gatename);
    if (baseId<0)
        throw cRuntimeError("dynamic module builder: %s has no %s[] gate",modp->fullPath().c_str(), gatename);
    int n = modp->gate(baseId)->size();
    for (int i=0; i<n; i++)
        if (!modp->gate(baseId+i)->isConnectedInside())
            return modp->gate(baseId+i);
    throw cRuntimeError("%s[] gates are all connected, no gate left for `++' operator",modp->fullPath().c_str(), gatename);
}

cGate *cNEDNetworkBuilder::getFirstUnusedSubmodGate(cModule *modp, const char *gatename)
{
    int oldsize = modp->gateSize(gatename);
    if (oldsize>0)
    {
        int baseId = modp->findGate(gatename, 0);
        for (int i=0; i<oldsize; i++)
            if (!modp->gate(baseId+i)->isConnectedOutside())
                return modp->gate(baseId+i);
    }
    //FIXME old, more efficient code:
    // int newBaseId = modp->setGateSize(gatename, n+1);
    // return modp->gate(newBaseId+n);
    modp->setGateSize(gatename, oldsize+1);   //FIXME could be more optimal?
    return modp->gate(gatename, oldsize);
}

void cNEDNetworkBuilder::getFirstUnusedParentModInoutGate(cModule *modp, const char *gatename,
                                                          cGate *&gatein, cGate *&gateout)
{
__asm int 3; //FIXME to be implemented
/*
    int baseId = modp->findGate(gatename);
    if (baseId<0)
        throw cRuntimeError("dynamic module builder: %s has no %s[] gate",modp->fullPath().c_str(), gatename);
    int n = modp->gate(baseId)->size();
    for (int i=0; i<n; i++)
        if (!modp->gate(baseId+i)->isConnectedInside())
            return modp->gate(baseId+i);
    throw cRuntimeError("%s[] gates are all connected, no gate left for `++' operator",modp->fullPath().c_str(), gatename);
*/
}

//FIXME these four functions sholud probably be moved into cModule. Could be a lot more efficient
void cNEDNetworkBuilder::getFirstUnusedSubmodInoutGate(cModule *modp, const char *gatename,
                                                       cGate *&gatein, cGate *&gateout)
{
    int oldsize = modp->gateSize(gatename);
    if (oldsize>0)
    {
        int iBaseId = modp->gateHalf(gatename, cGate::INPUT, 0)->id();  //FIXME make better API to get these two in one step!
        int oBaseId = modp->gateHalf(gatename, cGate::OUTPUT, 0)->id();
        for (int i=0; i<oldsize; i++)
        {
            if (!modp->gate(iBaseId+i)->isConnectedOutside() && !modp->gate(oBaseId+i)->isConnectedOutside())
            {
                gatein = modp->gate(iBaseId+i);
                gateout = modp->gate(oBaseId+i);
                return;
            }
        }
    }
    modp->setGateSize(gatename, oldsize+1);   //FIXME could be more optimal? (don't look up name again)
    gatein = modp->gateHalf(gatename, cGate::INPUT, oldsize);
    gateout = modp->gateHalf(gatename, cGate::OUTPUT, oldsize);
}

cChannel *cNEDNetworkBuilder::createChannel(ChannelSpecNode *channelspec, cModule *parentmodp)
{
    // create channel object
    cChannel *channelp = NULL;
    std::string channeltypename;
    if (strnull(channelspec->getLikeType()))
    {
        channeltypename = strnull(channelspec->getType()) ? "cBasicChannel" : channelspec->getType();
    }
    else
    {
        // type may be given either in ExpressionNode child or "like-param" attribute
        if (!strnull(channelspec->getLikeParam()))
        {
            channeltypename = parentmodp->par(channelspec->getLikeParam()).stringValue();
        }
        else
        {
            ExpressionNode *likeParamExpr = findExpression(channelspec, "like-param");
            ASSERT(likeParamExpr); // either attr or expr must be there
            channeltypename = evaluateAsString(likeParamExpr, parentmodp, false);
        }
        //XXX check actual type fulfills likeType
    }

    cChannelType *channeltype = findAndCheckChannelType(channeltypename.c_str());
    channelp = channeltype->create("channel", parentmodp); //FIXME must give unique names, otherwise channel properties() won't work!!!

    return channelp;
}

cChannelType *cNEDNetworkBuilder::findAndCheckChannelType(const char *channeltypename)
{
    // try first in local scope, then in global scope
    std::string localTypeName = std::string(currentDecl->name())+"::"+channeltypename;
    cChannelType *channeltype = cChannelType::find(localTypeName.c_str());
    if (!channeltype)
        channeltype = cChannelType::find(channeltypename);
    if (!channeltype)
        throw cRuntimeError("dynamic network builder: channel type definition `%s' not found "
                            "(not in the loaded NED files?)", channeltypename);
    return channeltype;
}

ExpressionNode *cNEDNetworkBuilder::findExpression(NEDElement *node, const char *exprname)
{
    // find expression with given name in node
    if (!node)
        return NULL;
    for (NEDElement *child=node->getFirstChildWithTag(NED_EXPRESSION); child; child = child->getNextSiblingWithTag(NED_EXPRESSION))
    {
        ExpressionNode *expr = (ExpressionNode *)child;
        if (!strcmp(expr->getTarget(),exprname))
            return expr;
    }
    return NULL;
}

cParValue *cNEDNetworkBuilder::getOrCreateExpression(ExpressionNode *exprNode, cPar::Type type, bool inSubcomponentScope)
{
    cParValue *p = currentDecl->getCachedExpression(exprNode);
    if (!p)
    {
        cDynamicExpression *e = cExpressionBuilder().process(exprNode, inSubcomponentScope);
        p = cParValue::createWithType(type);
        cExpressionBuilder::assign(p, e);
        currentDecl->putCachedExpression(exprNode, p);
    }
    return p;
}

long cNEDNetworkBuilder::evaluateAsLong(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope)
{
    cParValue *p = getOrCreateExpression(exprNode, cPar::LONG, inSubcomponentScope);
    return p->longValue(context);
}

bool cNEDNetworkBuilder::evaluateAsBool(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope)
{
    cParValue *p = getOrCreateExpression(exprNode, cPar::BOOL, inSubcomponentScope);
    return p->boolValue(context);
}

std::string cNEDNetworkBuilder::evaluateAsString(ExpressionNode *exprNode, cComponent *context, bool inSubcomponentScope)
{
    cParValue *p = getOrCreateExpression(exprNode, cPar::STRING, inSubcomponentScope);
    return p->stdstringValue(context);
}

