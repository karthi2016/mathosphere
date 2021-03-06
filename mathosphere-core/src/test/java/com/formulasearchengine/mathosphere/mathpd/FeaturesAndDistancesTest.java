package com.formulasearchengine.mathosphere.mathpd;

import com.formulasearchengine.mathosphere.TestUtils;
import com.formulasearchengine.mathosphere.mathpd.contracts.TextExtractorMapper;
import com.formulasearchengine.mathosphere.mathpd.pojos.ArxivDocument;
import com.formulasearchengine.mathosphere.mathpd.pojos.ExtractedMathPDDocument;
import com.google.common.base.Throwables;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by Felix Hamborg <felixhamborg@gmail.com> on 09.12.2016.
 * Unit test cases for features and distances.
 */
public class FeaturesAndDistancesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextExtractorMapper.class);

    private static String decodePath(String urlEncodedPath) {
        try {
            return URLDecoder.decode(urlEncodedPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    private static ExtractedMathPDDocument testResourceToExtractedMathPDDocument(String path) throws IOException, TransformerException, XPathExpressionException, ParserConfigurationException {
        final ArxivDocument arxivDocument = TextExtractorMapper.arxivTextToDocument(TestUtils.getFileContents(path));
        return TextExtractorMapper.convertArxivToExtractedMathPDDocument(arxivDocument);
    }

    private String resourcePath(String resourceName) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(resourceName);
        return decodePath(resource.getFile());
    }

    @Test
    public void testDistanceSameFile() throws Exception {
        final String resourceSimple = "com/formulasearchengine/mathosphere/mathpd/simple.xhtml";
        ExtractedMathPDDocument doc1 = testResourceToExtractedMathPDDocument(resourceSimple);
        ExtractedMathPDDocument doc2 = testResourceToExtractedMathPDDocument(resourceSimple);

        final double distanceAbsoluteAllFeatures = Distances.distanceAbsoluteAllFeatures(doc1, doc2);
        LOGGER.debug("absolute distance = " + distanceAbsoluteAllFeatures);

        final double distanceRelativeAllFeatures = Distances.distanceRelativeAllFeatures(doc1, doc2);
        LOGGER.debug("relative distance = " + distanceRelativeAllFeatures);

        final double distanceEarthMoverAllFeatures = Distances.computeEarthMoverAbsoluteDistance(doc1.getHistogramCi(), doc2.getHistogramCi());
        LOGGER.debug("earth mover distance = " + distanceEarthMoverAllFeatures);

        assertTrue(distanceAbsoluteAllFeatures
                + distanceRelativeAllFeatures
                + distanceEarthMoverAllFeatures == 0.0);
    }

    @Test
    public void testEarthMoverDistance() throws ParserConfigurationException, TransformerException, XPathExpressionException, IOException {
        final String resourceSimple = "com/formulasearchengine/mathosphere/mathpd/simple.xhtml";
        ExtractedMathPDDocument doc1 = testResourceToExtractedMathPDDocument(resourceSimple);
        ExtractedMathPDDocument doc2 = testResourceToExtractedMathPDDocument(resourceSimple);

        final double distanceAbsoluteEarthMoverCiSame = Distances.computeEarthMoverAbsoluteDistance(doc1.getHistogramCi(), doc2.getHistogramCi());
        LOGGER.debug("earthmover absolute distance = " + distanceAbsoluteEarthMoverCiSame);

        assertTrue(distanceAbsoluteEarthMoverCiSame == 0.0);
    }

    @Test
    public void testHistogramExtractionAndAbsoluteDistance() throws ParserConfigurationException, TransformerException, XPathExpressionException, IOException {
        final String resourceSimple = "com/formulasearchengine/mathosphere/mathpd/simple.xhtml";
        ExtractedMathPDDocument document = testResourceToExtractedMathPDDocument(resourceSimple);

        // bound variables
        assertTrue(document.getHistogramBvar().size() == 0.0);

        // identifiers
        HashMap<String, Integer> histogramCi = new HashMap<>();
        histogramCi.put("\uD835\uDC4E", 1);
        histogramCi.put("\uD835\uDC4F", 1);
        histogramCi.put("\uD835\uDC50", 1);
        histogramCi.put("\uD835\uDC51", 1);
        assertTrue(Distances.computeAbsoluteDistance(document.getHistogramCi(), histogramCi) == 0.0);

        // numbers
        HashMap<String, Integer> histogramCn = new HashMap<>();
        histogramCn.put("1", 1);
        histogramCn.put("2", 1);
        histogramCn.put("3", 1);
        histogramCn.put("4", 1);
        assertTrue(Distances.computeAbsoluteDistance(document.getHistogramCn(), histogramCn) == 0.0);

        // symbols
        HashMap<String, Integer> histogramCsymbol = new HashMap<>();
        histogramCsymbol.put("minus", 1);
        histogramCsymbol.put("plus", 3);
        histogramCsymbol.put("times", 1);
        histogramCsymbol.put("divide", 1);
        histogramCsymbol.put("eq", 2);
        histogramCsymbol.put("list", 1);
        assertTrue(Distances.computeAbsoluteDistance(document.getHistogramCsymbol(), histogramCsymbol) == 0.0);

    }

    @Test
    public void testEarthMoverDistanceBasic() {
        HashMap<String, Integer> histogramCi1 = new HashMap<>();
        histogramCi1.put("a", 3);
        histogramCi1.put("b", 1);
        histogramCi1.put("c", 1);

        HashMap<String, Integer> histogramCi2 = new HashMap<>();
        histogramCi2.put("a", 1);
        histogramCi2.put("b", 1);
        histogramCi2.put("c", 3);

        final double distance = Distances.computeEarthMoverAbsoluteDistance(histogramCi1, histogramCi2);
        LOGGER.debug("earth mover distance = " + distance);
    }

}