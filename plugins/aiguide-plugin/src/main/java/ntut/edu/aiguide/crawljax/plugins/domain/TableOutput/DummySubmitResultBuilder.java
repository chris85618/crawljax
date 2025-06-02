package ntut.edu.aiguide.crawljax.plugins.domain.TableOutput;

import java.util.List;

public class DummySubmitResultBuilder extends SubmitResultBuilder {
    public DummySubmitResultBuilder() {
        super(null);
    }

    @Override
    public SubmitResultBuilder addTitleRow() {
        return this;
    }

    @Override
    public SubmitResultBuilder addRow(List<String> data) {
        return this;
    }

    @Override
    public SubmitResultBuilder addRows(List<List<String>> data) {
        return this;
    }

    @Override
    public SubmitResultBuilder writeToFile() {
        return this;
    }

    @Override
    public long getRowCount(){
        return 0;
    }
}
