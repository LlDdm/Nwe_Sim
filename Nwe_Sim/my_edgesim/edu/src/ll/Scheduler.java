package ll;

import org.apache.commons.math3.analysis.function.Max;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

class Scheduler {
    private PriorityBlockingQueue<APP> apps;
    EdgeDevice this_edgeDevice;


    public Scheduler(EdgeDevice this_edgeDevice) {
        this.apps = new PriorityBlockingQueue<>();
        this.this_edgeDevice = this_edgeDevice;
    }

    public void addApp(APP app) {
        apps.put(app);
    }

    // 启动调度器
    public void startDevice(String orchestratorPolicy) {
        // 接收APP,并调度
        new Thread(() -> {
            while (SimManager.getInstance().isRunning()) {
                if(!apps.isEmpty()) {
                    listenForApps(orchestratorPolicy);  // 持续监听app
                }else {
                    Thread.yield();
                }
            }
        }).start();
    }

    // 将收到的app的b-level排序存入调度队列中
    private void listenForApps(String orchestratorPolicy) {
            try {
                APP app = apps.take();// 获取队列中的app
                List<Task>sortedTask = B_levelSort(app);
                sortedTask.removeIf(task -> task.get_taskId() == -1 || task.get_taskId() == -2);
                scheduleTasks(orchestratorPolicy,sortedTask,app);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    private void scheduleTasks(String orchestratorPolicy, List<Task>sortedTask,APP app) {
            List<EdgeDevice> edgeDevices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdge_devices();
            NetWork netWork = SimManager.getInstance().getNetworkModel();
            List<MobileDevice> mobileDevices = SimManager.getInstance().getLoadGeneratorModel().getMobileDevices();
            MobileDevice this_mobileDevice = mobileDevices.get(app.getMobileDeviceId());
            Map<Integer, EdgeDevice> deviceMap = edgeDevices.stream().collect(Collectors.toMap(EdgeDevice ::getDeviceId, EdgeDevice -> EdgeDevice));

            switch (orchestratorPolicy) {
                case "COFE":
                    allocateTasksToDevices_COFE(deviceMap, sortedTask, netWork, this_mobileDevice, app);
                    break;
                case "DCDS":
                    allocateTasksToDevices_DCDS(deviceMap, sortedTask, netWork, this_mobileDevice);
                    break;
                case "LL":
                    allocateTasksToDevices_LL(deviceMap, sortedTask, netWork, this_mobileDevice, app);
                    break;
                case "RAN":
                    allocateTasksToDevices_RAN(deviceMap,sortedTask);
                    break;
                default:
                    System.out.println("没找到对应算法");
                    break;
            }
    }

    // 为任务选择合适的设备
    private void allocateTasksToDevices_COFE(Map<Integer, EdgeDevice> devices, List<Task> sorted_tasks, NetWork netWork, MobileDevice mobileDevice,
                                             APP app) {
        double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice.getDevice_location());

        //一次对一个应用的所有任务进行调度
        for(Task task : sorted_tasks) {
            DAG dag = app.getDag();
            EdgeDevice targetDevice = null;
            List<EdgeDevice> matchedDevices = new ArrayList<>(); // 存储满足第一步要求的设备
            List<Task> preTasks = task.getPredecessors();
            long Min_EstimateComplete_time = Long.MAX_VALUE;

            // COFE第一步，找最小前延迟的设备
            for (Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
                //计算前延迟
                long Max_PreOutputPrepared_time = 0;
                for (Task pre : preTasks) {
                    double distance;
                    long tra_delay;
                    double BW;
                    if (pre.get_taskId() == -1) {
                        BW = calculate_Mobile_BW(mobileDevice, netWork);
                        long MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                        long NtoE_delay;
                        if (this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId())
                            NtoE_delay = 0;
                        else {
                            distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                            NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                        }
                        tra_delay = NtoE_delay + MtoN_delay;
                    } else {
                        EdgeDevice preDevice = devices.get(pre.getDevice_Id());
                        if (preDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            tra_delay = 0;
                        } else {
                            distance = calculateDistance(entry.getValue().getlocation(), preDevice.getlocation());
                            BW = calculateBW(preDevice, entry.getValue(), netWork);
                            tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preDevice.getUploadspeed());
                        }
                    }
                    long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                    if (pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                        Max_PreOutputPrepared_time = pre_outputPrepared_time;
                    }
                }

                //计算任务的估计开始时间，选择数据等待时间和队列等待时间的最大值
                task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));
                //估计完成时间
                long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
                task.setEstimate_complete_time(task_estimate_complete_time);

                if (task_estimate_complete_time <= Min_EstimateComplete_time) {
                    if (task_estimate_complete_time < Min_EstimateComplete_time) {
                        Min_EstimateComplete_time = task_estimate_complete_time;

                        if (matchedDevices.isEmpty()) {
                            matchedDevices.add(entry.getValue());
                        } else {
                            matchedDevices.clear();
                            matchedDevices.add(entry.getValue());
                        }
                    } else {
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
                for (EdgeDevice edgedevice : matchedDevices) {
                    for (Task preTask : preTasks) {
                        if (edgedevice.getTaskSets().contains(preTask) && dag.getCriticalTasks().contains(preTask) && dag.getCriticalTasks().contains(task)) {
                            targetDevice = edgedevice;
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1) {
                        break;
                    }
                    if (edgedevice.getIdle() < Min_Idle) {
                        Min_Idle = edgedevice.getIdle();
                        min_Idle_device = edgedevice;
                    }
                }
                if (flag == 0) {
                    targetDevice = min_Idle_device;
                }
            }

            // 将任务交给传输线程传输给目标设备
            assert targetDevice != null;
            task.setDevice_Id(targetDevice.getDeviceId());
            targetDevice.addTask(task);
            task.allocate_semaphore.release();
            targetDevice.addTaskSets(task);
            //System.out.println("mobile:" + task.getMobileDeviceId() + " APP:" + task.getAppid() + " task:" + task.get_taskId() + " device:" + targetDevice.getDeviceId());
        }
    }

    private void allocateTasksToDevices_DCDS(Map<Integer, EdgeDevice> devices, List<Task> sorted_tasks, NetWork netWork, MobileDevice mobileDevice) {
        double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice.getDevice_location());
        devices.remove(0);//DCDS算法在分配设备时先不考虑云
        APP app = mobileDevice.getApp().get(sorted_tasks.get(0).getAppid());

        for (Task task : sorted_tasks) {
            long MtoN_delay = 0;
            EdgeDevice targetDevice = null;
            List<Task> preTasks = task.getPredecessors();
            long Min_EstimateComplete_time_and_averageDelay = Long.MAX_VALUE;

            // 找最小前延迟的设备
            for (Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
                long Max_PreOutputPrepared_time = 0;
                for (Task pre : preTasks) {
                    double distance;
                    long tra_delay;
                    double BW;
                    if (pre.get_taskId() == -1) {
                        BW = calculate_Mobile_BW(mobileDevice, netWork);
                        MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                        long NtoE_delay;
                        if (this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            NtoE_delay = 0;
                        } else {
                            distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                            NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                        }
                        tra_delay = NtoE_delay + MtoN_delay;
                    } else {
                        EdgeDevice preEdgeDevice = devices.get(pre.getDevice_Id());
                        if (preEdgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            tra_delay = 0;
                        } else {
                            distance = calculateDistance(entry.getValue().getlocation(), preEdgeDevice.getlocation());
                            BW = calculateBW(preEdgeDevice, entry.getValue(), netWork);
                            tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preEdgeDevice.getUploadspeed());
                        }
                    }
                    long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                    if (pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                        Max_PreOutputPrepared_time = pre_outputPrepared_time;
                    }
                }

                //获取任务的估计开始时间时间和估计完成时间
                task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));
                long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
                task.setEstimate_complete_time(task_estimate_complete_time);

                long OutputAverageTra_delay = 0;
                double distance;
                long BW;
                if (task.getSuccessors().get(0).get_taskId() == -2) {
                    long EtoN_delay;
                    if (entry.getValue().getDeviceId() == this_edgeDevice.getDeviceId()) {
                        EtoN_delay = 0;
                    } else {
                        distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                        BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                        EtoN_delay = EstimateTra_delay(task, task.getSuccessors().get(0), distance, BW, this_edgeDevice.getDownloadspeed(), entry.getValue().getUploadspeed());
                    }
                    OutputAverageTra_delay = EtoN_delay + MtoN_delay;
                } else {
                    for (Map.Entry<Integer, EdgeDevice> other_entry : devices.entrySet()) {
                        if (entry.getValue().getDeviceId() != other_entry.getValue().getDeviceId()) {
                            distance = calculateDistance(other_entry.getValue().getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(other_entry.getValue(), entry.getValue(), netWork);
                            OutputAverageTra_delay += EstimateTra_avgDelay(distance, BW, other_entry.getValue().getDownloadspeed(), entry.getValue().getUploadspeed());
                        }
                    }
                    OutputAverageTra_delay = OutputAverageTra_delay / devices.size();
                }

                long estimateComplete_time_and_average_delay = task_estimate_complete_time + OutputAverageTra_delay;

                if (estimateComplete_time_and_average_delay < Min_EstimateComplete_time_and_averageDelay) {
                    Min_EstimateComplete_time_and_averageDelay = estimateComplete_time_and_average_delay;
                    targetDevice = entry.getValue();
                }
            }


            // 将任务交给传输线程传输给目标设备
            assert targetDevice != null;
            task.setDevice_Id(targetDevice.getDeviceId());
            targetDevice.addTask(task);
            task.allocate_semaphore.release();
            targetDevice.addTaskSets(task);
        }
    }

    private void allocateTasksToDevices_LL(Map<Integer, EdgeDevice> devices, List<Task> sorted_tasks, NetWork netWork,MobileDevice mobileDevice,
                                           APP app) {
        EdgeDevice cloud = devices.get(0);
        devices.remove(0);
        //反向预测每个任务可能被分配到的设备
        backward_predicting(sorted_tasks, devices, netWork, this_edgeDevice);

        double MtoN_distance = calculateDistance(this_edgeDevice.getlocation(), mobileDevice.getDevice_location());

        if(isAppOverdue(devices,sorted_tasks,app,mobileDevice,netWork,MtoN_distance)){
            System.out.println("mobileDevice:" + mobileDevice.getDeviceId() + " app:" + app.getAppid() + " 可能超过截止时间，应提交给云执行");
            for(Task task : sorted_tasks){
                task.setDevice_Id(cloud.getDeviceId());
                cloud.addTask(task);
                task.allocate_semaphore.release();
                cloud.addTaskSets(task);
            }
            return;
        }

        for (Task task : sorted_tasks) {
            DAG dag = app.getDag();
            long MtoN_delay = 0;
            EdgeDevice targetDevice = null;
            List<EdgeDevice> matchedDevices = new ArrayList<>(); // 存储满足第一步要求的设备
            List<Task> preTasks = task.getPredecessors();
            long Min_EstimateComplete_time_and_averageDelay = Long.MAX_VALUE;

            //获取当前任务的直接后继任务的预测服务器
            Map<Integer, EdgeDevice> suc_PredictingDevices = getSuc_PredictingDevices(task, devices);

            // 第一步，找当前任务估计完成时间+输出数据平均传输时间最小的设备
            for (Map.Entry<Integer, EdgeDevice> entry : devices.entrySet()) {
                long Max_PreOutputPrepared_time = 0;
                for (Task pre : preTasks) {
                    double distance;
                    long tra_delay;
                    double BW;
                    if (pre.get_taskId() == -1) {
                        BW = calculate_Mobile_BW(mobileDevice, netWork);
                        MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                        long NtoE_delay;
                        if (this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            NtoE_delay = 0;
                        } else {
                            distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                            NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                        }
                        tra_delay = NtoE_delay + MtoN_delay;
                    } else {
                        EdgeDevice preEdgeDevice = devices.get(pre.getDevice_Id());
                        if (preEdgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            tra_delay = 0;
                        } else {
                            distance = calculateDistance(entry.getValue().getlocation(), preEdgeDevice.getlocation());
                            BW = calculateBW(preEdgeDevice, entry.getValue(), netWork);
                            tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preEdgeDevice.getUploadspeed());
                        }
                    }
                    long pre_outputPrepared_time = pre.getEstimate_complete_time() + tra_delay;
                    if (pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                        Max_PreOutputPrepared_time = pre_outputPrepared_time;
                    }
                }

                task.setEstimate_start_time(Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time));
                long task_estimate_complete_time = (long) (task.getEstimate_start_time() + Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
                task.setEstimate_complete_time(task_estimate_complete_time);

                //与直接后继任务的预测设备输出延迟最小
                long OutputAverageTra_delay = 0;
                double distance;
                long BW;
                if (task.getSuccessors().get(0).get_taskId() == -2) {
                    long EtoN_delay;
                    if (entry.getValue().getDeviceId() == this_edgeDevice.getDeviceId()) {
                        EtoN_delay = 0;
                    } else {
                        distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                        BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                        EtoN_delay = EstimateTra_delay(task, task.getSuccessors().get(0), distance, BW, this_edgeDevice.getDownloadspeed(), entry.getValue().getUploadspeed());
                    }
                    OutputAverageTra_delay = EtoN_delay + MtoN_delay;
                } else {
                    for (Map.Entry<Integer, EdgeDevice> other_entry : suc_PredictingDevices.entrySet()) {
                        if (entry.getValue().getDeviceId() != other_entry.getValue().getDeviceId()) {
                            distance = calculateDistance(other_entry.getValue().getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(other_entry.getValue(), entry.getValue(), netWork);
                            OutputAverageTra_delay += EstimateTra_avgDelay(distance, BW, other_entry.getValue().getDownloadspeed(), entry.getValue().getUploadspeed());
                        }
                    }
                    OutputAverageTra_delay = OutputAverageTra_delay / suc_PredictingDevices.size();
                }

                long estimateComplete_time_and_average_delay = task_estimate_complete_time + OutputAverageTra_delay;

                if (estimateComplete_time_and_average_delay <= Min_EstimateComplete_time_and_averageDelay) {
                    if (estimateComplete_time_and_average_delay < Min_EstimateComplete_time_and_averageDelay) {
                        Min_EstimateComplete_time_and_averageDelay = estimateComplete_time_and_average_delay;
                        if (!matchedDevices.isEmpty()) {
                            matchedDevices.clear();
                        }
                        matchedDevices.add(entry.getValue());

                    } else {
                        matchedDevices.add(entry.getValue());
                    }
                }
            }


            // 利用COFE第二步和第三步优化
            if (matchedDevices.size() == 1) {
                targetDevice = matchedDevices.get(0);
            } else if (matchedDevices.size() > 1) {
                long Min_Idle = Long.MAX_VALUE;
                EdgeDevice min_Idle_device = null;
                int flag = 0;
                for (EdgeDevice edgedevice : matchedDevices) {
                    for (Task preTask : preTasks) {
                        if (edgedevice.getTaskSets().contains(preTask) && dag.getCriticalTasks().contains(preTask) && dag.getCriticalTasks().contains(task)) {
                            targetDevice = edgedevice;
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1) {
                        break;
                    }
                    if (edgedevice.getIdle() < Min_Idle) {
                        Min_Idle = edgedevice.getIdle();
                        min_Idle_device = edgedevice;
                    }
                }
                if (flag == 0) {
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
    }

    public void allocateTasksToDevices_RAN(Map<Integer,EdgeDevice> devices, List<Task> sorted_tasks) {
        for(Task task : sorted_tasks) {
            int rand_deviceID = (int)(Math.random()*devices.size());
            EdgeDevice targetDevice = devices.get(rand_deviceID);
            assert targetDevice != null;
            task.setDevice_Id(targetDevice.getDeviceId());
            targetDevice.addTask(task);
            task.allocate_semaphore.release();
            targetDevice.addTaskSets(task);
        }
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
        long outputSize = predecessor.getSuccessorsMap().get(thisTask);
        return (long) (outputSize / BW + distance / 299792458 * 1000 + (double) (outputSize * 1000) / downloadSpeed + (double) (outputSize * 1000) / uploadSpeed);

    }

    private long EstimateTra_avgDelay(double distance, double BW, long downloadSpeed, long uploadSpeed){
        return (long) (1000 / BW + distance / 299792458 * 1000 + (double) (1000 * 1000) / downloadSpeed + (double) (1000 * 1000) / uploadSpeed);
    }

    private Map<Integer,EdgeDevice> getSuc_PredictingDevices(Task task,Map<Integer,EdgeDevice> devices){
        Map<Integer,EdgeDevice> result = new HashMap<>();
        for(Task suc : task.getSuccessors()){
            for(int id : suc.getPredicting_device_Id()){
                result.put(id,devices.get(id));
            }
        }
        return result;
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

    public void backward_predicting(List<Task> sorted_tasks,Map<Integer, EdgeDevice> devices, NetWork netWork,
                                    EdgeDevice this_edgeDevice) {
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
            task.addPredicting_device_Id(matchedDevices_id);
        }
    }

    public boolean isAppOverdue(Map<Integer,EdgeDevice> devices,List<Task> sorted_tasks,APP app,MobileDevice mobileDevice,NetWork netWork,double MtoN_distance) {
        long MtoN_delay;
        app.getstartTask().setPredicting_complete_time(app.getStartTime());

        for(Task task : sorted_tasks) {
            long Max_PreOutputPrepared_time = 0;

            Map<Integer,EdgeDevice> predictingDevices = new HashMap<>();
            for(int id : task.getPredicting_device_Id()){
                predictingDevices.put(id,devices.get(id));
            }
            long Max_predicting_complete_time = Long.MIN_VALUE;

            for(Map.Entry<Integer, EdgeDevice> entry : predictingDevices.entrySet()) {
                for (Task pre : task.getPredecessors()) {
                    double distance;
                    long tra_delay;
                    double BW;
                    if (pre.get_taskId() == -1) {
                        BW = calculate_Mobile_BW(mobileDevice, netWork);
                        MtoN_delay = EstimateTra_delay(pre, task, MtoN_distance, BW, this_edgeDevice.getDownloadspeed(), mobileDevice.getUploadSpeed());

                        long NtoE_delay;
                        if (this_edgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            NtoE_delay = 0;
                        } else {
                            distance = calculateDistance(this_edgeDevice.getlocation(), entry.getValue().getlocation());
                            BW = calculateBW(this_edgeDevice, entry.getValue(), netWork);
                            NtoE_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), this_edgeDevice.getUploadspeed());
                        }
                        tra_delay = NtoE_delay + MtoN_delay;
                    } else {
                        EdgeDevice preEdgeDevice = devices.get(pre.getMaxDelay_device_Id());
                        if (preEdgeDevice.getDeviceId() == entry.getValue().getDeviceId()) {
                            tra_delay = 0;
                        } else {
                            distance = calculateDistance(entry.getValue().getlocation(), preEdgeDevice.getlocation());
                            BW = calculateBW(preEdgeDevice, entry.getValue(), netWork);
                            tra_delay = EstimateTra_delay(pre, task, distance, BW, entry.getValue().getDownloadspeed(), preEdgeDevice.getUploadspeed());
                        }
                    }

                    long pre_outputPrepared_time = pre.getPredicting_complete_time() + tra_delay;
                    if (pre_outputPrepared_time > Max_PreOutputPrepared_time) {
                        Max_PreOutputPrepared_time = pre_outputPrepared_time;
                    }
                }

                //计算任务的预测完成时间
                long task_predicting_complete_time = (long) (Math.max(entry.getValue().getQueueTask_EstimateMaxComplete(), Max_PreOutputPrepared_time) +
                        Math.ceil(task.getSize() * 2 / entry.getValue().getMips()) + entry.getValue().getIdle());
                task.setPredicting_complete_time((task_predicting_complete_time));

                if (task_predicting_complete_time > Max_predicting_complete_time) {
                    Max_predicting_complete_time = task_predicting_complete_time;
                    task.setMaxDelay_device_Id(entry.getValue().getDeviceId());
                }
            }
            task.setPredicting_complete_time(Max_predicting_complete_time);
        }

        Task endTask = app.getendTask();
        long max_end_traDelay = Long.MIN_VALUE;
        for(Task pre : endTask.getPredecessors()) {
            long tra_delay;
            if(pre.getMaxDelay_device_Id() == this_edgeDevice.getDeviceId())
                tra_delay = 0;
            else{
                EdgeDevice pre_device = devices.get(pre.getMaxDelay_device_Id());
                double distance = calculateDistance(pre_device.getlocation(),this_edgeDevice.getlocation());
                double BW = calculateBW(pre_device,this_edgeDevice,netWork);
                tra_delay = EstimateTra_delay(pre,endTask,distance,BW,this_edgeDevice.getDownloadspeed(),pre_device.getUploadspeed());
            }

            if(tra_delay + pre.getPredicting_complete_time() > max_end_traDelay) {
                max_end_traDelay = tra_delay + pre.getPredicting_complete_time();
            }
        }
        endTask.setPredicting_complete_time(max_end_traDelay);

        return app.getDeadline() < endTask.getPredicting_complete_time();
    }

}

