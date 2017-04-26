package coyote.commons.network.mqtt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import coyote.commons.network.mqtt.ClientListener;
import coyote.commons.network.mqtt.MqttConnectOptions;
import coyote.commons.network.mqtt.MqttDeliveryToken;
import coyote.commons.network.mqtt.MqttDeliveryTokenImpl;
import coyote.commons.network.mqtt.MqttException;
import coyote.commons.network.mqtt.MqttMessage;
import coyote.commons.network.mqtt.client.TestClientFactory;
import coyote.commons.network.mqtt.properties.TestProperties;
import coyote.commons.network.mqtt.utilities.Utility;
import coyote.loader.log.Log;


/**
 * Client test which rapidly tries random combinations of connect,
 * disconnect, publish and subscribe on a single thread with varying options
 * (retained, qos etc) and verifies the results. A log is produced
 * of the history of commands tried which can be fed back into the
 * test to re-produce a previous run (See run(String filename)
 */

public class ModelTestCase implements ClientListener {

  private static URI serverURI;
  private static TestClientFactory clientFactory;

  public static final String LOGDIR = "./";
  public static final String CLIENTID = "mqtt.MTC";

  public String logFilename = null;
  public File logDirectory = null;
  public PrintWriter logout = null;
  public HashMap<String, Integer> subscribedTopics;
  public Object lock;
  public ArrayList<MqttMessage> messages;
  public ArrayList<String> topics;
  public Random random;
  public MqttBlockingClient client;
  public HashMap<String, MqttMessage> retainedPublishes;
  public HashMap<MqttDeliveryTokenImpl, String> currentTokens;
  public boolean cleanSession;

  private int numOfIterations = 500;




  /**
   * Constructor
   **/
  public ModelTestCase() {
    logDirectory = new File( LOGDIR );
    if ( logDirectory.exists() ) {
      deleteLogFiles();
      logFilename = "mqttv3.ModelTestCase." + System.currentTimeMillis() + ".log";

      File logfile = new File( logDirectory, logFilename );
      try {
        logout = new PrintWriter( new FileWriter( logfile ) );
      } catch ( IOException e ) {
        logout = null;
      }
    }
  }




  /**
   * @throws IOException 
   */
  private void deleteLogFiles() {
    Log.info( "Deleting log files" );
    File[] files = logDirectory.listFiles( new FilenameFilter() {

      public boolean accept( File dir, String name ) {
        return name.matches( "mqttv3\\.ModelTestCase\\..*\\.log" );
      }
    } );

    for ( File file : files ) {
      boolean isDeleted = file.delete();
      if ( isDeleted == false ) {
        Log.info( "    failed to delete: " + file.getAbsolutePath() );
        file.deleteOnExit();
      }
    }
  }




  /**
   * Test definitions
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    try {
      String methodName = Utility.getMethodName();

      serverURI = TestProperties.getServerURI();
      clientFactory = new TestClientFactory();
      clientFactory.open();
    } catch ( Exception exception ) {
      Log.error( "caught exception:", exception );
      throw exception;
    }
  }




  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    String methodName = Utility.getMethodName();

    try {
      if ( clientFactory != null ) {
        clientFactory.close();
        clientFactory.disconnect();
      }
    } catch ( Exception exception ) {
      Log.error( "caught exception:", exception );
    }
  }




  /**
   * @throws Exception 
   */
  @Test
  public void testRunModel() throws Exception {
    Log.info( "Test core operations and parameters by random selection" );
    Log.info( "See file: " + logFilename + " for details of selected test sequence" );
    initialise();
    try {
      this.run( numOfIterations );
    }
    finally {
      finish();
    }
  }




  /**
   * @param msg
   */
  public void logToFile( String msg ) {

    DateFormat df = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss.SSS" );
    Date d = new Date();
    String tsMsg = df.format( d ) + " " + msg;

    if ( logout != null ) {
      logout.println( tsMsg );
    } else {
      System.out.println( tsMsg );
    }
  }




  /**
   * @param e
   */
  public void logToFile( Throwable e ) {
    e.printStackTrace( System.out );
    if ( logout != null ) {
      e.printStackTrace( logout );
    }
  }




  /**
   * @throws Exception 
   */
  public void initialise() throws Exception {
    random = new Random();
    subscribedTopics = new HashMap<String, Integer>();
    messages = new ArrayList<MqttMessage>();
    topics = new ArrayList<String>();
    lock = new Object();
    retainedPublishes = new HashMap<String, MqttMessage>();
    currentTokens = new HashMap<MqttDeliveryTokenImpl, String>();

    client = clientFactory.createMqttClient( serverURI, CLIENTID );
    client.setCallback( this );
    // Clean any hungover state
    MqttConnectOptions connOpts = new MqttConnectOptions();
    connOpts.setCleanSession( true );
    client.connect( connOpts );
    client.disconnect();
  }




  /**
   * @throws Exception 
   */
  public void finish() throws Exception {
    if ( logout != null ) {
      logout.flush();
      logout.close();
    }
    client.close();
  }

  private final double[] CONNECTED_TABLE = new double[] { 0.05, // disconnect
      0.2, // subscribe
      0.2, // unsubscribe
      0.5, // publish
      0.05 // pendingDeliveryTokens 
  };
  private final double[] DISCONNECTED_TABLE = new double[] { 0.5, // connect
      0.2, // pendingDeliveryTokens
      0.3, // disconnect
  };




  /**
   * @param table
   */
  private int getOption( double[] table ) {
    double n = random.nextDouble();
    double c = 0;
    for ( int i = 0; i < table.length; i++ ) {
      c += table[i];
      if ( c > n ) {
        return i;
      }
    }
    return -1;
  }




  /**
   * @param iterations
   * @throws Exception 
   */
  public void run( int iterations ) throws Exception {
    try {
      for ( int i = 0; i < iterations; i++ ) {
        if ( client.isConnected() ) {
          int o = getOption( CONNECTED_TABLE );
          switch ( o ) {
            case 0:
              disconnect( true );
              break;
            case 1:
              subscribe();
              break;
            case 2:
              unsubscribe();
              break;
            case 3:
              publish();
              break;
            case 4:
              pendingDeliveryTokens();
              break;
          }
        } else {
          int o = getOption( DISCONNECTED_TABLE );
          switch ( o ) {
            case 0:
              connect();
              break;
            case 1:
              pendingDeliveryTokens();
              break;
            case 2:
              disconnect( false );
              break;
          }
        }
      }
    } catch ( Exception e ) {
      logToFile( e );
      throw ( e );
    }
    finally {
      try {
        if ( client.isConnected() ) {
          client.disconnect();
        }

        client.close();
      } catch ( Exception e ) {
        // ignore - cleanup for error cases, allow any previous exception to be seen
      }
    }
  }




  // TODO:
  /**
   * @param filename
   * @throws Exception
   */
  public void run( String filename ) throws Exception {
    /*
    26/07/2010 16:28:46.972 connect [cleanSession:false]
    26/07/2010 16:28:46.972 disconnect [cleanSession:false][isConnected:true]
    26/07/2010 16:28:46.972 subscribe [topic:5f00344b-530a-414a-91e6-57f7d8662b80][qos:2][expectRetained:true]
    26/07/2010 16:28:46.972 unsubscribe [topic:0c94dacd-71b0-4692-8b49-4cb225e5f505][existing:false]
    26/07/2010 16:28:46.972 publish [topic:5f00344b-530a-414a-91e6-57f7d8662b80][payload:0dbc3ef9-85b3-4cf0-b1f3-e086289d8152][qos:2][retained:true][subscribed:false][waitForCompletion:false]
    26/07/2010 16:28:46.972 pendingDeliveryTokens [count:0]
     */
    Pattern pConnect = Pattern.compile( "^.*? connect \\[cleanSession:(.+)\\]$" );
    Pattern pDisconnect = Pattern.compile( "^.*? disconnect \\[cleanSession:(.+)\\]\\[isConnected:(.+)\\]$" );
    Pattern pSubscribe = Pattern.compile( "^.*? subscribe \\[topic:(.+)\\]\\[qos:(.+)\\]\\[expectRetained:(.+)\\]$" );
    Pattern pUnsubscribe = Pattern.compile( "^.*? unsubscribe \\[topic:(.+)\\]\\[existing:(.+)\\]$" );
    Pattern pPublish = Pattern.compile( "^.*? publish \\[topic:(.+)\\]\\[payload:(.+)\\]\\[qos:(.+)\\]\\[retained:(.+)\\]\\[subscribed:(.+)\\]\\[waitForCompletion:(.+)\\]$" );
    Pattern pPendingDeliveryTokens = Pattern.compile( "^.*? pendingDeliveryTokens \\[count:(.+)\\]$" );
    BufferedReader in = new BufferedReader( new FileReader( filename ) );
    String line;
    try {
      while ( ( line = in.readLine() ) != null ) {
        Matcher m = pConnect.matcher( line );
        if ( m.matches() ) {
          connect( Boolean.parseBoolean( m.group( 1 ) ) );
        } else if ( ( m = pDisconnect.matcher( line ) ).matches() ) {
          disconnect( Boolean.parseBoolean( m.group( 1 ) ), Boolean.parseBoolean( m.group( 2 ) ) );
        } else if ( ( m = pSubscribe.matcher( line ) ).matches() ) {
          subscribe( m.group( 1 ), Integer.parseInt( m.group( 2 ) ), Boolean.parseBoolean( m.group( 3 ) ) );
        } else if ( ( m = pUnsubscribe.matcher( line ) ).matches() ) {
          unsubscribe( m.group( 1 ), Boolean.parseBoolean( m.group( 2 ) ) );
        } else if ( ( m = pPublish.matcher( line ) ).matches() ) {
          publish( m.group( 1 ), m.group( 2 ), Integer.parseInt( m.group( 3 ) ), Boolean.parseBoolean( m.group( 4 ) ), Boolean.parseBoolean( m.group( 5 ) ), Boolean.parseBoolean( m.group( 6 ) ) );
        } else if ( ( m = pPendingDeliveryTokens.matcher( line ) ).matches() ) {
          pendingDeliveryTokens( Integer.parseInt( m.group( 1 ) ) );
        }
      }
    } catch ( Exception e ) {
      if ( client.isConnected() ) {
        client.disconnect();
      }
      throw e;
    }
  }




  /**
   * @throws Exception
   */
  public void connect() throws Exception {
    cleanSession = random.nextBoolean();
    connect( cleanSession );
  }




  /**
   * Connects the client.
   * @param cleanSession1 whether to connect clean session.
   * @throws Exception
   */
  public void connect( boolean cleanSession1 ) throws Exception {
    logToFile( "connect [cleanSession:" + cleanSession1 + "]" );
    if ( cleanSession1 ) {
      subscribedTopics.clear();
    }
    MqttConnectOptions opts = new MqttConnectOptions();
    opts.setCleanSession( cleanSession1 );
    client.connect( opts );
  }




  /**
   * @param connected
   * @throws Exception
   */
  public void disconnect( boolean connected ) throws Exception {
    disconnect( cleanSession, connected );
  }




  /**
   * Disconnects the client
   * @param cleanSession1 whether this is a clean session being disconnected
   * @param isConnected whether we think the client is currently connected
   * @throws Exception
   */
  public void disconnect( boolean cleanSession1, boolean isConnected ) throws Exception {
    logToFile( "disconnect [cleanSession:" + cleanSession1 + "][isConnected:" + client.isConnected() + "]" );
    if ( isConnected != client.isConnected() ) {
      throw new Exception( "Client state mismatch [expected:" + isConnected + "][actual:" + client.isConnected() + "]" );
    }
    if ( isConnected && cleanSession1 ) {
      subscribedTopics.clear();
    }
    try {
      client.disconnect();
    } catch ( MqttException e ) {
      if ( ( ( e.getReasonCode() != MqttException.UNEXPECTED_ERROR ) && ( e.getReasonCode() != MqttException.CLIENT_ALREADY_DISCONNECTED ) ) || isConnected ) {
        throw e;
      }
    }
  }




  /**
   * @throws Exception
   */
  public void subscribe() throws Exception {
    String topic;
    boolean expectRetained;
    if ( !retainedPublishes.isEmpty() && ( random.nextInt( 5 ) == 0 ) ) {
      Object[] topics1 = retainedPublishes.keySet().toArray();
      topic = (String)topics1[random.nextInt( topics1.length )];

      expectRetained = true;
    } else {
      topic = UUID.randomUUID().toString();
      expectRetained = false;
    }
    int qos = random.nextInt( 3 );
    subscribe( topic, qos, expectRetained );
  }




  /**
   * Subscribes to a given topic at the given qos
   * @param topic the topic to subscribe to
   * @param qos the qos to subscribe at
   * @param expectRetained whether a retained message is expected to exist on this topic
   * @throws Exception
   */
  public void subscribe( String topic, int qos, boolean expectRetained ) throws Exception {
    logToFile( "subscribe [topic:" + topic + "][qos:" + qos + "][expectRetained:" + expectRetained + "]" );
    subscribedTopics.put( topic, new Integer( qos ) );
    client.subscribe( topic, qos );
    if ( expectRetained ) {
      waitForMessage( topic, retainedPublishes.get( topic ), true );
    }
  }




  /**
   * @throws Exception
   */
  public void unsubscribe() throws Exception {
    String topic;
    boolean existing = false;
    if ( random.nextBoolean() && ( subscribedTopics.size() > 0 ) ) {
      Object[] topics1 = subscribedTopics.keySet().toArray();
      topic = (String)topics1[random.nextInt( topics1.length )];
      existing = true;
    } else {
      topic = UUID.randomUUID().toString();
    }
    unsubscribe( topic, existing );
  }




  /**
   * Unsubscribes the given topic
   * @param topic the topic to unsubscribe from
   * @param existing whether we think we're currently subscribed to the topic
   * @throws Exception
   */
  public void unsubscribe( String topic, boolean existing ) throws Exception {
    logToFile( "unsubscribe [topic:" + topic + "][existing:" + existing + "]" );
    client.unsubscribe( topic );
    Object o = subscribedTopics.remove( topic );
    if ( existing == ( o == null ) ) {
      throw new Exception( "Subscription state mismatch [topic:" + topic + "][expected:" + existing + "]" );
    }
  }




  /**
   * @throws Exception
   */
  public void publish() throws Exception {
    String topic;
    boolean subscribed = false;
    if ( random.nextBoolean() && ( subscribedTopics.size() > 0 ) ) {
      Object[] topics1 = subscribedTopics.keySet().toArray();
      topic = (String)topics1[random.nextInt( topics1.length )];
      subscribed = true;
    } else {
      topic = UUID.randomUUID().toString();
    }
    String payload = UUID.randomUUID().toString();
    int qos = random.nextInt( 3 );
    boolean retained = random.nextInt( 3 ) == 0;

    // If the message is retained then we should wait for completion. If this isn't done there
    // is a risk that a subsequent subscriber could be created to receive the message before it
    // has been fully delivered and hence would not see the retained flag.
    boolean waitForCompletion = ( retained || ( random.nextInt( 1000 ) == 1 ) );
    publish( topic, payload, qos, retained, subscribed, waitForCompletion );

    // For QoS0 messages, wait for completion takes no effect as there is no feedback from
    // the server, and so even though not very deterministic, a small sleep is taken.
    if ( waitForCompletion && retained && ( qos == 0 ) ) {
      Thread.sleep( 50 );
    }
  }




  /**
   * Publishes to the given topic
   * @param topic the topic to publish to
   * @param payload the payload to publish
   * @param qos the qos to publish at
   * @param retained whether to publish retained
   * @param subscribed whether we think we're currently subscribed to the topic
   * @param waitForCompletion whether we should wait for the message to complete delivery
   * @throws Exception
   */
  public void publish( String topic, String payload, int qos, boolean retained, boolean subscribed, boolean waitForCompletion ) throws Exception {
    logToFile( "publish [topic:" + topic + "][payload:" + payload + "][qos:" + qos + "][retained:" + retained + "][subscribed:" + subscribed + "][waitForCompletion:" + waitForCompletion + "]" );
    if ( subscribed != subscribedTopics.containsKey( topic ) ) {
      throw new Exception( "Subscription state mismatch [topic:" + topic + "][expected:" + subscribed + "]" );
    }
    MqttMessage msg = new MqttMessage( payload.getBytes() );
    msg.setQos( qos );
    msg.setRetained( retained );
    if ( retained ) {
      retainedPublishes.put( topic, msg );
    }
    MqttDeliveryTokenImpl token = client.getTopic( topic ).publish( msg );
    synchronized( currentTokens ) {
      if ( !token.isComplete() ) {
        currentTokens.put( token, "[" + topic + "][" + msg.toString() + "]" );
      }
    }

    if ( retained || waitForCompletion ) {
      token.waitForCompletion();
      synchronized( currentTokens ) {
        currentTokens.remove( token );
      }
    }
    if ( subscribed ) {
      waitForMessage( topic, msg, false );
    }
  }




  /**
   * @throws Exception
   */
  public void pendingDeliveryTokens() throws Exception {
    MqttDeliveryToken[] tokens = client.getPendingDeliveryTokens();

  }




  /**
   * Checks the pending delivery tokens
   * @param count the expected number of tokens to be returned
   * @throws Exception
   */
  public void pendingDeliveryTokens( int count ) throws Exception {
    MqttDeliveryToken[] tokens = client.getPendingDeliveryTokens();
    logToFile( "pendingDeliveryTokens [count:" + tokens.length + "]" );
    if ( !client.isConnected() && ( tokens.length != count ) ) {
      throw new Exception( "Unexpected pending tokens [expected:" + count + "][actual:" + tokens.length + "]" );
    }
  }




  /**
   * @param cause 
   */
  public void connectionLost( Throwable cause ) {
    logToFile( "Connection Lost:" );
    logToFile( cause );
  }




  /**
   * @param token 
   */
  public void deliveryComplete( MqttDeliveryToken token ) {
    synchronized( currentTokens ) {
      currentTokens.remove( token );
    }
  }




  /**
   * Waits for the next message to arrive and checks it's values
   * @param topic the topic expected
   * @param message the message expected
   * @param expectRetained whether the retain flag is expected to be set
   * @throws Exception
   */
  public void waitForMessage( String topic, MqttMessage message, boolean expectRetained ) throws Exception {
    synchronized( lock ) {
      int count = 0;
      while ( messages.size() == 0 && ++count < 10 ) {
        lock.wait( 1000 );
      }
      if ( messages.size() == 0 ) {
        throw new Exception( "message timeout [topic:" + topic + "]" );
      }
      String rtopic = topics.remove( 0 );
      MqttMessage rmessage = messages.remove( 0 );
      if ( !rtopic.equals( topic ) ) {
        if ( rmessage.isRetained() && !expectRetained ) {
          throw new Exception( "pre-existing retained message [expectedTopic:" + topic + "][expectedPayload:" + message.toString() + "] [receivedTopic:" + rtopic + "][receivedPayload:" + rmessage.toString() + "]" );
        }
        throw new Exception( "message topic mismatch [expectedTopic:" + topic + "][expectedPayload:" + message.toString() + "] [receivedTopic:" + rtopic + "][receivedPayload:" + rmessage.toString() + "]" );
      }
      if ( !rmessage.toString().equals( message.toString() ) ) {
        if ( rmessage.isRetained() && !expectRetained ) {
          throw new Exception( "pre-existing retained message [expectedTopic:" + topic + "][expectedPayload:" + message.toString() + "] [receivedTopic:" + rtopic + "][receivedPayload:" + rmessage.toString() + "]" );
        }
        throw new Exception( "message payload mismatch [expectedTopic:" + topic + "][expectedPayload:" + message.toString() + "] [receivedTopic:" + rtopic + "][receivedPayload:" + rmessage.toString() + "]" );
      }
      if ( expectRetained && !rmessage.isRetained() ) {
        throw new Exception( "message not retained [topic:" + topic + "]" );
      } else if ( !expectRetained && rmessage.isRetained() ) {
        throw new Exception( "message retained [topic:" + topic + "]" );
      }
    }
  }




  /**
   * @param topic 
   * @param message 
   * @throws Exception 
   */
  public void messageArrived( String topic, MqttMessage message ) throws Exception {
    synchronized( lock ) {
      messages.add( message );
      topics.add( topic );
      lock.notifyAll();
    }
  }




  @Override
  public void connectComplete( boolean reconnect, String serverURI ) {
    // TODO Auto-generated method stub

  }
}
