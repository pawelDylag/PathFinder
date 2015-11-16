/**
 * Interfejs systemu przeszukiwania labiryntu.
 * @author oramus
 *
 */
public interface PathFinderInterface {
    /**
     * Metoda ustala liczbe watkow, ktora moze i powinien poslugiwac sie
     * kod implementujacy ten interfejs.
     * @param i liczba watkow
     */
    void setMaxThreads( int i );

    /**
     * Przekazuje referencje do obiektu bedacego wejsciem do labiryntu.
     * Przekazanie referencji uruchamia proces przeszukiwania labiryntu.
     * @param mi referencja do obiektu wejscia do labiryntu
     */
    void entranceToTheLabyrinth( RoomInterface mi );

    /**
     * Metoda rejestruje obiekt-obserwator. Obiekt ten jest
     * uzywany do zasygnalizowania zakonczenia pracy przez system.
     * @param code referencja do obiektu-obserwatora
     */
    void registerObserver( Runnable code );

    /**
     * Metoda pozwala na sprawdzenie czy wyjscie zostalo juz odnalezione.
     * @return true - znaleziono wyjscie, false - wyjscie nie zostalo jeszcze odnalezione
     */
    boolean exitFound();

    /**
     * Zwraca najkrotsza odleglosc pomiedzy wejsciem a wyjsciem.
     * @return Double.MAX_VALUE gdy nie znaleziono jeszcze zadnego wyjscia,
     * dlugosc najkrotszej odkrytej drogi do wyjscia
     */
    double getShortestDistanceToExit();
}