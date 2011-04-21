package org.xmlcml.cml.testutil;

import org.junit.Test;
import org.xmlcml.cml.element.CMLArray;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLAtomSet;

public class TestCmlAssert {

	@Test
	public void testAssertEqualsStringStringArrayCMLAtomSetTrue() {
		String message="";
		String[] expectedAtomIds=new String[]{"a1","a2"};
		CMLAtomSet atomSet=new CMLAtomSet();
		atomSet.addAtom(new CMLAtom("a1"));
		atomSet.addAtom(new CMLAtom("a2"));
		CMLAssert.assertEquals(message, expectedAtomIds,atomSet);
	}
	
	@Test (expected=AssertionError.class)
	public void testAssertEqualsStringStringArrayCMLAtomSetFail() {
		String message="";
		String[] expectedAtomIds=new String[]{"a1","a2"};
		CMLAtomSet atomSet=new CMLAtomSet();
		atomSet.addAtom(new CMLAtom("a1"));
		atomSet.addAtom(new CMLAtom("a2"));
		atomSet.addAtom(new CMLAtom("a3"));
		CMLAssert.assertEquals(message, expectedAtomIds,atomSet);
	}

	@Test (expected=AssertionError.class)
	public void testAlwaysFail() {
		CMLAssert.alwaysFail("fail");
	}

	@Test (expected=AssertionError.class)
	public void testAssertEqualsStringCMLArrayCMLArrayDoubleNull() {
		String msg="";
		CMLArray test=null;
		CMLArray expected=null;
		double epsilon=0;
		CMLAssert.assertEquals(msg, test, expected, epsilon);
	}

	@Test
	public void testAssertEqualsStringCMLArrayCMLArrayDouble() {
		String msg="";
		CMLArray test=new CMLArray(new double[]{1.0,2.0,3.0});
		CMLArray expected=new CMLArray(new double[]{1.00000000001,2.0,3.00000000001});
		double epsilon=0.0001;
		CMLAssert.assertEquals(msg, test, expected, epsilon);
	}
}
