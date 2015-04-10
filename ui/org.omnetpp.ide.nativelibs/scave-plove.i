%{
#include "scave/node.h"
#include "scave/dataflowmanager.h"
#include "scave/nodetype.h"
#include "scave/nodetyperegistry.h"
#include "scave/arraybuilder.h"
#include "scave/xyarray.h"
%}

// covariant return type warning disabled
#pragma SWIG nowarn=822

%include "commondefs.i"
%include "std_common.i"
%include "std_string.i"
%include "std_map.i"
%include "std_vector.i"
%include "std_list.i"    // our custom version

namespace std {

   //
   // Without the following macro (defined in std_map.i in the swig library),
   // get() returns string *pointer* (because of T& retval); this macro defines
   // get() to return T so it'll be just string. Same with NodeType*.
   //
   specialize_std_map_on_both(std::string,,,,std::string,,,);

   // generate needed template specializations
   %template(StringMap) map<string,string>;
   %template(NodeTypeVector) vector<NodeType*>;
};

//
// We publish very little of the Node and Port classes into Java,
// just enough to assemble the data-flow engine. We hide Channel and
// Datum altogether.
//

class NodeType;
class XYArray;

class Port
{
  private:
    Port(); // prevent generating a default public one
    ~Port();
  public:
    Node *getNode();
};

%newobject Node::getArray;

class Node
{
  private:
    Node(); // prevent generating a default public one
    ~Node();
  public:
    NodeType *getNodeType() const;
    %extend {
        XYArray *getArray() {
            ArrayBuilderNode *abNode = dynamic_cast<ArrayBuilderNode *>(self);
            return abNode ? abNode->getArray() : NULL;
        }
    }
};


%ignore DataflowManager::addNode; // it's included in NodeType::create()

%ignore XYArray::XYArray;

%ignore NodeTypeRegistry::parseFilterSpec;
//
// NodeType, NodeTypeRegistry and DataflowManager are published in their entirety
// to Java.
//
%include "scave/nodetype.h"
%include "scave/nodetyperegistry.h"
%include "scave/dataflowmanager.h"
%include "scave/xyarray.h"


