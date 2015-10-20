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
public class RequestMaskFromJCREvent {
    
    private String maskUrl;
    
    public RequestMaskFromJCREvent(String maskUrl){
        this.maskUrl = maskUrl;
    }

    /**
     * @return the maskUrl
     */
    public String getMaskUrl() {
        return maskUrl;
    }

    /**
     * @param maskUrl the maskUrl to set
     */
    public void setMaskUrl(String maskUrl) {
        this.maskUrl = maskUrl;
    }
    
}
