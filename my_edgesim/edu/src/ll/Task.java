package ll;

import java.util.*;

class Task {
    private int id; // 任务ID
    private long size; // 任务大小
    private int APPid;
    private double startime;
    private double completetime;
    private double executionTime;
    private double waitingTime;
    private Map<Task, Long> predecessors; // 前驱任务列表及边的大小
    private Map<Task, Long> successors;   // 后继任务列表及边的大小
    private long R;
    private int device_Id;
    private Map<Task,Double> output_uploadDelay;
    private Map<Task,Double> output_downloadDelay;
    private Map<Task,Double> output_traDelay;
    private Map<Task,Double> output_sentQueue_waitTime;
    private double traDelay;
    private double uploadDelay;
    private double downloadDelay;
    private double ScheduleQueue_waitTime;


    Task(int id, long size, int APPid) {
        this.id = id;
        this.size = size;
        this.APPid = APPid;
        this.predecessors = new HashMap<>();
        this.successors = new HashMap<>();
        this.device_Id = -1;
    }

    // 添加前驱任务并记录边的大小
    public void addPredecessor(Task task, long edgeSize) {
        this.predecessors.put(task, edgeSize);
    }
    // 添加后继任务并记录边的大小
    public void addSuccessor(Task task, long edgeSize) {
        this.successors.put(task, edgeSize);
    }

    public List<Task> getPredecessors() {
        return new ArrayList<>(predecessors.keySet());
    }
    public List<Task> getSuccessors() {
        return new ArrayList<>(successors.keySet());
    }

    public Map<Task, Long> getPredecessorsMap() {
        return predecessors;
    }
    public Map<Task, Long> getSuccessorsMap() {
        return successors;
    }

    public Map<Task, Double> getOutputUploadDelay() { return output_uploadDelay; }
    public void setOutput_uploadDelay(Task task, Double output_uploadDelay) { this.output_uploadDelay.put(task,output_uploadDelay); }

    public Map<Task, Double> getOutputDownloadDelay() { return output_downloadDelay; }
    public void setOutput_downloadDelay(Task task, Double output_downloadDelay) { this.output_downloadDelay.put(task,output_downloadDelay); }

    public Map<Task, Double> getOutputTraDelay() { return output_traDelay; }
    public void setOutput_traDelay(Task task, double output_traDelay) { this.output_traDelay.put(task,output_traDelay); }

    public void setDevice_Id(int device_Id) {
        this.device_Id = device_Id;
    }
    public int getDevice_Id() {
        return device_Id;
    }

    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }

    public int getAppid() {
        return APPid;
    }
    public void setAppid(int appid) {
        this.APPid = appid;
    }

    public double getStarTime() {return startime;}
    public void setStarTime(double time) {
        this.startime = time;
    }

    public double getCompleteTime() {
        return completetime;
    }
    public void setCompleteTime(double time) {
        this.completetime = time;
    }

    public long getR() {
        return R;
    }
    public void setR(long time) {
        this.R = time;
    }

    public void set_task_Id(int id) {
        this.id = id;
    }
    public int get_taskId() {
        return id;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }
    public double getExecutionTime() {
        return executionTime;
    }

    public void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }
    public double getWaitingTime() {
        return waitingTime;
    }

    public void setTraDelay(double traDelay) { this.traDelay = traDelay; }
    public double getTraDelay() { return traDelay; }

    public void setUploadDelay(double uploadDelay) { this.uploadDelay = uploadDelay; }
    public double getUploadDelay() { return uploadDelay; }

    public void setDownloadDelay(double downloadDelay) { this.downloadDelay = downloadDelay; }
    public double getDownloadDelay() { return downloadDelay; }

    public void setScheduleQueue_waitTime(double queue_waitTime) { this.ScheduleQueue_waitTime = queue_waitTime; }
    public double getScheduleQueue_waitTime() { return ScheduleQueue_waitTime; }

    public void setOutput_sentQueue_waitTime(Task task,double sentQueue_waitTime) { this.output_sentQueue_waitTime.put(task,sentQueue_waitTime); }
    public Map<Task, Double> getOutput_sentQueue_waitTime() { return output_sentQueue_waitTime; }


    @Override
    public String toString() {
        return "Task{id=" + id + ", size=" + size + "}";
    }
}

