/**
 * Created by paweldylag on 06/11/15.
 */
public class Main {

    public static void main (String[] args) {
        PathFinder pathFinder = new PathFinder();
        pathFinder.setMaxThreads(4);
        // rejestracja obserwatora
        pathFinder.registerObserver(new Runnable() {

            @Override
            public void run() {
                PMO_SOUT.println("Wywolano observer.run()");
            }
        });
        pathFinder.entranceToTheLabyrinth(Labirynth.manualLabirynth());
    }

}