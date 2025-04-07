package ll;

public class FirtTaskTransferThread extends Thread {
    private Task task;
    private Task sucTask;
    private EdgeDevice edgeDevice;
    private int Attractiveness;
    private int connectionType;
    private double[] mobileDevice_location = new double[2];

    public FirtTaskTransferThread(Task task,Task sucTask, EdgeDevice edgeDevice,
                                  int Attractiveness, int connectionType, double[] mobileDevice_location) {
        this.task = task;
        this.sucTask = sucTask;
        this.edgeDevice = edgeDevice;
        this.Attractiveness = Attractiveness;
        this.connectionType = connectionType;
        this.mobileDevice_location = mobileDevice_location;
    }

    @Override
    public void run() {
        double delay = 0;
        long outputsize = task.getSuccessorsMap().get(sucTask);
        NetWork netWork_model = SimManager.getInstance().getNetworkModel();
        EdgeDevice nativeEdge = SimManager.getInstance().getNativeEdgeDeviceGenerator().getNativeDevicesMap().get(Attractiveness);
        double distance1 = calculateDistance(mobileDevice_location, nativeEdge.getlocation());
        delay = switch (connectionType) {
            case 0 -> outputsize * distance1 / netWork_model.getLAN_BW(Attractiveness);
            case 1 -> outputsize * distance1 / netWork_model.getWLAN_BW(Attractiveness);
            case 2 -> outputsize * distance1 / netWork_model.getGSM_BW(Attractiveness);
            default -> delay;
        };
        delay += outputsize / nativeEdge.getDownloadspeed() + outputsize / edgeDevice.getUploadspeed();
        double distance2 = calculateDistance(nativeEdge.getlocation(), edgeDevice.getlocation());
        if(Attractiveness == edgeDevice.getAttractiveness()){
            delay += outputsize * distance2 / netWork_model.getLAN_BW(Attractiveness);
        }
        else {
            delay += outputsize * distance2 / netWork_model.getMAN_BW();
        }
        double receive_delay = outputsize / edgeDevice.getDownloadspeed();
        delay += receive_delay;
        task.setOutput_traDelay(sucTask, delay);
        try {
            // 模拟网络传输延迟
            Thread.sleep((long) (delay * 1000));  // 转换为毫秒
            edgeDevice.receiveInput(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public double calculateDistance(double[] this_location, double[] other_location) {
        double lat1 = Math.toRadians(this_location[0]);
        double lon1 = Math.toRadians(this_location[1]);
        double lat2 = Math.toRadians(other_location[0]);
        double lon2 = Math.toRadians(other_location[1]);

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
