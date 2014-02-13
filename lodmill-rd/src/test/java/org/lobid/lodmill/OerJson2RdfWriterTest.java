/* Copyright 2014  Pascal Christoph.
 * Licensed under the Eclipse Public License 1.0 */
package org.lobid.lodmill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FileUtils;
import org.culturegraph.mf.Flux;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.pipe.StreamTee;
import org.culturegraph.mf.stream.source.DirReader;
import org.culturegraph.mf.stream.source.FileOpener;
import org.junit.Test;

/**
 * @author Pascal Christoph (dr0i)
 * 
 */
@SuppressWarnings("javadoc")
public class OerJson2RdfWriterTest {
	private final String targetPath = "tmp/";
	private static final String TEST_FILENAME = "ocw.nt";

	@Test
	public void testFlow() throws URISyntaxException {
		transformDataInDirectory("ocw/consortiumMembers", "true");
		transformDataInDirectory("ocw/organizationId", "false");
		// FileUtils.deleteDirectory(new File(PATH));
		File testFile;
		try {
			testFile =
					MabXmlTar2lobidTest.concatenateGeneratedFilesIntoOneFile(targetPath
							+ "ocw/", "tmp/ocw.nt");
			AbstractIngestTests.compareFiles(testFile, new File(Thread
					.currentThread().getContextClassLoader().getResource(TEST_FILENAME)
					.toURI()));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void transformDataInDirectory(String pathToDirectory,
			String arrayOfOjects) throws URISyntaxException {
		final DirReader dirReader = new DirReader();
		final FileOpener opener = new FileOpener();
		final JsonDecoder jsonDecoder = new JsonDecoder();
		jsonDecoder.setArrayOfObjects(arrayOfOjects);
		final Metamorph morph =
				new Metamorph(
						"src/test/resources/morph-ocwConsortiumMembers-to-rdf.xml");
		PipeEncodeTriples encoder = new PipeEncodeTriples();
		final Triples2RdfModel triple2model = new Triples2RdfModel();
		triple2model.setInput("N-TRIPLE");
		final RdfModelFileWriter writer =
				createWriter(targetPath + pathToDirectory);
		StreamTee streamTee = new StreamTee();
		final Stats stats = new Stats();
		streamTee.addReceiver(stats);
		streamTee.addReceiver(encoder);
		encoder.setReceiver(triple2model).setReceiver(writer);
		opener.setReceiver(jsonDecoder);
		jsonDecoder.setReceiver(morph).setReceiver(streamTee);
		dirReader.setReceiver(opener);
		dirReader.process((new File(Thread.currentThread().getContextClassLoader()
				.getResource(pathToDirectory).toURI())).getAbsolutePath());
		opener.closeStream();

	}

	private static RdfModelFileWriter createWriter(final String PATH) {
		final RdfModelFileWriter writer = new RdfModelFileWriter();
		writer.setProperty("http://purl.org/dc/elements/1.1/identifier");
		writer.setEndIndex(1);
		writer.setStartIndex(0);
		writer.setFileSuffix("nt");
		writer.setSerialization("N-TRIPLE");
		writer.setTarget(PATH);
		return writer;
	}

	// @Test
	public void testFlux() throws IOException, URISyntaxException,
			RecognitionException {
		File fluxFile =
				new File(Thread.currentThread().getContextClassLoader()
						.getResource("xmlSplitterRdfWriter.flux").toURI());
		Flux.main(new String[] { fluxFile.getAbsolutePath() });
		FileUtils.deleteDirectory(new File(targetPath));
	}
}
