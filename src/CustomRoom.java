import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by paweldylag on 06/11/15.
 */
public class CustomRoom implements RoomInterface, Comparable {

    private int id;
    private boolean isExit;
    private double distance;
    private ArrayList<RoomInterface> corridors;

    public CustomRoom(int id, boolean isExit, double distance) {
        this.id = id;
        this.isExit = isExit;
        this.distance = distance;
        corridors = new ArrayList<RoomInterface>();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof RoomInterface) {
            RoomInterface anotherRoom = (RoomInterface) o;
                return Double.compare(this.distance, anotherRoom.getDistanceFromStart());
        } else throw new ClassCastException("Items are not comparable");
    }

    @Override
    public boolean isExit() {
        return isExit;
    }

    @Override
    public double getDistanceFromStart() {
        return distance;
    }

    @Override
    public RoomInterface[] corridors() {
        if (corridors != null)
        return corridors.toArray(new RoomInterface[corridors.size()]);
        else return new RoomInterface[0];
    }

    public void addRoom(RoomInterface room) {
        this.corridors.add(room);
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", isExit=" + isExit +
                ", distance=" + distance +
                ", corridors=" + corridors +
                '}' + "\n";
    }
}
