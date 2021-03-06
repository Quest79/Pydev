package org.python.pydev.shared_core.auto_edit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.python.pydev.shared_core.partitioner.PartitionCodeReader;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.testutils.TestUtils;

public class PartitionCodeReaderTest extends TestCase {

    public void testPartitionCodeReader() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader(IDocument.DEFAULT_CONTENT_TYPE);
        Document document = new Document("aaaa bbbb");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(0, 4, "cat1"));
        document.addPosition(category, new TypedPosition(4, 5, IDocument.DEFAULT_CONTENT_TYPE));

        reader.configureForwardReader(document, 0, document.getLength() - 1);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(" ", "b", "b", "b"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, document.getLength());
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("b", "b", "b", "b", " "), TestUtils.listToExpected(found));
    }

    public void testPartitionCodeReader2() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(3, 3, "cat1"));
        document.addPosition(category, new TypedPosition(8, 3, "cat1"));

        reader.configureForwardReader(document, 0, document.getLength() - 1);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("a", " ", "b", "b", " ", "c"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, document.getLength());
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("c", " ", "b", "b", " ", "a"), TestUtils.listToExpected(found));
    }

    public void testPartitionCodeReaderBoundaries() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(3, 3, "cat1"));
        document.addPosition(category, new TypedPosition(8, 3, "cat1"));

        reader.configureForwardReader(document, 4, 6);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(" ", "b"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, 5);
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("b", " ", "a"), TestUtils.listToExpected(found));
    }

    public void testPartitionCodeReaderKeepingPositions() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader(IDocument.DEFAULT_CONTENT_TYPE);
        Document document = new Document("abcde");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(1, 1, "cat1")); //skip b
        document.addPosition(category, new TypedPosition(3, 1, "cat1")); //skip d

        reader.configureForwardReader(document, 3, document.getLength(), true);
        FastStringBuffer buf = new FastStringBuffer(document.getLength());
        readAll(reader, buf);
        assertEquals("e", buf.toString());

        reader.configureForwardReaderKeepingPositions(2, document.getLength());
        buf.clear();
        readAll(reader, buf);
        assertEquals("ce", buf.toString());

        reader.configureForwardReaderKeepingPositions(0, document.getLength());
        buf.clear();
        readAll(reader, buf);
        assertEquals("ace", buf.toString());
    }

    public void testPartitionCodeReaderUnread() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("abcde");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(1, 1, "cat1")); //skip b
        document.addPosition(category, new TypedPosition(3, 1, "cat1")); //skip d

        reader.configureForwardReader(document, 0, document.getLength(), true);

        assertEquals('b', reader.read());
        reader.unread();
        assertEquals('b', reader.read());
        reader.unread();

        assertEquals('b', reader.read());
        assertEquals('d', reader.read());
        assertEquals(-1, reader.read());
        reader.unread();
        assertEquals(-1, reader.read());
        reader.unread();
        assertEquals(-1, reader.read());
        reader.unread();
        reader.unread();
        assertEquals('d', reader.read());
        for (int i = 0; i < 5; i++) {
            reader.read(); //read past EOF (but should actually stay at EOF)
        }
        reader.unread();
        assertEquals(-1, reader.read());
        reader.unread();
        reader.unread();
        assertEquals('d', reader.read());
        for (int i = 0; i < 5; i++) {
            reader.unread(); //unread to the beggining
        }

        FastStringBuffer buf = new FastStringBuffer(document.getLength());
        readAll(reader, buf);
        reader.unread(); //EOF
        reader.unread(); //d
        reader.unread(); //b
        readAll(reader, buf);
        assertEquals("bdbd", buf.toString());

        reader.configureForwardReaderKeepingPositions(1, document.getLength());
        buf.clear();
        readAll(reader, buf);
        reader.unread(); //EOF
        reader.unread(); //d
        reader.unread(); //b
        readAll(reader, buf);
        assertEquals("bdbd", buf.toString());

        reader.configureForwardReaderKeepingPositions(2, document.getLength());
        buf.clear();
        readAll(reader, buf);
        reader.unread(); //EOF
        reader.unread(); //d
        reader.unread(); //b
        readAll(reader, buf);
        assertEquals("dd", buf.toString());
    }

    public void testPartitionCodeReaderUnread2() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader(IDocument.DEFAULT_CONTENT_TYPE);
        Document document = new Document("abcde");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(1, 1, "cat1")); //skip b
        document.addPosition(category, new TypedPosition(3, 1, "cat1")); //skip d

        reader.configureForwardReader(document, 0, document.getLength());
        FastStringBuffer buf = new FastStringBuffer(document.getLength());
        readAll(reader, buf);
        reader.unread(); //EOF
        reader.unread(); //e
        reader.unread(); //c
        readAll(reader, buf);
        reader.unread(); //EOF
        reader.unread(); //e
        reader.unread(); //c
        reader.unread(); //a
        readAll(reader, buf);
        reader.unread(); //EOF
        assertEquals(-1, reader.read());
        reader.unread(); //EOF
        reader.unread(); //e
        readAll(reader, buf);
        assertEquals("aceceacee", buf.toString());
    }

    private void readAll(PartitionCodeReader reader, FastStringBuffer buf) {
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            buf.append((char) read);
        }
    }

    public void testPartitionCodeReaderEmpty() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);
        reader.configureForwardReader(document, 4, 6);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, 5);
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(), TestUtils.listToExpected(found));
    }

    public String setupDocument(Document document) {
        IPartitionTokenScanner scanner = new RuleBasedPartitionScanner();
        FastPartitioner partitioner = new FastPartitioner(scanner, new String[] { IDocument.DEFAULT_CONTENT_TYPE });
        String[] managingPositionCategories = partitioner.getManagingPositionCategories();
        String category = managingPositionCategories[0];
        document.setDocumentPartitioner(partitioner);
        document.addPositionCategory(category);
        return category;
    }
}
