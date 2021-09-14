package org.workflowsim;

import org.cloudbus.cloudsim.Cloudlet;

import java.util.Comparator;

public class PairingItem implements Comparable<PairingItem> {
    public Cloudlet getCloudlet() {
        return cloudlet;
    }

    public void setCloudlet(Cloudlet cloudlet) {
        this.cloudlet = cloudlet;
    }

    public CondorVM getVm() {
        return vm;
    }

    public void setVm(CondorVM vm) {
        this.vm = vm;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    private Cloudlet cloudlet;
    private CondorVM vm;
    private double rank;

    public PairingItem(Cloudlet c, CondorVM v) {
        this.cloudlet = c;
        this.vm = v;
    }

    @Override
    public String toString() {
        return cloudlet.getCloudletId() + " : " + vm.getId()+ " : " + vm.getState() + " : " + rank + " : "+ cloudlet.isScheduled();
    }

    @Override
    public int compareTo(PairingItem o) {
        return Comparators.RANK.compare(this, o);
    }



    public static class Comparators {

        public static Comparator<PairingItem> RANK = new Comparator<PairingItem>() {
            @Override
            public int compare(PairingItem o1, PairingItem o2) {
                if (o1.getRank() < o2.getRank()){ return 1; }
                if (o1.getRank() > o2.getRank()){ return -1; }
                return 0;
            }
        };
    }


}
