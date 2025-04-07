package ll;

import java.util.List;

public class AppTransferThread extends Thread {
    private APP app;
    private Scheduler scheduler;
    private double[] thisLocation;
    private double[] otherLocation;
    private int Attractiveness;
    private int connetionTyoe;

    public AppTransferThread(APP app, Scheduler scheduler, double[] thisLocation,double[] otherLocation,
                             int Attractiveness, int connetionTyoe) {
        this.app = app;
        this.scheduler = scheduler;
        this.thisLocation = thisLocation;
        this.otherLocation = otherLocation;
        this.Attractiveness = Attractiveness;
        this.connetionTyoe = connetionTyoe;
    }

    @Override
    public void run() {
        double delay = 0;
        long app_inputSize = app.getInputsize();
        NetWork netWork_model = SimManager.getInstance().getNetworkModel();
        double distance = calculateDistance(thisLocation, otherLocation);
        try {
            delay = switch (connetionTyoe) {
                case 0 -> app_inputSize * distance / netWork_model.getLAN_BW(Attractiveness);
                case 1 -> app_inputSize * distance / netWork_model.getWLAN_BW(Attractiveness);
                case 2 -> app_inputSize * distance / netWork_model.getGSM_BW(Attractiveness);
                default -> delay;
            };
            // 模拟网络传输延迟
            Thread.sleep((long) (delay * 1000));  // 转换为毫秒
            scheduler.addApp(app);
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

