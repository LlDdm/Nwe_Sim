package ll;

public class FirstTaskTransferThread extends Thread {
    private Task task;
    private Task sucTask;
    private EdgeDevice edgeDevice;
    private int Attractiveness;
    private int connectionType;
    private double[] mobileDevice_location = new double[2];
    private double mobileUploadSpeed;

    public FirstTaskTransferThread(double mobileUploadSpeed, Task task, Task sucTask, EdgeDevice edgeDevice,
                                   int Attractiveness, int connectionType, double[] mobileDevice_location) {
        this.task = task;
        this.sucTask = sucTask;
        this.edgeDevice = edgeDevice;
        this.Attractiveness = Attractiveness;
        this.connectionType = connectionType;
        this.mobileDevice_location = mobileDevice_location;
        this.mobileUploadSpeed = mobileUploadSpeed;
    }

    @Override
    public void run() {
        long delay = 0;
        long outputSize = task.getSuccessorsMap().get(sucTask);
        NetWork netWork_model = SimManager.getInstance().getNetworkModel();
        EdgeDevice nativeEdge = SimManager.getInstance().getNativeEdgeDeviceGenerator().getNativeDevicesMap().get(Attractiveness);
        long distance1_delay = (long) (calculateDistance(mobileDevice_location, nativeEdge.getlocation()) / 299792458 * 1000);
        // 移动设备到本地边缘设备的传输延迟
        delay = switch (connectionType) {
            case 0 -> outputSize  / netWork_model.getLAN_BW() + distance1_delay;
            case 1 -> outputSize  / netWork_model.getWLAN_BW() + distance1_delay;
            case 2 -> outputSize  / netWork_model.getGSM_BW() + distance1_delay;
            default -> delay;
        };
        // 移动设备的上传延迟+本地边缘设备的下载延迟
        delay += (long) ((double) (outputSize * 1000) / nativeEdge.getDownloadspeed() + outputSize *1000 / mobileUploadSpeed);

        // 本地边缘设备到目的边缘设备的传输延迟
        long distance2_delay = (long) (calculateDistance(nativeEdge.getlocation(), edgeDevice.getlocation()) / 299792458 * 1000);
        if (Attractiveness == edgeDevice.getAttractiveness()) {
            delay += outputSize  / netWork_model.getLAN_BW() + distance2_delay;
        }
        else if(edgeDevice.getDeviceId() != 0){
            delay +=  outputSize  / netWork_model.getMAN_BW() + distance2_delay;
        }
        else {
            delay += outputSize  / netWork_model.getWAN_BW() + distance2_delay;
        }
        // 本地边缘设备的上传延迟+目的边缘设备的下载延迟
        long receive_delay = (long) (outputSize / edgeDevice.getDownloadspeed() + outputSize / nativeEdge.getUploadspeed());
        delay += receive_delay;

        delay = (long) Math.ceil(delay);
        task.setOutput_traDelay(sucTask, delay);
        try {
            // 模拟网络传输延迟
            Thread.sleep(delay );  // 毫秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 任务到达标志
        sucTask.wait_pre.countDown();
    }

    public static double calculateDistance(double[] this_location, double[] other_location) {
        double dLat = this_location[0] - other_location[0];
        double dLon = this_location[1] - other_location[1];


        return Math.sqrt(Math.pow(dLat,2) + Math.pow(dLon, 2)) * 1000;  // Returns the distance in kilometers
    }

}
