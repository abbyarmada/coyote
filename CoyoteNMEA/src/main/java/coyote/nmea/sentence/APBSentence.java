package coyote.nmea.sentence;

import coyote.nmea.DataStatus;
import coyote.nmea.Direction;
import coyote.nmea.Sentence;


/**
 * Autopilot sentence "type B", cross-track error, bearings and heading towards
 * destination waypoint. 
 * 
 * <p>This is a fixed form of the APA sentence with some ambiguities 
 * removed.
 * 
 * <p>Note: Some autopilots, Robertson in particular, misinterpret "bearing 
 * from origin to destination" as "bearing from present position to 
 * destination".
 * 
 * <p>Example:<pre>$GPAPB,A,A,0.10,R,N,V,V,011,M,DEST,011,M,011,M*82</pre>
 */
public interface APBSentence extends Sentence {

  /** Kilometers */
  public static final char KM = 'K';

  /** Nautical miles */
  public static final char NM = 'N';




  /**
   * Returns the bearing from current position to destination waypoint.
   * 
   * @return bearing in degrees
   */
  double getBearingPositionToDestination();




  /**
   * Returns bearing from origin to destination.
   * 
   * @return bearing in degrees
   */
  double getBearingOriginToDestination();




  /**
   * Returns the cross-track error magnitude/distance.
   * 
   * @return Cross-track error distance in nautical miles
   */
  double getCrossTrackError();




  /**
   * Returns the unit of cross-track error.
   * 
   * @return Unit char indicator
   */
  char getCrossTrackUnits();




  /**
   * Returns the Loran-C cycle lock status, not used for GPS.
   */
  DataStatus getCycleLockStatus();




  /**
   * Returns the destination waypoint id/name.
   * 
   * @return Waypoint id
   */
  String getDestionationWaypointId();




  /**
   * Returns the heading to steer to destination waypoint.
   * 
   * @return Heading in degrees
   */
  double getHeadingToDestionation();




  /**
   * Returns the signal/fix status, LORAN-C Blink or SNR warning.
   * 
   * @return DataStatus
   */
  DataStatus getStatus();




  /**
   * Returns the direction in which to steer in order to get back on route.
   * 
   * @return {@link Direction#LEFT} or {@link Direction#RIGHT}
   */
  Direction getSteerTo();




  /**
   * Tells if vessel has entered the arrival circle.
   * 
   * @return True if entered, otherwise false.
   */
  boolean isArrivalCircleEntered();




  /**
   * Tells if the bearing from origin to destination is true or magnetic.
   * 
   * @return True if true heading, false for magnetic.
   */
  boolean isBearingOriginToDestionationTrue();




  /**
   * Tells if the bearing from current position to destination is true or
   * magnetic.
   * 
   * @return True if true heading, false for magnetic.
   */
  boolean isBearingPositionToDestinationTrue();




  /**
   * Tells if the heading to destination is true or magnetic.
   * 
   * @return True if true heading, false for magnetic.
   */
  boolean isHeadingToDestinationTrue();




  /**
   * Tells if vessel has passed perpendicular at waypoint.
   * 
   * @return True if passed, otherwise false.
   */
  boolean isPerpendicularPassed();




  /**
   * Sets the arrival circle enter status.
   * 
   * @param isEntered True if entered, otherwise false.
   */
  void setArrivalCircleEntered( boolean isEntered );




  /**
   * Sets the bearing from origin to destination.
   * 
   * @param bearing bearing in degrees
   */
  void setBearingOriginToDestination( double bearing );




  /**
   * Sets the bearing from origin to destination true or magnetic.
   * 
   * @param isTrue True if true bearing, false for magnetic.
   */
  void setBearingOriginToDestionationTrue( boolean isTrue );




  /**
   * Sets the bearing from current position to destination waypoint.
   * 
   * @param bearing bearing in degrees
   */
  void setBearingPositionToDestination( double bearing );




  /**
   * Sets the bearing from current position to destination true or magnetic.
   * 
   * @param isTrue True if true bearing, false for magnetic.
   */
  void setBearingPositionToDestinationTrue( boolean isTrue );




  /**
   * Sets the cross-track error magnitude/distance.
   * 
   * @param distance Cross-track error distance in nautical miles
   */
  void setCrossTrackError( double distance );




  /**
   * Sets the unit of cross-track error.
   * 
   * @param unit Unit char to set
   */
  void setCrossTrackUnits( char unit );




  /**
   * Sets the Loran-C cycle lock status. Not used for GPS, may be omitted or
   * {@link DataStatus#VOID}.
   * 
   * @param status DataStatus to set
   */
  void setCycleLockStatus( DataStatus status );




  /**
   * Returns the destination waypoint id/name.
   * 
   * @param id Waypoint ID to set.
   */
  void setDestinationWaypointId( String id );




  /**
   * Sets the heading to destination waypoint.
   * 
   * @param heading heading in degrees
   */
  void setHeadingToDestination( double heading );




  /**
   * Sets the heading to destination true or magnetic.
   * 
   * @param isTrue True if true heading, false for magnetic.
   */
  void setHeadingToDestinationTrue( boolean isTrue );




  /**
   * Sets the perpendicular to waypoint pass status.
   * 
   * @param isPassed True if passed, otherwise false.
   */
  void setPerpendicularPassed( boolean isPassed );




  /**
   * Sets the signal/fix status, LORAN-C Blink or SNR warning.
   * 
   * @param status DataStatus to set.
   */
  void setStatus( DataStatus status );




  /**
   * Set direction in which to steer in order to get back on route.
   * 
   * @param direction {@link Direction#RIGHT} or {@link Direction#LEFT}
   */
  void setSteerTo( Direction direction );

}
