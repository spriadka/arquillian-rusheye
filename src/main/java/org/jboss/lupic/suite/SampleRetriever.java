package org.jboss.lupic.suite;

import java.awt.image.BufferedImage;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.Validate;
import org.jboss.lupic.retriever.RetrieverException;
import org.jboss.lupic.suite.utils.Instantiator;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SampleRetriever")
public class SampleRetriever extends TypeProperties implements org.jboss.lupic.retriever.sample.SampleRetriever {

    @XmlTransient
    org.jboss.lupic.retriever.sample.SampleRetriever sampleRetriever;

    @Override
    public void setType(String value) {
        super.setType(value);
        Validate.notNull(type);
        sampleRetriever = new Instantiator<org.jboss.lupic.retriever.sample.SampleRetriever>().getInstance(type);
    }

    @Override
    public BufferedImage retrieve(String source, Properties localProperties) throws RetrieverException {
        return sampleRetriever.retrieve(source, localProperties);
    }

    @Override
    public Properties mergeProperties(Properties localProperties) {
        return sampleRetriever.mergeProperties(localProperties);
    }

    @Override
    public void setGlobalProperties(Properties properties) {
        sampleRetriever.setGlobalProperties(properties);
    }

    @Override
    public Set<String> getNewSources() {
        return null;
    }
}