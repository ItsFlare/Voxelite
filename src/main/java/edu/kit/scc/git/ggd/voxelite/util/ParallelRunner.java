package edu.kit.scc.git.ggd.voxelite.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class ParallelRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelRunner.class);

    private final String       name;
    private final List<Runner> runners = new ArrayList<>();

    public ParallelRunner(String name, int parallelism) {
        this.name = name;
        for (int i = 0; i < parallelism; i++) {
            var runner = new Runner();
            runners.add(runner);
        }
    }

    protected abstract void run() throws Exception;

    public void start() {
        runners.forEach(Thread::start);
    }

    public void setParallelism(int parallelism) {
        int diff = parallelism - runners.size();
        if(diff == 0) return;

        if(diff > 0) {
            for (int i = 0; i < diff; i++) {
                var runner = new Runner();
                runners.add(runner);
                runner.start();
            }
        } else {
            for (int i = 0; i < -diff; i++) {
                if(runners.isEmpty()) return;
                var runner = runners.remove(runners.size() - 1);
                runner.interrupt();
            }
        }
    }

    private class Runner extends Thread {
        public Runner() {
            super();
            setName(name + " " + hashCode());
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    ParallelRunner.this.run();
                } catch (Exception e) {
                    LOGGER.error(getName(), e);
                }
            }
        }
    }

}
