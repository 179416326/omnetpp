//==========================================================================
// neddtdvalidatorbase.cc -
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <ctype.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>
#include "nederror.h"
#include "neddtdvalidatorbase.h"


void NEDDTDValidatorBase::checkSequence(NEDElement *node, int tags[], char mult[])
{
    NEDElement *p = node->getFirstChild();
    for (int i=0; tags[i]; i++)
    {
       switch (mult[i])
       {
         case '1':
           if (!p || p->getTagCode()!=tags[i])
               {errors->add(node,"DTD validation error: child element '%s' unexpected", (p?p->getTagName():"")); return;}
           p = p->getNextSibling();
           break;
         case '?':
           if (p && p->getTagCode()==tags[i])
               p = p->getNextSibling();
           break;
         case '+':
           if (!p || p->getTagCode()!=tags[i])
               {errors->add(node, "DTD validation error: child element '%s' unexpected", (p?p->getTagName():"")); return;}
           p = p->getNextSibling();
           while (p && p->getTagCode()==tags[i])
               p = p->getNextSibling();
           break;
         case '*':
           while (p && p->getTagCode()==tags[i])
               p = p->getNextSibling();
           break;
       }
    }
    if (p)
        errors->add(node, "DTD validation error: child element '%s' unexpected", p->getTagName());
}

static int isInVector(int a, int v[])
{
    for (int i=0; v[i]; i++)  // v[] is zero-terminated
        if (v[i]==a)
            return true;
    return false;
}

void NEDDTDValidatorBase::tryCheckChoice(NEDElement *node, NEDElement *&curchild, int tags[], char mult)
{
    // note: 'node' argument is solely used by errors->add() (when curchild==NULL)
    if (mult=='?')
    {
        // skip optional matching element
        if (curchild && isInVector(curchild->getTagCode(), tags))
            curchild = curchild->getNextSibling();
    }
    else if (mult=='1' || mult=='+')
    {
        // match and skip first element of "1" or "+" sequence
        if (!curchild || !isInVector(curchild->getTagCode(), tags))
            {errors->add(node,"DTD validation error: child element of multiplicity '1' or '+' missing"); return;}
        curchild = curchild->getNextSibling();
    }

    if (mult=='+' || mult=='*')
    {
        // skip potential further elements of "+" or "*" sequence
        while (curchild && isInVector(curchild->getTagCode(), tags))
            curchild = curchild->getNextSibling();
    }
}

void NEDDTDValidatorBase::checkChoice(NEDElement *node, int tags[], char mult)
{
    NEDElement *curchild = node->getFirstChild();
    tryCheckChoice(node, curchild, tags, mult);
    if (curchild)
        errors->add(node,"DTD validation error: child element '%s' unexpected", curchild->getTagName());
}

void NEDDTDValidatorBase::checkSeqOfChoices(NEDElement *node, Choice choices[], int n)
{
    NEDElement *curchild = node->getFirstChild();
    for (int i=0; i<n; i++)
        tryCheckChoice(node, curchild, choices[i].tags, choices[i].mult);
    if (curchild)
        errors->add(node,"DTD validation error: child element '%s' unexpected", curchild->getTagName());
}

void NEDDTDValidatorBase::checkEmpty(NEDElement *node)
{
    if (node->getFirstChild())
        errors->add(node,"DTD validation error: EMPTY element has children\n");
}

void NEDDTDValidatorBase::checkRequiredAttribute(NEDElement *node, const char *attr)
{
    const char *s = node->getAttribute(attr);
    assert(s);
    if (!*s)
        errors->add(node,"DTD validation error: required attribute '%s' is empty", attr);
}

void NEDDTDValidatorBase::checkEnumeratedAttribute(NEDElement *node, const char *attr, const char *vals[], int n)
{
    const char *s = node->getAttribute(attr);
    assert(s);
    for (int i=0; i<n; i++)
        if (!strcmp(s, vals[i]))
            return;
    if (n==0)
        INTERNAL_ERROR1(node,"no allowed values for enumerated attribute '%s'", attr);
    errors->add(node,"DTD validation error: invalid value '%s' for attribute '%s', not one of the "
                     "enumerated values ('%s',...)", s, attr, vals[0]);
}

void NEDDTDValidatorBase::checkNameAttribute(NEDElement *node, const char *attr)
{
    const char *s = node->getAttribute(attr);
    assert(s);
    if (!*s)
        return;
    if (!isalpha(*s) && *s!='_')
        errors->add(node,"DTD validation error: attribute %s='%s' starts with invalid character (valid NED identifier expected)", attr, node->getAttribute(attr));
    while (*++s)
        if (!isalpha(*s) && !isdigit(*s) && *s!='_')
            {errors->add(node,"DTD validation error: attribute %s='%s' contains invalid character (valid NED identifier expected)", attr, node->getAttribute(attr)); return;}
}

void NEDDTDValidatorBase::checkCommentAttribute(NEDElement *node, const char *attr)
{
    const char *s = node->getAttribute(attr);
    assert(s);
    if (!*s)
        return;
    bool incomment = false;
    for (; *s; s++)
    {
        if (!incomment)
        {
            if (*s=='/' && *(s+1)=='/')
                {s++; incomment = true;}
            else if (*s!=' ' && *s!='\t' && *s!='\n' && *s!='\r')
                {errors->add(node,"DTD validation error: attribute %s='%s' does not contain a valid NED comment", attr, node->getAttribute(attr)); return;}
        }
        else if (incomment)
        {
            if (*s=='\n' || *s=='\r')
                incomment = false;
        }
    }
}

void NEDDTDValidatorBase::checkNMTokenAttribute(NEDElement *node, const char *attr)
{
    //
    // DEVIATION FROM W3C STANDARDS. NMTOKEN should allow letters, digits, period,
    // dash, underscore, colon, combining characters, extenders. Letters would include
    // international characters as well. However, in our NED DTD we use NMTOKEN for
    // NED (and C++) identifiers which allow ASCII letters, digits and underscore ONLY.
    //
    const char *s = node->getAttribute(attr);
    assert(s);
    if (!*s)
        return;
    for (; *s; s++)
        if (!isalpha(*s) && !isdigit(*s) && *s!='_')
            {errors->add(node,"DTD validation error: attribute %s='%s' contains invalid character", attr, node->getAttribute(attr)); return;}
}



