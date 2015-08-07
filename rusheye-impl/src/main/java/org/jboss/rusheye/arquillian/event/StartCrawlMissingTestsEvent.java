/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.rusheye.arquillian.event;

import java.util.List;

/**
 *
 * @author spriadka
 */
public class StartCrawlMissingTestsEvent {
    
    private List<String> missingTests;
    
    public StartCrawlMissingTestsEvent(List<String> missingTests){
        this.missingTests = missingTests;
    }

    /**
     * @return the missingTests
     */
    public List<String> getMissingTests() {
        return missingTests;
    }

    /**
     * @param missingTests the missingTests to set
     */
    public void setMissingTests(List<String> missingTests) {
        this.missingTests = missingTests;
    }
    
}
