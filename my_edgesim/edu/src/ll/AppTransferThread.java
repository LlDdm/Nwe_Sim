package ll;

import java.util.List;

public class AppTransferThread extends Thread {
    private APP app;
    private Scheduler scheduler;
    private double distance;
    private int Attractiveness;
    private int connetionTyoe;

    public AppTransferThread(APP app, Scheduler scheduler, double distance, int Attractiveness, int connetionTyoe) {
        this.app = app;
        this.scheduler = scheduler;
        this.distance = distance;
        this.Attractiveness = Attractiveness;
        this.connetionTyoe = connetionTyoe;
    }

    @Override
    public void run() {
        double delay = 0;
        long app_inputSize = app.getInputsize();
        NetWork netWork_model = SimManager.getInstance().getNetworkModel();
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
}

