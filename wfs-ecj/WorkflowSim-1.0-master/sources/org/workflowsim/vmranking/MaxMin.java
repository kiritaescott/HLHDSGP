package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;
import org.workflowsim.WorkflowSimTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MaxMin extends VMRanking {

    private static final List<Boolean> hasChecked = new ArrayList<>();

    public double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){
        double rank = Double.MAX_VALUE;

        ArrayList <PairingItem> rankedPairs = new ArrayList<>();

        //calculate the rank according to the MINMIN heuristic
        //return the rank of the targetVm
        int size = cloudletList.size();
        hasChecked.clear();
        for (int t = 0; t < size; t++) {
            hasChecked.add(false);
        }
        for (int i = 0; i < size; i++) {
            int maxIndex = 0;
            Cloudlet maxCloudlet = null;
            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) cloudletList.get(j);
                if (!hasChecked.get(j)) {
                    maxCloudlet = cloudlet;
                    maxIndex = j;
                    break;
                }
            }
            if (maxCloudlet == null) {
                break;
            }

            for (int j = 0; j < size; j++) {
                Cloudlet cloudlet = (Cloudlet) cloudletList.get(j);
                if (hasChecked.get(j)) {
                    continue;
                }
                long length = cloudlet.getCloudletLength();
                if (length > maxCloudlet.getCloudletLength()) {
                    maxCloudlet = cloudlet;
                    maxIndex = j;
                }
            }
            hasChecked.set(maxIndex, true);

            // sort vms by current requested total mips (in order of the most to the least)
            Collections.sort(vmList, new Comparator<CondorVM>() {
                @Override
                public int compare(CondorVM v1, CondorVM v2) {
                    return Double.compare(v2.getCurrentRequestedTotalMips(), v1.getCurrentRequestedTotalMips());
                }
            });

            for (Object vmObj : vmList){
                CondorVM vm = (CondorVM) vmObj;
                //machine is idle, create a pair and add it to the list
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE){
                    PairingItem pairingItem = new PairingItem(maxCloudlet, vm);
                    rankedPairs.add(pairingItem);
                }
            }

        }

        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            //System.out.println("Mxmn ranked pairs: "+p.toString()+" index: "+rankedPairs.indexOf(p));
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    //System.out.println(" MnMn Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rankedPairs.indexOf(p));
                    return rankedPairs.indexOf(p);
                }
            }
        }

        //System.out.println("Shouldn't get here");
        return rank;
    }
}
