package ntut.edu.aiguide.crawljax.plugins.domain.TableOutput;

import java.util.List;

public interface ITableOutput<T> {
    ITableOutput<T> addRow(List<T> data);
    ITableOutput<T> addRows(List<List<T>> data);
    ITableOutput<T> writeToFile();
    long getRowCount();
}
