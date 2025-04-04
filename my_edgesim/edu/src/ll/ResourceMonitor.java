package ll;

class ResourceMonitor {
    public static void monitorResources(EdgeDevice device) {
        System.out.println("Monitoring device " + device.getDeviceId() + "...");
        System.out.println("Processing power: " + device.getProcessingPower());
        System.out.println("Bandwidth: " + device.getBandwidth());
        System.out.println("Task queue length: " + device.taskQueue.size());
    }

    public double getGSMBandwidth(){}
}

