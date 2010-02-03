package org.xmlcml.cml.testutil;

import java.io.File;



import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nu.xom.tests.XOMTestCase;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.euclid.EC;
import org.xmlcml.euclid.EuclidRuntimeException;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.euclid.Util;

/**
 * 
 * <p>
 * Utility library of common methods for unit tests
 * </p>
 * 
 * @author Peter Murray-Rust
 * @version 5.0
 * 
 */
public final class JumboTestUtils implements CMLConstants {

	/** logger */
	public final static Logger logger = Logger.getLogger(JumboTestUtils.class);

	public static final String OUTPUT_DIR_NAME = "target/test-outputs";

	/**
	 * tests 2 XML objects for equality using canonical XML. uses
	 * XOMTestCase.assertEquals. This treats different prefixes as different and
	 * compares floats literally.
	 * 
	 * @param message
	 * @param refNode
	 *            first node
	 * @param testNode
	 *            second node
	 */
	public static void assertEqualsCanonically(String message, Node refNode,
			Node testNode) {
		try {
			XOMTestCase.assertEquals(message, refNode, testNode);
		} catch (ComparisonFailure e) {
			reportXMLDiff(message, e.getMessage(), refNode, testNode);
		} catch (AssertionFailedError e) {
			reportXMLDiff(message, e.getMessage(), refNode, testNode);
		}
	}

	/**
	 * compares two XML nodes and checks float near-equivalence (can also be
	 * used for documents without floats) usesTestUtils.assertEqualsCanonically
	 * and only uses PMR code if fails
	 * 
	 * @param message
	 * @param refNode
	 * @param testNode
	 * @param eps
	 */
	public static void assertEqualsIncludingFloat(String message, Node refNode,
			Node testNode, boolean stripWhite, double eps) {
		if (stripWhite && refNode instanceof Element
				&& testNode instanceof Element) {
			refNode = stripWhite((Element) refNode);
			testNode = stripWhite((Element) testNode);
		}
		try {
			assertEqualsIncludingFloat(message, refNode, testNode, eps);
		} catch (AssertionError e) {
			logger.warn(e);
			reportXMLDiffInFull(message, e.getMessage(), refNode, testNode);
		}
	}

	public static void assertEqualsIncludingFloat(String message,
			String expectedS, Node testNode, boolean stripWhite, double eps) {
		assertEqualsIncludingFloat(message, JumboTestUtils
				.parseValidString(expectedS), testNode, stripWhite, eps);
	}

	private static void assertEqualsIncludingFloat(String message,
			Node refNode, Node testNode, double eps) {
		try {
			Assert.assertEquals(message + ": classes", testNode.getClass(),
					refNode.getClass());
			if (refNode instanceof Text) {
				testStringDoubleEquality(message + " on node: "
						+ path(testNode), refNode.getValue(), testNode
						.getValue(), eps);
			} else if (refNode instanceof Comment) {
				Assert.assertEquals(message + " comment", refNode.getValue(),
						testNode.getValue());
			} else if (refNode instanceof ProcessingInstruction) {
				Assert.assertEquals(message + " pi",
						(ProcessingInstruction) refNode,
						(ProcessingInstruction) testNode);
			} else if (refNode instanceof Element) {
				int refNodeChildCount = refNode.getChildCount();
				int testNodeChildCount = testNode.getChildCount();
				String path = path(testNode);
				// FIXME? fails to resolve in tests
//				Assert.assertEquals("number of children of " + path,
//						refNodeChildCount, testNodeChildCount);
				if (refNodeChildCount != testNodeChildCount) {
					Assert.fail("number of children of " + path + " "+
						refNodeChildCount + " != " + testNodeChildCount);
				}
				for (int i = 0; i < refNodeChildCount; i++) {
					assertEqualsIncludingFloat(message, refNode.getChild(i),
							testNode.getChild(i), eps);
				}
				Element refElem = (Element) refNode;
				Element testElem = (Element) testNode;
				Assert.assertEquals(message + " name", refElem.getLocalName(),
						testElem.getLocalName());
				Assert.assertEquals(message + " namespace", refElem
						.getNamespaceURI(), testElem.getNamespaceURI());
				Assert.assertEquals(message + " attributes on "
						+ refElem.getClass(), refElem.getAttributeCount(),
						testElem.getAttributeCount());
				for (int i = 0; i < refElem.getAttributeCount(); i++) {
					Attribute refAtt = refElem.getAttribute(i);
					String attName = refAtt.getLocalName();
					String attNamespace = refAtt.getNamespaceURI();
					Attribute testAtt = testElem.getAttribute(attName,
							attNamespace);
					if (testAtt == null) {
						// CMLUtil.debug((Element)refNode, "XXXXXXXXXXX");
						// CMLUtil.debug((Element)testNode, "TEST");
						Assert.fail(message + " attribute on ref not on test: "
								+ attName);
					}

					testStringDoubleEquality(message + " attribute "
							+ path(testAtt) + " values differ:", refAtt
							.getValue(), testAtt.getValue(), eps);
				}
			} else {
				Assert.fail(message + "cannot deal with XMLNode: "
						+ refNode.getClass());
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String path(Node testNode) {
		List<String> fullpath = path(testNode, new ArrayList<String>());
		Collections.reverse(fullpath);
		StringBuilder sb = new StringBuilder();
		for (String p : fullpath) {
			sb.append(p);
		}
		return sb.toString();
	}

	private static List<String> path(Node testNode, List<String> path) {
		if (testNode instanceof Element) {
			Element e = (Element) testNode;
			StringBuilder frag = new StringBuilder("/");
			if (!"".equals(e.getNamespacePrefix())) {
				frag.append(e.getNamespacePrefix()).append(":");
			}
			path.add(frag.append(e.getLocalName()).append("[").append(
					siblingOrdinal(e)).append("]").toString());

		} else if (testNode instanceof Attribute) {
			Attribute a = (Attribute) testNode;
			path.add(new StringBuilder("@").append(a.getNamespacePrefix())
					.append(":").append(a.getLocalName()).toString());
		} else if (testNode instanceof Text) {
			path.add("/text()");
		}
		return (testNode.getParent() != null) ? path(testNode.getParent(), path)
				: path;
	}

	private static int siblingOrdinal(Element e) {
		Element parent = (Element) e.getParent();
		if (parent == null) {
			return 0;
		} else {
			Elements els = parent.getChildElements(e.getLocalName(), e
					.getNamespaceURI());
			for (int i = 0; i < els.size(); i++) {
				if (els.get(i).equals(e)) {
					return i;
				}
			}
			throw new RuntimeException(
					"Element was not a child of its parent. Most perplexing!");
		}
	}

	private static void testStringDoubleEquality(String message,
			String refValue, String testValue, double eps) {
		Error ee = null;
		try {
			try {
				double testVal = new Double(testValue).doubleValue();
				double refVal = new Double(refValue).doubleValue();
				Assert
						.assertEquals(message + " doubles ", refVal, testVal,
								eps);
			} catch (NumberFormatException e) {
				Assert.assertEquals(message + " String ", refValue, testValue);
			}
		} catch (ComparisonFailure e) {
			ee = e;
		} catch (AssertionError e) {
			ee = e;
		}
		if (ee != null) {
			throw new RuntimeException("" + ee);
		}
	}

	/**
	 * tests 2 XML objects for equality using canonical XML. uses
	 * XOMTestCase.assertEquals. This treats different prefixes as different and
	 * compares floats literally.
	 * 
	 * @param message
	 * @param refNode
	 *            first node
	 * @param testNode
	 *            second node
	 * @param stripWhite
	 *            if true remove w/s nodes
	 */
	public static void assertEqualsCanonically(String message, Element refNode,
			Element testNode, boolean stripWhite) {
		assertEqualsCanonically(message, refNode, testNode, stripWhite, true);
	}

	/**
	 * tests 2 XML objects for equality using canonical XML.
	 * 
	 * @param message
	 * @param refNode
	 *            first node
	 * @param testNode
	 *            second node
	 * @param stripWhite
	 *            if true remove w/s nodes
	 */
	private static void assertEqualsCanonically(String message,
			Element refNode, Element testNode, boolean stripWhite,
			boolean reportError) throws Error {
		if (stripWhite) {
			refNode = stripWhite(refNode);
			testNode = stripWhite(testNode);
		}
		Error ee = null;
		try {
			XOMTestCase.assertEquals(message, refNode, testNode);
		} catch (ComparisonFailure e) {
			ee = e;
		} catch (AssertionFailedError e) {
			ee = e;
		}
		if (ee != null) {
			if (reportError) {
				reportXMLDiffInFull(message, ee.getMessage(), refNode, testNode);
			} else {
				throw (ee);
			}
		}
	}

	private static Element stripWhite(Element refNode) {
		refNode = new Element(refNode);
		CMLUtil.removeWhitespaceNodes(refNode);
		return refNode;
	}

	public static void reportXMLDiff(String message, String errorMessage,
			Node refNode, Node testNode) {
		Assert.fail(message + " ~ " + errorMessage);
	}

	public static void reportXMLDiffInFull(String message, String errorMessage,
			Node refNode, Node testNode) {
		try {
			System.err.println("==========XMLDIFF reference=========");
			CMLUtil.debug((Element) refNode, System.err, 2);
			System.err.println("------------test---------------------");
			CMLUtil.debug((Element) testNode, System.err, 2);
			System.err.println("==============" + message
					+ "===================");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Assert.fail(message + " ~ " + errorMessage);
	}

	/**
	 * tests 2 XML objects for non-equality using canonical XML. uses
	 * XOMTestCase.assertEquals. This treats different prefixes as different and
	 * compares floats literally.
	 * 
	 * @param message
	 * @param node1
	 *            first node
	 * @param node2
	 *            second node
	 */
	public static void assertNotEqualsCanonically(String message, Node node1,
			Node node2) {
		try {
			XOMTestCase.assertEquals(message, node1, node2);
			String s1 = CMLUtil.getCanonicalString(node1);
			String s2 = CMLUtil.getCanonicalString(node2);
			Assert.fail(message + "nodes should be different " + s1 + " != "
					+ s2);
		} catch (ComparisonFailure e) {
		} catch (AssertionFailedError e) {
		}
	}

	/**
	 * test the writeHTML method of element.
	 * 
	 * @param element
	 *            to test
	 * @param expected
	 *            HTML string
	 */
	public static void assertWriteHTML(CMLElement element, String expected) {
		StringWriter sw = new StringWriter();
		try {
			element.writeHTML(sw);
			sw.close();
		} catch (IOException e) {
			Assert.fail("should not throw " + e);
		}
		String s = sw.toString();
		Assert.assertEquals("HTML output ", expected, s);
	}

	/**
	 * convenience method to parse test string.
	 * 
	 * @param s
	 *            xml string (assumed valid)
	 * @return root element
	 */
	public static Element parseValidString(String s) {
		Element element = null;
		if (s == null) {
			throw new RuntimeException("NULL VALID JAVA_STRING");
		}
		try {
			element = new CMLBuilder().parseString(s);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("ERROR " + e + e.getMessage() + "..."
					+ s.substring(0, Math.min(100, s.length())));
			Util.BUG(e);
		}
		return element;
	}

	/**
	 * convenience method to parse test file. uses resource
	 * 
	 * @param filename
	 *            relative to classpath
	 * @return root element
	 */
	public static Element parseValidFile(String filename) {
		Element root = null;
		try {
			URL url = Util.getResource(filename);
			CMLBuilder builder = new CMLBuilder();
			root = builder.build(new File(url.toURI())).getRootElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return root;
	}

	/**
	 * used by Assert routines. copied from Assert
	 * 
	 * @param message
	 *            prepends if not null
	 * @param expected
	 * @param actual
	 * @return message
	 */
	public static String getAssertFormat(String message, Object expected,
			Object actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + CMLConstants.S_SPACE;
		}
		return formatted + "expected:<" + expected + "> but was:<" + actual
				+ ">";
	}

	public static void neverFail(Exception e) {
		Assert.fail("should never throw " + e);
	}

	public static void alwaysFail(String message) {
		Assert.fail("should always throw " + message);
	}

	public static void neverThrow(Exception e) {
		throw new EuclidRuntimeException("should never throw " + e);
	}
	
// ====================== CML and Euclid ===================

	/**
	 * Asserts equality of double arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param a expected array
	 * @param b actual array
	 * @param eps tolerance for agreement
	 */
	public static void assertEquals(String message, double[] a, double[] b,
			double eps) {
		String s = testEquals(a, b, eps);
		if (s != null) {
			Assert.fail(message + "; " + s);
		}
	}

	public static void assertObjectivelyEquals(String message, double[] a,
			double[] b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + EC.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (!(((Double) a[i]).equals(b[i]) || !Real.isEqual(a[i], b[i],
						eps))) {
					s = "unequal element at (" + i + "), " + a[i] + " != "
							+ b[i];
					break;
				}
			}
		}
		if (s != null) {
			Assert.fail(message + "; " + s);
		}
	}

	/**
	 * Asserts non equality of double arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param a expected array
	 * @param b actual array
	 * @param eps tolerance for agreement
	 */
	public static void assertNotEquals(String message, double[] a, double[] b,
			double eps) {
		String s = testEquals(a, b, eps);
		if (s == null) {
			Assert.fail(message + "; arrays are equal");
		}
	}

	public static String testEquals(String message, double[] a, double[] b, double eps) {
		String msg = testEquals(a, b, eps);
		return (msg == null) ? null : message+"; "+msg;
	}
	
	/**
	 * returns a message if arrays differ.
	 * 
	 * @param a array to compare
	 * @param b array to compare
	 * @param eps tolerance
	 * @return null if arrays are equal else indicative message
	 */
	static String testEquals(double[] a, double[] b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + EC.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (!Real.isEqual(a[i], b[i], eps)) {
					s = "unequal element at (" + i + "), " + a[i] + " != "
							+ b[i];
					break;
				}
			}
		}
		return s;
	}

	/**
	 * returns a message if arrays of arrays differ.
	 * 
	 * @param a array to compare
	 * @param b array to compare
	 * @param eps tolerance
	 * @return null if array are equal else indicative message
	 */
	static String testEquals(double[][] a, double[][] b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + EC.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i].length != b[i].length) {
					s = "row (" + i + ") has unequal lengths: " + a[i].length
							+ EC.S_SLASH + b[i].length;
					break;
				}
				for (int j = 0; j < a[i].length; j++) {
					if (!Real.isEqual(a[i][j], b[i][j], eps)) {
						s = "unequal element at (" + i + ", " + j + "), ("
								+ a[i][j] + " != " + b[i][j] + EC.S_RBRAK;
						break;
					}
				}
			}
		}
		return s;
	}
	// Real2
	/**
	 * returns a message if arrays differ.
	 * 
	 * @param a array to compare
	 * @param b array to compare
	 * @param eps tolerance
	 * @return null if arrays are equal else indicative message
	 */
	public static String testEquals(Real2 a, Real2 b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else {
			if (!Real.isEqual(a.x, b.x, eps) ||
				!Real.isEqual(a.y, b.y, eps)) {
				s = ""+a+" != "+b;
			}
		}
		return s;
	}
// double arrays and related
	
	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Transform2 expected, Transform2 test,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + EC.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + EC.S_RBRAK,
				expected);
		JumboTestUtils.assertEquals(msg, expected
				.getMatrixAsArray(), test.getMatrixAsArray(),  epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg message
	 * @param test 16 values
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Transform2 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + EC.S_RBRAK, test);
		Assert.assertEquals("test should have 16 elements (" + msg + EC.S_RBRAK,
				9, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + EC.S_RBRAK,
				expected);
		JumboTestUtils.assertEquals(msg, test, expected.getMatrixAsArray(),
				epsilon);
	}

	
}