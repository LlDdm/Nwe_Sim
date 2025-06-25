package ll;

import java.util.List;

public class EndTaskThread extends Thread {
    private Task task;
    private Task sucTask;
    private EdgeDevice edgeDevice;

    public EndTaskThread(Task task, Task sucTask, EdgeDevice edgeDevice) {
        this.task = task;
        this.sucTask = sucTask;
        this.edgeDevice = edgeDevice;
    }

    @Override
    public void run() {
        //System.out.println("开始传输最后任务 mobile: " + task.getMobileDeviceId() + " APP: " + task.getAppid() + " task: " + task.get_taskId());
        long delay;
        long delay1;
        long outputSize = task.getSuccessorsMap().get(sucTask);
        NetWork netWork_model = SimManager.getInstance().getNetworkModel();
        List<MobileDevice> mobileDevices = SimManager.getInstance().getLoadGeneratorModel().getMobileDevices();
        MobileDevice suc_mobile_device = mobileDevices.get(sucTask.getMobileDeviceId());
        EdgeDevice nativeEdge = SimManager.getInstance().getNativeEdgeDeviceGenerator().getNativeDevicesMap().get(suc_mobile_device.getDevice_attractiveness());

        if(edgeDevice.getDeviceId() == nativeEdge.getDeviceId()) {
            delay1 = 0;
        }
        else {
            delay1 = 0;
            long distance1_delay = (long) (calculateDistance(edgeDevice.getlocation(), nativeEdge.getlocation()) / 299792458 * 1000);
            // 边缘设备到本地边缘设备的传输延迟
            if (edgeDevice.getAttractiveness() == nativeEdge.getAttractiveness()) {
                delay1 += outputSize / netWork_model.getLAN_BW() + distance1_delay;
            } else if (edgeDevice.getAttractiveness() == 4) {
                delay1 += outputSize / netWork_model.getWAN_BW() + distance1_delay;
            } else {
                delay1 += outputSize / netWork_model.getMAN_BW() + distance1_delay;
            }

            // 边缘设备上传延迟+本地边缘设备下载延迟
            long receive_delay = (outputSize * 1000 / nativeEdge.getDownloadspeed() + outputSize * 1000 / edgeDevice.getUploadspeed());
            delay1 += receive_delay;
        }

        long distance2_delay = (long) (calculateDistance(nativeEdge.getlocation(), suc_mobile_device.getMobiledevice_location()) / 299792458 * 1000);
        delay = switch (suc_mobile_device.getConnectionType()) {
            case 0 -> outputSize  / netWork_model.getLAN_BW() + distance2_delay;
            case 1 -> outputSize  / netWork_model.getWLAN_BW() + distance2_delay;
            case 2 -> outputSize  / netWork_model.getGSM_BW() + distance2_delay;
            default -> 0;
        };

        // 移动设备下载延迟+本地边缘设备的上传延迟
        delay += (outputSize *1000 / nativeEdge.getUploadspeed() + outputSize * 1000 / suc_mobile_device.getDownloadSpeed()) + delay1;
        delay = (long) Math.ceil(delay);

        task.setOutput_traDelay(sucTask, delay);
        try {
            // 模拟网络传输延迟
            Thread.sleep(delay);  // 毫秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 任务到达标志
        //System.out.println("最终任务 mobile: " + task.getMobileDeviceId() + " APP: " + task.getAppid() + " task: " + task.get_taskId() + " Pre_countDown:" + sucTask.wait_pre);
        sucTask.wait_pre.countDown();
        //System.out.println("最终任务 mobile: " + task.getMobileDeviceId() + " APP: " + task.getAppid() + " task: " + task.get_taskId() + " Suc_countDown:" + sucTask.wait_pre);
        //System.out.println("最终任务传输完成 mobile: " + task.getMobileDeviceId() + " APP: " + task.getAppid() + " task: " + task.get_taskId());
    }

    public static double calculateDistance(double[] this_location, double[] other_location) {
        double dLat = this_location[0] - other_location[0];
        double dLon = this_location[1] - other_location[1];


        return Math.sqrt(Math.pow(dLat,2) + Math.pow(dLon, 2)) * 1000;  // Returns the distance in kilometers
    }

}
