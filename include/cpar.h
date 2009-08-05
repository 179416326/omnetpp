//==========================================================================
//   CPAR.H  - part of
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

#ifndef __CPAR_H
#define __CPAR_H

#include "cownedobject.h"
#include "cexpression.h"
#include "cexception.h"

class cParValue;
class cExpression;
class cXMLElement;
class cProperties;
class cComponent;

//FIXME we could this cObject, noncopyable (and cParValue cObject); maybe cGate too?
// only problem: how to inspect them in Tkenv?
// inspectors could be changed to cObject-based, but ev.objectDeleted() is still cOwnedObject!!!
// if we put ev.objectDeleted() into cObject's dtor, there'll be too many notifications (??? -- controlInfo objects are all cObject)
// should we make a cNotifyingPolymorphic from which we subclass cPar and cGate???
//               or name this ^^^ cLightweightObject ? (cPolym + ev.objectDeleted())
// how fast is an std::map lookup? that's the cost of an ev.objectDeleted() call

/**
 * FIXME
 *
 * Delegates everything to cParValue.
 *
 * Not meant for subclassing. This is also the reason why none of the
 * methods are virtual.
 *
 * @ingroup SimCore
 */
class SIM_API cPar : public cObject  //turn into cObject; delegate name to p! add ownercomponent!
{
  friend class cComponent;
  private:
    cComponent *ownercomponent;
    cParValue *p;
  public:
    enum Type {
        BOOL = 'B',
        DOUBLE = 'D',
        LONG = 'L',
        STRING = 'S',
        XML = 'X'
    };

  private:
    cPar(cComponent *component, cParValue *p);
    // internal utility function
    void copyIfShared();
    // internal: called each time after the value of this object changes.
    void afterChange();
    // internal: called as part of read(): gets the value from omnetpp.ini or from the user.
    void doReadValue();

  public:
    // internal, for netbuilder
    void reassign(cParValue *p);

  public:
    /**
     * Destructor.
     */
    virtual ~cPar();

    /**
     * Returns the parameter name.
     */
    virtual const char *name() const;

    /**
     * Returns a one-line description of the object.
     * @see detailedInfo()
     */
    virtual std::string info() const;

    /**
     * Returns a long description of the object.
     */
    virtual std::string detailedInfo() const;

    /**
     * Returns the component (module/channel) this parameter belongs to.
     */
    virtual cObject *owner() const;

    /**
     * Assignment
     */
    void operator=(const cPar& other);
    //@}

    /** @name Type, flags. */
    //@{
    /**
     * Returns the parameter type
     */
    Type type() const;

    /**
     * Returns the given type as a string.
     */
    static const char *typeName(Type t);

    /**
     * Returns true if the stored value is of a numeric type.
     */
    bool isNumeric() const;

    /**
     * Returns true if this parameter is marked in the NED file as "function".
     * This flag affects the operation of setExpression().
     */
    bool isVolatile() const;

    /**
     * Returns true if the stored value is a constant, anf false if it is
     * an expression. (It is not examined whether the expression yields
     * a constant value or not.)
     */
    bool isConstant() const;

    /**
     * Returns true if the parameter value expression is shared among several
     * modules to save memory. This flag is purely informational, and whether
     * a parameter is shared or not does not affect anything at all
     * (except memory footprint of the whole simulation).
     */
    bool isShared() const;

    /**
     * Return the properties for this parameter. Properties cannot be changed
     * at runtime.
     */
    cProperties *properties() const;
    //@}

    /** @name Setter functions. Note that overloaded assignment operators also exist. */
    //@{

    /**
     * Sets the value to the given bool value.
     */
    cPar& setBoolValue(bool b);

    /**
     * Sets the value to the given long value.
     */
    cPar& setLongValue(long l);

    /**
     * Sets the value to the given double value.
     */
    cPar& setDoubleValue(double d);

    /**
     * Sets the value to the given string value.
     * The cPar will make its own copy of the string. NULL is also accepted
     * and treated as an empty string.
     */
    cPar& setStringValue(const char *s);

    /**
     * Sets the value to the given string value.
     */
    cPar& setStringValue(const std::string& s)  {setStringValue(s.c_str()); return *this;}

    /**
     * Sets the value to the given cXMLElement.
     */
    cPar& setXMLValue(cXMLElement *node);

    /**
     * Sets the value to the given expression. This object will assume
     * the responsibility to delete the expression object.
     *
     * Note: if the parameter is marked as non-volatile (isVolatile()==false),
     * one should not set an expression as value. This is not enforced
     * by cPar though.
     */
    cPar& setExpression(cExpression *e);
    //@}

    /** @name Getter functions. Note that overloaded conversion operators also exist. */
    //@{

    /**
     * Returns value as a boolean. The cPar type must be BOOL.
     */
    bool boolValue() const;

    /**
     * Returns value as long. The cPar type must be LONG or DOUBLE.
     */
    long longValue() const;

    /**
     * Returns value as double. The cPar type must be LONG or DOUBLE.
     */
    double doubleValue() const;

    /**
     * Returns value as const char *. The cPar type must be STRING.
     * This method may can only be invoked when the parameter's value is a
     * string constant and not the result of expression evaluation (otherwise
     * an error is thrown). This practically means this method cannot be used
     * on parameters declared as "volatile string" in NED; they can only be
     * accessed using stdstringValue().
     */
    const char *stringValue() const;

    /**
     * Returns value as string. The cPar type must be STRING.
     */
    std::string stdstringValue() const;

    /**
     * Returns value as pointer to cXMLElement. The cPar type must be XML.
     */
    cXMLElement *xmlValue() const;

    /**
     * Returns pointer to the expression stored by the object, or NULL.
     */
    cExpression *expression() const;
    //@}

    /** @name Miscellaneous utility functions. */
    //@{

    /**
     * This method does the final touches on the parameter. It is invoked
     * at some point on all parameter objects before we start the simulation.
     *
     * - if the parameter is not set, gets the value from omnetpp.ini or
     *   interactively from the user, or sets the default value.
     *
     * - if the parameter is non-volatile, (isVolatile()==false), converts
     *   possible expression value to a constant (see convertToConst()).
     *
     * - if the parameter is volatile but contains "const" subexpressions,
     *   these parts are converted to a constant value.
     */
    void read();

    /**
     * Replaces for non-const values, replaces the stored expression with its
     * evaluation.
     */
    void convertToConst();

    /**
     * Returns the value in text form.
     */
    std::string toString() const;

    /**
     * Convert the value from string, and store the result.
     * Returns true on success, false otherwise. No error message is generated.
     */
    bool parse(const char *text);
    //@}

    /** @name Overloaded assignment and conversion operators. */
    //@{

    /**
     * Equivalent to setBoolValue().
     */
    cPar& operator=(bool b)  {return setBoolValue(b);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(char c)  {return setLongValue((long)c);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned char c)  {return setLongValue((long)c);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(int i)  {return setLongValue((long)i);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned int i)  {return setLongValue((long)i);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(short i)  {return setLongValue((long)i);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned short i)  {return setLongValue((long)i);}

    /**
     * Equivalent to setLongValue().
     */
    cPar& operator=(long l)  {return setLongValue(l);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned long l) {return setLongValue((long)l);}

    /**
     * Equivalent to setDoubleValue().
     */
    cPar& operator=(double d)  {return setDoubleValue(d);}

    /**
     * Converts the argument to double, and calls setDoubleValue().
     */
    cPar& operator=(long double d)  {return setDoubleValue((double)d);}

    /**
     * Equivalent to setStringValue().
     */
    cPar& operator=(const char *s)  {return setStringValue(s);}

    /**
     * Equivalent to setStringValue().
     */
    cPar& operator=(const std::string& s)  {return setStringValue(s);}

    /**
     * Equivalent to setXMLValue().
     */
    cPar& operator=(cXMLElement *node)  {return setXMLValue(node);}

    /**
     * Equivalent to boolValue().
     */
    operator bool() const  {return boolValue();}

    /**
     * Calls longValue() and converts the result to char.
     */
    operator char() const  {return (char)longValue();}

    /**
     * Calls longValue() and converts the result to unsigned char.
     */
    operator unsigned char() const  {return (unsigned char)longValue();}

    /**
     * Calls longValue() and converts the result to int.
     */
    operator int() const  {return (int)longValue();}

    /**
     * Calls longValue() and converts the result to unsigned int.
     */
    operator unsigned int() const  {return (unsigned int)longValue();}

    /**
     * Calls longValue() and converts the result to short.
     */
    operator short() const  {return (short)longValue();}

    /**
     * Calls longValue() and converts the result to unsigned short.
     */
    operator unsigned short() const  {return (unsigned short)longValue();}

    /**
     * Equivalent to longValue().
     */
    operator long() const  {return longValue();}

    /**
     * Calls longValue() and converts the result to unsigned long.
     */
    operator unsigned long() const  {return longValue();}

    /**
     * Equivalent to doubleValue().
     */
    operator double() const  {return doubleValue();}

    /**
     * Calls doubleValue() and converts the result to long double.
     */
    operator long double() const  {return doubleValue();}

    /**
     * Equivalent to stringValue().
     */
    operator const char *() const  {return stringValue();}

    /**
     * Equivalent to stdstringValue().
     */
    operator std::string() const  {return stdstringValue();}

    /**
     * Equivalent to xmlValue().
     */
    operator cXMLElement *() const  {return xmlValue();}

    //@}

    /** @name Compare functions */
    //@{

    /**
     * Evaluates both expressions and compares the resulting values.
     * If either of the objects is not set (isSet()==false) or they are
     * of different type(), false is returned.
     */
    bool equals(cPar& other);
    //@}
};

#endif




