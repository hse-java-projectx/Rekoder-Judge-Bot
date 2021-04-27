package rekoder.util;

public class UnsupportedPageFormat extends Exception {
    public UnsupportedPageFormat(String message) {
        super(message);
    }

    public UnsupportedPageFormat(Throwable t) {
        super(t);
    }
}
