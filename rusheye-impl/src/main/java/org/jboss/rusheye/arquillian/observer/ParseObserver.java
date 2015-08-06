package org.jboss.rusheye.arquillian.observer;

import com.beust.jcommander.IStringConverter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.rusheye.arquillian.configuration.RusheyeConfiguration;
import org.jboss.rusheye.arquillian.event.InsertDescriptorAndPatternsHandlerEvent;
import org.jboss.rusheye.arquillian.event.ParsingDoneEvent;
import org.jboss.rusheye.arquillian.event.StartParsingEvent;
import org.jboss.rusheye.internal.Instantiator;
import org.jboss.rusheye.listener.SuiteListener;
import org.jboss.rusheye.parser.Parser;
import org.jboss.rusheye.suite.Properties;
import org.jboss.rusheye.arquillian.event.StartCrawlingEvent;
import org.jboss.rusheye.arquillian.event.StartReclawEvent;

/**
 *
 * @author jhuska
 */
public class ParseObserver {

    @Inject
    private Instance<RusheyeConfiguration> rusheyeConfiguration;
    
    @Inject
    private Instance<ReporterConfiguration> reporterConfiguration;

    @Inject
    private Event<ParsingDoneEvent> parsingDoneEvent;
    
    @Inject
    private Event<StartCrawlingEvent> startCrawlingEvent;
    
    @Inject
    private Event<StartReclawEvent> startReclawEvent;
    
    @Inject
    private Event<InsertDescriptorAndPatternsHandlerEvent> iEvent;
    
    

    private Properties properties = new Properties();
    private final SuiteListenerConverter suiteListenerConverter = new SuiteListenerConverter();

    public void parse(@Observes StartParsingEvent event) {
        initialize(event);

        Parser parser = new Parser();
        parser.setProperties(properties);

        if (rusheyeConfiguration.get().getSuiteListener() != null) {
            SuiteListener converted = suiteListenerConverter.convert(rusheyeConfiguration.get().getSuiteListener());
            parser.registerListener(converted);
        }
        //SUITE DESCRIPTOR EXISTS
        
        File suiteDescriptor = new File(event.getPatternAndDescriptorFolder()
                + File.separator
                + rusheyeConfiguration.get().getSuiteDescriptor());
        
        parser.parseFile(suiteDescriptor, event.getFailedTestsCollection(), event.getVisuallyUnstableCollection());
        parsingDoneEvent.fire(new ParsingDoneEvent());
    }

    public void initialize(StartParsingEvent event) {
        RusheyeConfiguration conf = rusheyeConfiguration.get();
        ReporterConfiguration rconf = reporterConfiguration.get();
        initializeCrawlIfNeeded(event);
        properties.setProperty("result-output-file", conf.getWorkingDirectory() + File.separator + conf.getResultOutputFile());
        properties.setProperty("samples-directory", event.getSamplesFolder());
        properties.setProperty("patterns-directory", event.getPatternAndDescriptorFolder()
                + getRelativePatternsDir(event.getSamplesFolder()));
        properties.setProperty("file-storage-directory", conf.getWorkingDirectory() + File.separator + conf.getDiffsDir());
        properties.setProperty("masks-directory", conf.getMaskBase());
        properties.setProperty("report-file", rconf.getFile());
    }
    
    private void initializeCrawlIfNeeded(StartParsingEvent event){
        File suite = new File(event.getPatternAndDescriptorFolder() + File.separator + rusheyeConfiguration.get().getSuiteDescriptor());
        if (suite.isFile()){
            System.out.println("FOUND DESCRIPTOR");
            System.out.println(suite.getAbsolutePath());
            File suiteDescriptor = new File(event.getPatternAndDescriptorFolder()
                + File.separator
                + rusheyeConfiguration.get().getSuiteDescriptor());
            startReclawEvent.fire(new StartReclawEvent(suiteDescriptor, event.getSamplesFolder(), event.getFailedTestsCollection(), event.getVisuallyUnstableCollection()));
            
        }
        else {
            System.out.println("CREATED DESCRIPTOR");
            startReclawEvent.fire(new StartReclawEvent(null, event.getSamplesFolder(), event.getFailedTestsCollection(), event.getVisuallyUnstableCollection()));
            sendRequestForRetrievalDescriptionAndPatternsHandler();
            
        }
        
        
    }
    
    private void sendRequestForRetrievalDescriptionAndPatternsHandler(){
        iEvent.fire(new InsertDescriptorAndPatternsHandlerEvent());
    }

    private String getRelativePatternsDir(String samplesDir) {
        String absoluteWorking = rusheyeConfiguration.get().getWorkingDirectory().getAbsolutePath();
        return samplesDir.replace(absoluteWorking, "");
    }

    public class SuiteListenerConverter implements IStringConverter<SuiteListener> {

        @Override
        public SuiteListener convert(String type) {
            return new Instantiator<SuiteListener>().getInstance(type);
        }
    }

}
