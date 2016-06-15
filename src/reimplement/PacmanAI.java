package reimplement;

import com.jme3.math.Vector3f;
import mmaracic.gameaiframework.AgentAI;
import mmaracic.gameaiframework.PacmanAgent;
import mmaracic.gameaiframework.PacmanVisibleWorld;
import mmaracic.gameaiframework.WorldEntity;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.pow;

/**
 *
 */
public class PacmanAI extends AgentAI {
  protected static class Location implements Comparable<Location> {
    int x = 0, y = 0;

    Location(int x, int y) {
      this.x = x;
      this.y = y;
    }

    int getX() {
      return x;
    }

    int getY() {
      return y;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Location) {
        Location temp = (Location) o;
        return (temp.x == this.x) && (temp.y == this.y);
      } else
        return false;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 79 * hash + this.x;
      hash = 79 * hash + this.y;
      return hash;
    }

    public float distanceTo(Location other) {
      int distanceX = other.x - x;
      int distanceY = other.y - y;

      return (float) abs(distanceX) + abs(distanceY);
//            return (float) Math.sqrt(distanceX*distanceX + distanceY+distanceY);
    }

    @Override
    public int compareTo(Location o) {
      if (x == o.x) {
        return Integer.compare(y, o.y);
      } else {
        return Integer.compare(x, o.x);
      }
    }
  }

  private HashSet<Location> points = new HashSet<>();
  private Location myLocation = new Location(0, 0);

  private float[] w = {10, -500, 1};

  private static final int BUFFER_SIZE = 1000;
  private ArrayList<Location> lastLocations = new ArrayList<Location>(BUFFER_SIZE);
  private int index = 0;

  private float getDistance(PacmanVisibleWorld mySurroundings, Location location, String element) {
    int radiusX = mySurroundings.getDimensionX() / 2;
    int radiusY = mySurroundings.getDimensionY() / 2;

    float distance = Float.MAX_VALUE;
    for (int i = -radiusX; i <= radiusX; i++) {
      for (int j = -radiusY; j <= radiusY; j++) {
        if (i==0 && j==0) continue;
        Location tempLocation = new Location(i, j);
        if (abs(tempLocation.getX()) > radiusX || abs(tempLocation.getY()) > radiusY ) continue;
        ArrayList<WorldEntity.WorldEntityInfo> neighPosInfos = mySurroundings.getWorldInfoAt(tempLocation.getX(), tempLocation.getY());
        if (neighPosInfos != null) {
          for (WorldEntity.WorldEntityInfo info : neighPosInfos) {
            if (info.getIdentifier().compareToIgnoreCase(element) == 0) {
              //Remember him!

              float currDistance = location.distanceTo(tempLocation);
              if (element == "Point" || element == "Powerup") {
                points.add(new Location(myLocation.getX()+tempLocation.getX(), myLocation.getY() + tempLocation.getY()));
              }
              if (currDistance < distance) {
                distance = currDistance;
              }
            }
          }
        }
      }
    }
    return distance;

  }

  private float isLastLocation(Location loc) {

    if (lastLocations.size() == 0)
      return 1;
    int counter = BUFFER_SIZE;
    //System.out.println(loc.equals(lastLocation));
    for (int i=index-1; i >=0; i--) {
      if (lastLocations.get(i).equals(loc)) {
        int occurrences = Collections.frequency(lastLocations, lastLocations.get(i));
        return -1.0f * occurrences * counter / BUFFER_SIZE;
      }
      counter++;
    }

    if (lastLocations.size()<BUFFER_SIZE)
      return 1;

    for (int i=BUFFER_SIZE-1; i >=index; i--) {
      if (lastLocations.get(i).equals(loc)) {
        int occurrences = Collections.frequency(lastLocations, lastLocations.get(i));
        return -1.0f * occurrences * counter / BUFFER_SIZE;
      }
      counter++;
    }
    return 1;
  }

  private float qScore(PacmanVisibleWorld mySurroundings, Location loc, WorldEntity.WorldEntityInfo myInfo) {
    ArrayList<WorldEntity.WorldEntityInfo> neighPosInfos = mySurroundings.getWorldInfoAt(loc.getX(), loc.getY());
    if (neighPosInfos != null) {
      for (WorldEntity.WorldEntityInfo info : neighPosInfos) {
        if (info.getIdentifier().compareToIgnoreCase("Wall") == 0) {
          return -Float.MAX_VALUE;
        }
      }
    }

    float ghostDistance = getDistance(mySurroundings, loc, "Ghost");
    float pointDistance = getDistance(mySurroundings, loc, "Point");
    float powerupDistance = getDistance(mySurroundings, loc, "Powerup");
    float memorizedPointsDistance = getPointsDistance(new Location(loc.getX() + myLocation.getX(), loc.getY()+myLocation.getY()));
    if (powerupDistance < pointDistance) pointDistance = powerupDistance;
    if (memorizedPointsDistance < pointDistance) pointDistance = memorizedPointsDistance;
    //System.out.println("Distance to ghost:" + ghostDistance);
    //System.out.println("Distance to point:" + pointDistance);
    Location location = new Location(myLocation.getX() + loc.getX(), myLocation.getY() + loc.getY());
    boolean powerUP = myInfo.hasProperty(PacmanAgent.powerupPropertyName);
    int reverse = 1;
    if (powerUP) reverse = -1;
    float q = w[0] / (pointDistance + 0.1f) + reverse * w[1] / (ghostDistance + 0.1f)+ w[2] * isLastLocation(location) ;
    return q;
  }

  private float getPointsDistance(Location location) {
    float minDistance = Float.MAX_VALUE;
    for (Location point : points) {
      System.out.println(point.getX() + " " + point.getY());
      float distance = location.distanceTo(point);
      if (distance < minDistance) minDistance = distance;
    }

    System.out.println(minDistance);
    return minDistance;
  }

  private float bestActionScore(PacmanVisibleWorld mySurroundings, Location loc, WorldEntity.WorldEntityInfo myInfo) {
    float leftScore = qScore(mySurroundings, new Location(loc.getX()-1, loc.getY()), myInfo);
    float rightScore = qScore(mySurroundings, new Location(loc.getX()+1, loc.getY()), myInfo);
    float upScore = qScore(mySurroundings, new Location(loc.getX(), loc.getY()+1), myInfo);
    float downScore = qScore(mySurroundings, new Location(loc.getX(), loc.getY()-1), myInfo);
    return max(max(leftScore, rightScore), max(upScore, downScore));
  }

  @Override
  public int decideMove(ArrayList<int[]> moves, PacmanVisibleWorld mySurroundings, WorldEntity.WorldEntityInfo myInfo) {
    points.remove(myLocation);
    //System.out.println("Pocetak");
    float q = qScore(mySurroundings, new Location(0, 0), myInfo);

    Location nextLocation = myLocation;
    int moveIndex = 0;
    Random rand = new Random();

    float gamma = 0.7f;
    float maxScore = -Float.MAX_VALUE;
    for (int i = moves.size() - 1; i >= 0; i--) {
      int[] move = moves.get(i);
      //System.out.printf("%d %d\n", move[0], move[1]);
      Location moveLocation = new Location(move[0], move[1]);
      float qMove = qScore(mySurroundings, moveLocation, myInfo) + gamma*bestActionScore(mySurroundings, moveLocation, myInfo);
      //System.out.println(qMove);
      //System.out.println(maxScore);
      if (qMove > maxScore) {
        maxScore = qMove;
        moveIndex = i;
        //System.out.println("MoveIndex: " + moveIndex);
      }
    }
    //System.out.println("Kraj");


    int[] move = moves.get(moveIndex);
    nextLocation = new Location(myLocation.getX() + move[0], myLocation.getY() + move[1]);
    lastLocations.add(index, myLocation);
    index = (index + 1) % BUFFER_SIZE;
    points.remove(myLocation);
    myLocation = nextLocation;
    points.remove(myLocation);

    return moveIndex;
  }
}
