package de.tuberlin.dima.schubotz.wiki.preprocess;

import de.tuberlin.dima.schubotz.common.utils.ExtractHelper;
import de.tuberlin.dima.schubotz.wiki.types.WikiTuple;
import eu.stratosphere.api.java.functions.FlatMapFunction;
import eu.stratosphere.util.Collector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

@SuppressWarnings("serial")
/**
 * Map wiki text to {@link de.tuberlin.dima.schubotz.wiki.types.WikiTuple}
 */
public class ProcessWikiMapper extends FlatMapFunction<String, WikiTuple> {
	/**
	 * See {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#STR_SPLIT}
	 */
	private final String STR_SPLIT;
    private final String namespace = "http://www.w3.org/1998/Math/MathML";
    private final String namespace_tag = "xmlns:m";
	private final Log LOG = LogFactory.getLog(ProcessWikiMapper.class);
	
	/**
	 * @param {@link de.tuberlin.dima.schubotz.wiki.WikiProgram#STR_SPLIT} passed in to ensure serializability
	 */
	@SuppressWarnings("hiding")
	public ProcessWikiMapper(String STR_SPLIT) {
		this.STR_SPLIT = STR_SPLIT;
	}

	/**
	 * Takes in wiki string, parses wikiID and latex
	 */
	@Override
	public void flatMap (String in, Collector<WikiTuple> out) {
		final Document doc;
		
		try {
			doc = Jsoup.parse(in, "", Parser.xmlParser()); //using jsoup b/c wiki html is invalid, also handles entities
		} catch (final RuntimeException e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Unable to parse XML in wiki: " + in);
			}
            e.printStackTrace();
			return;
		}
	    String docID;
        if (doc.select("title").first() == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not find title tag, assigning this_was_null: " + in);
            }
            docID = "this_was_null";
        } else {
            docID = doc.select("title").first().text();
            if (docID == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("docID was null, assigning this_was_null: " + in);
                }
                docID = "this_was_null";
            }
        }

		final Elements MathElements = doc.select("math");
		if (MathElements.isEmpty()) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("Unable to find math tags: " + in);
			}
			return;
		}

        StringBuilder outputLatex = new StringBuilder();
        StringBuilder cmml = new StringBuilder();
        StringBuilder pmml = new StringBuilder();
		for (final Element MathElement : MathElements) {
            //Assume only one root element and that it is <semantic>
            Element SemanticElement;
            try {
                SemanticElement = MathElement.child(0);
            } catch (final RuntimeException e) {
                if (LOG.isWarnEnabled()) {
					LOG.warn("Unable to find semantics elements: " + docID + ": " + MathElement.text());
				}
                e.printStackTrace();
                continue;
            }
            if (MathElement.children().size() > 1) {
                if (LOG.isWarnEnabled()) {
					LOG.warn("Multiple elements under math: " + in);
				}
            }
			final Elements MMLElements = SemanticElement.children();
			
			final Elements PmmlElements = new Elements(); //how are we handling multiple math tags per wiki?
			final Elements CmmlElements = new Elements();
            final Elements LatexElements = new Elements();
			//All data is well formed: 1) Presentation MML 2) annotation-CMML 3) annotation-TEX
		    //Any element not under annotation tag is Presentation MML
		    for (final Element curElement : MMLElements) {
                try {
                    if ("annotation-xml".equals(curElement.tagName())) {
                        //MathML-Content
                        //Again, always assuming one root element per MathML type
                        //E.g. there will never be two <mrow> elements under <annotation-xml>
                        //Add namespace information so canonicalizer can parse it
                        curElement.child(0).attr(namespace_tag, namespace);
                        CmmlElements.add(curElement.child(0));
                    } else if ("annotation".equals(curElement.tagName())) {
                        curElement.attr(namespace_tag, namespace);
                        LatexElements.add(curElement); //keep annotation tags b/c parsed by ExtractLatex
                    } else {
                        curElement.attr(namespace_tag, namespace);
                        PmmlElements.add(curElement);
                    }
                } catch (final RuntimeException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Badly formatted wiki xml: " + in);
                    }
                    e.printStackTrace();
                    return;
                }
			}
            final String curLatex,curCmml,curPmml;
            try {
                curLatex = ExtractHelper.extractLatex(LatexElements, STR_SPLIT);
			    curCmml = ExtractHelper.extractCanonicalizedDoc(CmmlElements);
			    curPmml = ExtractHelper.extractCanonicalizedDoc(PmmlElements);
            } catch (final Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Extraction/canonicalization failed. Exiting: " + in);
                }
                e.printStackTrace();
                return;
            }

            if (curLatex == null || curCmml == null || curPmml == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Extract helper failed on wiki or wiki has no math: " + in);
                }
                return;
            } else{
                outputLatex = outputLatex.append(curLatex);
                cmml = cmml.append(curCmml);
                pmml = pmml.append(curPmml);
            }
		} //End loop of MathElement : MathElements

		out.collect(new WikiTuple(docID, outputLatex.toString(), cmml.toString(), pmml.toString()));
        if (LOG.isInfoEnabled()) {
            LOG.info(docID + " complete!");
        }
	}
}