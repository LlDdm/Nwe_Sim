package ll;

import org.apache.commons.math3.analysis.function.Max;
import org.apache.poi.ss.formula.functions.T;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

class Scheduler {
    private PriorityBlockingQueue<APP> apps;
    private BlockingQueue<Task> preparedTasks;
    EdgeDevice this_edgeDevice;


    public Scheduler(EdgeDevice this_edgeDevice) {
        this.apps = new PriorityBlockingQueue<>();
        this.preparedTasks = new LinkedBlockingQueue<>();
        this.this_edgeDevice = this_edgeDevice;
    }

    public void addApp(APP app) {
        apps.put(app);
    }

    // 启动调度器
    public void startDevice(String orchestratorPolicy, String simScenario) {
        // 接收APP
        new Thread(() -> {
            while (SimManager.getInstance().isRunning()) {
                if(!apps.isEmpty()) {
                    listenForApps();  // 持续监听app
                }else {
                    Thread.yield();
                }
            }
        }).start();

        // 调度任务
        new Thread(() -> {
            while (SimManager.getInstance().isRunning()) {
                if(!preparedTasks.isEmpty()) {
                    scheduleTask(orchestratorPolicy, simScenario); // 持续调度任务
                }else {
                    Thread.yield();
                }
            }
        }).start();
    }

    // 将收到的app的b-level排序存入调度队列中
    private void listenForApps() {
            try {
                APP app = apps.take();// 获取队列中的app
                List<Task>sortedTask = B_levelSort(app);
                sortedTask.removeIf(task -> task.get_taskId() == -1 || task.get_taskId() == -2);
                for (Task task : sortedTask) {
                    preparedTasks.put(task);
                }
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    private void scheduleTask(String orchestratorPolicy, String simScenario) {
        try {
            Task task = preparedTasks.take();
            List<EdgeDevice> edgeDevices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
            NetWork netWork = SimManager.getInstance().getNetworkModel();
            List<MobileDevice> mobileDevices = SimManager.getInstance().getLoadGeneratorModel().getMobileDevices();
            MobileDevice this_mobileDevice = mobileDevices.get(task.getMobileDeviceId());
            DAG dag = this_mobileDevice.getApp().get(task.getAppid()).getDag();
            Map<Integer, EdgeDevice> deviceMap = edgeDevices.stream().collect(Collectors.toMap(EdgeDevice ::getDeviceId, EdgeDevice -> EdgeDevice));
            //deviceMap.remove(0);//移除云，因为选择时先不考虑云

            switch (orchestratorPolicy) {
                case "COFE":
                    allocateTasksToDevices_COFE(deviceMap, task, netWork, mobileDevices, this_edgeDevice, dag);
                    break;
                case "DCDS":
                    allocateTasksToDevices_DCDS(deviceMap, task, netWork, mobileDevices, this_edgeDevice);
                    break;
                case "LL":
                    allocateTasksToDevices_LL(deviceMap, task, netWork, mobileDevices, this_edgeDevice, dag);
                    break;
            }
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 分配任务到相应的设备，改进的任务调度方法
    private void allocateTasksToDevices_COFE(Map<Integer, EdgeDevice> devices, Task task, NetWork netWork, List<MobileDevice> mobileDevices,
                                             EdgeDevice this_edgeDevice, DAG dag) {
        MobileDevice mobileDevice = mobileDevices.get(task.getMobileDeviceId());
        double[] mobileDevice_location = mobileDevice.getDevice_location();
        double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice_location);

        EdgeDevice targetDevice = null;
        List<EdgeDevice> matchedDevices = new ArrayList<>(); // 存储满足第一步要求的设备
        List<Task> preTasks = task.getPredecessors();
        long Min_EstimateComplete_time = Long.MAX_VALUE;

        // COFE第一步，找当前任务估计完成时间最小的设备
        for(Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
            //计算数据等待时间
            long Max_PreOutputPrepared_time = 0;
            for(Task pre: preTasks) {
                double distance;
                long tra_delay;
                double BW;
                if(pre.get_taskId() == -1) {
                    BW = calculate_Mobile_BW(mobileDevice, netWork);
                    long MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                    long NtoE_delay;
                    if(this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId())
                        NtoE_delay = 0;
                    else {
                        distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                        BW = calculateBW(this_edgeDevice,entry.getValue(),netWork);
                        NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                    }
                    tra_delay = NtoE_delay + MtoN_delay;
                } else {
                    EdgeDevice preDevice = devices.get(pre.getDevice_Id());
                    if (preDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                        tra_delay = 0;
                    } else {
                        distance = calculateDistance(entry.getValue().getlocation(), preDevice.getlocation());
                        BW = calculateBW(preDevice,entry.getValue(),netWork);
                        tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preDevice.getUploadspeed());
                    }
                }
                long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                if(pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                    Max_PreOutputPrepared_time = pre_outputPrepared_time;
                }
            }

            //计算任务的估计开始时间，选择数据等待时间和队列等待时间的最大值
            task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));
            //估计完成时间
            long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
            task.setEstimate_complete_time(task_estimate_complete_time);

            if(task_estimate_complete_time <= Min_EstimateComplete_time) {
                if (task_estimate_complete_time < Min_EstimateComplete_time) {
                    Min_EstimateComplete_time = task_estimate_complete_time;

                    if (matchedDevices.isEmpty()) {
                        matchedDevices.add(entry.getValue());
                    } else {
                        matchedDevices.clear();
                        matchedDevices.add(entry.getValue());
                    }
                }else {
                    matchedDevices.add(entry.getValue());
                }
            }
        }

        // COFE第二步和第三步
        if (matchedDevices.size() == 1) {
            targetDevice = matchedDevices.get(0);
        } else if (matchedDevices.size() > 1) {
            matchedDevices.remove(devices.get(0));
            long Min_Idle = Long.MAX_VALUE;
            EdgeDevice min_Idle_device = null;
            int flag = 0;
            for(EdgeDevice edgedevice: matchedDevices) {
                for(Task preTask: preTasks) {
                    if(edgedevice.getTaskSets().contains(preTask) && dag.getCriticalTasks().contains(preTask) && dag.getCriticalTasks().contains(task)) {
                        targetDevice = edgedevice;
                        flag = 1;
                        break;
                    }
                }
                if(flag == 1) {break;}
                if(edgedevice.getIdle() < Min_Idle) {
                    Min_Idle = edgedevice.getIdle();
                    min_Idle_device = edgedevice;
                }
            }
            if(flag == 0) {
                targetDevice = min_Idle_device;
            }
        }

        // 将任务交给传输线程传输给目标设备
        assert targetDevice != null;
        task.setDevice_Id(targetDevice.getDeviceId());
        targetDevice.addTask(task);
        task.allocate_semaphore.release();
        targetDevice.addTaskSets(task);
    }

    private void allocateTasksToDevices_DCDS(Map<Integer, EdgeDevice> devices, Task task, NetWork netWork, List<MobileDevice> mobileDevices,
                                           EdgeDevice this_edgeDevice) {
        MobileDevice mobileDevice = mobileDevices.get(task.getMobileDeviceId());
        double[] mobileDevice_location = mobileDevice.getDevice_location();
        double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice_location);
        long MtoN_delay = 0;

        devices.remove(0);//DCDS算法在分配设备时先不考虑云
        EdgeDevice targetDevice = null;
        List<EdgeDevice> matchedDevices = new ArrayList<>(); // 存储满足第一步要求的设备
        List<Task> preTasks = task.getPredecessors();
        long Min_EstimateComplete_time_and_averageDelay = Long.MAX_VALUE;

        // 第一步，找当前任务估计完成时间+输出数据平均传输时间最小的设备
        for(Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
            long Max_PreOutputPrepared_time = 0;
            for(Task pre: preTasks) {
                double distance;
                long tra_delay;
                double BW;
                if(pre.get_taskId() == -1) {
                    BW = calculate_Mobile_BW(mobileDevice, netWork);
                    MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                    long NtoE_delay;
                    if(this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                        NtoE_delay = 0;
                    }
                    else {
                        distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                        BW = calculateBW(this_edgeDevice,entry.getValue(),netWork);
                        NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                    }
                    tra_delay = NtoE_delay - MtoN_delay;
                } else{
                    EdgeDevice preEdgeDevice = devices.get(pre.getDevice_Id());
                    if(preEdgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                        tra_delay = 0;
                    }
                    else {
                        distance = calculateDistance(entry.getValue().getlocation(), preEdgeDevice.getlocation());
                        BW = calculateBW(preEdgeDevice,entry.getValue(),netWork);
                        tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preEdgeDevice.getUploadspeed());
                    }
                }
                long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                if(pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                    Max_PreOutputPrepared_time = pre_outputPrepared_time;
                }
            }

            task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));
            long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
            task.setEstimate_complete_time(task_estimate_complete_time);

            long OutputAverageTra_delay = 0;
            double distance;
            long BW;
            if(task.getSuccessors().get(0).get_taskId() == -2){
                long EtoN_delay;
                if(entry.getValue().getDeviceId() == this_edgeDevice.getDeviceId()) {
                    EtoN_delay = 0;
                }
                else {
                    distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                    BW = calculateBW(this_edgeDevice,entry.getValue(),netWork);
                    EtoN_delay = EstimateTra_delay(task, task.getSuccessors().get(0), distance, BW, this_edgeDevice.getDownloadspeed(), entry.getValue().getUploadspeed());
                }
                OutputAverageTra_delay = EtoN_delay + MtoN_delay;
            }else {
                for (Map.Entry<Integer, EdgeDevice> other_entry : devices.entrySet()) {
                    if(entry.getValue().getDeviceId() != other_entry.getValue().getDeviceId()) {
                        distance = calculateDistance(other_entry.getValue().getlocation(), entry.getValue().getlocation());
                        BW = calculateBW(other_entry.getValue(),entry.getValue(),netWork);
                        OutputAverageTra_delay += EstimateTra_avgDelay(distance,BW,other_entry.getValue().getDownloadspeed(), entry.getValue().getUploadspeed());
                    }
                }
                OutputAverageTra_delay = OutputAverageTra_delay / devices.size();
            }

            long estimateComplete_time_and_average_delay = task_estimate_complete_time + OutputAverageTra_delay;

            if(estimateComplete_time_and_average_delay <= Min_EstimateComplete_time_and_averageDelay) {
                if (estimateComplete_time_and_average_delay < Min_EstimateComplete_time_and_averageDelay) {
                    Min_EstimateComplete_time_and_averageDelay = estimateComplete_time_and_average_delay;
                    if (!matchedDevices.isEmpty()) {
                        matchedDevices.clear();
                    }
                    matchedDevices.add(entry.getValue());

                }else {
                    matchedDevices.add(entry.getValue());
                }
            }
        }


        // 将任务交给传输线程传输给目标设备
        targetDevice = matchedDevices.get(0);
        assert targetDevice != null;
        task.setDevice_Id(targetDevice.getDeviceId());
        targetDevice.addTask(task);
        task.allocate_semaphore.release();
        targetDevice.addTaskSets(task);
    }

    private void allocateTasksToDevices_LL(Map<Integer, EdgeDevice> devices, Task task, NetWork netWork, List<MobileDevice> mobileDevices,
                                           EdgeDevice this_edgeDevice, DAG dag, EdgeDevice cloud) {
        EdgeDevice targetDevice = null;
        List<EdgeDevice> matchedDevices = new ArrayList<>(); // 存储满足第一步要求的设备
        List<Task> preTasks = task.getPredecessors();
        long Min_EstimateComplete_time_and_averageDelay = Long.MAX_VALUE;

        // 第一步，找当前任务估计完成时间+输出数据平均传输时间最小的设备
        for(Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
            long Max_PreOutputPrepared_time = 0;
            for(Task pre: preTasks) {
                double distance;
                long tra_delay;
                double BW;
                if(pre.get_taskId() == -1) {
                    MobileDevice mobileDevice = mobileDevices.get(pre.getMobileDeviceId());
                    double[] mobileDevice_location = mobileDevice.getDevice_location();
                    double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice_location);
                    BW = switch (mobileDevice.getConnectionType()) {
                        case 0 -> netWork.getLAN_BW();
                        case 1 -> netWork.getWLAN_BW();
                        case 2 -> netWork.getGSM_BW();
                        default -> 0;
                    };
                    long MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());
                    distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                    if (this_edgeDevice.getAttractiveness() == entry.getValue().getAttractiveness())
                        BW = netWork.getLAN_BW();
                    else
                        BW = netWork.getMAN_BW();
                    long NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                    tra_delay = NtoE_delay - MtoN_delay;
                } else{
                    EdgeDevice preEdgeDevice = devices.get(pre.getDevice_Id());
                    distance = calculateDistance(this_edgeDevice.getlocation(), preEdgeDevice.getlocation());
                    if(this_edgeDevice.getAttractiveness() == preEdgeDevice.getAttractiveness())
                        BW = netWork.getLAN_BW();
                    else
                        BW = netWork.getMAN_BW();
                    tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                }
                long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                if(pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                    Max_PreOutputPrepared_time = pre_outputPrepared_time;
                }
            }

            if(task.getSuccessors().get(0).get_taskId() == -2) {
                task.setEstimate_start_time(Max_PreOutputPrepared_time);
            } else {task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));}

            long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()));
            task.setEstimate_complete_time(task_estimate_complete_time);

            long OutputAverageTra_delay = 0;
            if(task.getSuccessors().get(0).get_taskId() == -2) {
                OutputAverageTra_delay = 1;
            }
            else {
                for (Map.Entry<Integer, EdgeDevice> other_entry : devices.entrySet()) {
                    if (other_entry.getValue().getAttractiveness() == entry.getValue().getAttractiveness()) {
                        OutputAverageTra_delay += (long) (calculateDistance(other_entry.getValue().getlocation(), entry.getValue().getlocation()) / netWork.getLAN_BW());
                    } else {
                        OutputAverageTra_delay += (long) (calculateDistance(other_entry.getValue().getlocation(), entry.getValue().getlocation()) / netWork.getMAN_BW());
                    }
                }
            }
            OutputAverageTra_delay = OutputAverageTra_delay / devices.size();

            long estimateComplete_time_and_average_delay = task_estimate_complete_time + OutputAverageTra_delay;

            if(estimateComplete_time_and_average_delay <= Min_EstimateComplete_time_and_averageDelay) {
                if (estimateComplete_time_and_average_delay < Min_EstimateComplete_time_and_averageDelay) {
                    Min_EstimateComplete_time_and_averageDelay = estimateComplete_time_and_average_delay;
                    if (!matchedDevices.isEmpty()) {
                        matchedDevices.clear();
                    }
                    matchedDevices.add(entry.getValue());

                }else {
                    matchedDevices.add(entry.getValue());
                }
            }
        }


        // 将任务交给传输线程传输给目标设备
        targetDevice = matchedDevices.get(0);
        assert targetDevice != null;
        task.setDevice_Id(targetDevice.getDeviceId());
        targetDevice.addTask(task);
        task.allocate_semaphore.release();
        targetDevice.addTaskSets(task);

    }

    public static double calculateDistance(double[] this_location, double[] other_location) {
        double dLat = this_location[0] - other_location[0];
        double dLon = this_location[1] - other_location[1];

        return Math.sqrt(Math.pow(dLat,2) + Math.pow(dLon, 2)) * 1000;  // Returns the distance in kilometers
    }

    public long calculate_Mobile_BW(MobileDevice device, NetWork netWork) {
        return switch (device.getConnectionType()) {
            case 0 -> netWork.getLAN_BW();
            case 1 -> netWork.getWLAN_BW();
            case 2 -> netWork.getGSM_BW();
            default -> 0;
        };
    }

    public long calculateBW(EdgeDevice srcD, EdgeDevice tar, NetWork netWork) {
        if (srcD.getAttractiveness() == tar.getAttractiveness())
            return netWork.getLAN_BW();
        else if (tar.getAttractiveness() == 4 || srcD.getAttractiveness() == 4)
            return netWork.getWAN_BW();
        else
            return netWork.getMAN_BW();
    }

    private long EstimateTra_delay(Task predecessor, Task thisTask, double distance, double BW, long downloadSpeed, long uploadSpeed){
        long outputSize = predecessor.getSuccessorsMap().get(thisTask) * 8;
        return (long) (outputSize / BW + distance / 299792458 * 1000 + (double) (outputSize * 1000) / downloadSpeed + (double) (outputSize * 1000) / uploadSpeed);

    }

    private long EstimateTra_avgDelay(double distance, double BW, long downloadSpeed, long uploadSpeed){
        return (long) (1000 / BW + distance / 299792458 * 1000 + (double) (1000 * 1000) / downloadSpeed + (double) (1000 * 1000) / uploadSpeed);
    }

    public List<Task> B_levelSort(APP app) {
        List<Task> revisedTasks = app.getDag().getTpoSort();
        Collections.reverse(revisedTasks);

        for (Task task : revisedTasks) {
            if (task.getSuccessors() == null || task.getSuccessors().isEmpty()) {
                task.setR(0);
            }else {
                List<Task> successors = task.getSuccessors();
                long R = Long.MIN_VALUE;
                for(Task successor : successors) {
                    if(successor.getR() + task.getSuccessorsMap().get(successor) > R) {
                        R = successor.getR() + task.getSuccessorsMap().get(successor);
                    }
                }
                task.setR(R);
            }
        }

        revisedTasks.sort(Comparator.comparing(Task::getR).reversed());

        return revisedTasks;
    }

    public void backward_predicting(List<Task> sorted_tasks,Map<Integer, EdgeDevice> devices, NetWork netWork, List<MobileDevice> mobileDevices,
                                    EdgeDevice this_edgeDevice, DAG dag, EdgeDevice cloud) {
        devices.remove(0);//LL算法在分配设备时先不考虑云

        sorted_tasks.sort(Comparator.comparing(Task::getR).reversed());
        for(Task task : sorted_tasks) {
            long tra_delay = 0;
            long Min_delay = Long.MAX_VALUE;
            double distance;
            long BW;
            List<Integer> matchedDevices_id = new ArrayList<>();

            for(Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()){
                task.setPredicting_complete_time((long) (entry.getValue().getQueueTask_EstimateMaxComplete() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle()));
                if(task.getSuccessors().get(0).get_taskId() == -2) {
                    if (this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                        tra_delay = 0;
                    } else {
                        distance = calculateDistance(entry.getValue().getlocation(), this_edgeDevice.getlocation());
                        BW = calculateBW(entry.getValue(), this_edgeDevice, netWork);
                        tra_delay = EstimateTra_delay(task, task.getSuccessors().get(0), distance, BW, this_edgeDevice.getDownloadspeed(), entry.getValue().getUploadspeed());
                    }
                }else {
                    int sucDevice_num = 0;
                    for(Task suc : task.getSuccessors()) {
                        for(int deviceID : suc.getPredicting_device_Id()){
                            EdgeDevice Pred_sucDevice = devices.get(deviceID);
                            if(entry.getValue().getDeviceId() == Pred_sucDevice.getDeviceId()) {
                                tra_delay += 0;
                            }else {
                                distance = calculateDistance(entry.getValue().getlocation(), Pred_sucDevice.getlocation());
                                BW = calculateBW(entry.getValue(), Pred_sucDevice, netWork);
                                tra_delay += EstimateTra_delay(task,suc, distance, BW, Pred_sucDevice.getDownloadspeed(), entry.getValue().getUploadspeed());
                            }
                            sucDevice_num++;
                        }
                    }
                    tra_delay = tra_delay / sucDevice_num;
                }
                if(tra_delay <= Min_delay) {
                    if(tra_delay < Min_delay){
                        Min_delay = tra_delay;
                        if(!matchedDevices_id.isEmpty()){
                            matchedDevices_id.clear();
                        }
                        matchedDevices_id.add(entry.getValue().getDeviceId());
                    }
                    else {
                        matchedDevices_id.add(entry.getValue().getDeviceId());
                    }
                }
            }
            task.setPredicting_device_Id(matchedDevices_id);
        }
    }

}

