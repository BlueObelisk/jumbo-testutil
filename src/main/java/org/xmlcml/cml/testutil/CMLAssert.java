package org.xmlcml.cml.testutil;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import nu.xom.Attribute;
import nu.xom.Comment;
import nu.xom.Element;
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
import org.xmlcml.cml.element.CMLArray;
import org.xmlcml.cml.element.CMLAtomSet;
import org.xmlcml.cml.element.CMLBondSet;
import org.xmlcml.cml.element.CMLCellParameter;
import org.xmlcml.cml.element.CMLFormula;
import org.xmlcml.cml.element.CMLLatticeVector;
import org.xmlcml.cml.element.CMLLine3;
import org.xmlcml.cml.element.CMLMatrix;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLPlane3;
import org.xmlcml.cml.element.CMLPoint3;
import org.xmlcml.cml.element.CMLTransform3;
import org.xmlcml.cml.element.CMLVector3;
import org.xmlcml.euclid.EC;
import org.xmlcml.euclid.EuclidRuntimeException;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.euclid.IntMatrix;
import org.xmlcml.euclid.IntSet;
import org.xmlcml.euclid.Line3;
import org.xmlcml.euclid.Plane3;
import org.xmlcml.euclid.Point3;
import org.xmlcml.euclid.Point3Vector;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Vector;
import org.xmlcml.euclid.Real3Range;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.RealMatrix;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.euclid.RealSquareMatrix;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.euclid.Transform3;
import org.xmlcml.euclid.Util;
import org.xmlcml.euclid.Vector2;
import org.xmlcml.euclid.Vector3;

public class CMLAssert {
	private static final Logger LOG = Logger.getLogger(CMLAssert.class);

	/**
	 * tests equality against list of ids. (order of elements in set is
	 * undefined)
	 * 
	 * @param message
	 * @param expectedAtomIds
	 * @param atomSet
	 */
	public static void assertEquals(String message, String[] expectedAtomIds,
			CMLAtomSet atomSet) {
		Assert.assertEquals(message + "; unequal sizes; expected "
				+ expectedAtomIds.length + ", found: " + atomSet.size(),
				expectedAtomIds.length, atomSet.size());
		Set<String> expectedSet = new HashSet<String>();
		for (String es : expectedAtomIds) {
			expectedSet.add(es);
		}
		Set<String> foundSet = new HashSet<String>();
		String[] fss = atomSet.getAtomIDs();
		for (String fs : fss) {
			foundSet.add(fs);
		}
		Assert.assertTrue("compare atom sets", expectedSet.equals(foundSet));
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test, Point3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getArray(), epsilon);
	}

	/**
	 * Asserts equality of double arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param a
	 *            expected array
	 * @param b
	 *            actual array
	 * @param eps
	 *            tolerance for agreement
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
			s = "unequal arrays: " + a.length + CMLConstants.S_SLASH + b.length;
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
	 * @param a
	 *            expected array
	 * @param b
	 *            actual array
	 * @param eps
	 *            tolerance for agreement
	 */
	public static void assertNotEquals(String message, double[] a, double[] b,
			double eps) {
		String s = testEquals(a, b, eps);
		if (s == null) {
			Assert.fail(message + "; arrays are equal");
		}
	}

	/**
	 * returns a message if arrays differ.
	 * 
	 * @param a
	 *            array to compare
	 * @param b
	 *            array to compare
	 * @param eps
	 *            tolerance
	 * @return null if arrays are equal else indicative message
	 */
	static String testEquals(double[] a, double[] b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + CMLConstants.S_SLASH + b.length;
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
	 * @param a
	 *            array to compare
	 * @param b
	 *            array to compare
	 * @param eps
	 *            tolerance
	 * @return null if array are equal else indicative message
	 */
	static String testEquals(double[][] a, double[][] b, double eps) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + CMLConstants.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i].length != b[i].length) {
					s = "row (" + i + ") has unequal lengths: " + a[i].length
							+ CMLConstants.S_SLASH + b[i].length;
					break;
				}
				for (int j = 0; j < a[i].length; j++) {
					if (!Real.isEqual(a[i][j], b[i][j], eps)) {
						s = "unequal element at (" + i + ", " + j + "), ("
								+ a[i][j] + " != " + b[i][j] + CMLConstants.S_RBRAK;
						break;
					}
				}
			}
		}
		return s;
	}

	/**
	 * tests 2 XML objects for equality using canonical XML.
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
	 * used for documents without floats) uses JumboTestUtils.assertEqualsCanonically and only
	 * uses PMR code if fails
	 * 
	 * @param message
	 * @param refNode
	 * @param testNode
	 * @param eps
	 */
	public static void assertEqualsIncludingFloat(String message, Node refNode,
			Node testNode, boolean stripWhite, double eps) {
		assertEqualsIncludingFloat(message, refNode, testNode, stripWhite, eps,
				true);
	}

	/**
	 * compares two XML nodes and checks float near-equivalence (can also be
	 * used for documents without floats) uses JumboTestUtils.assertEqualsCanonically and only
	 * uses PMR code if fails
	 * 
	 * @param message
	 * @param refNode
	 * @param testNode
	 * @param eps
	 */
	public static void assertEqualsIncludingFloat(String message, Node refNode,
			Node testNode, boolean stripWhite, double eps, boolean report) {
		if (stripWhite && refNode instanceof Element
				&& testNode instanceof Element) {
			refNode = stripWhite((Element) refNode);
			testNode = stripWhite((Element) testNode);
		}
		try {
			assertEqualsIncludingFloat(message, refNode, testNode, eps);
		} catch (RuntimeException e) {
			if (report) {
				reportXMLDiffInFull(message, e.getMessage(), refNode, testNode);
			}
		}
	}

	private static void assertEqualsIncludingFloat(String message,
			Node refNode, Node testNode, double eps) {
		try {
			Assert.assertEquals(message + ": classes", testNode.getClass(),
					refNode.getClass());
			if (refNode instanceof Text) {
				testStringDoubleEquality(message, refNode.getValue(), testNode
						.getValue(), eps);
			} else if (refNode instanceof Comment) {
				Assert.assertEquals(message + " pi", (Comment) refNode,
						(Comment) testNode);
			} else if (refNode instanceof ProcessingInstruction) {
				Assert.assertEquals(message + " pi",
						(ProcessingInstruction) refNode,
						(ProcessingInstruction) testNode);
			} else if (refNode instanceof Element) {
				int refNodeChildCount = refNode.getChildCount();
				int testNodeChildCount = testNode.getChildCount();
				Assert.assertEquals("number of children", testNodeChildCount,
						refNodeChildCount);
				for (int i = 0; i < refNodeChildCount; i++) {
					assertEqualsIncludingFloat(message, refNode.getChild(i),
							testNode.getChild(i), eps);
				}
				Element refElem = (Element) refNode;
				Element testElem = (Element) testNode;
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
						Assert.fail(message + " attribute on ref not on test: "
								+ attName);
					}
					testStringDoubleEquality(message, refAtt.getValue(),
							testAtt.getValue(), eps);
				}
			} else {
				Assert.fail(message + "cannot deal with XMLNode: "
						+ refNode.getClass());
			}
		} catch (Throwable t) {
			throw new RuntimeException("" + t);
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

	static protected void reportXMLDiff(String message, String errorMessage,
			Node refNode, Node testNode) {
		Assert.fail(message + " ~ " + errorMessage);
	}

	static protected void reportXMLDiffInFull(String message,
			String errorMessage, Node refNode, Node testNode) {
		try {
			System.err.println("Error: "+errorMessage);
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
	 * tests 2 XML objects for non-equality using canonical XML.
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
			Assert.assertEquals(message, node1, node2);
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
			root = new CMLBuilder().build(new File(url.toURI()))
					.getRootElement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return root;
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
	 * resource
	 */
	public final static String TOOLS_RESOURCE = "org" + CMLConstants.U_S + "xmlcml" + CMLConstants.U_S
			+ "cml" + CMLConstants.U_S + "tools";
	/**
	 * examples
	 */
	public final static String TOOLS_EXAMPLES = TOOLS_RESOURCE + CMLConstants.U_S
			+ "examples";

	/**
	 * crystal examples
	 */
	public final static String CRYSTAL_EXAMPLES = TOOLS_EXAMPLES + CMLConstants.U_S
			+ "cryst";

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

	/**
	 * Asserts equality of String arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * equality if individual elements are equal or both elements are null
	 * 
	 * @param message
	 * @param a
	 *            expected array may include nulls
	 * @param b
	 *            actual array may include nulls
	 */
	public static void assertEquals(String message, String[] a, String[] b) {
		String s = testEquals(a, b);
		if (s != null) {
			Assert.fail(message + "; " + s);
		}
	}

	/**
	 * Asserts equality of String arrays.
	 * 
	 * convenience method where test is a whitespace-separated set of tokens
	 * 
	 * @param message
	 * @param a
	 *            expected array as space concatenated
	 * @param b
	 *            actual array may not include nulls
	 */
	public static void assertEquals(String message, String a, String[] b) {
		String[] aa = a.split(EC.S_SPACE);
		String s = testEquals(aa, b);
		if (s != null) {
			Assert.fail(message + "; " + s);
		}
	}

	/**
	 * match arrays. error is a == null or b == null or a.length != b.length or
	 * a[i] != b[i] nulls match
	 * 
	 * @param a
	 * @param b
	 * @return message if errors else null
	 */
	public static String testEquals(String[] a, String[] b) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + CMLConstants.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i] == null && b[i] == null) {
					// both null, match
				} else if (a[i] == null || b[i] == null || !a[i].equals(b[i])) {
					s = "unequal element (" + i + "), expected: " + a[i]
							+ " found: " + b[i];
					break;
				}
			}
		}
		return s;
	}

	/**
	 * Asserts non equality of String arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param a
	 *            expected array
	 * @param b
	 *            actual array
	 */
	public static void assertNotEquals(String message, String[] a, String[] b) {
		String s = testEquals(a, b);
		if (s == null) {
			Assert.fail(message + "; arrays are equal");
		}
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Real2 test, Real2 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getXY(), expected.getXY(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 2
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test, Real2 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 2", 2, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getXY(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLArray test,
			CMLArray expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		if ((test.getDataType() == null || test.getDataType()
				.equals(CMLConstants.XSD_STRING))
				&& (expected.getDataType() == null || expected.getDataType()
						.equals(CMLConstants.XSD_STRING))) {
			Assert.assertEquals(msg, test.getStrings(), expected.getStrings());
		} else if (test.getDataType().equals(CMLConstants.XSD_DOUBLE)
				&& expected.getDataType().equals(CMLConstants.XSD_DOUBLE)) {
			CMLAssert.assertEquals(msg, test.getDoubles(), expected.getDoubles(), epsilon);
		} else if (test.getDataType().equals(CMLConstants.XSD_INTEGER)
				&& expected.getDataType().equals(CMLConstants.XSD_INTEGER)) {
			Assert.assertEquals(msg, test.getInts(), expected.getInts());
		} else {
			Assert.fail("inconsistent dataTypes" + test.getDataType() + " / "
					+ expected.getDataType());
		}
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLArray expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		if (!expected.getDataType().equals(CMLConstants.XSD_DOUBLE)) {
			Assert.fail("expected should be double");
		}
		CMLAssert.assertEquals(msg, test, expected.getDoubles(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, int[] test, CMLArray expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		if (!expected.getDataType().equals(CMLConstants.XSD_INTEGER)) {
			Assert.fail("expected should be int");
		}
		Assert.assertEquals(msg, test, expected.getInts());
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, String[] test, CMLArray expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		if (expected.getDataType() != null
				&& !expected.getDataType().equals(CMLConstants.XSD_STRING)) {
			Assert.fail("expected should be String");
		}
		Assert.assertEquals(msg, test, expected.getStrings());
	}

	/**
	 * asserts equality of double arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param formula1
	 *            expected formula
	 * @param formula2
	 *            actual formula
	 * @param eps
	 *            tolerance for agreement
	 */
	public static void assertEqualsConcise(String message, CMLFormula formula1,
			CMLFormula formula2, double eps) {
		if (formula1 == null) {
			Assert.fail(getAssertFormat(message, "formula", "null"));
		}
		if (formula2 == null) {
			Assert.fail(getAssertFormat(message, "formula", "null"));
		}
		Assert.assertEquals("equal concise", true, formula1.equals(formula2,
				eps));
	}

	/**
	 * compare two molecules. ignore whitespace nodes in either.
	 * 
	 * @param mol
	 *            to compare
	 * @param filename
	 *            containing molecule as root element
	 */
	public static void assertEqualsCanonically(CMLMolecule mol, String filename) {
		CMLMolecule mol1 = null;
		try {
			mol1 = (CMLMolecule) new CMLBuilder().build(new File(filename))
					.getRootElement();
		} catch (Exception e) {
			neverThrow(e);
		}
		assertEqualsCanonically(mol, mol1);
	}

	/**
	 * compare two molecules. ignore whitespace nodes in either.
	 * 
	 * @param mol
	 *            to compare
	 * @param fixture
	 *            .mol1 other molecule
	 */
	public static void assertEqualsCanonically(CMLMolecule mol, CMLMolecule mol1) {
		mol = new CMLMolecule(mol);
		CMLUtil.removeWhitespaceNodes(mol);
		mol1 = new CMLMolecule(mol1);
		CMLUtil.removeWhitespaceNodes(mol1);
		String molS = mol.getCanonicalString();
		String mol1S = mol1.getCanonicalString();
		Assert.assertEquals("MOLECUL equality: ", molS, mol1S);
		assertEqualsCanonically("molecule equality", mol, mol1);
	}

	/**
	 * tests equality against list of ids. (order of elements in set is
	 * undefined)
	 * 
	 * @param message
	 * @param expectedBondIds
	 * @param bondSet
	 */
	public static void assertEquals(String message, String[] expectedBondIds,
			CMLBondSet bondSet) {
		Assert.assertEquals(message + "; unequal sizes; expected "
				+ expectedBondIds.length + ", found: " + bondSet.size(),
				expectedBondIds.length, bondSet.size());
		Set<String> expectedSet = new HashSet<String>();
		for (String es : expectedBondIds) {
			expectedSet.add(es);
		}
		Set<String> foundSet = new HashSet<String>();
		List<String> fss = bondSet.getBondIDs();
		for (String fs : fss) {
			foundSet.add(fs);
		}
		Assert.assertTrue("compare atom sets", expectedSet.equals(foundSet));
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLCellParameter test,
			CMLCellParameter expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getType(), test.getXMLContent(), expected,
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param type
	 *            of parameter
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, String type, double[] test,
			CMLCellParameter expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("type should not be null (" + msg + CMLConstants.S_RBRAK, type);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertNotNull("expected should not have null type (" + msg
				+ CMLConstants.S_RBRAK, expected.getType());
		Assert.assertEquals("types must be equal", 3, test.length);
		CMLAssert.assertEquals(msg, test, expected.getXMLContent(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLLatticeVector test,
			CMLLatticeVector expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getXMLContent(), expected.getXMLContent(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLLatticeVector expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getXMLContent(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLLine3 test,
			CMLLine3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getEuclidLine3(), expected.getEuclidLine3(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param testVector
	 * @param testPoint
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLPoint3 testPoint,
			CMLVector3 testVector, CMLLine3 expected, double epsilon) {
		Assert.assertNotNull("testVector should not be null (" + msg + CMLConstants.S_RBRAK,
				testVector);
		Assert.assertNotNull("testPoint should not be null (" + msg + CMLConstants.S_RBRAK,
				testPoint);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, testVector.getEuclidVector3(), new Vector3(expected
				.getVector3()), epsilon);
		CMLAssert.assertEquals(msg, testPoint.getEuclidPoint3(), new Point3(expected
				.getPoint3()), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLMatrix test,
			CMLMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		if (test.getEuclidRealMatrix() != null) {
			CMLAssert.assertEquals(msg, test.getEuclidRealMatrix(), expected
					.getEuclidRealMatrix(), epsilon);
		} else if (test.getEuclidIntMatrix() != null) {
			Assert.assertEquals(msg, test.getEuclidIntMatrix(), expected
					.getEuclidIntMatrix());
		} else {
			Assert.fail("both matrices must be either real or int" + test);
		}
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param rows
	 * @param cols
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, int rows, int cols,
			double[] test, CMLMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("rows ", rows, expected.getRows());
		Assert.assertEquals("columns ", cols, expected.getColumns());
		CMLAssert.assertEquals(msg, rows, cols, test, expected.getEuclidRealMatrix(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param rows
	 * @param cols
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, int rows, int cols, int[] test,
			CMLMatrix expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("rows ", rows, expected.getRows());
		Assert.assertEquals("columns ", cols, expected.getColumns());
		CMLAssert.assertEquals(msg, rows, cols, test, expected.getEuclidIntMatrix());
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLPlane3 test,
			CMLPlane3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLPlane3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 4", 4, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLPoint3 test,
			CMLPoint3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getXYZ3(), expected.getXYZ3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLPoint3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getXYZ3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLTransform3 test,
			CMLTransform3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getEuclidTransform3(), expected
				.getEuclidTransform3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLTransform3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 16", 16, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getEuclidTransform3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, CMLVector3 test,
			CMLVector3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getXYZ3(), expected.getXYZ3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			CMLVector3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getXYZ3(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, IntArray test, IntArray expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals(msg, test.getArray(), expected.getArray());
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, int[] test, IntArray expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("must be of equal length ", test.length, expected
				.getArray().length);
		Assert.assertEquals(msg, test, expected.getArray());
	}

	/**
	 * equality test. true if both args not null and equal within epsilon and
	 * rows are present and equals and columns are present and equals
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, IntMatrix test,
			IntMatrix expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertNotNull("expected should have columns (" + msg + CMLConstants.S_RBRAK,
				expected.getCols());
		Assert.assertNotNull("expected should have rows (" + msg + CMLConstants.S_RBRAK,
				expected.getRows());
		Assert.assertNotNull("test should have columns (" + msg + CMLConstants.S_RBRAK, test
				.getCols());
		Assert.assertNotNull("test should have rows (" + msg + CMLConstants.S_RBRAK, test
				.getRows());
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getRows(), expected.getRows());
		Assert.assertEquals("columns should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getCols(), expected.getCols());
		Assert.assertEquals(msg, test.getMatrixAsArray(), expected.getMatrixAsArray());
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param rows
	 * @param cols
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, int rows, int cols, int[] test,
			IntMatrix expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, rows,
				expected.getRows());
		Assert.assertEquals("columns should be equal (" + msg + CMLConstants.S_RBRAK, cols,
				expected.getCols());
		Assert.assertEquals(msg, test, expected.getMatrixAsArray());
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, IntSet test, IntSet expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals(msg, test.getElements(), expected.getElements());
	}

	/**
	 * equality test. true if both args not null and equal
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 */
	public static void assertEquals(String msg, int[] test, IntSet expected) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("must be of equal length ", test.length, expected
				.getElements().length);
		Assert.assertEquals(msg, test, expected.getElements());
	}

	/**
	 * Asserts equality of int arrays.
	 * 
	 * checks for non-null, then equality of length, then individual elements
	 * 
	 * @param message
	 * @param a
	 *            expected array
	 * @param b
	 *            actual array
	 */
	public static void assertEquals(String message, int[] a, int[] b) {
		String s = testEquals(a, b);
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
	 * @param a
	 *            expected array
	 * @param b
	 *            actual array
	 */
	public static void assertNotEquals(String message, int[] a, int[] b) {
		String s = testEquals(a, b);
		if (s == null) {
			Assert.fail(message + "; arrays are equal");
		}
	}

	/**
	 * compare integer arrays.
	 * 
	 * @param a
	 * @param b
	 * @return message or null
	 */
	public static String testEquals(int[] a, int[] b) {
		String s = null;
		if (a == null) {
			s = "a is null";
		} else if (b == null) {
			s = "b is null";
		} else if (a.length != b.length) {
			s = "unequal arrays: " + a.length + CMLConstants.S_SLASH + b.length;
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					s = "unequal element (" + i + "), " + a[i] + " != " + b[i];
					break;
				}
			}
		}
		return s;
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Line3 test, Line3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getPoint(), expected.getPoint(), epsilon);
		CMLAssert.assertEquals(msg, test.getVector(), expected.getVector(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param testPoint
	 * @param testVector
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Point3 testPoint,
			Vector3 testVector, Line3 expected, double epsilon) {
		Assert.assertNotNull("testPoint should not be null (" + msg + CMLConstants.S_RBRAK,
				testPoint);
		Assert.assertNotNull("testVector should not be null (" + msg + CMLConstants.S_RBRAK,
				testVector);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, testPoint, expected.getPoint(), epsilon);
		CMLAssert.assertEquals(msg, testVector, expected.getVector(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Plane3 test, Plane3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 4
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test, Plane3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 4", 4, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Point3Vector test,
			Point3Vector expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Point3Vector expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("must be of equal length ", test.length, expected
				.getArray().length);
		CMLAssert.assertEquals(msg, test, expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Real2Vector expected,
			Real2Vector test, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, expected.getXY().getArray(), test.getXY().getArray(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Real2Vector expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("must be of equal length ", test.length, expected
				.getXY().getArray().length);
		CMLAssert.assertEquals(msg, test, expected.getXY().getArray(), epsilon);
	}

	/**
	 * test ranges for equality.
	 * 
	 * @param msg
	 * @param r3ref
	 * @param r3
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Real3Range r3ref,
			Real3Range r3, double epsilon) {
		CMLAssert.assertEquals("xRange", r3.getXRange(), r3ref.getXRange(), epsilon);
		CMLAssert.assertEquals("yRange", r3.getYRange(), r3ref.getYRange(), epsilon);
		CMLAssert.assertEquals("zRange", r3.getZRange(), r3ref.getZRange(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, RealArray test,
			RealArray expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			RealArray expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("must be of equal length ", test.length, expected
				.getArray().length);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon and
	 * rows are present and equals and columns are present and equals
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, RealMatrix test,
			RealMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertNotNull("expected should have columns (" + msg + CMLConstants.S_RBRAK,
				expected.getCols());
		Assert.assertNotNull("expected should have rows (" + msg + CMLConstants.S_RBRAK,
				expected.getRows());
		Assert.assertNotNull("test should have columns (" + msg + CMLConstants.S_RBRAK, test
				.getCols());
		Assert.assertNotNull("test should have rows (" + msg + CMLConstants.S_RBRAK, test
				.getRows());
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getRows(), expected.getRows());
		Assert.assertEquals("columns should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getCols(), expected.getCols());
		CMLAssert.assertEquals(msg, test.getMatrixAsArray(), expected.getMatrixAsArray(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param rows
	 * @param cols
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, int rows, int cols,
			double[] test, RealMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, rows,
				expected.getRows());
		Assert.assertEquals("columns should be equal (" + msg + CMLConstants.S_RBRAK, cols,
				expected.getCols());
		CMLAssert.assertEquals(msg, test, expected.getMatrixAsArray(), epsilon);
	}

	/**
	 * tests equality of ranges.
	 * 
	 * @param msg
	 *            message
	 * @param ref
	 * @param r
	 * @param epsilon
	 */
	public static void assertEquals(String msg, RealRange ref, RealRange r,
			double epsilon) {
		Assert.assertEquals(msg + " min", r.getMin(), ref.getMin(), epsilon);
		Assert.assertEquals(msg + " max", r.getMax(), ref.getMax(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon and
	 * rows are present and equals and columns are present and equals
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, RealSquareMatrix test,
			RealSquareMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertNotNull("expected should have columns (" + msg + CMLConstants.S_RBRAK,
				expected.getCols());
		Assert.assertNotNull("expected should have rows (" + msg + CMLConstants.S_RBRAK,
				expected.getRows());
		Assert.assertNotNull("test should have columns (" + msg + CMLConstants.S_RBRAK, test
				.getCols());
		Assert.assertNotNull("test should have rows (" + msg + CMLConstants.S_RBRAK, test
				.getRows());
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getRows(), expected.getRows());
		Assert.assertEquals("columns should be equal (" + msg + CMLConstants.S_RBRAK, test
				.getCols(), expected.getCols());
		CMLAssert.assertEquals(msg, test.getMatrixAsArray(), expected.getMatrixAsArray(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param rows
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, int rows, double[] test,
			RealSquareMatrix expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		Assert.assertEquals("rows should be equal (" + msg + CMLConstants.S_RBRAK, rows,
				expected.getRows());
		CMLAssert.assertEquals(msg, test, expected.getMatrixAsArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Transform2 test,
			Transform2 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getMatrixAsArray(), expected.getMatrixAsArray(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            16 values
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Transform2 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("test should have 16 elements (" + msg + CMLConstants.S_RBRAK,
				9, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getMatrixAsArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Transform3 test,
			Transform3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getMatrixAsArray(), expected.getMatrixAsArray(),
				epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            16 values
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Transform3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("test should have 16 elements (" + msg + CMLConstants.S_RBRAK,
				16, test.length);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getMatrixAsArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Vector3 test, Vector3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 *            array must be of length 3
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, double[] test,
			Vector3 expected, double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertEquals("must be of length 3", 3, test.length);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test, expected.getArray(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Vector2 test, Vector2 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("expected should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getXY(), expected.getXY(), epsilon);
	}

	/**
	 * equality test. true if both args not null and equal within epsilon
	 * 
	 * @param msg
	 *            message
	 * @param test
	 * @param expected
	 * @param epsilon
	 */
	public static void assertEquals(String msg, Point3 test, Point3 expected,
			double epsilon) {
		Assert.assertNotNull("test should not be null (" + msg + CMLConstants.S_RBRAK, test);
		Assert.assertNotNull("ref should not be null (" + msg + CMLConstants.S_RBRAK,
				expected);
		CMLAssert.assertEquals(msg, test.getArray(), expected.getArray(), epsilon);
	}

}
