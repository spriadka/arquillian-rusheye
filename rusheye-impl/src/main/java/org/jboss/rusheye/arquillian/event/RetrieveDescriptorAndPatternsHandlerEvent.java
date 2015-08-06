/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.rusheye.arquillian.event;

/**
 *
 * @author spriadka
 */
public class RetrieveDescriptorAndPatternsHandlerEvent {
    
    private String descriptorAndPatternsHandler;
    
    public RetrieveDescriptorAndPatternsHandlerEvent(String descriptorAndPatternsHandler){
        this.descriptorAndPatternsHandler = descriptorAndPatternsHandler;
        
    }

    /**
     * @return the descriptorAndPatternsHandler
     */
    public String getDescriptorAndPatternsHandler() {
        return descriptorAndPatternsHandler;
    }

    /**
     * @param descriptorAndPatternsHandler the descriptorAndPatternsHandler to set
     */
    public void setDescriptorAndPatternsHandler(String descriptorAndPatternsHandler) {
        this.descriptorAndPatternsHandler = descriptorAndPatternsHandler;
    }
}
