package ntut.edu.aiguide.crawljax.plugins.adapter.TableOutput;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.IOException;
import java.io.FileWriter;
import java.util.List;
import ntut.edu.aiguide.crawljax.plugins.domain.TableOutput.ITableOutput;

public class CsvTableOutput implements ITableOutput<String> {
    private final CSVPrinter csvPrinter;
    private final FileWriter fileWriter;

    public CsvTableOutput(String filePath) {
        try {
            this.fileWriter = new FileWriter(filePath, false);
            this.csvPrinter = new CSVPrinter(this.fileWriter, CSVFormat.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException("Error while initializing CSVPrinter", e);
        }
    }

    @Override
    public ITableOutput<String> addRow(List<String> data) {
        try {
            this.csvPrinter.printRecord(data);
        } catch (IOException e) {
            throw new RuntimeException("Error while adding row to CSV", e);
        }
        return this;
    }

    @Override
    public ITableOutput<String> addRows(List<List<String>> data) throws RuntimeException {
        for (List<String> row : data) {
            addRow(row);
        }
        return this;
    }

    @Override
    public long getRowCount() {
        return csvPrinter.getRecordCount();
    }

    @Override
    public ITableOutput<String> writeToFile() {
        try {
            this.csvPrinter.flush();
            this.fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error while flushing CSV or file writer", e);
        }
        return this;
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("toString method is not supported for CsvTableOutput");
    }
}
