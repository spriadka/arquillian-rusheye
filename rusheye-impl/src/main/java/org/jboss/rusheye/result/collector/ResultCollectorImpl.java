/**
 * JBoss, Home of Professional Open Source Copyright ${year}, Red Hat, Inc. and
 * individual contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.jboss.rusheye.result.collector;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import org.jboss.rusheye.internal.Instantiator;
import org.jboss.rusheye.result.ResultCollectorAdapter;
import org.jboss.rusheye.result.ResultEvaluator;
import org.jboss.rusheye.result.ResultStatistics;
import org.jboss.rusheye.result.ResultStorage;
import org.jboss.rusheye.result.writer.ResultWriter;
import org.jboss.rusheye.suite.ComparisonResult;
import org.jboss.rusheye.suite.Pattern;
import org.jboss.rusheye.suite.Properties;
import org.jboss.rusheye.suite.ResultConclusion;
import org.jboss.rusheye.suite.Test;
import org.jboss.rusheye.suite.VisualSuite;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:lfryc@redhat.com">Lukas Fryc</a>
 * @version $Revision$
 */
public class ResultCollectorImpl extends ResultCollectorAdapter {

    Properties properties;
    ResultStorage storage;
    ResultEvaluator evaluator;
    ResultWriter writer;
    ResultStatistics statistics;

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void onConfigurationReady(VisualSuite visualSuite) {
        String storageClass = (String) properties.getProperty("result-storage");
        storage = new Instantiator<ResultStorage>().getInstance(storageClass);
        storage.setProperties(properties);

        String writerClass = (String) properties.getProperty("result-writer");
        writer = new Instantiator<ResultWriter>().getInstance(writerClass);
        writer.setProperties(properties);

        String statisticsClass = (String) properties.getProperty("result-statistics");
        statistics = new Instantiator<ResultStatistics>().getInstance(statisticsClass);
        statistics.setProperties(properties);

        evaluator = new ResultEvaluator();
    }

    @Override
    public void onPatternCompleted(Test test, Pattern pattern, ComparisonResult comparisonResult) {

        ResultConclusion conclusion = evaluator.evaluate(test.getPerception(), comparisonResult);
        pattern.setConclusion(conclusion);

        if (comparisonResult == null) {
            pattern.setErrorOutput(getCause(pattern));
        }

        if (conclusion == ResultConclusion.DIFFER || conclusion == ResultConclusion.PERCEPTUALLY_SAME) {
            String location = storage.store(test, pattern, comparisonResult.getDiffImage());
            pattern.setOutput(location);
        }

        if (comparisonResult != null && comparisonResult.getDiffImage() != null) {
            comparisonResult.getDiffImage().flush();
        }

        pattern.setComparisonResult(comparisonResult);

        statistics.onPatternCompleted(pattern);
    }

    @Override
    public void onTestCompleted(Test test) {
        writer.write(test);
        statistics.onTestCompleted(test);
    }

    @Override
    public void onSuiteCompleted(VisualSuite visualSuite) {
        writer.close();
        statistics.onSuiteCompleted();
        storage.end();
    }

    private String getCause(Pattern pattern) {
        File reportFile = properties.getProperty("report-file", File.class);
        String result = null;
        String patternNameBeforeFailed = pattern.getName().substring(0, pattern.getName().lastIndexOf("."));
        String className = patternNameBeforeFailed.substring(0, patternNameBeforeFailed.lastIndexOf("."));
        String methodName = pattern.getName().substring(className.length() + 1, pattern.getName().lastIndexOf("."));
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            Document reportXml;
            db = dbf.newDocumentBuilder();
            reportXml = db.parse(reportFile);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            result = xPath.compile("/report/suite/class[@name=" + "'" + className + "'" + "]/method[@name=" + "'" + methodName + "'" + "]/exception/text()").evaluate(reportXml);
            return result;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ResultCollectorImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(ResultCollectorImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ResultCollectorImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(ResultCollectorImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;

    }
}
