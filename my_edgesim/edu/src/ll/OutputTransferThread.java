package ll;
import java.util.*;

public class OutputTransferThread extends Thread {

        private Task task;
        private Task sucTask;
        private EdgeDevice edgeDevice;
        private double distance;
        private int Attractiveness;

        public OutputTransferThread(Task task,Task sucTask, EdgeDevice edgeDevice, double distance, int Attractiveness{
            this.task = task;
            this.sucTask = sucTask;
            this.edgeDevice = edgeDevice;
            this.distance = distance;
            this.Attractiveness = Attractiveness;
        }

        @Override
        public void run() {
            double delay;
            long outputsize = task.getSuccessorsMap().get(sucTask);
            NetWork netWork_model = SimManager.getInstance().getNetworkModel();
            try {
                if(Attractiveness == edgeDevice.getAttractiveness()){
                    delay = outputsize * distance / netWork_model.getLAN_BW(Attractiveness);
                }
                else {
                    delay = outputsize * distance / netWork_model.getMAN_BW();
                }
                double receive_delay = outputsize / edgeDevice.getAttractiveness();
                delay += receive_delay;
                // 模拟网络传输延迟
                Thread.sleep((long) (delay * 1000));  // 转换为毫秒
                edgeDevice.receiveInput(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
}


