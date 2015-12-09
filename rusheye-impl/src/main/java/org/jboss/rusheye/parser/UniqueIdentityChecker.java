/**
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.rusheye.parser;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Unmarshaller;

import org.jboss.rusheye.exception.ConfigurationValidationException;
import org.jboss.rusheye.suite.Mask;
import org.jboss.rusheye.suite.Pattern;
import org.jboss.rusheye.suite.Test;

import org.jboss.logging.Logger;
import org.jboss.rusheye.suite.GlobalConfiguration;

public class UniqueIdentityChecker extends Unmarshaller.Listener {
    private Context context;
    private List<String> maskIdsInConfiguration = new ArrayList<>();
    private Logger LOGGER = Logger.getLogger(UniqueIdentityChecker.class);

    UniqueIdentityChecker(Context context) {
        this.context = context;
    }

    @Override
    public void afterUnmarshal(Object target, Object parent) {
        if (target instanceof Test) {
            Test test = (Test) target;
            if (context.getTestNames().contains(test.getName())) {
                throw new ConfigurationValidationException("test's \"name\" attribute have to be unique across suite");
            }
            context.getTestNames().add(test.getName());
        }
        if (target instanceof Pattern) {
            Pattern pattern = (Pattern) target;
            if (context.getPatternNames().contains(pattern.getName())) {
                throw new ConfigurationValidationException(
                    "pattern's \"name\" attribute have to be unique across suite");
            }
            context.getPatternNames().add(pattern.getName());
        }
        /*if (target instanceof Mask) {
            LOGGER.info("UNMARSHALLING");
            addMaskIds();
            Mask mask = (Mask) target;
            if (!maskIdsInConfiguration.contains(mask.getId())) {
                throw new ConfigurationValidationException("mask's \"id\" attribute is not in global configuration");
            }
            //context.getMaskIds().add(mask.getId());
        }*/
    }
    
    private void addMaskIds(){
        if (context.getCurrentConfiguration() instanceof GlobalConfiguration){
            if (!context.getCurrentConfiguration().getMasks().isEmpty()){
                LOGGER.info(context.getMaskIds().toArray().toString());
                maskIdsInConfiguration.addAll(context.getMaskIds());
            }
        }
    }
}
