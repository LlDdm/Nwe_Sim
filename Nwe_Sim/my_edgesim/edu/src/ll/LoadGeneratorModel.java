package ll;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.poi.ss.formula.functions.T;

public class LoadGeneratorModel {
    private final Random rng;
    protected int numberOfMobileDevices;
    //protected long simulationTime;
    private long loadGenerate_Time;
    private long AppStart_time_offset;
    private int App_num;

    protected String simScenario;
    private String useScenario;

    private List<MobileDevice> mobileDevices;

    private int[][] mobileDeviceCluster_num;

    public LoadGeneratorModel(int _numberOfMobileDevices, String _simScenario, String _useScenario,long _loudGenerate_time,
                              long AppStart_time_offset) {
        this.numberOfMobileDevices =  _numberOfMobileDevices;
        //this.simulationTime = _simulationTime;
        this.simScenario = _simScenario;
        this.rng = ThreadLocalRandom.current();
        this.mobileDevices = new ArrayList<>();
        this.mobileDeviceCluster_num = new int[SimSettings.getInstance().Attractiveness_NUM][3];
        this.useScenario = _useScenario;
        this.loadGenerate_Time = _loudGenerate_time;
        this.AppStart_time_offset = AppStart_time_offset;
        this.App_num = 0;
    }

    public List<MobileDevice> getMobileDevices() {
        return mobileDevices;
    }

    public int[][] getMobileDeviceCluster() { return mobileDeviceCluster_num; }

    public String getUseScenario() { return useScenario; }

    private int generateNormalint(int mean, int stdDev){
        return (int) Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private long generateNormalLong(long mean, long stdDev) {
        return Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private int generateUniformInt(int min, int max) {return rng.nextInt(min, max + 1);}

    public int getApp_num(){return App_num;}


    public void initializeModel() {
        double [][] APPlookuptable = SimSettings.getInstance().getAppLookUpTable();
        if (APPlookuptable == null || APPlookuptable.length == 0)
            throw new IllegalStateException("App lookup table is not initialized or is empty.");

        long off_line_start_time = System.currentTimeMillis();

        //Each mobile device utilizes an app type (task type)
        for(int i=0; i<numberOfMobileDevices; i++) {
            List<APP> apps = new ArrayList<>();
            int app_id = 0;
            int randomAppType = -1;
            double appTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
            double appTypePercentage = 0;
            for (int j = 0; j < APPlookuptable.length; j++) {
                appTypePercentage += APPlookuptable[j][0];
                if (appTypeSelector <= appTypePercentage) {
                    randomAppType = j;
                    break;
                }
            }
            if (randomAppType == -1) {
                System.out.println("Impossible is occurred! no random task type!");
                continue;
            }
            String AppName = SimSettings.getInstance().getAppName(randomAppType);

            //随机连接方式：0,lan or 1,wlan or 2,GSM
            int randomconnectionType = -1;
            double connectionTypeSelector = SimUtils.getRandomDoubleNumber(0, 100);
            double connectionTypePercentage = 0;
            for (int j = 0; j < SimSettings.getInstance().getconnectiontypeLookUpTable().length; j++) {
                connectionTypePercentage += SimSettings.getInstance().getconnectiontypeLookUpTable()[j][0];
                if (connectionTypeSelector <= connectionTypePercentage) {
                    randomconnectionType = j;
                    break;
                }
            }
            if (randomconnectionType == -1) {
                System.out.println("Impossible is occurred! no random connection type!");
                continue;
            }


            double poissonMean = APPlookuptable[randomAppType][1];
            double activePeriod = APPlookuptable[randomAppType][2] ;
            double idlePeriod = APPlookuptable[randomAppType][3];
            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME ,
                    (SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod));  //active period starts shortly after the simulation started (e.g. 10 seconds)
            double virtualTime = activePeriodStartTime;

            ExponentialDistribution ps = new ExponentialDistribution(poissonMean);

            while (virtualTime < loadGenerate_Time) {

                double interval = ps.sample();

                while (interval <= 0) {
                    System.out.println("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
                    interval = ps.sample();
                }

                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                virtualTime += interval;

                if (virtualTime > activePeriodStartTime + activePeriod) {
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }

                long appinputSize = (long) APPlookuptable[randomAppType][4];
                long inputSizeBias = appinputSize * 5 / 100;
                appinputSize = rng.nextLong(appinputSize - inputSizeBias, appinputSize + inputSizeBias + 1);

                long applength = (long) APPlookuptable[randomAppType][6];
                long lengthBias = applength * 5 / 100;
                applength = rng.nextLong(applength - lengthBias, applength + lengthBias + 1);

                long appoutputSize = (long) APPlookuptable[randomAppType][5];
                long outSizeBias = appoutputSize * 5 / 100;
                appoutputSize = rng.nextLong(appoutputSize - outSizeBias, appoutputSize + outSizeBias + 1);

                int avg_task_num = (int) APPlookuptable[randomAppType][7];
                int task_num_Bias = avg_task_num / 10;
                avg_task_num = rng.nextInt(avg_task_num - task_num_Bias, avg_task_num + task_num_Bias + 1);

                long avg_task_size = (applength / avg_task_num);

                double CCR = APPlookuptable[randomAppType][9];
                long total_tra_size = (long) (CCR * applength);

                long avg_task_tra_size = total_tra_size / avg_task_num;

                double ev_ds =  APPlookuptable[randomAppType][10];

                double shape_factor = APPlookuptable[randomAppType][8];

                int max_width = (int) (Math.sqrt(avg_task_num) * shape_factor);

                DAG dag = generateDAG(avg_task_num, max_width, avg_task_size, avg_task_tra_size, ev_ds,
                        appinputSize, appoutputSize, app_id,i);
                dag.computeCriticalPath();
                dag.setTasksMap();

                // 设置任务前驱同步信号量
                for(Task task : dag.getTasks()){
                    if(task.get_taskId() != -1) {
                        task.wait_pre = new CountDownLatch(task.getPredecessors().size());
                        //System.out.println("mobile:" + task.getMobileDeviceId() + " APP:" + task.getAppid() + " task:" + task.get_taskId() + " wait:" + task.wait_pre);
                    }
                }

                long start_time;

                if(Objects.equals(useScenario, "OFF_Line")){
                    start_time = (long) (off_line_start_time + (virtualTime + AppStart_time_offset) * 1000);
                }else {
                    start_time = (long) (System.currentTimeMillis() + (virtualTime + AppStart_time_offset) * 1000);
                }


                long execution_time = generateUniformInt(5, 10);

                long end_time = start_time + execution_time * 60000;

                APP app = new APP(app_id, AppName, start_time, end_time, appinputSize, appoutputSize, applength, dag, CCR, shape_factor, i);
                apps.add(app);
                app_id++;
                App_num++;

                if(Objects.equals(useScenario, "OFF_Line"))
                    break;
            }

            //移动设备位置随机设置
            double[][] edgeDeviceLookUpTable = SimSettings.getInstance().getEdgedeviceLookUpTable();
            int random_edgedevice = rng.nextInt(1, edgeDeviceLookUpTable.length);

            double moniledevice_latitude = edgeDeviceLookUpTable[random_edgedevice][3]; // 纬度
            moniledevice_latitude = SimUtils.getRandomDoubleNumber(moniledevice_latitude - 1, moniledevice_latitude + 1);
            double mobiledevice_longitude = edgeDeviceLookUpTable[random_edgedevice][4];
            mobiledevice_longitude = SimUtils.getRandomDoubleNumber(mobiledevice_longitude - 1, mobiledevice_longitude + 1);
            int mobiledevice_attractiveness = (int) edgeDeviceLookUpTable[random_edgedevice][5];

            mobileDevices.add(new MobileDevice(apps, moniledevice_latitude, mobiledevice_longitude, mobiledevice_attractiveness,
                    randomconnectionType, i, (long) edgeDeviceLookUpTable[random_edgedevice][6], (long) edgeDeviceLookUpTable[random_edgedevice][7]));

            mobileDeviceCluster_num[mobiledevice_attractiveness][randomconnectionType]++;
        }

    }

    public DAG generateDAG(int taskNum, int maxWidth, long avg_task_size, long avg_task_tra_size,
                           double ev_ds, long appinputSize, long appoutputSize, int appid, int mobileDeviceId) {
        List<Task> taskList = new ArrayList<>();
        DAG dag = new DAG();

        // 1. 创建虚拟节点
        Task startTask = new Task(-1, 0, appid, mobileDeviceId);  // 起点
        Task endTask = new Task(-2, 0, appid, mobileDeviceId);    // 汇点
        dag.addTask(startTask);
        dag.addTask(endTask);

        // 2. 创建任务节点
        for (int i = 0; i < taskNum; i++) {
            long bias = avg_task_size * 20 / 100;
            long task_size = rng.nextLong(avg_task_size - bias, avg_task_size +  bias + 1);
            Task task = new Task(i, task_size, appid, mobileDeviceId);
            taskList.add(task);
            dag.addTask(task);
        }

        // 3. 在每层之间生成随机的依赖关系
        List<List<Task>> layers = new ArrayList<>();
        int currentLayer = 0;
        int currentTask_num = 0;

        // 在DAG的深度范围内生成层次
        while (currentTask_num < taskNum) {
            List<Task> layer = new ArrayList<>();
            int layerWidth = generateUniformInt(1, maxWidth);; // 每层的宽度随机
            for (int i = 0; i < layerWidth; i++) {
                Task task = taskList.get(currentTask_num + i);
                layer.add(task);
                if(currentTask_num + i + 1 == taskNum)
                    break;
            }
            currentTask_num += layerWidth;
            layers.add(layer);
            currentLayer++;
        }

        List<Task> lastTasks = new ArrayList<>();
        List<Task> preTasks = new ArrayList<>();
        preTasks.add(startTask);
        // 4. 生成依赖关系：确保每个节点有至少一个前驱节点
        for (int i = 1; i < layers.size() ; i++) {
            List<Task> currentLayerTasks = layers.get(i);
            List<Task> preLayerTasks = layers.get(i-1);
            preTasks.addAll(preLayerTasks);

            // 为当前层的任务添加依赖于下一层任务的关系
            for (Task currTask : currentLayerTasks) {
                while (true) {
                    for (Task preTask : preLayerTasks) {
                        if (rng.nextBoolean()) {
                            long bias = avg_task_tra_size * 10 / 100;
                            long edgeSize = rng.nextLong(avg_task_tra_size - bias, avg_task_tra_size + bias + 1);
                            currTask.addPredecessor(preTask, edgeSize);
                            preTask.addSuccessor(currTask, edgeSize);
                        }
                    }
                    if(currTask.getPredecessors().isEmpty())
                        continue;
                    else
                        break;
                }

                // 按概率为当前任务添加一个跨层的前驱
                for(int k=0; k<2;k++){
                    Task preTask =  preTasks.get(rng.nextInt(preTasks.size()));
                    if(!currTask.getPredecessors().contains(preTask)) {
                        if (Math.random() < (double) 1 /taskNum ) {
                            long bias = avg_task_tra_size * 10 / 100;
                            long edgeSize = rng.nextLong(avg_task_tra_size - bias, avg_task_tra_size + bias + 1);
                            currTask.addPredecessor(preTask, edgeSize);
                            preTask.addSuccessor(currTask, edgeSize);
                        }
                    }
                }
            }

            for(Task preTask : preLayerTasks) {
                if(preTask.getSuccessors().isEmpty()){
                    lastTasks.add(preTask);
                }
            }
        }

        // 5. 将起点连接到第一层任务，汇点连接到最后一层任务
        List<Task> firstLayer = layers.get(0);
        List<Long>allocatedSizes = randomsize(firstLayer.size(), appinputSize);
        int i = 0;
        for (Task task : firstLayer) {
            startTask.addSuccessor(task, allocatedSizes.get(i));
            task.addPredecessor(startTask, allocatedSizes.get(i));
            i++;
        }

        List<Task> lastLayer = layers.get(layers.size() -1 );
        lastTasks.addAll(lastLayer);
        //随机选取跨层的末尾任务
        for(int k=0; k<2;k++){
            Task preTask =  preTasks.get(rng.nextInt(preTasks.size()));
            if(!lastTasks.contains(preTask)) {
                if (Math.random() < (double) 1 /taskNum ) {
                    lastTasks.add(preTask);
                }
            }
        }

        allocatedSizes = randomsize(lastTasks.size(),appoutputSize);
        i = 0;
        for (Task task : lastTasks) {
            task.addSuccessor(endTask, allocatedSizes.get(i));
            endTask.addPredecessor(task, allocatedSizes.get(i));
            i++;
        }



        // 设置任务前驱输出同步量

        dag.setDepth(currentLayer);
        dag.setMaxWidth(maxWidth);

        // 6. 返回DAG及任务列表
        return dag;
    }

    //为起始点与第一层节点之间的边和最后一层节点与结束节点之间的边生成随机大小的传输数据
    public List<Long> randomsize(int length, long in_or_out_size){
        List<Long> randomSizes = new ArrayList<>();

        while (length > 1) {  // 保证最后一个部分分配时可以直接填充剩余值
            // 计算每个部分的允许范围
            long maxSize = in_or_out_size * 70 / 100;

            // 确保 maxSize 至少为 1
            if (maxSize < 1) {
                maxSize = 1;
            }

            // 生成一个在 minSize 到 maxSize 之间的随机数
            long num = rng.nextLong(1, maxSize + 1 );

            // 确保生成的 num 在剩余分配的大小范围内
            if (num > in_or_out_size - (length - 1)) {
                num = in_or_out_size - (length - 1); // 确保最后一个值不会大于剩余值
            }

            // 更新长度和剩余的总大小
            length--;
            in_or_out_size -= num;

            // 将生成的随机数加入列表
            randomSizes.add(num);
        }

        // 最后一个部分直接分配剩余的全部大小
        randomSizes.add(in_or_out_size);

        return randomSizes;
    }

}