import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Pawel Dylag on 25/10/15.
 *
 * This class searches labyrinth for closest exit using fixed number of threads.
 */
class PathFinder implements PathFinderInterface {

    /** Class tag */
    private static final String TAG = PathFinder.class.getSimpleName();
    /** Initial entrance distance value */
    private static final double INITIAL_ENTRANCE_DISTANCE = -1d;

    /** Shared info about exit search result */
    private boolean mExitFound;
    /** Shared info about shortest distance to exit */
    private double mShortestDistanceToExit;

    /** Observer notified about search finish */
    private Runnable mObserver;
    /** Thread pool manager object */
    private ThreadPoolExecutor mExecutor;

    public PathFinder() {
        mExitFound = false;
        mShortestDistanceToExit = INITIAL_ENTRANCE_DISTANCE;
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
        if (Double.compare(mi.getDistanceFromStart(), 0.0d) != 0 ) {
            throw new IllegalArgumentException("Entrance must have initial distance equal to 0.0"
                                                    + "{distance="
                                                    + mi.getDistanceFromStart()
                                                    + "}");
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
        return mExitFound;
    }

    @Override
    public double getShortestDistanceToExit() {
        return mShortestDistanceToExit;
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
        mExecutor.execute(new VisitRoomTask(mi, INITIAL_ENTRANCE_DISTANCE));
        while (mExecutor.getActiveCount() > 0 || mExecutor.getQueue().size() > 0) {
           // Wait for job to be done - active tasks must be equal to 0, and task queue must be empty
        }
        // notify observer about search finished
        notifyObserver();
        // print results (rather for debug)
        System.out.println(TAG + " FINISHED => exitFound = " + exitFound() + ", shortestDistanceToExit = " + getShortestDistanceToExit());
        // command mExecutor to stop receiving new tasks and to terminate itself.
        mExecutor.shutdown();
    }

    /**
     * Notifies observer runnable, about search finished
     */
    private void notifyObserver() {
        if (mObserver == null) {
            mExecutor.shutdown();
            throw new NullPointerException("Observer object must be provided");
        }
        mObserver.run();
    }

    private void setExitFound(){
        mExitFound = true;
    }

    private void setShortestDistanceToExit(double distance) {
        mShortestDistanceToExit = distance;
    }

    /**
     * Adds new rooms to queue. This method is synchronized between threads.
     * @param room - room to visit
     * @param distanceToParentRoom - room parent distance
     */
    private synchronized void addNewRoomsToVisit(RoomInterface room, double distanceToParentRoom) {
        // push new task to main thread pool
        mExecutor.execute(new VisitRoomTask(room, distanceToParentRoom));
    }

    /**
     * Visits room, and updates search info if necessary
     * @param room - room to visit
     * @return - false if this room is an exit or has no corridors to visit, true otherwise
     */
    private synchronized boolean visitRoom(RoomInterface room) {
        // setup result flag and assume room has no corridors
        boolean hasCorridors = false;
        // check if this room has an exit
        if (room.isExit()) {
            // check if any exit has been found earlier
            if (exitFound()) {
                // check if this exit is closer to entrance than previous exit
                boolean isCloserThanCurrentExit = Double.compare(room.getDistanceFromStart(), getShortestDistanceToExit()) < 0;
                if (isCloserThanCurrentExit) {
                    // save this exit distance
                    setShortestDistanceToExit(room.getDistanceFromStart());
                    setExitFound();
                }
            } else {
                setShortestDistanceToExit(room.getDistanceFromStart());
                setExitFound();
            }
        } else if (room.corridors() != null && room.corridors().length > 0) {
            // if this room is not an exit, check if any exit has been found earlier
            if (exitFound()) {
                // if any exit was found, check, if this room distance is closer - if not, then skip this room corridors
                // their distance would be greater than current exit, so its bad :)
                boolean isCloserThanCurrentExit = Double.compare(room.getDistanceFromStart(), getShortestDistanceToExit()) < 0;
                if (isCloserThanCurrentExit) {
                    hasCorridors = true;
                }
            } else hasCorridors = true;
        }
        return hasCorridors;
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
            boolean isParentRoomDistanceValid = Double.compare(distanceFromParentRoom, room.getDistanceFromStart()) > 0;
            if (isParentRoomDistanceValid) {
                throw new IllegalArgumentException("Room distance must be greater than its parent distance. {room="
                        + room.getDistanceFromStart() + " , parent=" + distanceFromParentRoom + "}");
            }
            if (isParentRoomCloserThenCurrentExitFound()) {
                // check if room is not null
                if (room != null) {
                    // enter critical section, and search room / update info about exits
                    boolean hasCorridors = visitRoom(room);
                    // if recently visited room has any valid corridors to visit
                    if (hasCorridors) {
                        // Add every corridor to thread task pool
                        for (RoomInterface corridor : room.corridors()) {
                            addNewRoomsToVisit(corridor, room.getDistanceFromStart());
                        }
                    }
                }
            }
        }

        /**
         * This method checks if room parent has has shorter distance.
         * @return
         */
        private boolean isParentRoomCloserThenCurrentExitFound() {
            boolean isCloser = false;
            // check if an exit was found eariler
            if (exitFound()) {
                boolean isCloserThanCurrentExitFound = (exitFound() && Double.compare(distanceFromParentRoom, getShortestDistanceToExit()) < 0);
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
