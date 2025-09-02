package com.github.kuramastone.replenishingmines.utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler {
    private static final ConcurrentLinkedQueue<ScheduledTask> tasks = new ConcurrentLinkedQueue<>();

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(Scheduler::tick);
    }

    public static void schedule(Runnable task, int delayInTicks) {
        tasks.add(new ScheduledTask(task, delayInTicks));
    }

    private static void tick(MinecraftServer minecraftServer) {
        Iterator<ScheduledTask> iterator = new ArrayList<>(tasks).iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            task.delayInTicks--;
            if (task.delayInTicks <= 0) {
                task.runnable.run();
                if (!task.repeating) {
                    tasks.remove(task);
                }
                else {
                    task.delayInTicks = task.originalDelayInTicks;
                }
            }
        }
    }

    public static void scheduleRepeating(Runnable task, int delayInTicks) {
        tasks.add(new ScheduledTask(task, delayInTicks, true));
    }

    private static class ScheduledTask {
        public final Runnable runnable;
        public final int originalDelayInTicks;
        public int delayInTicks;
        public boolean repeating;

        public ScheduledTask(Runnable runnable, int delayInTicks) {
            this(runnable, delayInTicks, false);
        }

        public ScheduledTask(Runnable runnable, int delayInTicks, boolean repeating) {
            this.runnable = runnable;
            this.originalDelayInTicks = delayInTicks;
            this.delayInTicks = delayInTicks;
            this.repeating = repeating;
        }
    }
}