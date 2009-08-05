//==========================================================================
//   CXMLPAR.H  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CXMLPAR_H
#define __CXMLPAR_H

#include "cparvalue.h"

/**
 * FIXME revise docu in the whole class!!!!!!
 *
 * @ingroup Internals
 */
class SIM_API cXMLPar : public cParValue
{
  protected:
    // selector: flags & FL_ISEXPR
    cExpression *expr;
    cXMLElement *val;

  protected:
    cXMLElement *evaluate(cComponent *context) const;
    void deleteOld();

  public:
    /** @name Constructors, destructor, assignment. */
    //@{

    /**
     * Constructor.
     */
    explicit cXMLPar();

    /**
     * Copy constructor.
     */
    cXMLPar(const cXMLPar& other) {setName(other.name()); operator=(other);}

    /**
     * Destructor.
     */
    virtual ~cXMLPar();

    /**
     * Assignment operator.
     */
    void operator=(const cXMLPar& otherpar);
    //@}

    /** @name Redefined cObject member functions */
    //@{

    /**
     * Creates and returns an exact copy of this object.
     */
    virtual cXMLPar *dup() const  {return new cXMLPar(*this);}

    /**
     * Returns a multi-line description of the contained XML element.
     */
    virtual std::string detailedInfo() const;

    /**
     * Serializes the object into a buffer.
     */
    virtual void netPack(cCommBuffer *buffer);

    /**
     * Deserializes the object from a buffer.
     */
    virtual void netUnpack(cCommBuffer *buffer);
    //@}

    /** @name Redefined cParValue setter functions. */
    //@{

    /**
     * Raises an error: cannot convert bool to XML.
     */
    virtual void setBoolValue(bool b);

    /**
     * Raises an error: cannot convert long to XML.
     */
    virtual void setLongValue(long l);

    /**
     * Raises an error: cannot convert double to XML.
     */
    virtual void setDoubleValue(double d);

    /**
     * Raises an error: cannot convert string to XML.
     */
    virtual void setStringValue(const char *s);

    /**
     * Sets the value to the given cXMLElement tree.
     */
    virtual void setXMLValue(cXMLElement *node);

    /**
     * Sets the value to the given expression. This object will
     * assume the responsibility to delete the expression object.
     */
    virtual void setExpression(cExpression *e);
    //@}

    /** @name Redefined cParValue getter functions. */
    //@{

    /**
     * Raises an error: cannot convert XML to bool.
     */
    virtual bool boolValue(cComponent *context) const;

    /**
     * Raises an error: cannot convert XML to long.
     */
    virtual long longValue(cComponent *context) const;

    /**
     * Raises an error: cannot convert XML to double.
     */
    virtual double doubleValue(cComponent *context) const;

    /**
     * Raises an error: cannot convert XML to string.
     */
    virtual const char *stringValue(cComponent *context) const;

    /**
     * Raises an error: cannot convert XML to string.
     */
    virtual std::string stdstringValue(cComponent *context) const;

    /**
     * Returns the value of the parameter.
     */
    virtual cXMLElement *xmlValue(cComponent *context) const;

    /**
     * Returns pointer to the expression stored by the object, or NULL.
     */
    virtual cExpression *expression() const;
    //@}

    /** @name Type, prompt text, input flag, change flag. */
    //@{

    /**
     * Returns XML.
     */
    virtual Type type() const;

    /**
     * Returns false.
     */
    virtual bool isNumeric() const;
    //@}

    /** @name Redefined cParValue misc functions. */
    //@{

    /**
     * Replaces for non-const values, replaces the stored expression with its
     * evaluation.
     */
    virtual void convertToConst(cComponent *context);

    /**
     * Returns the value in text form.
     */
    virtual std::string toString() const;

    /**
     * Converts from text.
     */
    virtual bool parse(const char *text);

    /**
     * Object comparison.
     */
    virtual int compare(const cParValue *other) const;
    //@}
};

#endif


