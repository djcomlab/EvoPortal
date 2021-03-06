/*
 *  Copyright 2009 David Johnson, School of Biological Sciences,
 *  University of Reading, UK.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package uk.ac.rdg.evoportal.tasks;

import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;
import uk.ac.rdg.evoportal.beans.ScaleTest;
import uk.ac.rdg.evoportal.beans.ScaleTestComputeJob;
import uk.ac.rdg.evoportal.data.ScaleTestsDataProvider;

/**
 *
 * @author david
 */
public class ScaleTestStopTask extends TimerTask {

    private long testID;
    private transient Logger LOG = Logger.getLogger(ScaleTestStopTask.class.getName());

    public ScaleTestStopTask(long testID) {
        this.testID = testID;
    }

    @Override
    public void run() {
        LOG.fine("ScaleTestSubmitTask starting");
        ScaleTest scaleTest = ScaleTestsDataProvider.get(testID);
        List<ScaleTestComputeJob> scaleTestComputeJobs = scaleTest.getScaleTestComputeJobs();
        for(Iterator<ScaleTestComputeJob> i = scaleTestComputeJobs.iterator();i.hasNext();) {
            ScaleTestComputeJob scaleTestComputeJob = i.next();
            int jobID = scaleTestComputeJob.getJobID();
            ComputeJobStopTask computeJobStopTask = new ComputeJobStopTask(jobID);
            computeJobStopTask.run();
        }
        LOG.fine("ScaleTestSubmitTask finished");
    }

}
