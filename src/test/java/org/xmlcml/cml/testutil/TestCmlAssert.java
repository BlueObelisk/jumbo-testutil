/**
 *    Copyright 2011 Peter Murray-Rust et. al.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
