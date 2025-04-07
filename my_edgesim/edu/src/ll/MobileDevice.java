package ll;

import java.sql.Time;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class MobileDevice {
    private List<APP> appList;
    private double[] mobiledevice_location = new double[2];    // 位置
    private int mobiledevice_attractiveness;
    private int connectionType; //0:lan,1:wlan
    private int deviceId;
    private double downloadSpeed;
    private double uploadSpeed;
    private List<Task> receiveTasks;
    private String status;

    public MobileDevice(List<APP> appList, double latitude, double longitude, int attractiveness, int connectionType, int deviceId,
                        double downloadSpeed, double uploadSpeed) {
        this.appList = appList;
        this.mobiledevice_location[0] = latitude;
        this.mobiledevice_location[1] = longitude;
        this.mobiledevice_attractiveness = attractiveness;
        this.connectionType = connectionType;
        this.deviceId = deviceId;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.receiveTasks = new ArrayList<>();
        this.status = "IDLE";
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
    public double getDownloadSpeed() { return downloadSpeed; }
    public double getUploadSpeed() { return uploadSpeed; }
    public List<Task> getReceiveTasks() { return receiveTasks; }

    // 发送任务输出数据到后继任务的设备
    private void startSentFirstTask_Outputs(Task starttask){
        List<Task> sucTasks = starttask.getSuccessors();
        for(Task sucTask : sucTasks){
            new Thread(() -> sent(starttask, sucTask)).start();
        }
    }

    private void sent(Task startTask, Task sucTask){
        double stime = (double)System.currentTimeMillis();
        while (sucTask.getDevice_Id() == -1) {
            if ((double)System.currentTimeMillis() - stime > 3600) { // timeout 是预设的超时时间
                throw new RuntimeException("设备 ID 无效，超时退出");
            }
            // 等待设备 ID 更新
        }
        double etime = (double)System.currentTimeMillis();
        double waitTime = etime - stime;
        startTask.setOutput_sentQueue_waitTime(sucTask , waitTime);
        EdgeDevice sucDevice = getEdgeDevices().get(sucTask.getDevice_Id());
        if (sucDevice == null) {
            throw new RuntimeException("未找到对应的设备");
        }
        double outputSize = startTask.getSuccessorsMap().get(sucTask);
        double upload_delay = outputSize / uploadSpeed;
        try {
            // 模拟网络传输延迟
            Thread.sleep( (long)(upload_delay * 1000));  // 转换为毫秒
            FirtTaskTransferThread firtTaskTransferThread = new FirtTaskTransferThread(startTask, sucTask, sucDevice,
                    mobiledevice_attractiveness, connectionType, mobiledevice_location);
            firtTaskTransferThread.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<EdgeDevice> getEdgeDevices(){
        return SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
    }

    // 启动移动设备，开始生成app
    public void startDevice() {
        new Thread(() -> {
            for (APP app : appList) {
                try {
                    double waitTime = System.currentTimeMillis() - app.getStartTime();
                    Thread.sleep((long) (waitTime * 1000));
                    app.setStartTime(System.currentTimeMillis());
                    operateApp(app);  // 持续监听任务
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 监听并处理任务
    private synchronized void operateApp(APP app) {
            startSentApp(app);  // 将app上传到nativeEdgeDevice的scheduler进行调度
            startTask(app);
            startSentFirstTask_Outputs(app.getstartTask());
    }

    // 执行任务
    public synchronized void startTask(APP app) {
        if (status.equals("IDLE")) {
            setStatus("EXECUTING");
            System.out.println("mobile " + deviceId + " started app: " + app.getAppid());
            setStatus("IDLE");
        }
    }

    public void startSentApp(APP app){
        EdgeDevice nativeEdgeDevice = SimManager.getInstance().getNativeEdgeDeviceGenerator().getNativeDevicesMap().get(mobiledevice_attractiveness);

    }

    public void setStatus(String status) {
        this.status = status;
    }
}
