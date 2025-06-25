package ll;
import java.util.*;

public class OutputTransferThread extends Thread {

        private Task task;
        private Task sucTask;
        private EdgeDevice edgeDevice;
        private double[] thisLocation;
        private double[] sucLocation;
        private int Attractiveness;
        private double uploadSpeed;

        public OutputTransferThread(Task task,Task sucTask, EdgeDevice edgeDevice, double[] thisLocation, double[] sucLocation,
                                    int Attractiveness, double uploadSpeed) {
            this.task = task;
            this.sucTask = sucTask;
            this.edgeDevice = edgeDevice;
            this.thisLocation = thisLocation;
            this.sucLocation = sucLocation;
            this.Attractiveness = Attractiveness;
            this.uploadSpeed = uploadSpeed;
        }

        @Override
        public void run() {
            long delay;
            long outputSize = task.getSuccessorsMap().get(sucTask);
            NetWork netWork_model = SimManager.getInstance().getNetworkModel();
            long distance_delay = (long) (calculateDistance(thisLocation, sucLocation) / 299792458 * 1000);

            // 模拟传输延迟
            if(Attractiveness == edgeDevice.getAttractiveness()){
                delay = outputSize  / netWork_model.getLAN_BW() + distance_delay;
            }
            else if(edgeDevice.getDeviceId() != 0){
                delay = outputSize  / netWork_model.getMAN_BW() + distance_delay;
            }
            else {
                delay = outputSize  / netWork_model.getWAN_BW() + distance_delay;
            }

            long receive_delay = (long)(outputSize * 1000 / uploadSpeed + (double) (outputSize * 1000) / edgeDevice.getDownloadspeed());// 模拟上传下载延迟

            delay += receive_delay;
            delay = (long) Math.ceil(delay);
            task.setOutput_traDelay( sucTask, delay);
            try {
                // 模拟网络传输延迟
                Thread.sleep(delay);  // 毫秒
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


