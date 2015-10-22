package org.bbs.demo.apkparser.test;

import junit.framework.TestCase;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    public void testSuccess() {
        assertTrue(true);

        List mockedList = mock(List.class);
        mockedList.add("3");
        mockedList.clear();

        verify(mockedList).add("3");
//        verify(mockedList).add("a");
        verify(mockedList).clear();
        verify(mockedList).add("3");

        when(mockedList.get(0)).thenReturn("3");
        assertEquals("3", mockedList.get(0));
//        assertEquals("33", mockedList.get(0));
    }
}