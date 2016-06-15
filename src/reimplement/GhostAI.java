package reimplement;

import mmaracic.gameaiframework.*;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 *
 */
public class GhostAI extends AgentAI {
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

  private Location myLocation = new Location(0, 0);

  private float[] w = {500, 10};

  private static Location pacmanLocation = null;
  private static final int BUFFER_SIZE = 1000;
  private static ArrayList<Location> lastLocations = new ArrayList<Location>(BUFFER_SIZE);
  private static int index = 0;

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
              if (element == "Pacman") pacmanLocation = tempLocation;
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

    float pacmanDistance = getDistance(mySurroundings, loc, "Pacman");
    if (pacmanDistance > 10 && pacmanLocation != null) {
      pacmanDistance = loc.distanceTo(pacmanLocation);
    }
    Location location = new Location(myLocation.getX() + loc.getX(), myLocation.getY() + loc.getY());
    boolean powerUP = myInfo.hasProperty(PacmanAgent.powerupPropertyName);
    int reverse = 1;
    if (powerUP) reverse = -1;
    float q = reverse * w[0] / (pacmanDistance + 0.1f) + w[1] * isLastLocation(location) ;
    return q;
  }

  private void addLocation(Location loc) {
    if (lastLocations.size() < BUFFER_SIZE) {
      lastLocations.add(loc);
    }
    else {
      lastLocations.add(index, myLocation);
    }
    index = (index + 1) % BUFFER_SIZE;
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
    addLocation(myLocation);
    myLocation = nextLocation;
    if (pacmanLocation != null && myLocation.equals(pacmanLocation)) {
      pacmanLocation = null;
    }

    return moveIndex;
  }
}
