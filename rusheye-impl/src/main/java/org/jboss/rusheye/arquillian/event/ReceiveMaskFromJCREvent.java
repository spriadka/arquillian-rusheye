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
public class ReceiveMaskFromJCREvent {
    private File maskFile;
    
    public ReceiveMaskFromJCREvent(File maskFile){
        this.maskFile = maskFile;
    }

    /**
     * @return the maskFile
     */
    public File getMaskFile() {
        return maskFile;
    }

    /**
     * @param maskFile the maskFile to set
     */
    public void setMaskFile(File maskFile) {
        this.maskFile = maskFile;
    }
}
