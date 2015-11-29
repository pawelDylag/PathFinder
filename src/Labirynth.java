

import java.util.*;

/**
 * Created by paweldylag on 06/11/15.
 */
public class Labirynth {

    private static final double STEP = 1d;
    private static int idCounter;

    public static RoomInterface buildLabirynth(int exitCount, double shortestDistance, int roomCount) {
        if (exitCount <= 0)
            throw new IllegalArgumentException("exitCount must be positive");
        if (shortestDistance <= 0d)
            throw new IllegalArgumentException("shortestDistance must be positive");
        if (roomCount <= 0 )
            throw new IllegalArgumentException("roomCount must be positive");

        ArrayList<CustomRoom> allRooms = new ArrayList<CustomRoom>();

        // add exits
        for (int i = 0; i < exitCount; i++) {
            double distance = shortestDistance + i * 2;
            allRooms.add(new CustomRoom(idCounter++, true, distance));
            log("Creating exit: distance=" + distance);
        }
        // populate normal rooms
        for (int i = 0; i < roomCount; i++) {
            allRooms.add(new CustomRoom(idCounter++, false, i * STEP));
        }
        // sort rooms by distance from entrance
        Collections.sort(allRooms);
        // log created rooms
        log(allRooms.toString());



        return allRooms.get(0);
    }


    public static RoomInterface manualLabirynth() {
        CustomRoom entrace = new CustomRoom(0, false, 0.0);
        CustomRoom r1 = new CustomRoom(1, false, 1d);
        CustomRoom r2 = new CustomRoom(2, false, 1d);
        CustomRoom r3 = new CustomRoom(3, false, 1d);
        CustomRoom r4 = new CustomRoom(4, false, 2d);
        CustomRoom r5 = new CustomRoom(5, false, 2d);
        CustomRoom r6 = new CustomRoom(6, false, 3d);
        CustomRoom r7 = new CustomRoom(7, false, 3d);
        CustomRoom r8 = new CustomRoom(8, true, 3d);
        CustomRoom r9 = new CustomRoom(9, false, 4d);
        CustomRoom r10 = new CustomRoom(10, false, 4d);
        CustomRoom r11= new CustomRoom(11, true, 5d);
        CustomRoom r12 = new CustomRoom(12, false, 5d);
        CustomRoom r13 = new CustomRoom(13, false, 6d);
        CustomRoom r14 = new CustomRoom(14, true, 4d);
        CustomRoom r15 = new CustomRoom(15, true, 7d);
        CustomRoom r16 = new CustomRoom(16, true, 8d);
        // build tree
        entrace.addRoom(r1);
        entrace.addRoom(r2);
        entrace.addRoom(r3);
        r1.addRoom(r14);
        r1.addRoom(r4);
        r2.addRoom(r5);
        r2.addRoom(r6);
        r3.addRoom(r11);
        r4.addRoom(r9);
        r4.addRoom(r10);
        r4.addRoom(r12);
        r5.addRoom(r7);
        r5.addRoom(r8);
        r12.addRoom(r13);
        r9.addRoom(r15);
        r12.addRoom(r16);
        return entrace;
    }

    private static void log(String text) {
        System.out.println(text);
    }

}
