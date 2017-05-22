/*
 * Copyright (c) 2017 Stephan D. Cote' - All rights reserved.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import coyote.dataframe.DataFrame;
import coyote.dataframe.marshal.JSONMarshaler;


/**
 * 
 */
public class DatabaseContextTest {

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}




  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {}




  @Test
  public void contextWithLibraryAttribute() {
      DataFrame config = new DataFrame().set( "Context", new DataFrame()
        .set( "Class","DatabaseContext" )
        .set( "Target","jdbc:h2:[#$jobdir#]/test;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" )
        .set( "autocreate",true )
        .set( "library","jar:file:lib/ojdbc7_g.jar!/" )
        .set( "driver","org.h2.Driver" )
        .set( "username","sa" )
        .set( "password","" )
        .set( "fields",new DataFrame()
            .set( "SomeKey","SomeValue" )
            .set( "AnotherKey","AnotherValue" )
          )
      );
    
    System.out.println( JSONMarshaler.toFormattedString( config ) );
  }




  @Test
  public void contextWithoutLibraryAttribute() {
    DataFrame config = new DataFrame().set( "Context", new DataFrame()
        .set( "Class","DatabaseContext" )
        .set( "Target","jdbc:h2:[#$jobdir#]/test;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" )
        .set( "autocreate",true )
        .set( "driver","org.h2.Driver" )
        .set( "username","sa" )
        .set( "password","" )
        .set( "fields",new DataFrame()
            .set( "SomeKey","SomeValue" )
            .set( "AnotherKey","AnotherValue" )
          )
      );
    
    System.out.println( JSONMarshaler.toFormattedString( config ) );

    TransformContext context = new DatabaseContext();
    context.setConfiguration( config );

    context.open();

    // values are saved when the context is closed
    context.close();

    // run count should be incremented each time the context is opened
    context.open();

    context.close();

  }




  @Test
  public void emptyContext() {

  }




  @Test
  public void existingContext() {

  }




  @Test
  public void differentTypes() {

  }




  @Test
  public void runCount() {

  }




  @Test
  public void lastRun() {

  }

}