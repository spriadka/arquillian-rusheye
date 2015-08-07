package org.jboss.rusheye.arquillian.observer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.ArrayUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.rusheye.RushEye;
import org.jboss.rusheye.arquillian.configuration.RusheyeConfiguration;
import org.jboss.rusheye.arquillian.event.StartCrawlingEvent;
import org.jboss.rusheye.parser.listener.CompareListener;
import org.jboss.rusheye.result.collector.ResultCollectorImpl;
import org.jboss.rusheye.result.statistics.OverallStatistics;
import org.jboss.rusheye.result.storage.FileStorage;
import org.jboss.rusheye.result.writer.FileResultWriter;
import org.jboss.rusheye.retriever.mask.MaskFileRetriever;
import org.jboss.rusheye.retriever.pattern.PatternFileRetriever;
import org.jboss.rusheye.retriever.sample.FileSampleRetriever;
import org.jboss.rusheye.suite.MaskType;

import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jboss.arquillian.core.api.Event;
import org.jboss.rusheye.arquillian.event.CrawlingDoneEvent;
import org.jboss.rusheye.arquillian.event.StartCrawlMissingTestsEvent;
import org.jboss.rusheye.arquillian.event.StartReclawEvent;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:jhuska@redhat.com">Juraj Huska</a>
 */
public class CrawlObserver {

    @Inject
    private Instance<RusheyeConfiguration> rusheyeConfiguration;

    @Inject
    private Event<CrawlingDoneEvent> crawlingDoneEvent;
    
    @Inject
    private Event<StartCrawlMissingTestsEvent> startCrawlMissingEvent;

    private Document document;
    private Namespace ns;
    
    private List<String> newTests = new ArrayList<>();

    public void crawl(@Observes StartCrawlingEvent event) {
        document = DocumentHelper.createDocument();
        addDocumentRoot(event);
        writeDocument();
    }

    public void reCrawl(@Observes StartReclawEvent event) {
        if (event.getSuiteDescriptor() == null) {
            document = DocumentHelper.createDocument();
            addDocumentRoot(event);
            writeDocument();

        } else {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            org.w3c.dom.Document suiteXml = getDOMFromXMLFile(event.getSuiteDescriptor());
            try {
                int numberOfCrawledTests = Integer.parseInt(xPath.compile("count(/visual-suite/test)").evaluate(suiteXml));
                System.out.println("CRAWLED: " + numberOfCrawledTests);
                System.out.println(event.getSamplesFolder());
                int currentTests = getNumberOfCurrentTestsRecursive(new File(event.getSamplesFolder()));
                System.out.println("CURRENT: " + currentTests);
                if (numberOfCrawledTests < currentTests) {
                    SAXReader reader = new SAXReader();
                    document = reader.read(event.getSuiteDescriptor());
                    Element root = document.getRootElement();
                    addRemainingTest(root, event, getAlreadyCrawledTests(suiteXml));
                    writeMissingTestsToDocument();
                }
            } catch (XPathExpressionException | DocumentException ex) {
                Logger.getLogger(CrawlObserver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    private int getNumberOfCurrentTestsRecursive(File file){
        int result = 0;
        if (file.isFile()){
            result += 1;
        }
        else {
            for (File f : file.listFiles()){
                result += getNumberOfCurrentTestsRecursive(f);
            }
        }
        return result;
    }
    

    private List<String> getAlreadyCrawledTests(org.w3c.dom.Document doc) {
        List<String> result = new ArrayList<>();
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        try {
            NodeList alreadyCrawledTests = (NodeList) xPath.compile("/visual-suite/test/@name").evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < alreadyCrawledTests.getLength(); i++) {
                result.add(alreadyCrawledTests.item(i).getNodeValue());
            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(CrawlObserver.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    private void writeDocument() {
        OutputFormat format = OutputFormat.createPrettyPrint();
        OutputStream out = openOutputStream();
        XMLWriter writer = null;

        try {
            writer = new XMLWriter(out, format);
            writer.write(document);
            writer.flush();
            crawlingDoneEvent.fire(new CrawlingDoneEvent());
        } catch (IOException e) {
            PrintErrorUtils.printErrorMessage(e);
            System.exit(7);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                PrintErrorUtils.printErrorMessage(ex);
                System.exit(7);
            }
        }
    }
    
    private void writeMissingTestsToDocument() {
        OutputFormat format = OutputFormat.createPrettyPrint();
        OutputStream out = openOutputStream();
        XMLWriter writer = null;

        try {
            writer = new XMLWriter(out, format);
            writer.write(document);
            writer.flush();
            startCrawlMissingEvent.fire(new StartCrawlMissingTestsEvent(newTests));
        } catch (IOException e) {
            PrintErrorUtils.printErrorMessage(e);
            System.exit(7);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                PrintErrorUtils.printErrorMessage(ex);
                System.exit(7);
            }
        }
    }
    

    private OutputStream openOutputStream() {
        RusheyeConfiguration conf = rusheyeConfiguration.get();
        if (conf.getSuiteDescriptor() == null) {
            return System.out;
        }

        try {
            return new FileOutputStream(conf.getWorkingDirectory() + File.separator + conf.getSuiteDescriptor());
        } catch (IOException e) {
            PrintErrorUtils.printErrorMessage(e);
            System.exit(7);
            return null;
        }
    }

    private void addDocumentRoot(StartCrawlingEvent event) {
        ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        Element root = document.addElement(QName.get("visual-suite", ns));

        Namespace xsi = Namespace.get("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        QName schemaLocation = QName.get("schemaLocation", xsi);

        root.addNamespace("", ns.getURI());
        root.addNamespace(xsi.getPrefix(), xsi.getURI());
        root.addAttribute(schemaLocation, ns.getURI() + " " + RushEye.SCHEMA_LOCATION_VISUAL_SUITE);

        Element globalConfiguration = root.addElement(QName.get("global-configuration", ns));
        addSuiteListener(globalConfiguration);
        addRetrievers(globalConfiguration);
        addPerception(globalConfiguration);
        addMasksByType(rusheyeConfiguration.get().getMaskBase(), globalConfiguration);
        addTests(new File(event.getSamplesFolder()), root, event);
    }

    private void addDocumentRoot(StartReclawEvent event) {
        
        ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        Element root = document.addElement(QName.get("visual-suite", ns));

        Namespace xsi = Namespace.get("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        QName schemaLocation = QName.get("schemaLocation", xsi);

        root.addNamespace("", ns.getURI());
        root.addNamespace(xsi.getPrefix(), xsi.getURI());
        root.addAttribute(schemaLocation, ns.getURI() + " " + RushEye.SCHEMA_LOCATION_VISUAL_SUITE);

        Element globalConfiguration = root.addElement(QName.get("global-configuration", ns));
        addSuiteListener(globalConfiguration);
        addRetrievers(globalConfiguration);
        addPerception(globalConfiguration);
        addMasksByType(rusheyeConfiguration.get().getMaskBase(), globalConfiguration);
        addTests(new File(event.getSamplesFolder()), root, event);
    }

    private void addSuiteListener(Element globalConfiguration) {
        Element suiteListener = globalConfiguration.addElement(QName.get("listener", ns));
        suiteListener.addAttribute("type", CompareListener.class.getName());
        suiteListener.addElement(QName.get("result-collector", ns)).addText(ResultCollectorImpl.class.getName());
        suiteListener.addElement(QName.get("result-storage", ns)).addText(FileStorage.class.getName());
        suiteListener.addElement(QName.get("result-writer", ns)).addText(FileResultWriter.class.getName());
        suiteListener.addElement(QName.get("result-statistics", ns)).addText(OverallStatistics.class.getName());
    }

    private void addRetrievers(Element globalConfiguration) {
        globalConfiguration.addElement(QName.get("pattern-retriever", ns)).addAttribute("type", PatternFileRetriever.class.getName());
        globalConfiguration.addElement(QName.get("mask-retriever", ns)).addAttribute("type", MaskFileRetriever.class.getName());
        globalConfiguration.addElement(QName.get("sample-retriever", ns)).addAttribute("type",
                FileSampleRetriever.class.getName());
    }

    private void addPerception(Element base) {
        Element perception = base.addElement(QName.get("perception", ns));

        RusheyeConfiguration conf = rusheyeConfiguration.get();

        if (conf.getOnePixelTreshold() != null) {
            perception.addElement(QName.get("one-pixel-treshold", ns)).addText(String.valueOf(conf.getOnePixelTreshold()));
        }
        if (conf.getGlobalDifferenceTreshold() != null) {
            perception.addElement(QName.get("global-difference-treshold", ns))
                    .addText(String.valueOf(conf.getGlobalDifferenceTreshold()));
        }
        if (conf.getGlobalDifferenceAmount() != null) {
            perception.addElement(QName.get("global-difference-amount", ns)).addText(conf.getGlobalDifferenceAmount());
        }
    }

    private void addMasksByType(File dir, Element base) {
        for (MaskType maskType : MaskType.values()) {
            File maskDir = new File(dir, "masks-" + maskType.value());

            if (maskDir.exists() && maskDir.isDirectory() && maskDir.listFiles().length > 0) {
                addMasks(maskDir, base, maskType);
            }
        }
    }

    private void addMasks(File dir, Element base, MaskType maskType) {
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                String id = substringBeforeLast(file.getName(), ".");
                String source = getRelativePath(rusheyeConfiguration.get().getMaskBase(), file);
                String info = substringAfterLast(id, "--");
                String[] infoTokens = split(info, "-");

                Element mask = base.addElement(QName.get("mask", ns)).addAttribute("id", id)
                        .addAttribute("type", maskType.value()).addAttribute("source", source);

                for (String alignment : infoTokens) {
                    String attribute = ArrayUtils.contains(new String[]{"top", "bottom"}, alignment) ? "vertical-align"
                            : "horizontal-align";
                    mask.addAttribute(attribute, alignment);
                }
            }
        }
    }

    private void addTests(File dir, Element root, StartCrawlingEvent event) {
        if (dir.exists() && dir.isDirectory()) {
            tests:
            for (File testFile : dir.listFiles()) {
                for (MaskType mask : MaskType.values()) {
                    if (testFile.getName().equals("masks-" + mask.value())) {
                        continue tests;
                    }
                }
//                if (testFile.isDirectory() && testFile.listFiles().length > 0) {
//                    String Tname = testFile.getName();
//
//                    Element test = root.addElement(QName.get("test", ns));
//                    test.addAttribute("name", name);
//
//                    addPatterns(testFile, test);
//                    addMasksByType(testFile, test);
//                }
                if (testFile.isDirectory()) {
                    recursiveFindTestName(testFile, root, event);
                }

            }
        }
    }

    private void addTests(File dir, Element root, StartReclawEvent event) {
        if (dir.exists() && dir.isDirectory()) {
            tests:
            for (File testFile : dir.listFiles()) {
                for (MaskType mask : MaskType.values()) {
                    if (testFile.getName().equals("masks-" + mask.value())) {
                        continue tests;
                    }
                }
//                if (testFile.isDirectory() && testFile.listFiles().length > 0) {
//                    String Tname = testFile.getName();
//
//                    Element test = root.addElement(QName.get("test", ns));
//                    test.addAttribute("name", name);
//
//                    addPatterns(testFile, test);
//                    addMasksByType(testFile, test);
//                }
                if (testFile.isDirectory()) {
                    recursiveFindTestName(testFile, root, event);
                }

            }
        }
    }

    private void addRemainingTest(Element root, StartReclawEvent event, List<String> alreadyCrawled) {
        ns = Namespace.get(RushEye.NAMESPACE_VISUAL_SUITE);
        File dir = new File(event.getSamplesFolder());
        if (dir.exists() && dir.isDirectory()) {
            tests:
            for (File testFile : dir.listFiles()) {
                for (MaskType mask : MaskType.values()) {
                    if (testFile.getName().equals("masks-" + mask.value())) {
                        continue tests;
                    }
                }
//                if (testFile.isDirectory() && testFile.listFiles().length > 0) {
//                    String Tname = testFile.getName();
//
//                    Element test = root.addElement(QName.get("test", ns));
//                    test.addAttribute("name", name);
//
//                    addPatterns(testFile, test);
//                    addMasksByType(testFile, test);
//                }
                if (testFile.isDirectory()) {
                    recursiveFindRemainingTestName(testFile, root, event, alreadyCrawled);
                }

            }
        }

    }

    private void recursiveFindRemainingTestName(File dir, Element root, StartReclawEvent event, List<String> alreadyCrawled) {
        for (File testFile : dir.listFiles()) {

            if (testFile.isFile()) {

                String patterName = substringBeforeLast(testFile.getName(), ".");
                String testName = testFile.getParentFile().getParentFile().getName()
                        + "." + testFile.getParentFile().getName() + "." + patterName;

                if (!alreadyCrawled.contains(testName)) {
                    newTests.add(testName);
                    Element test = root.addElement(QName.get("test", ns));

                    test.addAttribute("name", testName);

                    String source = getRelativePath(new File(event.getSamplesFolder()), testFile);

                    Element pattern = test.addElement(QName.get("pattern", ns));
                    pattern.addAttribute("name", testName);
                    pattern.addAttribute("source", source);
                }
            } else if (testFile.isDirectory()) {
                recursiveFindRemainingTestName(testFile, root, event, alreadyCrawled);
            }

        }

    }

    private void recursiveFindTestName(File dir, Element root, StartReclawEvent event) {
        for (File testFile : dir.listFiles()) {

            if (testFile.isFile()) {

                String patterName = substringBeforeLast(testFile.getName(), ".");
                String testName = testFile.getParentFile().getParentFile().getName()
                        + "." + testFile.getParentFile().getName() + "." + patterName;

                Element test = root.addElement(QName.get("test", ns));

                test.addAttribute("name", testName);

                String source = getRelativePath(new File(event.getSamplesFolder()), testFile);

                Element pattern = test.addElement(QName.get("pattern", ns));
                pattern.addAttribute("name", testName);
                pattern.addAttribute("source", source);
            } else if (testFile.isDirectory()) {
                recursiveFindTestName(testFile, root, event);
            }

        }

    }

    /**
     * Adds recursively all screenshots. It presumes that screenshots are under
     * following directory structure:
     * [PatternBase]/[TestClassName]/[testMethodName]/nameOfScreenshot.[extension]
     *
     * @param dir
     * @param root
     */
    private void recursiveFindTestName(File dir, Element root, StartCrawlingEvent event) {
        for (File testFile : dir.listFiles()) {

            if (testFile.isFile()) {

                String patterName = substringBeforeLast(testFile.getName(), ".");
                String testName = testFile.getParentFile().getParentFile().getName()
                        + "." + testFile.getParentFile().getName() + "." + patterName;

                Element test = root.addElement(QName.get("test", ns));

                test.addAttribute("name", testName);

                String source = getRelativePath(new File(event.getSamplesFolder()), testFile);

                Element pattern = test.addElement(QName.get("pattern", ns));
                pattern.addAttribute("name", testName);
                pattern.addAttribute("source", source);
            } else if (testFile.isDirectory()) {
                recursiveFindTestName(testFile, root, event);
            }

        }

    }

    private void addPatterns(File dir, Element test, StartCrawlingEvent event) {
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    String name = substringBeforeLast(file.getName(), ".");
                    String source = getRelativePath(new File(event.getSamplesFolder()), file);

                    Element pattern = test.addElement(QName.get("pattern", ns));
                    pattern.addAttribute("name", name);
                    pattern.addAttribute("source", source);
                }
            }
        }
    }

    private String getRelativePath(File base, File file) {
        return substringAfter(file.getPath(), base.getPath()).replaceFirst("^/", "");
    }

    private org.w3c.dom.Document getDOMFromXMLFile(File xmlFile) {
        org.w3c.dom.Document result = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            result = db.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(CrawlObserver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
}
