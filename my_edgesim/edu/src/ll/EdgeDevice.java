package ll;

import java.util.*;


class EdgeDevice{
    private int deviceId;
    private double mips;   // 处理能力
    private double storage;         // 带宽
    private final double[] location = new double[2];          // 纬度     // 经度
    private double wlan_id;
    private double attractiveness;
    private Queue<Task> taskQueue;    // 任务队列
    private boolean isProcessing;     // 标志位，判断是否在处理任务
    private List<Task> receivedInputList;
    private Queue<Map<Task,Task>> sentOutputQueue;
    private String status;  // 空闲/执行中
    private long taskQueueLength;
    private double downloadspeed;
    private double uploadspeed;

    public EdgeDevice(int deviceId, double mips, double storage, double latitude, double longitude, double wlan_id,
                      double attractiveness, double downloadspeed, double uploadspeed) {
        this.deviceId = deviceId;
        this.mips = mips;
        this.storage = storage;
        this.location[0] = latitude;
        this.location[1] = longitude;
        this.wlan_id = wlan_id;
        this.attractiveness = attractiveness;
        this.taskQueue = new LinkedList<>();
        this.isProcessing = false;
        this.receivedInputList = new ArrayList<>();
        this.sentOutputQueue = new LinkedList<>();
        this.status = "IDLE";  // 初始状态为空闲
        this.taskQueueLength = 0;
        this.downloadspeed = downloadspeed;
        this.uploadspeed = uploadspeed;
    }

    // 获取设备当前状态
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public double getMips() {
        return mips;
    }

    public double getStorage() {
        return storage;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public synchronized void addTask(Task task) {
        taskQueue.add(task);
        this.taskQueueLength += task.getSize();
    }

    public synchronized void receiveInput(Task task) {
        receivedInputList.add(task);
    }

    public void sentOutput(Map<Task,Task> outputMap) {
        sentOutputQueue.add(outputMap);
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

    public double[] getlocation() {
        return location;
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
    public boolean getIsProcessing() {
        return isProcessing;
    }

    public double getDownloadspeed() {
        return downloadspeed;
    }
    public double getUploadspeed() {
        return uploadspeed;
    }

    // 启动边缘设备，开始监听任务
    public void startDevice() {
        new Thread(() -> {
            while (true) {
                listenForTasks();  // 持续监听任务
            }
        }).start();
    }

    // 监听并处理任务
    private synchronized void listenForTasks() {
        if (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();  // 获取队列中的任务
            startTask(task);  // 开始执行任务
        }
    }

    // 执行任务
    public synchronized void startTask(Task task) {
        if (status.equals("IDLE")) {
            setStatus("EXECUTING");
            System.out.println("Device " + deviceId + " started task: " + task.get_taskId());
            try {
                // 模拟任务执行
                Thread.sleep((task.getSize()));  // 假设每个任务执行的时间与任务长度成正比
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.taskQueueLength--;
            System.out.println("Device " + deviceId + " completed task: " + task.get_taskId());
            setStatus("IDLE");
        }
    }

    // 发送任务输出数据到后继任务的设备
    private void sentOutputs(){
        new Thread(() -> {
            while (true) {
                listenForOutputs();
            }
        }).start();
    }

    private synchronized void listenForOutputs() {
        if(!sentOutputQueue.isEmpty()) {
            Map<Task, Task> map = sentOutputQueue.poll();
            List<Task> Keys = new ArrayList<>(map.keySet());
            Task currentTask = Keys.get(0);
            Task toTask = map.get(currentTask);
            List<EdgeDevice> edges = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
            startSent(currentTask, toTask, edges);
        }
    }

    public synchronized void startSent(Task currentTask, Task toTask, List<EdgeDevice> edgeDevices) {

        double stime = (double)System.currentTimeMillis();
        do{
            if (toTask.getDevice_Id() == -1) {
                continue;
            }
            break;
        } while(true);
        double etime = (double)System.currentTimeMillis();
        double waitTime = etime - stime;
        currentTask.setOutput_sentQueue_waitTime(toTask, waitTime);

        double uploadDelay = currentTask.getSuccessorsMap().get(toTask)/uploadspeed;
        currentTask.setOutput_uploadDelay(toTask, uploadDelay);
        try {
            // 模拟上传执行
            Thread.sleep((long)uploadDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EdgeDevice sucDevice = edgeDevices.get(toTask.getDevice_Id());
        SimManager.getInstance().getNetworkModel().transferTask(currentTask, toTask,sucDevice);
        if(sucDevice.getAttractiveness() != attractiveness) {
            double man_BW = SimManager.getInstance().getNetworkModel().getMAN_BW();
            currentTask.setOutput_traDelay(toTask,man_BW);
        }

    }

}

