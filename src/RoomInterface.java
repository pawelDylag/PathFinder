/**
 * Interfejs dostepu do obiektu reprezentujacego
 * pomieszczenie w labityncie.
 * @author oramus
 *
 */
public interface RoomInterface {
    /**
     * Pozwala sprawdzic, czy pomieszczenie jest wyjsciem
     * @return true - pomieszczenie to wyjscie z labiryntu, false - pomieszcznie
     * nie pozwala na wyjscie z labiryntu
     */
    boolean isExit();

    /**
     * Odleglosc od wejscia do tego pomieszczenia.
     * @return odleglosc od wejscia; dla pomieszczenia, ktore jest wejsciem wynosi 0.0
     */
    double getDistanceFromStart();

    /**
     * Zwraca tablice pozwalajaca na dotarcie do kolejnych pomieszczen.
     * @return tablica kolejnych pomieszczen, null - pomieszczenie nie prowadzi nigdzie dalej
     */
    RoomInterface[] corridors();
}