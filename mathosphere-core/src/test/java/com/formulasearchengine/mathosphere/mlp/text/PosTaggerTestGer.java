package com.formulasearchengine.mathosphere.mlp.text;

import com.formulasearchengine.mathosphere.mlp.PatternMatchingRelationFinder;
import com.formulasearchengine.mathosphere.mlp.cli.FlinkMlpCommandConfig;
import com.formulasearchengine.mathosphere.mlp.contracts.TextAnnotatorMapper;
import com.formulasearchengine.mathosphere.mlp.pojos.Formula;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.pojos.Sentence;
import com.formulasearchengine.mathosphere.mlp.pojos.Word;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PosTaggerTestGer {

  private static final Logger LOGGER = LoggerFactory.getLogger(PosTaggerTestGer.class);
	private static final String GER = "edu/stanford/nlp/models/pos-tagger/german/german-hgc.tagger";

  @Test
  public void simpleGermanTest() throws Exception {
    FlinkMlpCommandConfig cfg = FlinkMlpCommandConfig.test();
    PosTagger nlpProcessor = PosTagger.create(cfg.getLanguage(), GER);
    String text = "Dies ist ein simpler Beispieltext.";

    List<MathTag> mathTags = WikiTextUtils.findMathTags(text);
    List<Formula> formulas = TextAnnotatorMapper.toFormulas(mathTags, false);

    String newText = WikiTextUtils.replaceAllFormulas(text, mathTags);
    String cleanText = WikiTextUtils.extractPlainText(newText);

    List<Sentence> result = nlpProcessor.process(cleanText, formulas);

    List<Word> expected = Arrays.asList(w("Dies", "PDS"), w("ist", "VAFIN"), w("ein", "ART"), w("simpler","ADJA"), w("Beispieltext", "NN"),
      w(".", "$."));

    List<Word> sentence = result.get(0).getWords();
    assertEquals(expected, sentence.subList(0, expected.size()));
    LOGGER.debug("full result: {}", result);
  }

  public static Word w(String word, String tag) {
    return new Word(word, tag);
  }

  public static String readText(String name) throws IOException {
    InputStream inputStream = PatternMatchingRelationFinder.class.getResourceAsStream(name);
    return IOUtils.toString(inputStream);
  }

}