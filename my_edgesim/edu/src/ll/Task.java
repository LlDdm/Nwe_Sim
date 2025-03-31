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
    private String device_name;


    Task(int id, long size, int APPid) {
        this.id = id;
        this.size = size;
        this.APPid = APPid;
        this.predecessors = new HashMap<>();
        this.successors = new HashMap<>();
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

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getDevice_name() {
        return device_name;
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
    public double getStartime() {
        return startime;
    }
    public void setStartime(double time) {
        this.startime = time;
    }
    public double getCompletetime() {
        return completetime;
    }
    public void setCompletetime(double time) {
        this.completetime = time;
    }
    public long getR() {
        return R;
    }
    public void setR(long time) {
        this.R = time;
    }

    public void settask_Id(int id) {
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

    @Override
    public String toString() {
        return "Task{id=" + id + ", size=" + size + "}";
    }
}

