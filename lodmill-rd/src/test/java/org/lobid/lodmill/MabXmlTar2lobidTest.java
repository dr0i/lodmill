/* Copyright 2013  hbz, Pascal Christoph.
 * Licensed under the Eclipse Public License 1.0 */
package org.lobid.lodmill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Scanner;

import org.culturegraph.mf.Flux;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.stream.converter.xml.XmlDecoder;
import org.culturegraph.mf.stream.pipe.ObjectTee;
import org.culturegraph.mf.stream.pipe.StreamTee;
import org.culturegraph.mf.stream.source.FileOpener;
import org.junit.Test;

/**
 * @author Pascal Christoph
 * 
 */
@SuppressWarnings("javadoc")
public final class MabXmlTar2lobidTest {
	private static final String TARGET_PATH = "tmp";
	private static final String TEST_FILENAME = "hbz01.nt";
	private static final String TARGET_SUBPATH = "/nt/";

	@SuppressWarnings("static-method")
	@Test
	public void testFlow() throws IOException, URISyntaxException {
		final FileOpener opener = new FileOpener();
		opener.setCompression("BZIP2");
		TarReader tarReader = new TarReader();
		final XmlDecoder xmlDecoder = new XmlDecoder();
		XmlTee xmlTee = new XmlTee();
		final MabXmlHandler handler = new MabXmlHandler();
		final Metamorph morph =
				new Metamorph("src/test/resources/morph-hbz01-to-lobid.xml");
		final Triples2RdfModel triple2model = new Triples2RdfModel();
		triple2model.setInput("N-TRIPLE");
		final ObjectTee<String> tee = new ObjectTee<String>();
		RdfModelFileWriter modelWriter = createModelWriter();
		modelWriter.setProperty("http://purl.org/lobid/lv#hbzID");
		triple2model.setReceiver(modelWriter);
		tee.addReceiver(triple2model);
		StreamTee streamTee = new StreamTee();
		final Stats stats = new Stats();
		stats.setFilename("tmp.stats.csv");
		streamTee.addReceiver(stats);
		PipeEncodeTriples encoder = new PipeEncodeTriples();
		streamTee.addReceiver(encoder);
		encoder.setReceiver(tee);

		XmlEntitySplitter xmlEntitySplitter = new XmlEntitySplitter();
		xmlEntitySplitter.setEntityName("ListRecords");
		XmlFilenameWriter xmlFilenameWriter = createXmlFilenameWriter();

		xmlTee.setReceiver(handler).setReceiver(morph).setReceiver(streamTee);
		xmlTee.addReceiver(xmlEntitySplitter);
		xmlEntitySplitter.setReceiver(xmlFilenameWriter);
		opener.setReceiver(tarReader).setReceiver(xmlDecoder).setReceiver(xmlTee);
		opener.process(new File("src/test/resources/hbz01XmlClobs.tar.bz2")
				.getAbsolutePath());
		opener.closeStream();

		final File testFile =
				concatenateGeneratedFilesIntoOneFile(TARGET_PATH + TARGET_SUBPATH,
						TARGET_PATH + "/" + TEST_FILENAME);
		// positive test
		AbstractIngestTests.compareFiles(testFile, new File(Thread.currentThread()
				.getContextClassLoader().getResource(TEST_FILENAME).toURI()));
		// negative test
		AbstractIngestTests.checkIfNoIntersection(
				testFile,
				new File(Thread.currentThread().getContextClassLoader()
						.getResource("hbz01negatives.ttl").toURI()));
		// testFile.deleteOnExit();
	}

	// static StringBuilder triples = new StringBuilder();
	static HashSet<String> triples = new HashSet<String>();

	static File concatenateGeneratedFilesIntoOneFile(String sourcePath,
			String targetFilename) throws FileNotFoundException, IOException {
		concatenateGeneratedFiles(sourcePath, targetFilename);
		final File testFile = new File(targetFilename);
		final FileOutputStream fos = new FileOutputStream(testFile);
		StringBuilder sb = new StringBuilder();
		for (String str : triples) {
			sb.append(str);
		}
		fos.write(sb.toString().getBytes());
		fos.close();
		return testFile;
	}

	private static void concatenateGeneratedFiles(String sourcePath,
			String targetFilename) {
		for (String file : (new File(sourcePath)).list()) {
			if ((new File(sourcePath + file)).isDirectory()) {
				concatenateGeneratedFiles(sourcePath + file + "/", targetFilename);
			} else {
				File toAppend = new File(sourcePath + "/" + file);
				if (toAppend.isFile()) {
					getFileContent(toAppend);
				}
			}
		}

	}

	private static void getFileContent(File file) {
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				final String actual = scanner.nextLine();
				if (!actual.isEmpty()) {
					triples.add(actual + "\n");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static XmlFilenameWriter createXmlFilenameWriter() {
		XmlFilenameWriter xmlFilenameWriter = new XmlFilenameWriter();
		xmlFilenameWriter.setStartIndex(2);
		xmlFilenameWriter.setEndIndex(7);
		xmlFilenameWriter.setTarget(TARGET_PATH + "/xml");
		xmlFilenameWriter
				.setProperty("/OAI-PMH/ListRecords/record/metadata/record/datafield[@tag='001']/subfield[@code='a']");
		xmlFilenameWriter.setCompression("bz2");
		xmlFilenameWriter.setFileSuffix("");
		xmlFilenameWriter.setEncoding("utf8");
		return xmlFilenameWriter;
	}

	private static RdfModelFileWriter createModelWriter() {
		RdfModelFileWriter modelWriter = new RdfModelFileWriter();
		modelWriter.setProperty("http://lobid.org/vocab/lobid#hbzID");
		modelWriter.setSerialization("N-TRIPLES");
		modelWriter.setStartIndex(2);
		modelWriter.setEndIndex(7);
		modelWriter.setTarget(TARGET_PATH + TARGET_SUBPATH);
		return modelWriter;
	}

	@SuppressWarnings("static-method")
	@Test
	public void testFlux() throws URISyntaxException {
		File fluxFile =
				new File(Thread.currentThread().getContextClassLoader()
						.getResource("hbz01-to-lobid.flux").toURI());
		try {
			Flux.main(new String[] { fluxFile.getAbsolutePath() });
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
