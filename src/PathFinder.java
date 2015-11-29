import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Pawel Dylag on 25/10/15.
 *
 * This class searches labyrinth for closest exit using fixed number of threads.
 */
class PathFinder implements PathFinderInterface {

    /** Class tag */
    private static final String TAG = PathFinder.class.getSimpleName();
    /** Initial entrance distance value */
    private static final double INITIAL_ENTRANCE_DISTANCE = Double.MAX_VALUE;

    /** Shared info about exit search result */
    private boolean mExitFound;
    /** Shared info about shortest distance to exit */
    private double mShortestDistanceToExit;
    private final ReadWriteLock mRoomLock;

    private AtomicInteger mActiveVisits;
    private Thread mfinisherThread;

    /** Observer notified about search finish */
    private Runnable mObserver;
    /** Thread pool manager object */
    private ThreadPoolExecutor mExecutor;

    public PathFinder() {
        mExitFound = false;
        mShortestDistanceToExit = INITIAL_ENTRANCE_DISTANCE;
        mRoomLock = new ReentrantReadWriteLock();
        mActiveVisits = new AtomicInteger();
    }

    @Override
    public void setMaxThreads(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Thread number must be positive");
        }
        // setup executor with fixed thread number
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(i);
    }

    @Override
    public void entranceToTheLabyrinth(RoomInterface mi) {
        if (mi == null){
            throw new NullPointerException("Entrance must not be null");
        }
        if (mExecutor == null) {
            throw new NullPointerException("Thread max number must be provided via setMaxThreads()");
        }
        // search exits
        runPathFinding(mi);
    }

    @Override
    public void registerObserver(Runnable code) {
        this.mObserver = code;
    }

    @Override
    public synchronized boolean exitFound() {
       return getExitFound();
    }

    @Override
    public double getShortestDistanceToExit() {
        return getCurrentDistanceToExit();
    }


    private boolean getExitFound(){
        boolean result = false;
        mRoomLock.readLock().lock();
        try {
            result = mExitFound;
        } finally {
            mRoomLock.readLock().unlock();
        }
        return result;
    }

    private double getCurrentDistanceToExit(){
        double result = -1d;
        mRoomLock.readLock().lock();
        try {
            result = mShortestDistanceToExit;
        } finally {
            mRoomLock.readLock().unlock();
        }
        return result;
    }


    /**
     * Main path finding loop.
     * 1.   It provides mExecutor with initial room.
     * 2.   Then it waits for job to stop.
     * 3.   Then notifies mObserver
     * @param mi - initial room (entrance)
     */
    private void runPathFinding(RoomInterface mi) {
        // run path searching
        mfinisherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                notifyObserver();
                mExecutor.shutdown();
            }
        });
        addNewRoomsToVisit(mi, INITIAL_ENTRANCE_DISTANCE);
    }

    private void finishSearch() {
        mfinisherThread.start();
    }

    /**
     * Notifies observer runnable, about search finished
     */
    private void notifyObserver() {
        if (mObserver == null) {
            mExecutor.shutdown();
            throw new NullPointerException("Observer object must be provided");
        } else {
            mObserver.run();
        }
    }


    private void setShortestDistanceToExit(double distance) {
        mRoomLock.writeLock().lock();
        try {
            mShortestDistanceToExit = distance;
            mExitFound = true;
        } finally {
            mRoomLock.writeLock().unlock();
        }
    }

    /**
     * Adds new rooms to queue. This method is synchronized between threads.
     * @param room - room to visit
     * @param distanceToParentRoom - room parent distance
     */
    private synchronized void addNewRoomsToVisit(RoomInterface room, double distanceToParentRoom) {
        // increment running visits count
        mActiveVisits.incrementAndGet();
        // push new task to main thread pool
        mExecutor.execute(new VisitRoomTask(room, distanceToParentRoom));
    }

    /**
     * Visits room, and updates search info if necessary
     * @param room - room to visit
     * @return - false if this room is an exit or has no corridors to visit, true otherwise
     */
    private void visitRoom(RoomInterface room) {
        boolean hasCorridors = false;
        // check if room is exit
        boolean isExit = room.isExit();
        double roomDistance = 0;
        // check if room is exit
        if (isExit) {
            // update info about exit found
            // check  if any exit has been found earlier
            if (getExitFound()) {
                // check if this exit is closer to entrance than previous exit
                // save this exit distance
                roomDistance = room.getDistanceFromStart();
                boolean isCloserThanCurrentExit = Double.compare(roomDistance, getCurrentDistanceToExit()) < 0;
                if (isCloserThanCurrentExit) {
                    setShortestDistanceToExit(roomDistance);
                }
            } else {
                roomDistance = room.getDistanceFromStart();
                setShortestDistanceToExit(roomDistance);
            }
        } else if (room.corridors() != null && room.corridors().length > 0) {
            // if this room is not an exit, check if any exit has been found earlier
            roomDistance = room.getDistanceFromStart();
            if (getExitFound()) {
                // if any exit was found, check, if this room distance is closer - if not, then skip this room corridors
                // their distance would be greater than current exit, so its bad :)
                boolean isCloserThanCurrentExit = Double.compare(roomDistance, getCurrentDistanceToExit()) < 0;
                if (isCloserThanCurrentExit) {
                    hasCorridors = true;
                }
            } else hasCorridors = true;
        }
        if (hasCorridors) {
            // Add every corridor to thread task pool
            for (RoomInterface corridor : room.corridors()) {
                addNewRoomsToVisit(corridor, roomDistance);
            }
        }
    }



    /**
     * Runnable task for visiting rooms in labyrinth.
     * It is executed by threads in ThreadPoolExecutor
     */
    private class VisitRoomTask implements Runnable {

        /** Room object for visit */
        private RoomInterface room;
        /** This room parent-room distance from entrance */
        private double distanceFromParentRoom;

        public VisitRoomTask(RoomInterface room, double distanceFromParentRoom) {
            this.room = room;
            this.distanceFromParentRoom = distanceFromParentRoom;
        }

        /**
         * This method processes one room from executor queue
         * 0.   It checks validity of labyrinth - corridors must have greater distance than their parent room.
         * 1.   It checks, if this room parent distance is closer than currently found exit.
         *      if no exit was found, it assumes that this room is an entrance,
         *      or a good candidate for searching - there might be an exit after this room.
         * 2.   Then it checks, if this room is not null, and then launches synchronized block of code
         *      visitRoom() which updates shared information if necessary.
         * 3.   If no exit was found, and this room has corridors,
         *      then it pushes them to mExecutor task queue.
         */
        @Override
        public void run() {
            if (isParentRoomCloserThenCurrentExitFound()) {
                // check if room is not null
                if (room != null) {
                    visitRoom(room);
                }
            }
            // decrement running visits count
            int activeVisits = mActiveVisits.decrementAndGet();
            // check if this is a last visit
            if (activeVisits == 0) {
                finishSearch();
            }
        }

        /**
         * This method checks if room parent has has shorter distance.
         * @return
         */
        private boolean isParentRoomCloserThenCurrentExitFound() {
            boolean isCloser = false;
            // check if an exit was found eariler
            if (getExitFound()) {
                boolean isCloserThanCurrentExitFound = Double.compare(distanceFromParentRoom, getCurrentDistanceToExit()) < 0;
                if (isCloserThanCurrentExitFound) {
                    isCloser = true;
                }
            } else {
                // if no exit was found earlier, assume this room is a good candidate for search, or it is an entrance
                isCloser = true;
            }
            return isCloser;
        }
    }

}
