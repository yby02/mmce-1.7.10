package github.kasuminova.mmce.common.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class SequentialTaskExecutor extends RecursiveAction {
    private final List<ForkJoinTask<?>> sequentialTaskList = new ArrayList<>();

    public SequentialTaskExecutor(List<ForkJoinTask<?>> taskList) {
        this.sequentialTaskList.addAll(taskList);
    }

    @Override
    protected void compute() {
        for (ForkJoinTask<?> task : sequentialTaskList) {
            task.fork().join();
        }
    }
}
