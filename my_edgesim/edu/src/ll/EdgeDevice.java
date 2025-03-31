package ll;

import java.util.LinkedList;
import java.util.Queue;

class EdgeDevice implements Runnable {
    private String deviceName;
    private double mips;   // 处理能力
    private double storage;         // 带宽
    private double latitude;          // 纬度
    private double longitude;         // 经度
    private double wlan_id;
    private double attractiveness;
    private Queue<Task> taskQueue;    // 任务队列
    private boolean isProcessing;     // 标志位，判断是否在处理任务

    public EdgeDevice(String deviceName, double mips, double storage, double latitude, double longitude, double wlan_id,
                      double attractiveness) {
        this.deviceName = deviceName;
        this.mips = mips;
        this.storage = storage;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wlan_id = wlan_id;
        this.attractiveness = attractiveness;
        this.taskQueue = new LinkedList<>();
        this.isProcessing = false;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public double getMips() {
        return mips;
    }

    public double getStorage() {
        return storage;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void addTask(Task task) {
        taskQueue.add(task);
    }

    public void setAttractiveness(double attractiveness) {
        this.attractiveness = attractiveness;
    }
    public double getAttractiveness() {
        return attractiveness;
    }

    public double getWlan_id() {
        return wlan_id;
    }

    public double getlatitude(){
        return latitude;
    }
    public double getlongitude(){
        return longitude;
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
    public boolean getIsProcessing() {
        return isProcessing;
    }

    @Override
    public void run() {
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();  // 按照FIFO顺序处理任务
            isProcessing = true;
            System.out.println("Device " + deviceName + " started task " + task.get_taskId());
            try {
                Thread.sleep((long) (task.getSize() * 1000 / mips));  // 模拟任务执行时间
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            task.setCompletetime(System.currentTimeMillis());
            task.setExecutionTime(task.getCompletetime() - task.getExecutionTime());
            System.out.println("Device " + deviceName + " finished task " + task.get_taskId());
            isProcessing = false;
        }
    }

    public double calculateDistance(EdgeDevice other) {
        double lat1 = Math.toRadians(this.latitude);
        double lon1 = Math.toRadians(this.longitude);
        double lat2 = Math.toRadians(other.getLatitude());
        double lon2 = Math.toRadians(other.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        final double R = 6371; // Radius of Earth in km
        return R * c;  // Returns the distance in kilometers
    }
}

