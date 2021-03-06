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
package coyote.dx.context;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coyote.commons.StringUtil;
import coyote.commons.template.Template;
import coyote.dataframe.DataField;
import coyote.dx.Database;
import coyote.dx.Symbols;
import coyote.dx.TransformEngine;
import coyote.loader.cfg.Config;


/**
 * This is an operational context for the transformation job as a whole.
 */
public class TransformContext extends OperationalContext {

  protected Config configuration = new Config();

  protected Map<String, Database> databases = new HashMap<String, Database>();

  protected TransformEngine engine = null;

  private volatile TransactionContext transactionContext = null;

  private static final String WORKING = "Working.";
  private static final String SOURCE = "Source.";
  private static final String TARGET = "Target.";

  protected volatile long openCount = 0;



  public TransformContext( List<ContextListener> listeners ) {
    super( listeners );
  }




  public TransformContext() {
    super();
  }




  public void addDataStore( Database store ) {
    if ( store != null && StringUtil.isNotBlank( store.getName() ) ) {
      databases.put( store.getName(), store );
    }
  }




  public Database getDatabase( String name ) {
    return databases.get( name );
  }




  /**
   * Open (initialize) the context.
   * 
   * <p>This is called when the engine begins running and is where we use the 
   * currently set symbol table to resolve all the variables set in this 
   * context.</p>
   */
  public void open() {

    // Increment the run count
    openCount++;
    engine.getSymbolTable().put( Symbols.RUN_COUNT, openCount );

    // If we have a configuration...
    if ( configuration != null ) {
      // fill the context with configuration data
      for ( DataField field : configuration.getFields() ) {
        if ( !field.isFrame() ) {
          if ( StringUtil.isNotBlank( field.getName() ) && !field.isNull() ) {
            String token = field.getStringValue();
            String value = Template.resolve( token, engine.getSymbolTable() );
            engine.getSymbolTable().put( field.getName(), value );
            set( field.getName(), value );
          } //name-value check
        } // if frame
      } // for
    }

  }




  /**
   * Close (terminate) the context.
   */
  public void close() {
    for ( Map.Entry<String, Database> entry : databases.entrySet() ) {
      //System.out.printf("Closing : %s %n", entry.getKey());
      try {
        entry.getValue().close();
      } catch ( IOException ignore ) {
        //System.out.printf("Problems closing : %s - %s %n", entry.getKey(), ignore.getMessage());
      }
    }

    // Set the closing disposition of the job 
    Map<String, Object> disposition = new HashMap<String, Object>();
    disposition.put( Symbols.RUN_COUNT, openCount);
    disposition.put( "StartTime", startTime);
    disposition.put( "EndTime", endTime);
    disposition.put( "ErrorState", errorFlag);
    disposition.put( "ErrorMessage", errorMessage);
    disposition.put( "FrameCount", currentFrame);
    properties.put( "TransformDisposition", disposition );
  }




  /**
   * @param engine the engine to which this context is associated.
   */
  public void setEngine( TransformEngine engine ) {
    this.engine = engine;
  }




  /**
   * @return the engine to which this context is associated.
   */
  protected TransformEngine getEngine() {
    return engine;
  }




  /**
   * Sets the current transaction in the transformation context.
   * 
   * <p>This allows all components in the transformation engine to access the 
   * current transaction frames.</p>
   * 
   * @param context the current transaction context being processed
   */
  public void setTransaction( TransactionContext context ) {
    transactionContext = context;
  }




  /**
  * @return the current transaction context
  */
  public TransactionContext getTransaction() {
    return transactionContext;
  }




  /**
   * Return the value of something in this transform context to a string value.
   * 
   * <p>The token is scanned for a prefix to determine if it is a field name 
   * in one of the data frames in the transaction context. If not, the token 
   * is checked against object values in the context and the string value of 
   * any returned object in the context with that name is returned. Finally, 
   * if the context did not contain an object with a name matching the token, 
   * the symbol table is checked and any matching symbol is returned.
   * 
   * <p>Matching is case sensitive.
   * 
   * <p>Naming Conventions:<ul>
   * <li>Working.[field name] field in the working frame of the transaction 
   * context.
   * <li>Source.[field name] field in the source frame of the transaction 
   * context.
   * <li>Target.[field name] field in the target frame of the transaction 
   * context.
   * <li>[field name] field in the working frame of the transaction
   * <li>[field name] context value
   * <li>[field name] symbol value</ul>
   * 
   * @param token the name of the field, context or symbol value
   *  
   * @return the string value of the named value in this context or null if not found.
   */
  public String resolveToString( String token ) {
    String retval = null;
    String value = resolveField( token );
    if ( value != null ) {
      retval = value;
    } else {
      Object obj = get( token );
      if ( obj != null ) {
        retval = obj.toString();
      } else {
        if ( symbols != null ) {
          if ( symbols.containsKey( token ) ) {
            retval = symbols.getString( token );
          }
        }
      }
    }
    return retval;
  }




  /**
   * Return the value of a data frame field currently set in the transaction 
   * context of this context.
   * 
   * <p>Matching is case sensitive.
   * 
   * <p>Naming Conventions:<ul>
   * <li>Working.[field name] field in the working frame of the transaction 
   * context.
   * <li>Source.[field name] field in the source frame of the transaction 
   * context.
   * <li>Target.[field name] field in the target frame of the transaction 
   * context.
   * <li>[field name] field in the working frame of the transaction</ul>
   * 
   * @param token the name of the field, context or symbol value
   *  
   * @return the string value of the named value in this context or null if not found.
   */
  public String resolveField( String token ) {
    String retval = null;
    if ( token.startsWith( WORKING ) ) {
      String name = token.substring( WORKING.length() );
      if ( transactionContext != null && transactionContext.getWorkingFrame() != null ) {
        retval = transactionContext.getWorkingFrame().getAsString( name );
      }
    } else if ( token.startsWith( SOURCE ) ) {
      String name = token.substring( SOURCE.length() );
      if ( transactionContext != null && transactionContext.getSourceFrame() != null ) {
        retval = transactionContext.getSourceFrame().getAsString( name );
      }
    } else if ( token.startsWith( TARGET ) ) {
      String name = token.substring( TARGET.length() );
      if ( transactionContext != null && transactionContext.getTargetFrame() != null ) {
        retval = transactionContext.getTargetFrame().getAsString( name );
      }
    } else {
      // assume a working frame field
      if ( transactionContext != null && transactionContext.getWorkingFrame() != null ) {
        retval = transactionContext.getWorkingFrame().getAsString( token );
      }
    }
    return retval;
  }


  public boolean containsField( String token ) {
    Boolean retval = false;
    if ( token.startsWith( WORKING ) ) {
      String name = token.substring( WORKING.length() );
      if ( transactionContext != null && transactionContext.getWorkingFrame() != null ) {
        retval = transactionContext.getWorkingFrame().contains( name );
      }
    } else if ( token.startsWith( SOURCE ) ) {
      String name = token.substring( SOURCE.length() );
      if ( transactionContext != null && transactionContext.getSourceFrame() != null ) {
        retval = transactionContext.getSourceFrame().contains( name );
      }
    } else if ( token.startsWith( TARGET ) ) {
      String name = token.substring( TARGET.length() );
      if ( transactionContext != null && transactionContext.getTargetFrame() != null ) {
        retval = transactionContext.getTargetFrame().contains( name );
      }
    } else {
      // assume a working frame field
      if ( transactionContext != null && transactionContext.getWorkingFrame() != null ) {
        retval = transactionContext.getWorkingFrame().contains( token );
      }
    }
    return retval;
  }


  /**
   * Resolve the argument.
   * 
   * <p>This will try to retrieve the value from the transform context using 
   * the given value as it may be a reference to a context property.</p>
   * 
   * <p>If no value was found in the look-up, then the value is treated as a 
   * literal and will be returned as the argument.</p>
   * 
   * <p>Regardless of whether or not the value was retrieved from the 
   * transform context as a reference value, the value is resolved as a 
   * template using the symbol table in the transform context. This allows for 
   * more dynamic values during the operation of the entire transformation 
   * process.</p>
   * 
   * @param value the value to resolve (or use as a literal)
   * 
   * @return the resolved value of the argument. 
   */
  public String resolveArgument( String value ) {
    String retval = null;

    // lookup the value in the transform context
    String cval = getAsString( value );

    // If the lookup failed, just use the value
    if ( StringUtil.isBlank( cval ) ) {
      cval = value;
    }

    // in case it is a template, resolve it to the context's symbol table
    if ( StringUtil.isNotBlank( cval ) ) {
      retval = Template.resolve( cval, getSymbols() );
    }
    return retval;
  }




  public void setConfiguration( Config config ) {
    configuration = config;
  }




  public Config getConfiguration() {
    return configuration;
  }




  /**
   * reset the context so it can be used again in subsequent (scheduled) runs
   */
  public void reset() {
    super.currentFrame = 0;
    super.startTime = 0;
    super.endTime = 0;
    super.errorFlag = false;
    super.errorMessage = null;
  }




}
