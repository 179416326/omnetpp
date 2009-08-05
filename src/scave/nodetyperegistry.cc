//=========================================================================
//  NODETYPEREGISTRY.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifdef _MSC_VER
#pragma warning(disable:4786)
#endif

#include "commonutil.h"
#include "nodetyperegistry.h"
#include "stringtokenizer.h"
#include "arraybuilder.h"
#include "vectorfilereader.h"
#include "vectorfilewriter.h"
#include "indexedvectorfile.h"
#include "filewriter.h"
#include "windowavg.h"
#include "slidingwinavg.h"
#include "filternodes.h"
#include "mergernodes.h"
#include "xyplotnode.h"
#include "diffquot.h"
#include "stddev.h"


NodeTypeRegistry *NodeTypeRegistry::inst;

NodeTypeRegistry *NodeTypeRegistry::instance()
{
    if (!inst)
        inst = new NodeTypeRegistry();
    return inst;
}

void NodeTypeRegistry::add(NodeType *nodetype)
{
    nodeTypeMap[nodetype->name()] = nodetype;
}

void NodeTypeRegistry::remove(NodeType *nodetype)
{
    NodeTypeMap::iterator it = nodeTypeMap.find(nodetype->name());
    if (it!=nodeTypeMap.end())
        nodeTypeMap.erase(it);
}

NodeTypeRegistry::NodeTypeRegistry()
{
    add(new ArrayBuilderNodeType());
    add(new StddevNodeType());
    add(new VectorFileReaderNodeType());
    add(new VectorFileWriterNodeType());
    add(new IndexedVectorFileWriterNodeType());
    add(new FileWriterNodeType());
    add(new MergerNodeType());
    add(new XYPlotNodeType());

    add(new WindowAverageNodeType());
    add(new SlidingWindowAverageNodeType());
    add(new MovingAverageNodeType());
    add(new DifferenceQuotientNodeType());
    add(new NopNodeType());
    add(new AdderNodeType());
    add(new MultiplierNodeType());
    add(new DividerNodeType());
    add(new ModuloNodeType());
    add(new DifferenceNodeType());
    add(new TimeDiffNodeType());
    add(new SumNodeType());
    add(new TimeShiftNodeType());
    add(new LinearTrendNodeType());
    add(new CropNodeType());
    add(new MeanNodeType());
    add(new RemoveRepeatsNodeType());
}

NodeTypeRegistry::~NodeTypeRegistry()
{
    for (NodeTypeMap::iterator it=nodeTypeMap.begin(); it!=nodeTypeMap.end(); it++)
        delete it->second;
}

bool NodeTypeRegistry::exists(const char *name)
{
    return nodeTypeMap.find(name)!=nodeTypeMap.end();
}

NodeType *NodeTypeRegistry::getNodeType(const char *name)
{
    NodeTypeMap::iterator it = nodeTypeMap.find(name);
    if (it==nodeTypeMap.end())
        throw opp_runtime_error("unknown node type `%s'", name);
    return it->second;
}

NodeTypeVector NodeTypeRegistry::getNodeTypes()
{
    NodeTypeVector vect;
    for (NodeTypeMap::iterator it=nodeTypeMap.begin(); it!=nodeTypeMap.end(); it++)
        vect.push_back(it->second);
    return vect;
}

Node *NodeTypeRegistry::createNode(const char *filterSpec, DataflowManager *mgr)
{
    // parse filterSpec
    std::string name;
    std::vector<std::string> args;
    parseFilterSpec(filterSpec, name, args);

    // look up node type
    NodeType *nodeType = getNodeType(name.c_str());

    // check number of args match
    StringMap attrs;
    nodeType->getAttrDefaults(attrs);
    if (attrs.size()!=args.size())
        throw opp_runtime_error("error in filter spec `%s' -- %s expects %d parameters", filterSpec, name.c_str(), attrs.size());

    // fill in args map
    int i=0;
    for (StringMap::iterator it=attrs.begin(); it!=attrs.end(); ++it, ++i)
        if (!args[i].empty())
            it->second = args[i];

    // create filter
    return nodeType->create(mgr, attrs);
}

void NodeTypeRegistry::parseFilterSpec(const char *filterSpec, std::string& name, std::vector<std::string>& args)
{
    args.clear();
    const char *paren = strchr(filterSpec, '(');
    if (!paren) {
        // no left paren -- treat the whole string as filter name
        name = filterSpec;
        return;
    }

    // check that string ends in right paren
    if (filterSpec[strlen(filterSpec)-1]!=')')
        throw opp_runtime_error("syntax error in filter spec `%s'", filterSpec);

    // filter name is the part before the left paren
    name.assign(filterSpec, paren-filterSpec);

    // arg list is the part between the parens -- split it up along commas
    std::string arglist(paren+1, strlen(paren)-2);
    StringTokenizer tokenizer(arglist.c_str(), ",");
    const char *token;
    while ((token = tokenizer.nextToken())!=NULL)
        args.push_back(token);
}


