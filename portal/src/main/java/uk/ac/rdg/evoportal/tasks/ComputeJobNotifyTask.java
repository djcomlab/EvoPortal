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

import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import uk.ac.rdg.evoportal.GlobalConstants;
import uk.ac.rdg.evoportal.beans.ComputeJob;
import uk.ac.rdg.evoportal.beans.PortalUser;
import uk.ac.rdg.util.HibernateUtil;

/**
 *
 * @author david
 */
public class ComputeJobNotifyTask extends TimerTask {

    private int jobID;
    private ComputeJob computeJob = null;
    private PortalUser user = null;
    private Session s;
    private Transaction tx;
    private transient Logger LOG = Logger.getLogger(ComputeJobNotifyTask.class.getName());

    public ComputeJobNotifyTask(int jobID) {
        this.jobID = jobID;
        s = HibernateUtil.getSessionFactory().openSession();
        tx = s.beginTransaction();
        Object result = s.createQuery("from ComputeJob cj where cj.jobID=" + jobID).uniqueResult();
        tx.commit();
        if (tx.wasCommitted()) {
            computeJob = (ComputeJob)result;
            tx = s.beginTransaction();
            Query q = s.createQuery("from PortalUser u where u.username='" + computeJob.getOwner() + "'");
            tx.commit();
            if (tx.wasCommitted()) {
                result = q.uniqueResult();
                if (result!=null && result instanceof PortalUser) {
                    user = (PortalUser)result;
                }
            }
        }
    }

    @Override
    public void run() {
        LOG.fine("ComputeJobNotifyTask started");
        if (!computeJob.isNotified()) {
            Properties p = System.getProperties();
            javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(p);
            MimeMessage m = new MimeMessage(mailSession);
            try {
                m.setFrom(new InternetAddress(GlobalConstants.getProperty("email.returnaddress")));
                m.addRecipients(Message.RecipientType.TO,
                                    InternetAddress.parse(user.getEmailAddress(), false));
                m.addRecipients(Message.RecipientType.CC, InternetAddress.parse(GlobalConstants.getProperty("email.returnaddress"), false));
                String subject = GlobalConstants.getProperty("email.computejob.subject");
                subject = subject.replace("{job_id}", Integer.toString(jobID));
                m.setSubject(subject);
                String content = GlobalConstants.getProperty("email.computejob.content");
                content = content.replace("{job_id}", Integer.toString(jobID));
                m.setText(content);
                m.setSentDate(new Date());
                Transport tr = mailSession.getTransport("smtp");
                tr.connect(GlobalConstants.getProperty("email.smtpserver"), GlobalConstants.getProperty("email.username"), GlobalConstants.getProperty("email.password"));
                m.saveChanges();      // don't forget this
                tr.sendMessage(m, m.getAllRecipients());
                tr.close();
                computeJob.setNotified(true);
                tx = s.beginTransaction();
                s.update(computeJob);
                tx.commit();
                if (!tx.wasCommitted()) {
                    // TODO implement rollback?
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            } finally {
                s.close();
            }
            LOG.fine("ComputeJobNotifyTask finished");
        }
    }



}
