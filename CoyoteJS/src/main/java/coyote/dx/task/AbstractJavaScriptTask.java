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

import coyote.dx.ConfigTag;


/**
 * 
 */
public abstract class AbstractJavaScriptTask extends AbstractTransformTask {

  protected String getProtocol() {
    return getString( ConfigTag.PROTOCOL );
  }

}
