package ntut.edu.aiguide.crawljax.plugins.domain.TableOutput;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SubmitResultBuilder implements ITableOutput<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitResultBuilder.class);
    public class RowBuilder {
        final private SubmitResultBuilder parent;
        private String url = "";
        private String xpath = "";
        private String value = "";
        private String result = "";

        public RowBuilder(SubmitResultBuilder parent) {
            this.parent = parent;
        }

        public RowBuilder setUrl(final String url) {
            this.url = url;
            return this;
        }

        public RowBuilder setXpath(final String xpath) {
            this.xpath = xpath;
            return this;
        }

        public RowBuilder setValue(final String value) {
            this.value = value;
            return this;
        }

        public RowBuilder setResult(final String result) {
            this.result = result;
            return this;
        }

        public void build() {
            final List<String> rowData = Arrays.asList(this.url, this.xpath, this.value, this.result);
            this.parent.addRow(rowData);
            LOGGER.info("Submitting form url: {}, value: {}, and xpath: {} with result: {}", this.url, this.value, this.xpath, this.result);
        }
    }

    final private ITableOutput<String> tableOutputBuilder;
    final private List<String> titalRow = Arrays.asList("URL", "XPath", "Value", "Submission Result");

    public SubmitResultBuilder(final ITableOutput<String> tableOutputBuilder) {
        this.tableOutputBuilder = tableOutputBuilder;
    }

    public SubmitResultBuilder addTitleRow() {
        this.addRow(this.titalRow);
        return this;
    }

    @Override
    public SubmitResultBuilder addRow(List<String> data) {
        this.tableOutputBuilder.addRow(data);
        return this;
    }

    @Override
    public SubmitResultBuilder addRows(List<List<String>> data) {
        this.tableOutputBuilder.addRows(data);
        return this;
    }

    @Override
    public SubmitResultBuilder writeToFile() {
        this.tableOutputBuilder.writeToFile();
        return this;
    }

    @Override
    public long getRowCount(){
        return this.tableOutputBuilder.getRowCount();
    }

    public RowBuilder getRowBuilder() {
        return new RowBuilder(this);
    }
}
