//==========================================================================
// nedbasicvalidator.cc
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
// Contents:
//   class NEDBasicValidator
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "nederror.h"
#include "nedbasicvalidator.h"


// FIXME TODO: assert types begin with capital letters, and submods/gates/params with lowercase! warning if not!


static struct { char *fname; int args; } known_funcs[] =
{
   /* <math.h> */
   {"fabs", 1},    {"fmod", 2},
   {"acos", 1},    {"asin", 1},    {"atan", 1},   {"atan2", 1},
   {"sin", 1},     {"cos", 1},     {"tan", 1},    {"hypot", 2},
   {"ceil", 1},    {"floor", 1},
   {"exp", 1},     {"pow", 2},     {"sqrt", 1},
   {"log",  1},    {"log10", 1},

   /* OMNeT++ general */
   {"min", 2},
   {"max", 2},

   /* OMNeT++: distributions without rng-id arg */
   {"uniform",2},
   {"exponential",1},
   {"normal",2},
   {"truncnormal",2},
   {"gamma_d",2},
   {"beta",2},
   {"erlang_k",2},
   {"chi_square",1},
   {"student_t",1},
   {"cauchy",2},
   {"triang",3},
   {"lognormal",2},
   {"weibull",2},
   {"pareto_shifted",3},
   {"intuniform",2},
   {"bernoulli",1},
   {"binomial",2},
   {"geometric",1},
   {"negbinomial",2},
   {"hypergeometric",3},
   {"poisson",1},

   /* OMNeT++: distributions with rng-id arg */
   {"uniform",3},
   {"exponential",2},
   {"normal",3},
   {"truncnormal",3},
   {"gamma_d",3},
   {"beta",3},
   {"erlang_k",3},
   {"chi_square",2},
   {"student_t",2},
   {"cauchy",3},
   {"triang",4},
   {"lognormal",3},
   {"weibull",3},
   {"pareto_shifted",4},
   {"intuniform",3},
   {"bernoulli",2},
   {"binomial",3},
   {"geometric",2},
   {"negbinomial",3},
   {"hypergeometric",4},
   {"poisson",2},

   /* OMNeT++: old genk_* stuff */
   {"genk_uniform",3},
   {"genk_intuniform",3},
   {"genk_exponential",2},
   {"genk_normal",3},
   {"genk_truncnormal",3},

   /* END */
   {NULL,0}
};

inline bool strnull(const char *s)
{
    return !s || !s[0];
}

inline bool strnotnull(const char *s)
{
    return s && s[0];
}

void NEDBasicValidator::checkUniqueness(NEDElement *node, int childtype, const char *attr)
{
    for (NEDElement *child1=node->getFirstChildWithTag(childtype); child1; child1=child1->getNextSiblingWithTag(childtype))
    {
        const char *attr1 = child1->getAttribute(attr);
        for (NEDElement *child2=node->getFirstChildWithTag(childtype); child2!=child1; child2=child2->getNextSiblingWithTag(childtype))
        {
            const char *attr2 = child2->getAttribute(attr);
            if (attr1 && attr2 && !strcmp(attr1,attr2))
                errors->add(child1, "name '%s' not unique",attr1);
        }
    }
}

void NEDBasicValidator::checkExpressionAttributes(NEDElement *node, const char *attrs[], bool optional[], int n)
{
    if (parsedExpressions)
    {
        // allow attribute values to be present, but check there are no
        // Expression children that are not in the list
        for (NEDElement *child=node->getFirstChildWithTag(NED_EXPRESSION); child; child=child->getNextSiblingWithTag(NED_EXPRESSION))
        {
            ExpressionNode *expr = (ExpressionNode *) child;
            const char *target = expr->getTarget();
            int i;
            for (i=0; i<n; i++)
                if (!strcmp(target, attrs[i]))
                    break;
            if (i==n)
                errors->add(child, "'expression' element with invalid target attribute '%s'",target);
        }
    }
    else
    {
        // check: should be no Expression children at all
        if (node->getFirstChildWithTag(NED_EXPRESSION))
            errors->add(node, "'expression' element found while using non-parsed expressions\n");
    }

    // check mandatory expressions are there
    for (int i=0; i<n; i++)
    {
       if (!optional[i])
       {
           if (parsedExpressions)
           {
               // check: Expression element must be there
               ExpressionNode *expr;
               for (expr=(ExpressionNode *)node->getFirstChildWithTag(NED_EXPRESSION); expr; expr=expr->getNextExpressionNodeSibling())
                   if (strnotnull(expr->getTarget()) && !strcmp(expr->getTarget(),attrs[i]))
                       break;
               if (!expr)
                   errors->add(node, "expression-valued attribute '%s' not present in parsed form (missing 'expression' element)", attrs[i]);
           }
           else
           {
               // attribute must be there
               if (!node->getAttribute(attrs[i]) || !(node->getAttribute(attrs[i]))[0])
                   errors->add(node, "missing attribute '%s'", attrs[i]);
           }
       }
    }
}

void NEDBasicValidator::validateElement(FilesNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(NedFileNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(CommentNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ImportNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(PropertyDeclNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ExtendsNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(InterfaceNameNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(SimpleModuleNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ModuleInterfaceNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(CompoundModuleNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ParametersNode *node)
{
    // make sure parameter names are unique
    checkUniqueness(node, NED_PARAM, "name");
}

void NEDBasicValidator::validateElement(ParamNode *node)
{
    //FIXME revise
    //TODO param declarations cannot occur in submodules, cannot be conditional, etc
}

void NEDBasicValidator::validateElement(PatternNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(PropertyNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(PropertyKeyNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(GatesNode *node)
{
    // make sure gate names are unique
    // TODO consider gate groups
    checkUniqueness(node, NED_GATE, "name");
}

void NEDBasicValidator::validateElement(TypesNode *node)
{
    // make sure type names are unique
    //FIXME checkUniqueness(node, NED_????, "name");
}

void NEDBasicValidator::validateElement(GateNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(SubmodulesNode *node)
{
    // make sure submodule names are unique
    checkUniqueness(node, NED_SUBMODULE, "name");
}

void NEDBasicValidator::validateElement(SubmoduleNode *node)
{
    //FIXME revise
    const char *expr[] = {"vector-size"};
    bool opt[] = {true};
    checkExpressionAttributes(node, expr, opt, 1);

    // if there's a "like", name should be an existing module parameter name
    if (strnotnull(node->getLikeParam()))
    {
        const char *paramName = node->getLikeParam();
        NEDElement *compound = node->getParentWithTag(NED_COMPOUND_MODULE);
        if (!compound)
            INTERNAL_ERROR0(node,"occurs outside a compound-module");
        NEDElement *params = compound->getFirstChildWithTag(NED_PARAMETERS);
        if (!params || params->getFirstChildWithAttribute(NED_PARAM, "name", paramName)==NULL)
            {errors->add(node, "compound module has no parameter named '%s'", paramName);return;}
    }
}

//void NEDBasicValidator::validateElement(SubstparamsNode *node)
//{
//    const char *expr[] = {"condition"};
//    bool opt[] = {true};
//    checkExpressionAttributes(node, expr, opt, 1);
//
//    // make sure parameter names are unique
//    checkUniqueness(node, NED_SUBSTPARAM, "name");
//}

// TODO merge into 'parameters'
//void NEDBasicValidator::validateElement(SubstparamNode *node)
//{
//    const char *expr[] = {"value"};
//    bool opt[] = {false};
//    checkExpressionAttributes(node, expr, opt, 1);
//}

// TODO merge into 'gates'
//void NEDBasicValidator::validateElement(GatesizesNode *node)
//{
//    const char *expr[] = {"condition"};
//    bool opt[] = {true};
//    checkExpressionAttributes(node, expr, opt, 1);
//
//    // make sure gate names are unique
//    checkUniqueness(node, NED_GATESIZE, "name");
//}

// TODO merge into 'gates'
//void NEDBasicValidator::validateElement(GatesizeNode *node)
//{
//    const char *expr[] = {"vector-size"};
//    bool opt[] = {true};
//    checkExpressionAttributes(node, expr, opt, 1);
//}

void NEDBasicValidator::validateElement(ConnectionsNode *node)
{
    //FIXME revise
    // TBD if check=true, make sure all gates are connected
}

void NEDBasicValidator::validateElement(ConnectionNode *node)
{
    //FIXME revise
    const char *expr[] = {"condition", "src-module-index", "src-gate-index", "dest-module-index", "dest-gate-index"};
    bool opt[] = {true, true, true, true, true};
    checkExpressionAttributes(node, expr, opt, 5);

    // plusplus and gate index expression cannot be both there
    bool srcGateIx =  node->getFirstChildWithAttribute(NED_EXPRESSION, "target", "src-gate-index")!=NULL;
    bool destGateIx = node->getFirstChildWithAttribute(NED_EXPRESSION, "target", "dest-gate-index")!=NULL;
    if (srcGateIx && node->getSrcGatePlusplus())
        errors->add(node, "wrong source gate: cannot have both gate index and '++' operator specified");
    if (destGateIx && node->getDestGatePlusplus())
        errors->add(node, "wrong destination gate: cannot have both gate index and '++' operator specified");
}

void NEDBasicValidator::validateElement(ChannelInterfaceNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ChannelNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ConnectionGroupNode *node)
{
    //FIXME revise
    // TODO check loop vars are unique etc
}

void NEDBasicValidator::validateElement(LoopNode *node)
{
    //TODO adapt
    //const char *expr[] = {"from-value", "to-value"};
    //bool opt[] = {false, false};
    //checkExpressionAttributes(node, expr, opt, 2);
}

void NEDBasicValidator::validateElement(ConditionNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(ExpressionNode *node)
{
    //FIXME revise
}

void NEDBasicValidator::validateElement(OperatorNode *node)
{
    //FIXME revise
    // check operator name is valid and argument count matches
    const char *op = node->getName();

    // next list uses space as separator, so make sure op does not contain space
    if (strchr(op, ' '))
    {
        errors->add(node, "invalid operator '%s' (contains space)",op);
        return;
    }

    // count arguments
    int args = 0;
    for (NEDElement *child=node->getFirstChild(); child; child=child->getNextSibling())
       args++;

    // unary?
    if (strstr("! ~",op))
    {
         if (args!=1)
            errors->add(node, "operator '%s' should have 1 operand, not %d", op, args);
    }
    // unary or binary?
    else if (strstr("-",op))
    {
         if (args!=1 && args!=2)
            errors->add(node, "operator '%s' should have 1 or 2 operands, not %d", op, args);
    }
    // binary?
    else if (strstr("+ * / % ^ == != > >= < <= && || ## & | # << >>",op))
    {
         if (args!=2)
            errors->add(node, "operator '%s' should have 2 operands, not %d", op, args);
    }
    // tertiary?
    else if (strstr("?:",op))
    {
         if (args!=3)
            errors->add(node, "operator '%s' should have 3 operands, not %d", op, args);
    }
    else
    {
        errors->add(node, "invalid operator '%s'",op);
    }
}

void NEDBasicValidator::validateElement(FunctionNode *node)
{
    //FIXME revise
    // if we know the function, check argument count
    const char *func = node->getName();
    int args = node->getNumChildren();

    // if it's an operator, treat specially
    if (!strcmp(func,"index"))
    {
         if (args!=0)
             errors->add(node, "operator 'index' does not take arguments");

         // find expression and submodule node we're under
         NEDElement *parent = node->getParent();
         while (parent && parent->getTagCode()!=NED_EXPRESSION)
             parent = parent->getParent();
         NEDElement *expr = parent;

         while (parent && parent->getTagCode()!=NED_SUBMODULE)
             parent = parent->getParent();
         NEDElement *submod = parent;

         if (!submod || submod->getFirstChildWithAttribute(NED_EXPRESSION, "target", "vector-size")==NULL)
             errors->add(node, "'index' may only occur in a submodule vector's definition");
         if (expr->getParent()==submod)
             errors->add(node, "'index' is not allowed here");
         return;
    }
    else if (!strcmp(func,"sizeof"))
    {
         if (args!=1)
             errors->add(node, "operator 'sizeof' takes one argument");
         //else if (node->getFirstChild()->getTagCode()!=NED_IDENT)
         //    errors->add(node, "argument of operator 'sizeof' should be an identifier");
         else
         {
             // TBD further check it's an existing parent module gate or submodule name
         }
         return;
    }
    else if (!strcmp(func,"input"))
    {
         if (args>2)
             errors->add(node, "operator 'input' takes 0, 1 or 2 arguments");
         NEDElement *op1 = node->getFirstChild();
         NEDElement *op2 = op1 ? op1->getNextSibling() : NULL;
         if (args==2)
             if (op2->getTagCode()!=NED_LITERAL || ((LiteralNode *)op2)->getType()!=NED_CONST_STRING)
                 errors->add(node, "second argument to 'input()' must be a string literal (prompt text)");
         NEDElement *parent = node->getParent();
         if (parent->getTagCode()!=NED_EXPRESSION)
             errors->add(node, "'input()' occurs in wrong place");
         return;
    }
    else if (!strcmp(func,"xmldoc"))
    {
         if (args!=1 && args!=2)
             {errors->add(node, "'xmldoc()' takes 1 or 2 arguments");return;}
         NEDElement *op1 = node->getFirstChild();
         NEDElement *op2 = op1 ? op1->getNextSibling() : NULL;
         if (op1->getTagCode()!=NED_LITERAL || ((LiteralNode *)op1)->getType()!=NED_CONST_STRING ||
             (op2 && (op2->getTagCode()!=NED_LITERAL || ((LiteralNode *)op2)->getType()!=NED_CONST_STRING)))
             errors->add(node, "'xmldoc()' arguments must be string literals");
         return;
    }

    // check if we know about it
    bool name_found = false;
    bool argc_matches = false;
    for (int i=0; known_funcs[i].fname!=NULL;i++)
    {
        if (!strcmp(func,known_funcs[i].fname))
        {
            name_found = true;
            if (known_funcs[i].args == args)
            {
                argc_matches = true;
                break;
            }
        }
    }
    if (name_found && !argc_matches)
    {
        errors->add(node, "function '%s' cannot take %d operands", func, args);
    }
}

void NEDBasicValidator::validateElement(IdentNode *node)
{
    //FIXME revise
    const char *expr[] = {"module-index", "param-index"};
    bool opt[] = {true, true};
    checkExpressionAttributes(node, expr, opt, 2);

    // FIXME loopvar and gatename for sizeof is also represented as IdentNode!!!

    // make sure parameter exists
    if (strnull(node->getModule()))
    {
        const char *paramName = node->getName();
        NEDElement *compound = node->getParentWithTag(NED_COMPOUND_MODULE);
        if (!compound)
            INTERNAL_ERROR0(node,"occurs outside a compound-module");
        NEDElement *params = compound->getFirstChildWithTag(NED_PARAMETERS);
        if (!params || params->getFirstChildWithAttribute(NED_PARAM, "name", paramName)==NULL)
        {
            // FIXME TODO
            //if (node->getParentWithTag(NED_FOR_LOOP))
            //    errors->add(node, "no compound module parameter or loop variable named '%s'", paramName);
            //else
            //    errors->add(node, "compound module has no parameter named '%s'", paramName);
        }
    }
}

// TODO merge into Ref code
//void NEDBasicValidator::validateElement(ObsoleteIdentNode *node)
//{
//    // ObsoleteIdentNode may occur: (1) as loop variable inside for-loops (2) argument to sizeof
//    if (node->getParent()->getTagCode()==NED_FUNCTION &&
//        !strcmp(((FunctionNode*)node->getParent())->getName(),"sizeof"))
//        return;
//
//    // make sure ident (loop variable) exists
//    const char *name = node->getName();
//    NEDElement *forloop = node->getParentWithTag(NED_FOR_LOOP);
//    if (!forloop)
//        INTERNAL_ERROR1(node,"loop variable '%s' occurs outside for loop", name);
//    if (forloop->getFirstChildWithAttribute(NED_LOOP_VAR, "param-name", name)==NULL)
//        errors->add(node, "no loop variable named '%s' in enclosing for loop", name);
//}

void NEDBasicValidator::validateElement(LiteralNode *node)
{
    // verify syntax of constant
    int type = node->getType();
    const char *value = node->getValue();
    //const char *text = node->getText();

    // Note: null value is valid as well, because that represents the "" string literal!
    if (strnull(value)) value="";

    if (type==NED_CONST_BOOL)
    {
        // check bool
        if (strcmp(value,"true") && strcmp(value,"false"))
            errors->add(node, "bool constant should be 'true' or 'false'");
        // TBD check that if text is present, it's the same as value
    }
    else if (type==NED_CONST_INT)
    {
        // check int
        char *s;
        strtol(value, &s, 0);
        if (s && *s)
            errors->add(node, "invalid integer constant '%s'", value);
        // TBD check that if text is present, it's the same as value
    }
    else if (type==NED_CONST_DOUBLE)
    {
        // check real
        char *s;
        strtod(value, &s);
        if (s && *s)
            errors->add(node, "invalid real constant '%s'", value);
        // TBD check that if text is present, it's the same as value
    }
    else if (type==NED_CONST_STRING)
    {
        // string: no restriction
        // TBD check that if text is present, it's the same as value
    }
    else if (type==NED_CONST_UNIT)
    {
        // value of a time/bandwidth/etc constant is a real number; text is the original form
        char *s;
        strtod(value, &s);
        if (s && *s)
            errors->add(node, "invalid value for unit constant '%s' (expected as real number)", value);
    }
}

void NEDBasicValidator::validateElement(MsgFileNode *node)
{
}

void NEDBasicValidator::validateElement(CplusplusNode *node)
{
}

void NEDBasicValidator::validateElement(StructDeclNode *node)
{
}

void NEDBasicValidator::validateElement(ClassDeclNode *node)
{
}

void NEDBasicValidator::validateElement(MessageDeclNode *node)
{
}

void NEDBasicValidator::validateElement(EnumDeclNode *node)
{
}

void NEDBasicValidator::validateElement(EnumNode *node)
{
}

void NEDBasicValidator::validateElement(EnumFieldsNode *node)
{
}

void NEDBasicValidator::validateElement(EnumFieldNode *node)
{
}

void NEDBasicValidator::validateElement(MessageNode *node)
{
}

void NEDBasicValidator::validateElement(ClassNode *node)
{
}

void NEDBasicValidator::validateElement(StructNode *node)
{
}

void NEDBasicValidator::validateElement(FieldsNode *node)
{
}

void NEDBasicValidator::validateElement(FieldNode *node)
{
    NEDElement *classNode = node->getParent()->getParent();
    bool isStruct = !strcmp(classNode->getTagName(), "struct");

    if (node->getIsAbstract() && isStruct)
          errors->add(node, "a struct cannot have abstract fields");

    if (node->getIsAbstract() && strnotnull(node->getDefaultValue()))
         errors->add(node, "an abstract field cannot be assigned a default value");

    if (node->getIsVector() && strnull(node->getVectorSize()) && isStruct)
         errors->add(node, "a struct cannot have dynamic array fields");

    // if (strnotnull(node->getDataType())) // type is there
    // {
    //      if (defined in base class too)
    //      {
    //          if (!node->getIsReadonly())
    //              errors->add(node, "field is already declared in a base class (only readonly fields can be overridden)");
    //          if (node->getIsReadonly() && type is not the same)
    //              errors->add(node, "field is already declared in a base class with a different type");
    //      }
    // }

    if (strnull(node->getDataType())) // type is missing
    {
         if (node->getIsAbstract())
             errors->add(node, "an abstract field needs a type");
         if (node->getIsVector())
             errors->add(node, "cannot set array field of the base class");
         if (strnotnull(node->getEnumName()))
             errors->add(node, "cannot specify enum for base class field");
         if (strnull(node->getDefaultValue()))
             errors->add(node, "missing field type");
    }

    // TBD check syntax of default value, and that its type agrees with field type

}

void NEDBasicValidator::validateElement(PropertiesNode *node)
{
}

void NEDBasicValidator::validateElement(MsgpropertyNode *node)
{
    // TBD check syntax of value
}


void NEDBasicValidator::validateElement(UnknownNode *node)
{
}

