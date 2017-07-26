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
package coyote.dx.task;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import coyote.commons.FileUtil;
import coyote.dx.CJS;
import coyote.dx.ConfigTag;
import coyote.dx.TaskException;
import coyote.dx.context.TransformContext;
import coyote.loader.Loader;
import coyote.loader.cfg.Config;
import coyote.loader.cfg.ConfigurationException;
import coyote.loader.log.ConsoleAppender;
import coyote.loader.log.Log;


/**
 * 
 */
public class ScriptedTask {

  private static final TransformContext context = new TransformContext();




  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Log.addLogger( Log.DEFAULT_LOGGER_NAME, new ConsoleAppender( Log.TRACE_EVENTS | Log.DEBUG_EVENTS | Log.INFO_EVENTS | Log.WARN_EVENTS | Log.ERROR_EVENTS | Log.FATAL_EVENTS ) );
  }




  @Test
  public void test() {
    Config cfg = new Config();
    cfg.put( ConfigTag.HOST, "smtp.gmail.com" );
    cfg.put( ConfigTag.PORT, 587 );
    cfg.put( ConfigTag.PROTOCOL, "GmailTLS" ); // case-sensitive - used as a class name
    cfg.put( Loader.ENCRYPT_PREFIX + ConfigTag.USERNAME, "q3YEnVqgPxdNauVIKi/0CNOIA0xDP7rvBNyXksa6eYCrKc9OGmZ2yWl+Yrrw9RNW3QpXrX0Aluw=" );
    cfg.put( Loader.ENCRYPT_PREFIX + ConfigTag.PASSWORD, "nrasR4FXrf+ZyM1cigtHktGWc+UeZ5EPoNj/Lack6tXPgx58hFwwXq7BiYmN5SfA" );
  
  }




 
}
