/*
 * Copyright (c) 2015 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and implementation
 */
package coyote.dx.eval;

import java.util.Arrays;
import java.util.Iterator;

import coyote.commons.StringUtil;
import coyote.commons.eval.AbstractEvaluator;
import coyote.commons.eval.BracketPair;
import coyote.commons.eval.Constant;
import coyote.commons.eval.Method;
import coyote.commons.eval.Operator;
import coyote.commons.eval.Parameters;
import coyote.dx.context.TransformContext;


/**
 * 
 */
public class BooleanEvaluator extends AbstractEvaluator<Boolean> {

  private static final String LITERAL_TRUE = "true";
  private static final String LITERAL_FALSE = "false";

  /** The transformation context from which we retrieve data */
  TransformContext transformContext = null;

  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
  // Operators
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  /** The negate unary operator.*/
  public final static Operator NEGATE = new Operator( "!", 1, Operator.Associativity.RIGHT, 3 );

  /** The logical AND operator.*/
  private static final Operator AND = new Operator( "&&", 2, Operator.Associativity.LEFT, 2 );

  /** The logical OR operator.*/
  public final static Operator OR = new Operator( "||", 2, Operator.Associativity.LEFT, 1 );

  /** The standard whole set of predefined operators */
  private static final Operator[] OPERATORS = new Operator[] { NEGATE, AND, OR };
  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
  // Methods
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /** Performs a case sensitive comparison between two string values */
  public static final Method EQUALS = new Method( "equals", 2 );

  /** Performs a regular expression match on the value of a field */
  public static final Method REGEX = new Method( "regex", 2 );

  /** Performs a case insensitive comparison between two string values*/
  public static final Method MATCH = new Method( "match", 2 );

  /** Checks if the given field contains a value */
  public static final Method EMPTY = new Method( "empty", 1 );

  /** Checks if the given field exists in the context */
  public static final Method EXISTS = new Method( "exists", 1 );

  /** The whole set of predefined functions */
  private static final Method[] METHODS = new Method[] { MATCH, EMPTY, EXISTS, REGEX, EQUALS };
  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
  // Constants
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  /** A constant that represents the current state of the isLastFrame() method call in the transaction context */
  public static final Constant LAST = new Constant( "islast" );

  /** The whole set of predefined constants */
  private static final Constant[] CONSTANTS = new Constant[] { LAST };
  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

  // Our default parameters
  private static Parameters DEFAULT_PARAMETERS;




  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  private static Parameters getParameters() {
    if ( DEFAULT_PARAMETERS == null ) {
      DEFAULT_PARAMETERS = getDefaultParameters();
    }
    return DEFAULT_PARAMETERS;
  }




  /**
   * Default constructor which uses the default evaluation parameters.
   */
  public BooleanEvaluator() {
    this( getParameters() );
  }




  /**
   * Our private constructor which uses the given evaluation parameters
   * 
   * @param parameters the evaluation parameters this evaluator should use
   */
  private BooleanEvaluator( Parameters parameters ) {
    super( parameters );
  }




  /**
   * Gets a copy of the default parameters.
   * 
   * <p>The returned parameters contains all the predefined operators, 
   * functions and constants.</p>
   * 
   * <p>Each call to this method create a new instance of Parameters.</p>
   *  
   * @return a Parameters instance
   */
  public static Parameters getDefaultParameters() {
    final Parameters retval = new Parameters();
    retval.addOperators( Arrays.asList( OPERATORS ) );
    retval.addMethods( Arrays.asList( METHODS ) );
    retval.addConstants( Arrays.asList( CONSTANTS ) );
    retval.addFunctionBracket( BracketPair.PARENTHESES );
    retval.addExpressionBracket( BracketPair.PARENTHESES );
    return retval;
  }




  /**
   * Return the value of a literal.
   * 
   * @see coyote.commons.eval.AbstractEvaluator#toValue(java.lang.String, java.lang.Object)
   */
  @Override
  protected Boolean toValue( String literal, Object evaluationContext ) {
    if ( LITERAL_TRUE.equalsIgnoreCase( literal ) || LITERAL_FALSE.equalsIgnoreCase( literal ) ) {
      return Boolean.valueOf( literal );
    } else {
      throw new IllegalArgumentException( "'" + literal + "' is not a valid boolean literal" );
    }
  }




  /**
   * Return the value of a method and its string arguments.
   * 
   * @see coyote.commons.eval.AbstractEvaluator#evaluate(coyote.commons.eval.Method, java.util.Iterator, java.lang.Object)
   */
  @Override
  protected Boolean evaluate( Method method, Iterator<String> arguments, Object evaluationContext ) {
    Boolean result;
    if ( EQUALS.equals( method ) ) {
      String arg2 = arguments.next();
      String arg1 = arguments.next();
      // do the thing with the stuff here
      result = performEquals( arg1, arg2 );
    } else if ( REGEX.equals( method ) ) {
      String arg2 = arguments.next();
      String arg1 = arguments.next();
      // do the thing with the stuff here
      result = performRegex( arg1, arg2 );
    } else if ( MATCH.equals( method ) ) {
      String arg2 = arguments.next();
      String arg1 = arguments.next();
      // do the thing with the stuff here
      result = performMatch( arg1, arg2 );
    } else if ( EMPTY.equals( method ) ) {
      String arg1 = arguments.next();
      // do the thing with the stuff here
      result = performEmpty( arg1 );
    } else if ( EXISTS.equals( method ) ) {
      String arg1 = arguments.next();
      // do the thing with the stuff here
      result = performExists( arg1 );
    } else {
      result = super.evaluate( method, arguments, evaluationContext );
    }

    return result;

  }




  /**
   * Return the value of a constant.
   * 
   * @see coyote.commons.eval.AbstractEvaluator#evaluate(coyote.commons.eval.Constant, java.lang.Object)
   */
  @Override
  protected Boolean evaluate( final Constant constant, final Object evaluationContext ) {
    if ( LAST.equals( constant ) ) {
      if ( transformContext != null && transformContext.getTransaction() != null ) {
        return new Boolean( transformContext.getTransaction().isLastFrame() );
      } else {
        return new Boolean( false );
      }
    } else {
      return super.evaluate( constant, evaluationContext );
    }
  }




  /**
   * @see coyote.commons.eval.AbstractEvaluator#evaluate(coyote.commons.eval.Operator, java.util.Iterator, java.lang.Object)
   */
  @Override
  protected Boolean evaluate( Operator operator, Iterator<Boolean> operands, Object evaluationContext ) {
    if ( operator == NEGATE ) {
      return !operands.next();
    } else if ( operator == OR ) {
      Boolean o1 = operands.next();
      Boolean o2 = operands.next();
      return o1 || o2;
    } else if ( operator == AND ) {
      Boolean o1 = operands.next();
      Boolean o2 = operands.next();
      return o1 && o2;
    } else {
      return super.evaluate( operator, operands, evaluationContext );
    }
  }




  public void setContext( TransformContext context ) {
    transformContext = context;
  }




  /**
   * Perform a case insensitive match between the two arguments.
   * 
   * <p>If the arguments did not return a frame value, assume a quoted string. 
   * And if the argument is still null, just use the raw argument.
   * 
   * @param arg1
   * @param arg2
   * 
   * @return true if a arguments match, false otherwise
   */
  private boolean performMatch( String arg1, String arg2 ) {
    if ( transformContext != null ) {
      String value = transformContext.resolveToString( arg1 );
      if ( value == null ) {
        value = StringUtil.getQuotedValue( arg1 );
        if ( value == null ) {
          value = arg1;
        }
      }
      String test = transformContext.resolveToString( arg2 );
      if ( test == null ) {
        test = StringUtil.getQuotedValue( arg2 );
        if ( test == null ) {
          test = arg2;
        }
      }

      if ( value.equalsIgnoreCase( test ) ) {
        return true;
      }
    } else {
      return false;
    }
    return false;
  }




  private Boolean performEquals( String arg1, String arg2 ) {
    if ( transformContext != null ) {
      String value = transformContext.resolveToString( arg1 );
      if ( value == null ) {
        value = StringUtil.getQuotedValue( arg1 );
        if ( value == null ) {
          value = arg1;
        }
      }
      String test = transformContext.resolveToString( arg2 );
      if ( test == null ) {
        value = StringUtil.getQuotedValue( arg2 );
        if ( value == null ) {
          value = arg2;
        }
      }

      if ( value.equals( test ) ) {
        return true;
      }
    } else {
      return false;
    }
    return false;
  }




  private Boolean performRegex( String arg1, String arg2 ) {
    Boolean retval = Boolean.FALSE;
    // TODO Auto-generated method stub
    return retval;
  }




  private Boolean performEmpty( String arg1 ) {
    Boolean retval = Boolean.FALSE;
    // TODO Auto-generated method stub
    return retval;
  }




  private Boolean performExists( String arg1 ) {
    Boolean retval = Boolean.FALSE;
    // TODO Auto-generated method stub
    return retval;
  }

}
