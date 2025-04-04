package ll;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.distribution.ExponentialDistribution;

public class LoadGeneratorModel {
    private final Random rng;
    protected int numberOfMobileDevices;
    protected double simulationTime;
    protected String simScenario;

    private List<APP> appList;
    private List<MobileDevice> mobileDevices;

    public LoadGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        this.numberOfMobileDevices =  _numberOfMobileDevices;
        this.simulationTime = _simulationTime;
        this.simScenario = _simScenario;
        this.rng = ThreadLocalRandom.current();
        this.appList = new ArrayList<>();
        this.mobileDevices = new ArrayList<>();
    }

    public List<MobileDevice> getMobileDevices() {
        return mobileDevices;
    }

    public List<APP> getAppList() {
        return appList;
    }

    private int generateNormalint(int mean, int stdDev){
        return (int) Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private long generateNormalLong(long mean, long stdDev) {
        return Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private int generateUniformInt(int min, int max) {
        return rng.nextInt(min, max + 1);
    }

    public void initializeModel() {
        double [][] APPlookuptable = SimSettings.getInstance().getAppLookUpTable();
        if (APPlookuptable == null || APPlookuptable.length == 0)
            throw new IllegalStateException("App lookup table is not initialized or is empty.");

        //Each mobile device utilizes an app type (task type)
        for(int i=0; i<numberOfMobileDevices; i++) {
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


            double poissonMean = APPlookuptable[randomAppType][2];
            double activePeriod = APPlookuptable[randomAppType][3];
            double idlePeriod = APPlookuptable[randomAppType][4];
            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME,
                    SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
            double virtualTime = activePeriodStartTime;

            ExponentialDistribution ps = new ExponentialDistribution(poissonMean);
            //ExponentialDistribution ps[] = new ExponentialDistribution[10];
            //for(int j=0; j<10; j++)
            //ps[j] = new ExponentialDistribution(poissonMean * ((double)1 + (double)j * (double) 0.12));

            if (virtualTime < simulationTime) {
                //int index = Math.min(9, (int)virtualTime / 15);
                //double interval = ps[9-index].sample();
                double interval = ps.sample();

                while(interval <= 0) {
                    System.out.println("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
                    interval = ps.sample();
                }

                if (virtualTime > activePeriodStartTime + activePeriod) {
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                }

                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                virtualTime += interval;

                //if (virtualTime > activePeriodStartTime + activePeriod) {
                //    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                //    virtualTime = activePeriodStartTime;
                //}

                long appinputSize = (long) APPlookuptable[randomAppType][5];
                long inputSizeBias = appinputSize / 10;
                appinputSize = SimUtils.getRandomLongNumber(appinputSize - inputSizeBias, appinputSize + inputSizeBias);

                long applength = (long) APPlookuptable[randomAppType][7];
                long lengthBias = applength / 10;
                applength = SimUtils.getRandomLongNumber(applength - lengthBias, applength + lengthBias);

                long appoutputSize = (long) APPlookuptable[randomAppType][6];
                long outSizeBias = appoutputSize / 10;
                appoutputSize = SimUtils.getRandomLongNumber(appoutputSize - outSizeBias, appoutputSize + outSizeBias);

                int avg_task_num = (int) APPlookuptable[randomAppType][13];
                int task_num_Bias = avg_task_num / 10;
                avg_task_num = (int) SimUtils.getRandomLongNumber(avg_task_num - task_num_Bias, avg_task_num + task_num_Bias);

                long avg_task_size = applength / avg_task_num;

                float CCR = (float) APPlookuptable[randomAppType][15];
                long total_tra_size = (long) CCR * applength;

                long avg_task_tra_size = total_tra_size / avg_task_num;

                float ev_ds = (float) APPlookuptable[randomAppType][16];

                float shape_factor = (float) APPlookuptable[randomAppType][14];

                int depth_of_DAG = (int) (Math.sqrt(avg_task_num) / shape_factor);
                depth_of_DAG = generateNormalint(depth_of_DAG, depth_of_DAG);
                if (depth_of_DAG < 2)
                    depth_of_DAG = 2;

                int max_width = (int) (Math.sqrt(avg_task_num) * shape_factor);

                DAG dag = generateDAG(avg_task_num, depth_of_DAG, max_width, avg_task_size, avg_task_tra_size, ev_ds,
                        appinputSize, appoutputSize, i);

                APP app = new APP(i,AppName,virtualTime,simulationTime,appinputSize,appoutputSize,
                        applength,dag,CCR,shape_factor,i);

                appList.add(app);

                //移动设备位置随机设置
                int random_edgedevice = rng.nextInt(SimSettings.getInstance().getEdgedeviceLookUpTable().length);
                double moniledevice_latitude = SimSettings.getInstance().getEdgedeviceLookUpTable()[random_edgedevice][2]; // 纬度
                moniledevice_latitude = SimUtils.getRandomDoubleNumber(moniledevice_latitude - 5, moniledevice_latitude + 5);
                double mobiledevice_longitude = SimSettings.getInstance().getEdgedeviceLookUpTable()[random_edgedevice][3];
                mobiledevice_longitude = SimUtils.getRandomDoubleNumber(mobiledevice_longitude - 5, mobiledevice_longitude + 5);
                int mobiledevice_attractiveness = (int)SimSettings.getInstance().getEdgedeviceLookUpTable()[random_edgedevice][5];

                mobileDevices.add(new MobileDevice(app,moniledevice_latitude,mobiledevice_longitude, mobiledevice_attractiveness,
                        randomconnectionType,i));
                // Assuming the creation of a workflow and its DAG is based on appType and inputFileSize

                //long outputFileSize =(long)SimSettings.getInstance().getAppLookUpTable()[randomTaskType][6];
                //long outputFileSizeBias = outputFileSize / 10;


                //int pesNumber = (int)SimSettings.getInstance().getAppLookUpTable()[randomTaskType][8];

                //outputFileSize = SimUtils.getRandomLongNumber(outputFileSize - outputFileSizeBias, outputFileSize + outputFileSizeBias);
                //length = SimUtils.getRandomLongNumber(length - lengthBias, length + lengthBias);
                //taskList.add(new TaskProperty(virtualTime, i, randomTaskType, pesNumber, applength, appinputSize, outputFileSize));
            }
        }

    }

    public DAG generateDAG(int taskNum, int depth, int maxWidth, long avg_task_size, long avg_task_tra_size,
                           float ev_ds, long appinputSize, long appoutputSize, int appid) {
        List<Task> taskList = new ArrayList<>();
        DAG dag = new DAG();

        // 1. 创建虚拟节点
        Task startTask = new Task(-1, 0, appid);  // 起点
        Task endTask = new Task(-2, 0, appid);    // 汇点
        dag.addTask(startTask);
        dag.addTask(endTask);

        // 2. 创建任务节点
        for (int i = 0; i < taskNum; i++) {
            long task_size = generateNormalLong(avg_task_size, (long)(avg_task_size / ev_ds));
            Task task = new Task(i, task_size, appid);
            taskList.add(task);
            dag.addTask(task);
        }

        // 3. 在每层之间生成随机的依赖关系
        List<List<Task>> layers = new ArrayList<>();
        int currentLayer = 0;

        // 在DAG的深度范围内生成层次
        while (currentLayer < depth) {
            List<Task> layer = new ArrayList<>();
            int layerWidth = generateUniformInt(1, maxWidth);; // 每层的宽度随机
            for (int i = 0; i < layerWidth; i++) {
                if (currentLayer * maxWidth + i < taskNum) {
                    Task task = taskList.get(currentLayer * maxWidth + i);
                    layer.add(task);
                }
            }
            layers.add(layer);
            currentLayer++;
        }

        // 4. 生成依赖关系：只关心当前层和下一层的任务依赖，确保每个节点有至少一个前驱节点
        for (int i = 1; i < layers.size() ; i++) {
            List<Task> currentLayerTasks = layers.get(i);
            List<Task> preLayerTasks = layers.get(i - 1);

            // 为当前层的任务添加依赖于下一层任务的关系
            for (Task currTask : currentLayerTasks) {
                // 确保每个任务有至少一个前驱节点
                Task predecessor = preLayerTasks.get(rng.nextInt(preLayerTasks.size()));
                long edgeSize = generateNormalLong(avg_task_tra_size, (long)(avg_task_tra_size / ev_ds));
                currTask.addPredecessor(predecessor, edgeSize);
                predecessor.addSuccessor(currTask, edgeSize);
            }
        }

        // 5. 将起点连接到第一层任务，汇点连接到最后一层任务
        List<Task> firstLayer = layers.get(0);
        List<Long>allocatedSizes = randomsize(firstLayer, appinputSize);
        int i = 0;
        for (Task task : firstLayer) {
            startTask.addSuccessor(task, allocatedSizes.get(i));
            task.addPredecessor(startTask, allocatedSizes.get(i++));
        }

        List<Task> lastLayer = layers.get(layers.size() - 1);
        allocatedSizes = randomsize(lastLayer,appoutputSize);
        i = 0;
        for (Task task : lastLayer) {
            task.addSuccessor(endTask, allocatedSizes.get(i));
            endTask.addPredecessor(task, allocatedSizes.get(i++));
        }

        // 6. 返回DAG及任务列表
        return dag;
    }

    //为起始点与第一层节点之间的边和最后一层节点与结束节点之间的边生成随机大小的传输数据
    public List<Long> randomsize(List<Task> Layer, long in_pr_out_size){
        List<Long> randomSizes = new ArrayList<>();
        for (int i=0;i<Layer.size()-1;i++) {
            randomSizes.add(rng.nextLong());
        }
        long totalRandomSum = 0;
        for (long size : randomSizes) {
            totalRandomSum += size;
        }
        List<Long> allocatedSizes = new ArrayList<>();
        for (long ratio : randomSizes) {
            allocatedSizes.add((in_pr_out_size * ratio / totalRandomSum));
        }
        long allocatedSum = 0;
        for (long size : allocatedSizes) {
            allocatedSum += size;
        }
        allocatedSizes.add(in_pr_out_size - allocatedSum);

        return allocatedSizes;
    }

}