package com.sql.interpreter;

import io.BinaryTupleReader;
import junit.framework.Assert;
import model.Tuple;
import org.junit.Test;
import util.Catalog;
import util.Constants.JoinMethod;
import util.Constants.SortMethod;

import java.util.HashSet;
import java.util.Set;


/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void testBNLJ_Memory() throws Exception{
        Handler.init(new String[0]);
        Catalog.getInstance().setJoinMethod(JoinMethod.BNLJ);
        Catalog.getInstance().setJoinBlockSize(2);
        Catalog.getInstance().setSortMethod(SortMethod.IN_MEMORY);
        Handler.parseSql();
        cmpResult();
    }

    @Test
    public void testSMJ_External() throws Exception{
        Handler.init(new String[0]);
        Catalog.getInstance().setJoinMethod(JoinMethod.SMJ);
        Catalog.getInstance().setSortMethod(SortMethod.EXTERNAL);
        Catalog.getInstance().setSortBlockSize(3);
        Handler.parseSql();
        cmpResult();
    }

    @Test
    public void testSMJ_MemorySort() throws Exception{
        Handler.init(new String[0]);
        Catalog.getInstance().setJoinMethod(JoinMethod.SMJ);
        Catalog.getInstance().setSortMethod(SortMethod.IN_MEMORY);
        Catalog.getInstance().setSortBlockSize(3);
        Handler.parseSql();
        cmpResult();
    }

    @Test
    public void testTNLJ() throws Exception{
        Handler.init(new String[0]);
        Catalog.getInstance().setJoinMethod(JoinMethod.TNLJ);
        Catalog.getInstance().setSortMethod(SortMethod.IN_MEMORY);
        Handler.parseSql();
        cmpResult();
    }

    private void cmpResult() throws Exception{
        for (int index = 1; index <= 12; ++index) {
            String outfile = Catalog.getInstance().getOutputPath() + String.valueOf(index);
            String expectOutputfile = "Samples/samples/expected/" + "query" + String.valueOf(index);
            BinaryTupleReader r1 = new BinaryTupleReader(outfile);
            BinaryTupleReader r2 = new BinaryTupleReader(expectOutputfile);
            Tuple t1 = null, t2 = null;
            Set<String> outputBin = new HashSet<>();
            Set<String> expectedBin = new HashSet<>();
            while ((t1 = r1.readNextTuple()) != null) {
                outputBin.add(t1.toString());
            }
            while ((t2 = r2.readNextTuple()) != null) {
                Assert.assertTrue("Index: " + index + ' ' + t2.toString() + " lost", outputBin.contains(t2.toString()));
                expectedBin.add(t2.toString());
            }
            Assert.assertEquals("index: " + index, outputBin, expectedBin);
//            while((t1 = r1.readNextTuple())!=null && (t2=r2.readNextTuple())!=null){
//                Assert.assertEquals(t1.toString(), t2.toString());
//            }
        }

        for (int index = 13; index <= 15; ++index) {
            String outfile = Catalog.getInstance().getOutputPath() + String.valueOf(index);
            String expectOutputfile = "Samples/samples/expected/" + "query" + String.valueOf(index);
            BinaryTupleReader r1 = new BinaryTupleReader(outfile);
            BinaryTupleReader r2 = new BinaryTupleReader(expectOutputfile);

            Tuple t1 = null, t2 = null;
            while ((t1 = r1.readNextTuple()) != null && (t2 = r2.readNextTuple()) != null) {
                Assert.assertEquals("" + index, t1.toString(), t2.toString());
            }
        }
    }
}
