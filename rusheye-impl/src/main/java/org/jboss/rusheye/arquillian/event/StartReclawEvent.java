/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.rusheye.arquillian.event;

import java.io.File;

/**
 *
 * @author spriadka
 */
public class StartReclawEvent {
    
    private File suiteDescriptor;

    /**
     * @return the suiteDescriptor
     */
    public File getSuiteDescriptor() {
        return suiteDescriptor;
    }

    /**
     * @param suiteDescriptor the suiteDescriptor to set
     */
    public void setSuiteDescriptor(File suiteDescriptor) {
        this.suiteDescriptor = suiteDescriptor;
    }
    
    private String samplesFolder;
    
    private FailedTestsCollection failedTestsCollection;
    
    private VisuallyUnstableTestsCollection visuallyUnstableCollection;

    public FailedTestsCollection getFailedTestsCollection() {
        return failedTestsCollection;
    }

    public void setFailedTestsCollection(FailedTestsCollection failedTestsCollection) {
        this.failedTestsCollection = failedTestsCollection;
    }

    public String getSamplesFolder() {
        return samplesFolder;
    }

    public void setSamplesFolder(String samplesFolder) {
        this.samplesFolder = samplesFolder;
    }

    public VisuallyUnstableTestsCollection getVisuallyUnstableCollection() {
        return visuallyUnstableCollection;
    }

    public void setVisuallyUnstableCollection(VisuallyUnstableTestsCollection visuallyUnstableCollection) {
        this.visuallyUnstableCollection = visuallyUnstableCollection;
    }
    
    public StartReclawEvent (File suiteDescriptor, String samplesFolder, FailedTestsCollection failedTestsCollection, VisuallyUnstableTestsCollection visuallyUnstableCollection ){
        this.suiteDescriptor = suiteDescriptor;
        this.samplesFolder = samplesFolder;
        this.failedTestsCollection = failedTestsCollection;
        this.visuallyUnstableCollection = visuallyUnstableCollection;
    }
    
}
