/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.workflowsim.scheduling;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.CondorVM;
import org.workflowsim.WorkflowSimTags;

/**
 * MCT algorithm
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class MCTSchedulingAlgorithm extends BaseSchedulingAlgorithm {

    public MCTSchedulingAlgorithm() {
        super();
    }

    @Override
    public void run() {


        int size = getCloudletList().size();

        double currentTime = CloudSim.clock();

        for(Object cloudObj : getCloudletList()){
            Cloudlet cloudlet = (Cloudlet) cloudObj;
            //System.out.print("cloudlet: "+cloudlet.getCloudletId());
            double allocationTime = currentTime;

            if (cloudlet.getAllocationTime() == null){
                cloudlet.setAllocationTime(allocationTime);
                System.out.println("Set allocation time of cloudlet: "+cloudlet.getCloudletId()+" to: "+cloudlet.getAllocationTime());
            } else {
                System.out.println("Cloudlet: "+cloudlet.getCloudletId()+" was already allocated at: "+cloudlet.getAllocationTime());
            }
        }

        for (int i = 0; i < size; i++) {
            Cloudlet cloudlet = (Cloudlet) getCloudletList().get(i);
            int vmSize = getVmList().size();
            CondorVM firstIdleVm = null;

            for (int j = 0; j < vmSize; j++) {
                CondorVM vm = (CondorVM) getVmList().get(j);
                if (vm.getState() == WorkflowSimTags.VM_STATUS_IDLE) {
                    firstIdleVm = vm;
                    break;
                }
            }
            if (firstIdleVm == null) {
                break;
            }

            for (int j = 0; j < vmSize; j++) {
                CondorVM vm = (CondorVM) getVmList().get(j);
                if ((vm.getState() == WorkflowSimTags.VM_STATUS_IDLE)
                        && (vm.getCurrentRequestedTotalMips() > firstIdleVm.getCurrentRequestedTotalMips())) {
                    firstIdleVm = vm;
                }
            }
            firstIdleVm.setState(WorkflowSimTags.VM_STATUS_BUSY);
            cloudlet.setVmId(firstIdleVm.getId());
            getScheduledList().add(cloudlet);
        }
    }
}
