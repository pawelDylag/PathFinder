import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PMO_StartTest {
    public static long DEFAULT_SLEEP_TIME = 125; // msec
    public static boolean observerCodeCalled = true;
    public static final int EXCEPTION_LIMIT = 5;
    private static AtomicInteger exceptionsCounter;

    public static long test(int threads) throws InterruptedException {
        exceptionsCounter = new AtomicInteger();

        // TODO sprawdzic czy to tak ma byc ustawione!
        observerCodeCalled = true;

        final PathFinderInterface pfi = new PathFinder();
        PMO_SOUT.println("--------------------------------------");
        PMO_SOUT.println("Test dla limitu " + threads + " watkow");
        PMO_SOUT.println("--------------------------------------");
        pfi.setMaxThreads(threads);

        if ( pfi.getShortestDistanceToExit() != Double.MAX_VALUE ) {
            PMO_SOUT.println( "Test nie ma sensu - poczatkowe ustawienie PathFinder-a nie jest zgodne z ustaleniami");
            PMO_SOUT.println( "Dystans wynosi : " + pfi.getShortestDistanceToExit() );
            System.exit( 1 );
        }

        if ( pfi.exitFound() ) {
            PMO_SOUT.println( "Test nie ma sensu - poczatowe ustawienia systemu juz stwierdzaja istnienie wyjscia" );
            System.exit( 1 );
        }

        PMO_GraphComposer gc = new PMO_GraphComposer();

        final PMO_Room entrance = gc.getEntrance1();
        entrance.setSlowDown(DEFAULT_SLEEP_TIME);
        entrance.setFinder(pfi);
        entrance.setHelpers(new AtomicBoolean(false), new AtomicBoolean(false),
                new AtomicInteger(0), new AtomicInteger(0));
        entrance.prepareToStart();

        // to extra czas potrzebny na aktywacje wszystkich watkow - pulapek
        Thread.sleep(100);

        final AtomicLong tf = new AtomicLong(0);

        final Thread main = Thread.currentThread();

        // rejestracja obserwatora
        pfi.registerObserver(new Runnable() {

            @Override
            public void run() {
                PMO_SOUT.println("Wywolano observer.run()");
                entrance.finishFlag.set(true);
                tf.set(System.currentTimeMillis());
                main.interrupt(); // budzimy main
            }
        });

        // watek uruchamiajacy test
        Thread th = new Thread(new Runnable() {

            @Override
            public void run() {
                pfi.entranceToTheLabyrinth(entrance);
            }
        });
        th.setDaemon(true); // watek uruchamiajacy obliczenia jest demonem i
        // jego potomkowie takze

//        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
//
//            @Override
//            public void uncaughtException(Thread t, Throwable e) {
//                int i = exceptionsCounter.incrementAndGet();
//                if (i < PMO_StartTest.EXCEPTION_LIMIT)
//                    PMO_SOUT.println("WYJATEK> numer " + i + " thread "
//                            + t.getName() + " " + e.getMessage());
//            }
//        });

        long t0 = System.currentTimeMillis();
        th.start();

        try {
            long sleepMax = (DEFAULT_SLEEP_TIME * (3 + 1) * 50) / (threads - 1); // maksymalny
            // czas
            // oczekiwania
            // na
            // rozwiazanie
            PMO_SOUT.println("Watek MAIN idzie spac na " + sleepMax + "msec");
            Thread.sleep(sleepMax);
        } catch (InterruptedException ie) {
            PMO_SOUT.println("Obudzono watek MAIN");
        }

        // test poprawnego uzycia watkow
        int maxTh = entrance.reportMaxThreads();

        if (maxTh == 1) {
            PMO_SOUT.println("Blad krytyczny: Wykryto prace tylko za pomoca jednego watku");
        } else if (maxTh > threads) {
            PMO_SOUT.println("Blad krytyczny: Wykryto uzywanie wiekszej niz dozwolona liczby watkow. Mialo byc "
                    + threads + " jest " + maxTh);
        } else if (maxTh == threads) {
            PMO_SOUT.println("!OK! Wykryta prawidlowa liczba uzywanych watkow "
                    + maxTh);
        } else {
            PMO_SOUT.println("BLAD: Nie wiem co wyszlo - wykryto " + maxTh
                    + " watkow");
        }

        // test uzyskania poprawnego rozwiazania
        double shortest = pfi.getShortestDistanceToExit();

        if (Math.abs(shortest - 14) < 0.01) {

            PMO_SOUT.println("!OK! Najlepsze znalezione rozwiazane to "
                    + shortest);

        } else {
            PMO_SOUT.println("BLAD: Jakas dziwna odleglosc od wyjscia... "
                    + shortest);
        }

        // blad poprawnosci wykonywania metod na poziomie pomieszczen
        if (!entrance.allOKCascade()) {
            PMO_SOUT.println("BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD");
            PMO_SOUT.println("  TESTY WYKONYWANE NA POZIOMIE POMIESZCZEN STWIERDZILY CO NAJMNIEJ JEDEN BLAD");
            PMO_SOUT.println("BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD");
        }

        // test czy doszlo do wyjatkow
        if (exceptionsCounter.get() > 0) {
            PMO_SOUT.println("BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD");
            PMO_SOUT.println("              W TRAKCIE TESTOW STWIERDZONO WYSAPIENIE WYJATKOW");
            PMO_SOUT.println("BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD BLAD");
        }

        // test wykonania observer.run()
        if (tf.get() == 0L) {
            PMO_SOUT.println("BLAD: Nie wywolano observer.run() - nie wiadomo czy i kiedy program skonczyl liczyc...");
            observerCodeCalled = false;
            return -1;
        } else {
            // raport czasu pracy
            return tf.get() - t0;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Map<Integer, Long> time = new HashMap<Integer, Long>();

        // testy powtarzane sa 3x
        for (int i = 0; i < 3; i++) {
            // test od 2 do 5-ciu watkow
            for (int j = 2; j < 6; j++)
                time.put(j, test(j));
        }

        if (observerCodeCalled) {
            PMO_SOUT.println("------------- zmierzone czasy wykonania --------------");
            for (Map.Entry<Integer, Long> res : time.entrySet()) {
                PMO_SOUT.println(res.getKey() + " ----> " + res.getValue());
            }
        } else {
            PMO_SOUT.println("O czasie wykonania nie sposob sie wypowiedziec - braklo wywolania observer.run()");
        }
    }
}