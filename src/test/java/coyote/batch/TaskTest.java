/*
 * Copyright (c) 2015 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and initial implementation
 */
package coyote.batch;

//import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import coyote.commons.template.SymbolTable;
import coyote.loader.log.Log;


/**
 * This tests the ability to simply run a bunch of tasks in order within a 
 * context to perform some generic function.
 */
public class TaskTest extends AbstractTest {

  @Test
  public void test() {

    // load the configuration from the class path
    TransformEngine engine = loadEngine( "tasktest" );
    assertNotNull( engine );

    TransformContext context = engine.getContext();
    assertNotNull( context );

    SymbolTable symbols = engine.getSymbolTable();
    assertNotNull( symbols );

    try {
      engine.run();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.error( e.getMessage() );
      fail( e.getMessage() );
    }
    try {
      engine.close();
    } catch ( IOException e ) {
      e.printStackTrace();
      Log.error( e.getMessage() );
    }

  }

}
