package org.workflowsim.scheduling;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.planning.HEFTPlanningAlgorithm;
import org.workflowsim.utils.Parameters;

import java.util.*;

/**
 * Heft algorithm. Copied from the MinMinScheduling Algorithm
 *
 * @author Kirita-Rose Escott
 * @since WorkflowSim Toolkit 1.0
 * @date Sep 9, 2019
 */

public class GRPHeftSchedulingAlgorithm extends BaseSchedulingAlgorithm  {

    private Map<Cloudlet, Map<CondorVM, Double>> computationCosts;
    private Map<Cloudlet, Map<Cloudlet, Double>> transferCosts;
    private Map<Cloudlet, Double> rank;
    private Map<CondorVM, List<Event>> schedules;
    private Map<Cloudlet, Double> earliestFinishTimes;
    private double averageBandwidth;
    private List<CloudletRank> cloudletRank;
    private List <Cloudlet> allCloudlets;
    private int HOUR = 3600;
    private boolean addingChangesCost = true;

    private class Event {

        public double start;
        public double finish;

        public Event(double start, double finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    private class CloudletRank implements Comparable<CloudletRank> {

        public Cloudlet cloudlet;
        public Double rank;

        public CloudletRank(Cloudlet cloudlet, Double rank) {
            this.cloudlet = cloudlet;
            this.rank = rank;
        }

        @Override
        public int compareTo(CloudletRank o) {
            return o.rank.compareTo(rank);
        }
    }

    public GRPHeftSchedulingAlgorithm()  {
        computationCosts = new HashMap<>();
        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();
        cloudletRank = new ArrayList<>();
        allCloudlets = new ArrayList<>();
    }

    public void getAllCloudletsChildren(List <Task> tasks, double allocationTime){
        for (Task t : tasks){
            if (!allCloudlets.contains(t)){
                allCloudlets.add(t);
                t.setAllocationTime(allocationTime);
                if (!t.getChildList().isEmpty()){
                    getAllCloudletsChildren(t.getChildList(), allocationTime);
                }
            }
        }
    }


    @Override
    public void run() throws Exception {

        int cloudletSize = getCloudletList().size();
        int vmSize = getVmList().size();

        if (cloudletSize == 0){
            return;
        }

        averageBandwidth = calculateAverageBandwidth();

        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;
            schedules.put(vm, new ArrayList<>());
        }

        double currentTime = CloudSim.clock();

        for(Object cloudObj : getCloudletList()){
            Cloudlet cloudlet = (Cloudlet) cloudObj;
            double allocationTime = currentTime;

            if (cloudlet.getAllocationTime() == null){
                cloudlet.setAllocationTime(allocationTime);
            }

            Job job = (Job) cloudlet;
            allCloudlets.add(cloudlet);
            getAllCloudletsChildren(job.getChildList(), allocationTime);
        }


        // Prioritization phase
        calculateComputationCosts();
        calculateTransferCosts();
        calculateRanks();

        // Selection phase
        allocateCloudlets();

        // De-allocate/remove allocation times for tasks that weren't ready
        deallocateNonReadyCloudlets();
    }

    private void deallocateNonReadyCloudlets() {
        for (Cloudlet c : allCloudlets){
            if (!getCloudletList().contains(c)){
                c.setAllocationTime(null);
                c.setVmId(-1);
            }
        }
    }

    /**
     * Calculates the average available bandwidth among all VMs in Mbit/s
     *
     * @return Average available bandwidth in Mbit/s
     */
    private double calculateAverageBandwidth() {
        double avg = 0.0;
        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;
            avg += vm.getBw();
        }
        return avg / getVmList().size();
    }

    /**
     * Populates the computationCosts field with the time in seconds to compute
     * a task in a vm.
     */
    private void calculateComputationCosts() {
        for (Object cloudletObject : allCloudlets) {
            Cloudlet cloudlet = (Cloudlet) cloudletObject;
            Map<CondorVM, Double> costsVm = new HashMap<>();
            for (Object vmObject : getVmList()) {
                CondorVM vm = (CondorVM) vmObject;
                if (vm.getNumberOfPes() < cloudlet.getNumberOfPes()) {
                    costsVm.put(vm, Double.MAX_VALUE);
                } else {
                    costsVm.put(vm,
                            cloudlet.getCloudletTotalLength() / vm.getMips());
                }
            }
            computationCosts.put(cloudlet, costsVm);
        }

//        System.out.println("size of computationcosts: "+computationCosts.size());
//        for (Map.Entry<Cloudlet, Map<CondorVM, Double>> entry : computationCosts.entrySet()){
//            System.out.println("cloudlet key: "+entry.getKey().getCloudletId());
//            Map<CondorVM, Double> map = entry.getValue();
//            for (Map.Entry<CondorVM, Double> entry1 : map.entrySet()){
//                System.out.println("vm key: "+entry1.getKey().getId()+" cost val: "+entry1.getValue());
//            }
//        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        // Initializing the matrix

        for (Object cloudletObj1 : allCloudlets){
            Cloudlet cloudlet1 = (Cloudlet) cloudletObj1;
            Map<Cloudlet, Double> cloudletTransferCosts = new HashMap<>();
            for (Object cloudletObj2 : allCloudlets) {
                Cloudlet cloudlet2 = (Cloudlet) cloudletObj2;
                cloudletTransferCosts.put(cloudlet2, 0.0);
            }
            transferCosts.put(cloudlet1, cloudletTransferCosts);
        }

        // Calculating the actual values
        for (Object parentObj : allCloudlets) {
            Cloudlet parent = (Cloudlet) parentObj;
            Job parentJob = (Job) parentObj;
            for (Task task : parentJob.getChildList()){
                Cloudlet child = null;
                for (Object childObj: allCloudlets){
                    Cloudlet childCloudlet = (Cloudlet) childObj;
                    if (task.getCloudletId()==childCloudlet.getCloudletId()){
                        child = childCloudlet;
                        break;
                    }
                }
                if (child == null){
                    break;
                }
                transferCosts.get(parent).put(child, calculateTransferCost(parent, child));
            }
        }

//        System.out.println("size of transfercosts: "+transferCosts.size());
//        for (Map.Entry<Cloudlet, Map<Cloudlet, Double>> entry : transferCosts.entrySet()){
//            System.out.println("cloudlet key: "+entry.getKey().getCloudletId());
//            Map<Cloudlet, Double> map = entry.getValue();
//            for (Map.Entry<Cloudlet, Double> entry1 : map.entrySet()){
//                System.out.println("c key: "+entry1.getKey().getCloudletId()+" cost val: "+entry1.getValue());
//            }
//        }
    }

    /**
     * Accounts the time in seconds necessary to transfer all files described
     * between parent and child
     *
     * @param parent
     * @param child
     * @return Transfer cost in seconds
     */
    private double calculateTransferCost(Cloudlet parent, Cloudlet child) {

        Job parentJob = (Job) parent;
        Job childJob = (Job) child;

        /**
         * In our experiments a Job object's task list will only ever have one Task
         * This is due to NOT making use of clustering in the Clustering Engine
         * Should that change we will need to modify our implementation of HEFT
         * */
        List<FileItem> parentFiles = parentJob.getFileList();
        List<FileItem> childFiles = childJob.getFileList();

        double acc = 0.0;

        for (FileItem parentFile : parentFiles) {
            if (parentFile.getType() != Parameters.FileType.OUTPUT) {
                continue;
            }

            for (FileItem childFile : childFiles) {
                if (childFile.getType() == Parameters.FileType.INPUT
                        && childFile.getName().equals(parentFile.getName())) {
                    acc += childFile.getSize();
                    break;
                }
            }
        }
        //file Size is in Bytes, acc in MB
        acc = acc / Consts.MILLION;
        // acc in MB, averageBandwidth in Mb/s
        return acc * 8 / averageBandwidth;
    }

    /**
     * Invokes calculateRank for each task to be scheduled
     */
    private void calculateRanks() {
        // only rank all the cloudlets
        for (Object cloudletObj : allCloudlets) {
            Cloudlet cloudlet = (Cloudlet) cloudletObj;
            calculateRank(cloudlet);
        }
    }

    /**
     * Populates rank.get(task) with the rank of task as defined in the HEFT
     * paper.
     *
     * @param cloudlet The task have the rank calculates
     * @return The rank
     */
    private double calculateRank(Cloudlet cloudlet) {
        if (rank.containsKey(cloudlet)) {
            return rank.get(cloudlet);
        }

        double averageComputationCost = 0.0;

        for (Double cost : computationCosts.get(cloudlet).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCosts.get(cloudlet).size();

        double max = 0.0;

        Job parent = (Job) cloudlet;
        for (Task taskChild : parent.getChildList()){
            for (Object cloudletChildObj: allCloudlets){
                Cloudlet child = (Cloudlet) cloudletChildObj;
                if (child.getCloudletId()==taskChild.getCloudletId()){
                    double childCost = transferCosts.get(cloudlet).get(child)
                            + calculateRank(child);
                    max = Math.max(max, childCost);
                }
            }

        }

        rank.put(cloudlet, averageComputationCost + max);

        return rank.get(cloudlet);
    }

    /**
     * Allocates all tasks to be scheduled in non-ascending order of schedule.
     */
    private void allocateCloudlets() {
        for (Cloudlet cloudlet : rank.keySet()) {
            cloudletRank.add(new CloudletRank(cloudlet, rank.get(cloudlet)));
        }

        // Sorting in descending order of rank
        Collections.sort(cloudletRank);
        for (CloudletRank rank : cloudletRank) {
            allocateCloudlet(rank.cloudlet);
        }

    }


        public void doesAddingChangeCost(double readyTime, double finishTime){

        if (readyTime < HOUR){
            if (finishTime <= HOUR){
                addingChangesCost = false;
            } else {
                addingChangesCost = true;
                System.out.println("COST GOES OVER 1 HOUR");
            }
        } else {
            double nearestHour = readyTime / HOUR;

            double nearestHourAfter = finishTime / HOUR;


            double nearestHourDiff = nearestHourAfter - nearestHour;


            if (nearestHourDiff <= 1){
                addingChangesCost = false;
            } else {
                addingChangesCost = true;
                System.out.println("NEAREST COST GOES OVER 1 HOUR");
            }
        }


    }

    private void allocateCloudlet(Cloudlet cloudlet) {
        CondorVM chosenVM = null;
        CondorVM Imin = null;
        CondorVM Imin2 = null;

        double finishTime;
        double finishTimeMin = Double.MAX_VALUE;
        double finishTimeMin2 = Double.MAX_VALUE;
        double earliestFinishTime = Double.MAX_VALUE;

        double minReadyTime = 0.0;
        double bestReadyTime = 0.0;

        Job job = (Job) cloudlet;
        List <Job> parents = job.getParentList();

        double currentTime = CloudSim.clock();


        minReadyTime = currentTime;

        for (Object vmObject : getVmList()) {
            CondorVM vm = (CondorVM) vmObject;

            if (vm.getState()==WorkflowSimTags.VM_STATUS_BUSY){ continue; }

            if (!parents.isEmpty()){
                for (Task parent : parents) {
                    Cloudlet p = parent;
                    double readyTime = 0.0;

                    if (earliestFinishTimes.containsKey(p) && transferCosts.containsKey(p)){
                        readyTime = earliestFinishTimes.get(p);

                        if (p.getVmId() != vm.getId()) {
                            readyTime += transferCosts.get(p).get(cloudlet);
                        }
                        minReadyTime = Math.max(minReadyTime, readyTime);
                    }

                }
            }

            finishTime = findFinishTime(cloudlet, vm, minReadyTime, false);
            doesAddingChangeCost(minReadyTime, finishTime);

            System.out.println("finish time to check of vm: "+vm.getId()+" is: "+finishTime);
            if (finishTime < finishTimeMin && vm.getState()==WorkflowSimTags.VM_STATUS_IDLE) {
                System.out.println("updating Imin to vm: "+vm.getId());
                finishTimeMin = finishTime;
                bestReadyTime = minReadyTime;
                Imin = vm;
                chosenVM = Imin;
                cloudlet.setVmId(chosenVM.getId());
            }

            if (!addingChangesCost && finishTime < finishTimeMin2
                    && vm.getState()==WorkflowSimTags.VM_STATUS_IDLE ){
                finishTimeMin2 = finishTime;
                bestReadyTime = minReadyTime;
                Imin2 = vm;
                chosenVM = Imin2;
                cloudlet.setVmId(chosenVM.getId());
            }
        }

        // no available vm in scheduler
        if (chosenVM == null ){
            //System.out.println("No available vms");
            return;
        }

        if (!Imin.equals(Imin2) ){
            if (Imin2 != null && Imin2.getState()==WorkflowSimTags.VM_STATUS_IDLE){
                // assign to this one
                chosenVM = Imin2;
                cloudlet.setVmId(chosenVM.getId());
                cloudlet.setScheduled(true);
                earliestFinishTime = finishTimeMin2;
            }
        }
        if (!cloudlet.isScheduled() && Imin.getState()==WorkflowSimTags.VM_STATUS_IDLE) {
            //asign to this one
            chosenVM = Imin;
            cloudlet.setVmId(chosenVM.getId());
            cloudlet.setScheduled(true);
            earliestFinishTime = finishTimeMin;
        }

        double fin = findFinishTime(cloudlet, chosenVM, bestReadyTime, true);
        earliestFinishTimes.put(cloudlet, earliestFinishTime);
        // need to allocate if a ready task
        if (getCloudletList().contains(cloudlet)){
            chosenVM.setState(WorkflowSimTags.VM_STATUS_BUSY);
            getScheduledList().add(cloudlet);
        }
    }


    private double findFinishTime(Cloudlet cloudlet, CondorVM vm, double readyTime,
                                  boolean occupySlot) {
        List<Event> sched = schedules.get(vm);
        double computationCost = computationCosts.get(cloudlet).get(vm);
        double start, finish;
        int pos;


        if (sched.isEmpty()) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost));
            }
            return readyTime + computationCost;
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return start + computationCost;
        }

        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }

            return start + computationCost;
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish));
        }
        return finish;
    }
}
