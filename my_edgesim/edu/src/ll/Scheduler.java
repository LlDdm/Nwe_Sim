package ll;

class Scheduler {
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

