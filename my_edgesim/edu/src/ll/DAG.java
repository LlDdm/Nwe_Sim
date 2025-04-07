package ll;

import java.util.*;

public class DAG {
    private List<Task> tasks;

    DAG() {
        this.tasks = new ArrayList<>();
    }

    void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public Task getTask(int id) {
        for (Task task : tasks) {
            if (task.get_taskId() == id) {
                return task;
            }
        }
        // 如果没有找到匹配的Task, 返回null或者抛出异常
        return null; // 或者 throw new NoSuchElementException("No Task with such ID found");
    }

    public List<Task> topologicalSort() {
        List<Task> sortedTasks = new ArrayList<>();
        HashMap<Integer, Integer> outDegree = new HashMap<>();
        Queue<Task> queue = new LinkedList<>();
        for (Task task : tasks) {
            outDegree.put(task.get_taskId(), task.getSuccessors() .size());
            if(task.getSuccessors().isEmpty()){
                queue.offer(task);
                task.setR(0);
            }
        }

        while(!queue.isEmpty()){
            Task task = queue.poll();
            sortedTasks.add(task);
            for(Task pre : task.getPredecessors()) {
                if (pre != null) {
                    queue.offer(pre);
                    long recent_max = 0;
                    for (Task suc : pre.getSuccessors()) {
                        long tra = pre.getSuccessorsMap().get(suc);
                        if (tra + suc.getR() > recent_max)
                            recent_max = tra + suc.getR();
                    }
                    pre.setR(recent_max + pre.getSize());
                }
            }
        }

        sortedTasks.sort(Comparator.comparingLong(Task::getR));
        Collections.reverse(sortedTasks);

        if (sortedTasks.size() != tasks.size()) {
            throw new IllegalArgumentException("The graph has at least one cycle, cannot perform topological sort.");
        }
        return sortedTasks;
    }
}
