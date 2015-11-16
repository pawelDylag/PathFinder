/**
 * Created by paweldylag on 06/11/15.
 */
public class Main {

    public static void main (String[] args) {
        PathFinder pathFinder = new PathFinder();
        pathFinder.setMaxThreads(3);
        pathFinder.entranceToTheLabyrinth(Labirynth.manualLabirynth());
    }

}