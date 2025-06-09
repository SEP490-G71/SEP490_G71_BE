package vn.edu.fpt.medicaldiagnosis.thread.manager;

public class RoomManager {
    private final boolean[] roomStatus;

    public RoomManager(int totalRooms) {
        this.roomStatus = new boolean[totalRooms];
    }

    public synchronized void markBusy(int roomId, boolean busy) {
        roomStatus[roomId] = busy;
    }

    public synchronized int getIdleRoomCount() {
        int count = 0;
        for (boolean busy : roomStatus) {
            if (!busy) count++;
        }
        return count;
    }

    public int getTotalRooms() {
        return roomStatus.length;
    }
}
