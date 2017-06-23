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
package coyote.dx.listener;

import coyote.dx.context.ContextListener;
import coyote.dx.context.OperationalContext;
import coyote.dx.context.TransformContext;
import coyote.dx.listener.AbstractListener;


/**
 * This sends email when the transform completes.
 * 
 * <p>The normal use case is to send an email if a job fails, but there are 
 * also times when people want to be notified when a particular job runs and 
 * its output is ready to be viewed or processed. This listener supports both. 
 */
public class EmailTransformListener extends AbstractListener implements ContextListener {

  /**
   * @see coyote.dx.listener.AbstractListener#onEnd(coyote.dx.context.OperationalContext)
   */
  @Override
  public void onEnd( OperationalContext context ) {

    if ( context instanceof TransformContext ) {
      //

    }
  }

}