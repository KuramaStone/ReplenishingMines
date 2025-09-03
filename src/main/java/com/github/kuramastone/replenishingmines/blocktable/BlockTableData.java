package com.github.kuramastone.replenishingmines.blocktable;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;

public record BlockTableData(List<BlockTableDataEntry> list) {

    private static SplittableRandom random = new SplittableRandom();

    public @Nullable BlockTableDataEntry random() {
        double sum = 0.0;
        for (BlockTableDataEntry entry : list) {
            if (0 < entry.weight())
                sum += entry.weight();
        }
        double rnd = random.nextDouble() * sum;
        double running = 0.0;
        for (BlockTableDataEntry entry : list) {
            if(entry.weight() < 0)
                continue;

            running += entry.weight();
            if (rnd < running) {
                return entry;
            }
        }

        return null;
    }
}
