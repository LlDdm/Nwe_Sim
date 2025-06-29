package ll;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MobileDevice {
    private List<APP> appList;
    private double[] mobiledevice_location = new double[2];    // 位置
    private int mobiledevice_attractiveness;
    private int connectionType; //0:lan,1:wlan
    private int deviceId;
    private long downloadSpeed;
    private long uploadSpeed;
    private HashSet<Task> receiveTasks;

    public MobileDevice(List<APP> appList, double latitude, double longitude, int attractiveness, int connectionType, int deviceId,
                        long downloadSpeed, long uploadSpeed) {
        this.appList = appList;
        this.mobiledevice_location[0] = latitude;
        this.mobiledevice_location[1] = longitude;
        this.mobiledevice_attractiveness = attractiveness;
        this.connectionType = connectionType;
        this.deviceId = deviceId;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.receiveTasks = new HashSet<>();
    }

    public List<APP> getApp() {
        return appList;
    }
    public double[] getDevice_location() {
        return mobiledevice_location;
    }
    public int getDevice_attractiveness() {
        return mobiledevice_attractiveness;
    }
    public int getConnectionType() {
        return connectionType;
    }
    public int getDeviceId() { return deviceId; }
    public long getDownloadSpeed() { return downloadSpeed; }
    public long getUploadSpeed() { return uploadSpeed; }
    public HashSet<Task> getReceiveTasks() { return receiveTasks; }
    public synchronized void addReceiveTask(Task t) { receiveTasks.add(t); }
    public double[] getMobiledevice_location() { return mobiledevice_location; }

    // 发送任务输出数据到后继任务的设备
    private void startSentFirstTask_Outputs(Task starttask){
        List<Task> sucTasks = starttask.getSuccessors();
        for(Task sucTask : sucTasks){
            new Thread(() -> sent(starttask, sucTask)).start(); // 使用线程池来处理传输任务
        }
    }

    private void sent(Task startTask, Task sucTask) {
        long sTime = System.currentTimeMillis();
        if(sucTask.getDevice_Id() == -1) {
            try {
                // 等待后继任务分配设备
                sucTask.allocate_semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long etime = System.currentTimeMillis();
        startTask.setSucLocated_waitTime(sucTask, etime - sTime);
        EdgeDevice sucDevice = getEdgeDevices().get(sucTask.getDevice_Id());
        if (sucDevice == null) {
            throw new RuntimeException("未找到对应的设备");
        }
        FirstTaskTransferThread firstTaskTransferThread = new FirstTaskTransferThread(uploadSpeed, startTask, sucTask, sucDevice,
                mobiledevice_attractiveness, connectionType, mobiledevice_location);
        firstTaskTransferThread.start();
    }

    public List<EdgeDevice> getEdgeDevices(){
        return SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
    }

    // 启动移动设备，开始生成app
    public void startDevice() {
        new Thread(() -> {
            if (appList == null || appList.isEmpty()) {
                System.out.println("appList is empty!");
            }else {
                for (APP app : appList) {
                    long waitTime = System.currentTimeMillis() - app.getStartTime();
                    if (waitTime < 0) {
                        try {
                            Thread.sleep(-waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    new Thread(() -> operateApp(app)).start();
                    Thread.yield();
                }
            }
        }).start();
    }

    // 处理当前app
    private void operateApp(APP app) {
        app.setStartTime(System.currentTimeMillis());
        startSentApp(app);  // 将app上传到nativeEdgeDevice的scheduler进行调度
        Task startTask = app.getstartTask();

        // 第一个虚拟任务的开始时间、完成时间和执行时间
        long time = System.currentTimeMillis();
        startTask.setStarTime(time);
        startTask.setCompleteTime(time);
        startTask.setExecutionTime(0);
        startTask.setScheduleQueue_waitTime(0);
        startTask.setAllocate_time(time);
        startTask.setArrival_time(time);
        startTask.setTra_delay(0);
        startTask.setEstimate_start_time(time);
        startTask.setEstimate_complete_time(time);
        startTask.setEstimate_scheduleQueue_waitTime(time);

        startSentFirstTask_Outputs(startTask);// 将虚拟任务的输出数据发送到后继任务所在的设备

        new Thread(() -> is_complete(app)).start();
    }

    public void startSentApp(APP app){
        EdgeDevice nativeEdgeDevice = SimManager.getInstance().getNativeEdgeDeviceGenerator().getNativeDevicesMap().get(mobiledevice_attractiveness);
        AppTransferThread appTransferThread = new AppTransferThread(app, nativeEdgeDevice, mobiledevice_location, nativeEdgeDevice.getlocation()
                , connectionType, uploadSpeed);
        appTransferThread.start();
    }

    public void is_complete(APP app){
        Task endTask = app.getendTask();
        long sTime = System.currentTimeMillis();
        try {
            // 等待前驱任务到达
            endTask.wait_pre.await();
            System.out.println("mobile_" + deviceId +  " app_" + app.getAppid() + " 完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long eTime = System.currentTimeMillis();
        endTask.setWait_pre_time(eTime - sTime);

        long time = System.currentTimeMillis();
        endTask.setStarTime(time);
        endTask.setTra_delay(0);
        endTask.setCompleteTime(time);
        endTask.setExecutionTime(0);
        endTask.setScheduleQueue_waitTime(0);
        endTask.setAllocate_time(time);
        endTask.setArrival_time(time);
        endTask.setEstimate_start_time(time);
        endTask.setEstimate_complete_time(time);
        endTask.setEstimate_scheduleQueue_waitTime(time);

        app.setCompleteTime(time);
        app.setComplete(true);
        long makeSpan = time - app.getStartTime();
        app.setMakeSpan(makeSpan);

        if(time > app.getDeadline())
            SimManager.getInstance().OverDeadline++;
        SimManager.getInstance().result.add(makeSpan);
        SimManager.getInstance().wait_complete.countDown();
    }

}
