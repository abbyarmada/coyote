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
package coyote.batch.writer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import coyote.batch.Batch;
import coyote.batch.ConfigTag;
import coyote.batch.ConfigurableComponent;
import coyote.batch.FrameWriter;
import coyote.batch.TransformContext;
import coyote.commons.JdbcUtil;
import coyote.commons.StringUtil;
import coyote.commons.jdbc.DriverDelegate;
import coyote.commons.template.SymbolTable;
import coyote.dataframe.DataField;
import coyote.dataframe.DataFrame;
import coyote.dataframe.DataFrameException;
import coyote.dataframe.FrameSet;
import coyote.loader.log.Log;
import coyote.loader.log.LogMsg;


/**
 * 
 */
public class JdbcWriter extends AbstractFrameWriter implements FrameWriter, ConfigurableComponent {

  protected static final SymbolTable symbolTable = new SymbolTable();

  protected Connection connection;

  protected int batchsize = 0;
  protected final FrameSet frameset = new FrameSet();
  protected String SQL = null;
  protected PreparedStatement ps = null;




  /**
   * @see coyote.batch.writer.AbstractFrameWriter#open(coyote.batch.TransformContext)
   */
  @Override
  public void open( TransformContext context ) {
    super.context = context;

    // If we don't have a connection, prepare to create one
    if ( connection == null ) {
      // get our configuration data
      setTarget( getString( ConfigTag.TARGET ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a target of {}", getTarget() ) );

      setTable( getString( ConfigTag.TABLE ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a table of {}", getTable() ) );

      setUsername( getString( ConfigTag.USERNAME ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a user of {}", getUsername() ) );

      setPassword( getString( ConfigTag.PASSWORD ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a password with a length of {}", StringUtil.isBlank( getPassword() ) ? 0 : getPassword().length() ) );

      setDriver( getString( ConfigTag.DRIVER ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a driver of {}", getDriver() ) );

      setAutoCreate( getBoolean( ConfigTag.AUTO_CREATE ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Auto Create tables = {}", isAutoCreate() ) );

      setBatchSize( getInteger( ConfigTag.BATCH ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a batch size of {}", getBatchSize() ) );

      setLibrary( getString( ConfigTag.LIBRARY ) );
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using a driver JAR of {}", getLibrary() ) );

    } else {
      Log.debug( "Using the existing connection" );
    }

    // validate and cache our batch size
    if ( getBatchSize() < 1 ) {
      batchsize = 0;
    } else {
      batchsize = getBatchSize();
    }

  }




  /**
   * @see coyote.batch.writer.AbstractFrameWriter#write(coyote.dataframe.DataFrame)
   */
  @Override
  public void write( final DataFrame frame ) {
    Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Writing {} fields", frame.size() ) );
    frameset.add( frame );

    if ( frameset.size() >= batchsize ) {
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Writing batch, size={} batch={}", frameset.size(), batchsize ) );
      writeBatch();
    }

  }




  private void writeBatch() {

    if ( SQL == null ) {
      SQL = generateSQL();
      Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Using SQL ==> {}", SQL ) );

      Connection connection = getConnection();
      try {
        ps = connection.prepareStatement( SQL );
      } catch ( SQLException e ) {
        Log.error( LogMsg.createMsg( Batch.MSG, "Writer.Could not create prepared statement: {}", e.getMessage() ) );
        context.setError( "Could not create prepared statement" );
      }
    }
    if ( context.isNotInError() ) {
      if ( batchsize <= 1 ) {
        DataFrame frame = frameset.get( 0 );
        Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Writing single frame {}", frame ) );

        int indx = 1;
        for ( String name : frameset.getColumns() ) {
          DataField field = frame.getField( name );
          setData( ps, indx++, field );
          if ( context.isInError() ) {
            break;
          }
        }

        Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.EXECUTING: {}", ps.toString() ) );

        try {
          ps.execute();
        } catch ( SQLException e ) {
          context.setError( "Could not insert single row: " + e.getMessage() );
          e.printStackTrace();
        }

      } else {
        // Now write a batch
        Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Writing batch of {} frames", frameset.size() ) );

        for ( DataFrame frame : frameset.getRows() ) {
          Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Writing {}", frame ) );

          int indx = 1;
          for ( String name : frameset.getColumns() ) {
            DataField field = frame.getField( name );
            if ( field != null && !field.isNull() ) {
              setData( ps, indx++, field );
            }
            if ( context.isInError() ) {
              break;
            }
          }

          // add this frame as a record to the batch
          try {
            ps.addBatch();
          } catch ( SQLException e ) {
            context.setError( "Could not add the record to the batch: " + e.getMessage() );
          }

        }
        if ( context.isNotInError() ) {
          try {
            ps.executeBatch();
          } catch ( SQLException e ) {
            context.setError( "Could not insert batch: " + e.getMessage() );
          }
        }
      }
      frameset.clearRows();
    }
  }




  /**
   * @param pstmt the prepared statement to which to add data
   * @param indx the index into the value set 
   * @param field the field containing the value to add
   */
  private void setData( PreparedStatement pstmt, int indx, DataField field ) {
    short type = field.getType();
    try {
      switch ( type ) {
        case DataField.FRAMETYPE:
          context.setError( "Cannot add complex objects to table" );
          break;
        case DataField.UDEF:
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.VARCHAR );
          } else {
            pstmt.setString( indx, "" );
          }
          break;
        case DataField.BYTEARRAY:
          context.setError( "Cannot add byte arrays to table" );
          break;
        case DataField.STRING:
          Log.debug( LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a String", field.getName(), indx ) );
          pstmt.setString( indx, field.getStringValue() );
          break;
        case DataField.S8:
        case DataField.U8:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a S8-byte", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.TINYINT );
          } else {
            pstmt.setByte( indx, (byte)field.getObjectValue() );
          }
          break;
        case DataField.S16:
        case DataField.U16:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as an S16-Short", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.SMALLINT );
          } else {
            pstmt.setShort( indx, (Short)field.getObjectValue() );
          }
          break;
        case DataField.S32:
        case DataField.U32:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a S32-Integer", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.INTEGER );
          } else {
            pstmt.setInt( indx, (Integer)field.getObjectValue() );
          }
          break;
        case DataField.S64:
        case DataField.U64:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a S64-Long", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.BIGINT );
          } else {
            pstmt.setLong( indx, (Integer)field.getObjectValue() );
          }
          break;
        case DataField.FLOAT:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a Float", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.FLOAT );
          } else {
            pstmt.setFloat( indx, (Float)field.getObjectValue() );
          }
          break;
        case DataField.DOUBLE:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a Double", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.DOUBLE );
          } else {
            pstmt.setDouble( indx, (Double)field.getObjectValue() );
          }
          break;
        case DataField.BOOLEANTYPE:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a Boolean", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.BOOLEAN );
          } else {
            pstmt.setBoolean( indx, (Boolean)field.getObjectValue() );
          }
          break;
        case DataField.DATE:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a Timestamp", field.getName(), indx ));
          if ( field.isNull() ) {
            pstmt.setNull( indx, java.sql.Types.TIMESTAMP );
          } else {
            Object obj = field.getObjectValue();
            pstmt.setTimestamp( indx, JdbcUtil.getTimeStamp( (Date)obj ) );
          }
          break;
        case DataField.URI:
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Saving {} (idx{}) as a String", field.getName(), indx ));
          pstmt.setString( indx, field.getStringValue() );
          break;
        case DataField.ARRAY:
          context.setError( "Cannot add arrays to table field" );
          break;
        default:
          pstmt.setNull( indx, java.sql.Types.VARCHAR );
          break;
      }
    } catch ( SQLException e ) {
      e.printStackTrace();
    }

  }




  /**
   * @return the insert SQL appropriate for this frameset
   */
  private String generateSQL() {
    StringBuffer c = new StringBuffer( "insert into " );
    StringBuffer v = new StringBuffer();

    c.append( this.getTable() );
    c.append( " (" );
    for ( String name : frameset.getColumns() ) {
      c.append( name );
      c.append( ", " );
      v.append( "?, " );
    }
    c.delete( c.length() - 2, c.length() );
    v.delete( v.length() - 2, v.length() );

    c.append( ") values (" );
    c.append( v.toString() );
    c.append( ")" );

    return c.toString();
  }




  /**
   * @param value
   */
  private void setBatchSize( int value ) {
    configuration.put( ConfigTag.BATCH, value );
  }




  public int getBatchSize() {
    try {
      return configuration.getAsInt( ConfigTag.BATCH );
    } catch ( DataFrameException ignore ) {}
    return 0;
  }




  /**
   * @param value
   */
  public void setAutoCreate( boolean value ) {
    configuration.put( ConfigTag.AUTO_CREATE, value );
  }




  public boolean isAutoCreate() {
    try {
      return configuration.getAsBoolean( ConfigTag.AUTO_CREATE );
    } catch ( DataFrameException ignore ) {}
    return false;
  }




  /**
   * @param value
   */
  private void setDriver( String value ) {
    configuration.put( ConfigTag.DRIVER, value );
  }




  public String getDriver() {
    return configuration.getAsString( ConfigTag.DRIVER );
  }




  /**
   * @param value
   */
  private void setPassword( String value ) {
    configuration.put( ConfigTag.PASSWORD, value );
  }




  public String getPassword() {
    return configuration.getAsString( ConfigTag.PASSWORD );
  }




  /**
   * @param value
   */
  public void setUsername( String value ) {
    configuration.put( ConfigTag.USERNAME, value );
  }




  public String getUsername() {
    return configuration.getAsString( ConfigTag.USERNAME );
  }




  /**
   * @param value
   */
  public void setTable( String value ) {
    configuration.put( ConfigTag.TABLE, value );
  }




  public String getTable() {
    return configuration.getAsString( ConfigTag.TABLE );
  }




  /**
   * @param value
   */
  private void setLibrary( String value ) {
    configuration.put( ConfigTag.LIBRARY, value );
  }




  public String getLibrary() {
    return configuration.getAsString( ConfigTag.LIBRARY );
  }




  /**
   * @param conn
   */
  public void setConnection( Connection conn ) {
    connection = conn;
  }




  private Connection getConnection() {

    if ( connection == null ) {
      // get the connection to the database
      try {
        URL u = new URL( getLibrary() );
        URLClassLoader ucl = new URLClassLoader( new URL[] { u } );
        Driver driver = (Driver)Class.forName( getDriver(), true, ucl ).newInstance();
        DriverManager.registerDriver( new DriverDelegate( driver ) );

        connection = DriverManager.getConnection( getTarget(), getUsername(), getPassword() );

        if ( connection != null ) {
          Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Connected to {}", getTarget() ));
        }
      } catch ( InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException | MalformedURLException e ) {
        Log.error( "Could not connect to database: " + e.getClass().getSimpleName() + " - " + e.getMessage() );
      }
    }
    return connection;
  }




  public void commit() throws SQLException {
    connection.commit();
  }




  /**
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() throws IOException {

    if ( frameset.size() > 0 ) {
      Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Completing batch size={}", frameset.size() ));
      writeBatch();
    }

    if ( ps != null ) {
      try {
        ps.close();
      } catch ( SQLException e ) {
        Log.error(  LogMsg.createMsg( Batch.MSG, "Writer.Could not close prepared statememt: {}", e.getMessage() ));
      }
    }

    if ( connection != null ) {
      try {
        commit();
      } catch ( SQLException e ) {
        Log.error(  LogMsg.createMsg( Batch.MSG, "Writer.Could not commit prior to close: {}", e.getMessage() ));
      }

      // if it looks like we created the connection ourselves (e.g. we have a 
      // configured target) close the connection
      if ( StringUtil.isNotBlank( getTarget() ) ) {
        Log.debug(  LogMsg.createMsg( Batch.MSG, "Writer.Closing connection to {}", getTarget() ));

        try {
          connection.close();
        } catch ( SQLException e ) {
          Log.error(  LogMsg.createMsg( Batch.MSG, "Writer.Could not close connection cleanly: {}", e.getMessage() ));
        }
      }
    }
  }

}
