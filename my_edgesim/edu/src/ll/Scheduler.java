package ll;

import java.util.LinkedList;
import java.util.Queue;

class Scheduler {
    private LinkedList<APP> apps;
    private Queue<Task> preparedTasks;
    private String orchestratorPolicy;
    private String simScenario;

    public Scheduler(String orchestratorPolicy, String simScenario) {
        this.orchestratorPolicy = orchestratorPolicy;
        this.simScenario = simScenario;
    }

    public synchronized void addApp(APP app) { apps.add(app); }
    public void addPreparedTask(Task task) { preparedTasks.offer(task); }

    public static void allocateTaskToDevice(EdgeDevice device, Task task) {
        device.addTask(task);
    }


    // 自动分配任务到空闲设备，改进的任务调度方法
    public static void allocateTasksToDevices(EdgeDevice[] devices, Task[] tasks) {
        for (Task task : tasks) {
            // 找到一个空闲的设备
            for (EdgeDevice device : devices) {
                if (!device.isProcessing()) {
                    device.addTask(task);
                    task.setDevice_name(device.getDeviceName());
                    break;
                }
            }
        }
    }
}

