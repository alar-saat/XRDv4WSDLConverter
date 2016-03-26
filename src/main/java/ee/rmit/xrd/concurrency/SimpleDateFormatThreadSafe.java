package ee.rmit.xrd.concurrency;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleDateFormatThreadSafe {
    private ThreadLocal<SimpleDateFormat> sdf;

    public SimpleDateFormatThreadSafe(final String format) {
        sdf = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
                simpleDateFormat.setLenient(false);
                return simpleDateFormat;
            }
        };
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return sdf.get();
    }

    public String format(Date date) {
        return sdf.get().format(date);
    }

    public Date parse(String date) throws ParseException {
        return sdf.get().parse(date);
    }
}
