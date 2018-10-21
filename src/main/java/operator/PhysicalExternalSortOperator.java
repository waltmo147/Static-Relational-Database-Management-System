package operator;

import io.BinaryTupleReader;
import io.BinaryTupleWriter;
import io.TupleReader;
import io.TupleWriter;
import logical.operator.SortOperator;
import model.Tuple;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import util.Catalog;
import util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PhysicalExternalSortOperator extends PhysicalSortOperator {
    private List<TupleReader> buffer;
    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private BinaryTupleWriter outputBuffer;
    int blockSize;
    int index = 0;
    int preRunCount = 0;
    String finalTemp;
    TupleReader tr;

    public PhysicalExternalSortOperator(PhysicalOperator operator, PlainSelect plainSelect) {
        super(operator, plainSelect);
        init();
    }

    public PhysicalExternalSortOperator(SortOperator logSortOp, Deque<PhysicalOperator> physChildren) {
        super(logSortOp, physChildren);
        init();

    }

    public PhysicalExternalSortOperator(List<OrderByElement> order, Deque<PhysicalOperator> physChildren) {
        super(order, physChildren);
        init();
    }

    private void init() {
        this.blockSize = Catalog.getInstance().getSortBlockSize();
        buffer = new ArrayList<>(blockSize - 1);
        firstRun();
        mergeSort();
        finalTemp = getFileLocation(id, preRunCount, 0);
        tr = new BinaryTupleReader(finalTemp);
    }

    private void firstRun() {
        try {
            index = 0;
            while (true) {
                int tupleCount = blockSize
                        * ((Constants.PAGE_SIZE - 2 * Constants.INT_SIZE)
                        / (schema.size() * Constants.INT_SIZE));
                List<Tuple> tupleList = new ArrayList<>();
                for (int i = 0; i < tupleCount; ++i) {
                    Tuple tuple = physChild.getNextTuple();
                    if (tuple == null) break;
                    tupleList.add(tuple);
                }
                if (tupleList.size() == 0) {
                    break;
                }
                Collections.sort(tupleList, new TupleComparator());
                TupleWriter tupleWriter = new BinaryTupleWriter(
                        getFileLocation(id, 0, index), schema.size());
                index++;
                for (Tuple tuple : tupleList) {
                    tupleWriter.writeNextTuple(tuple);
                }
                tupleWriter.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mergeSort() {
        int indexTemp = index;

        //int preRunCount = 0;
        while (indexTemp > 1) {
            index = 0;

            for (int i = 0; i < indexTemp; i += (blockSize - 1)) {
                buffer = new ArrayList<>();
                for (int j = 0; j < blockSize - 1 && (j < indexTemp - i); ++j) {
                    buffer.add(
                            new BinaryTupleReader(getFileLocation(id, preRunCount, i + j))
                    );
                }
                outputBuffer = new BinaryTupleWriter(getFileLocation(id, preRunCount + 1, index), schema.size());
                while (buffer.size() > 0) {
                    // find the minimum tuple
                    Tuple minimum_tuple = null;
                    int pos = -1;
                    for (int j = 0; j < buffer.size(); ++j) {
                        Tuple tuple = buffer.get(j).readNextTuple();
                        if (tuple == null) {
                            buffer.remove(j);
                            j--;
                            continue;
                        }
                        if (minimum_tuple == null) {
                            minimum_tuple = tuple;
                            pos = j;
                            continue;
                        }
                        if (new TupleComparator().compare(minimum_tuple, tuple) == 1) {
                            minimum_tuple = tuple;
                            pos = j;
                        }
                    }

                    outputBuffer.writeNextTuple(minimum_tuple);
                    // reset all unused buffer page
                    for (int j = 0; j < buffer.size(); ++j) {
                        if (j == pos) {
                            continue;
                        }
                        buffer.get(j).moveBack();
                    }
                }
                outputBuffer.finish();
                index++;
            }
            indexTemp = index;
            deletePrePassExtraTemp(preRunCount);
            preRunCount += 1;
        }

    }

    private void renameTempToFinalTemp(String tempFile) throws IOException {
        // File (or directory) with old name
        File file = new File("tempFile");

        // File (or directory) with new name
        File file2 = new File(Catalog.getInstance().getTempPath() + "\\" + id);

        if (file2.exists()) {
            file2.delete();
            //throw new java.io.IOException("file exists");
        }

        // Rename file (or directory)
        boolean success = file.renameTo(file2);

        if (!success) {
            int a = 1;
            throw new java.io.IOException("rename fail");
            // File was not successfully renamed
        }
    }

    private String getFileLocation(String id, int pass, int index) {
        return Catalog.getInstance().getTempPath() + id + '_' + pass + '_' + index;
    }

    private void deletePrePassExtraTemp(int pass) {
        //System.out.println(id + '\t' + pass);
        //System.out.println(Catalog.getInstance().getTempPath());
        File[] files = new File(Catalog.getInstance().getTempPath()).listFiles();
        for (File file : files) {
            if (file.getName().contains(id + '_' + pass + '_')) {
                file.delete();
            }
        }
    }

    @Override
    public Tuple getNextTuple() {
        return tr.readNextTuple();
    }

    @Override
    public void reset() {
        tr.reset();
    }

    public List<OrderByElement> getOrder() {
        return order;
    }

    public void recordTupleReader() {
        tr.recordPosition();
    }

    public void setRecordTupleReader() {
        tr.moveToPosition();
    }
}