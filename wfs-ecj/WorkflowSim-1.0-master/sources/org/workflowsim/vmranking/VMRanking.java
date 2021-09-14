package org.workflowsim.vmranking;

import org.cloudbus.cloudsim.Cloudlet;
import org.workflowsim.CondorVM;
import org.workflowsim.PairingItem;

import java.util.ArrayList;
import java.util.List;

public abstract class VMRanking {

     double getRanking(CondorVM targetVm, Cloudlet targetCloudlet, List cloudletList, List vmList){
        double ranking = 0;

        ArrayList<PairingItem> rankedPairs = new ArrayList<>();


        //after all have been ranked, find our target
        for (PairingItem p : rankedPairs){
            if (p.getCloudlet().equals(targetCloudlet)){
                if (p.getVm().equals(targetVm)){
                    ranking = rankedPairs.indexOf(p);
                    //System.out.println(" MxMn Target Rank of cloudlet: " + p.getCloudlet().getCloudletId() +" and vm: "+ p.getVm().getId() +" is : "+rank);
                }
            }
        }

        return ranking;
    }
}


